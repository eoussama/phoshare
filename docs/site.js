// The landing page's motion and its two toys (the trailer and double-tap to like). It's all decoration: without this
// file, or with reduced motion, the page shows the same content, just still.
const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
const root = document.documentElement;
const heartSvg = '<svg viewBox="0 0 24 24"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>';

// The headline rises word by word.
const h1 = document.querySelector('.hero h1');
h1.innerHTML = h1.textContent.trim().split(/\s+/)
	.map((word, i) => `<span class="w" style="--i: ${i}"><span>${word}</span></span>`).join(' ');

// The terminal types itself out, one line after the other.
const code = document.querySelector('.terminal code');
code.innerHTML = code.innerHTML.split('\n').map((line) => `<span class="ln">${line || ' '}</span>`).join('');
let typed = 0;
for (const line of code.querySelectorAll('.ln')) {
	const n = line.textContent.length;
	line.style.setProperty('--n', n);
	line.style.setProperty('--d', `${typed.toFixed(2)}s`);
	typed += line.textContent.trim() ? Math.min(.5, n * .012) + .08 : .05;
}

// Sections play their entrance once, when they scroll into view.
const reveal = new IntersectionObserver((entries) => {
	for (const entry of entries) {
		if (!entry.isIntersecting) continue;
		entry.target.classList.add('in');
		reveal.unobserve(entry.target);
	}
}, { threshold: .2 });
document.querySelectorAll('.stats, .section-head, .feature, .cards, .build').forEach((el) => reveal.observe(el));

// The hero's layers follow the pointer, each by its own depth.
const art = document.querySelector('.hero-art');
if (!reduce && matchMedia('(pointer: fine)').matches) {
	addEventListener('pointermove', (e) => {
		art.style.setProperty('--mx', (e.clientX / innerWidth - .5).toFixed(3));
		art.style.setProperty('--my', (e.clientY / innerHeight - .5).toFixed(3));
	}, { passive: true });
}

// Scroll position: the nav's progress bar and the dark band opening up.
const band = document.querySelector('.dark-band');
let queued = false;
function onScroll() {
	queued = false;
	const max = root.scrollHeight - innerHeight;
	root.style.setProperty('--scroll', max > 0 ? (scrollY / max).toFixed(4) : 0);
	const top = band.getBoundingClientRect().top;
	band.style.setProperty('--open', Math.min(1, Math.max(0, (innerHeight - top) / (innerHeight * .7))).toFixed(3));
}
addEventListener('scroll', () => {
	if (!queued) { queued = true; requestAnimationFrame(onScroll); }
}, { passive: true });
onScroll();

// Screenshots lean toward the mouse, and a double-tap likes them.
function like(phone, e) {
	const box = phone.getBoundingClientRect();
	const x = e.clientX - box.left, y = e.clientY - box.top;
	const hearts = [Object.assign(document.createElement('span'), { className: 'like', innerHTML: heartSvg })];
	for (let i = 0; i < 7; i++) {
		const mini = Object.assign(document.createElement('span'), { className: 'like mini', innerHTML: heartSvg });
		const angle = (i / 7) * Math.PI * 2 + Math.random() * .6, dist = 50 + Math.random() * 45;
		mini.style.setProperty('--dx', `${Math.cos(angle) * dist}px`);
		mini.style.setProperty('--dy', `${Math.sin(angle) * dist - 30}px`);
		hearts.push(mini);
	}
	for (const heart of hearts) {
		heart.style.left = `${x}px`;
		heart.style.top = `${y}px`;
		heart.addEventListener('animationend', () => heart.remove(), { once: true });
		phone.append(heart);
	}
	getSelection()?.removeAllRanges();
}
for (const phone of document.querySelectorAll('.shots .phone')) {
	phone.addEventListener('dblclick', (e) => like(phone, e));
	if (reduce) continue;
	phone.addEventListener('pointermove', (e) => {
		if (e.pointerType !== 'mouse') return;
		const box = phone.getBoundingClientRect();
		const x = (e.clientX - box.left) / box.width - .5, y = (e.clientY - box.top) / box.height - .5;
		phone.style.transform = `perspective(900px) rotateY(${x * 16}deg) rotateX(${-y * 16}deg) translateY(-8px)`;
	});
	phone.addEventListener('pointerleave', () => (phone.style.transform = ''));
}

// The trailer, in a dialog.
const dialog = document.querySelector('.trailer');
const video = dialog.querySelector('video');
document.querySelectorAll('[data-trailer]').forEach((button) => button.addEventListener('click', () => {
	dialog.showModal();
	video.currentTime = 0;
	video.play().catch(() => {});
}));
dialog.addEventListener('close', () => video.pause());
dialog.addEventListener('click', (e) => { if (e.target === dialog) dialog.close(); });
