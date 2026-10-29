// Renders the Google Play images in play/en-US/images/ from the HTML templates, with a headless Chrome or Edge:
//   node branding/render.mjs
// BROWSER can point to the browser's executable when it isn't in one of the usual places.
import { execFileSync } from "node:child_process";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { deflateSync, inflateSync, crc32 } from "node:zlib";

const root = dirname(fileURLToPath(import.meta.url));
const templates = join(root, "templates");
const images = join(root, "play", "en-US", "images");

const browser = [
	process.env.BROWSER,
	"C:/Program Files/Google/Chrome/Application/chrome.exe",
	"C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
	"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
	"/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
	"/usr/bin/google-chrome",
	"/usr/bin/chromium",
	"/usr/bin/microsoft-edge",
].find((path) => path && existsSync(path));

if (!browser) {
	console.error("No Chrome or Edge found. Set BROWSER to its executable.");
	process.exit(1);
}

// Chrome writes opaque captures as 24-bit RGB. This rewrites one as 32-bit RGBA, fully opaque, by undoing the
// scanline filters and adding an alpha byte to every pixel.
function toRgba(path) {
	const png = readFileSync(path);
	const chunks = [];
	for (let at = 8; at < png.length;) {
		const length = png.readUInt32BE(at);
		chunks.push({ type: png.toString("latin1", at + 4, at + 8), data: png.subarray(at + 8, at + 8 + length) });
		at += 12 + length;
	}
	const header = chunks.find((chunk) => chunk.type === "IHDR").data;
	const [width, height, depth, colorType, interlace] =
		[header.readUInt32BE(0), header.readUInt32BE(4), header[8], header[9], header[12]];
	if (colorType === 6) return;
	if (colorType !== 2 || depth !== 8 || interlace !== 0) throw new Error(`${path}: unexpected PNG format`);

	const raw = inflateSync(Buffer.concat(chunks.filter((chunk) => chunk.type === "IDAT").map((chunk) => chunk.data)));
	const stride = width * 3;
	const rows = Buffer.alloc(height * (1 + width * 4));
	let previous = Buffer.alloc(stride);
	for (let y = 0; y < height; y++) {
		const filter = raw[y * (stride + 1)];
		const row = Buffer.from(raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1)));
		for (let x = 0; x < stride; x++) {
			const left = x >= 3 ? row[x - 3] : 0;
			const up = previous[x];
			const upLeft = x >= 3 ? previous[x - 3] : 0;
			const paeth = () => {
				const p = left + up - upLeft;
				const [a, b, c] = [Math.abs(p - left), Math.abs(p - up), Math.abs(p - upLeft)];
				return a <= b && a <= c ? left : b <= c ? up : upLeft;
			};
			row[x] += [0, left, up, (left + up) >> 1, filter === 4 ? paeth() : 0][filter];
		}
		const out = y * (1 + width * 4);
		for (let x = 0; x < width; x++) {
			row.copy(rows, out + 1 + x * 4, x * 3, x * 3 + 3);
			rows[out + 1 + x * 4 + 3] = 255;
		}
		previous = row;
	}

	const chunk = (type, data) => {
		const length = Buffer.alloc(4);
		length.writeUInt32BE(data.length);
		const crc = Buffer.alloc(4);
		crc.writeUInt32BE(crc32(Buffer.concat([Buffer.from(type, "latin1"), data])));
		return Buffer.concat([length, Buffer.from(type, "latin1"), data, crc]);
	};
	const rgbaHeader = Buffer.from(header);
	rgbaHeader[9] = 6;
	writeFileSync(path, Buffer.concat([
		png.subarray(0, 8),
		chunk("IHDR", rgbaHeader),
		chunk("IDAT", deflateSync(rows, { level: 9 })),
		chunk("IEND", Buffer.alloc(0)),
	]));
}

const jobs = [
	// Play asks for the icon as a 32-bit PNG.
	{ template: "icon.html", out: "icon.png", width: 512, height: 512, alpha: true },
	{ template: "feature-graphic.html", out: "featureGraphic.png", width: 1024, height: 500 },
	...Array.from({ length: 8 }, (_, i) => ({
		template: `screenshot.html?shot=${i + 1}`,
		out: `phoneScreenshots/${i + 1}.png`,
		width: 1080,
		height: 1920,
	})),
];

mkdirSync(join(images, "phoneScreenshots"), { recursive: true });

for (const job of jobs) {
	const [file, query = ""] = job.template.split("?");
	const url = pathToFileURL(join(templates, file)).href + (query && `?${query}`);
	const out = join(images, job.out);
	execFileSync(browser, [
		"--headless=new",
		"--disable-gpu",
		"--hide-scrollbars",
		"--force-device-scale-factor=1",
		// Time for the web font and the screenshots to load before the capture.
		"--virtual-time-budget=5000",
		`--window-size=${job.width},${job.height}`,
		`--screenshot=${out}`,
		url,
	], { stdio: "ignore" });
	if (job.alpha) toRgba(out);
	console.log(`${job.out}  ${job.width} x ${job.height}`);
}
