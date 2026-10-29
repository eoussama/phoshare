# Google Play listing

What to enter in the Play Console for `es.ouss.phoshare`, section by section. The text and images live next to this
file in the layout `fastlane supply` reads (`en-US/`), so they can be pasted by hand or uploaded with
`fastlane supply --metadata_path branding/play`.

## Store listing (Grow › Store presence › Main store listing)

| Field | Value | Limit |
| --- | --- | --- |
| App name | [en-US/title.txt](en-US/title.txt) | 30 |
| Short description | [en-US/short_description.txt](en-US/short_description.txt) | 80 |
| Full description | [en-US/full_description.txt](en-US/full_description.txt) | 4,000 |
| App icon | [en-US/images/icon.png](en-US/images/icon.png), 512 x 512, 32-bit PNG | |
| Feature graphic | [en-US/images/featureGraphic.png](en-US/images/featureGraphic.png), 1024 x 500 | |
| Phone screenshots | [en-US/images/phoneScreenshots/](en-US/images/phoneScreenshots/), eight at 1080 x 1920 | 2 to 8 |

Tablet screenshots are optional, and Play only needs them to feature the app for tablets.

## Store settings (Grow › Store presence › Store settings)

| Field | Value |
| --- | --- |
| App or game | App |
| Category | Photography |
| Tags | Photography, Social, Photo sharing (pick the closest Play offers) |
| Email | Required and shown publicly: use an address meant for app support |
| Website | https://phoshare.ouss.es |

## Release notes

[en-US/changelogs/1.txt](en-US/changelogs/1.txt) is for `versionCode` 1, which is how fastlane matches them. Add one
file per new `versionCode`, at most 500 characters each.

## App content (Policy › App content)

**Privacy policy:** https://phoshare.ouss.es/private, rendered from [PRIVACY.md](../../PRIVACY.md) by the Pages workflow.

**Ads:** the app contains no ads.

**App access:** all functionality is available without special access. Reviewers make their own account on the login
screen. Accounts are local, so there are no test credentials to give.

**Content rating (IARC questionnaire):** category *Utility, Productivity, Communication or Other*. Answer no to
violence, sexuality, language, controlled substances, gambling and purchases. For the questions about users
interacting or sharing content, the answer is no: every account is on the same device, nothing is sent to other
people through the app, and photos only leave through Android's share sheet into an app the user picks. Expect an
Everyone / PEGI 3 rating.

**Target audience:** 13 and over. Leave out the under-13 groups so the Families policy doesn't apply; the privacy
policy says the app isn't directed at children.

**News app, government app, financial features, health:** no to all.

**Data safety:**

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | No |
| Is all of the user data collected by your app encrypted in transit? | Not asked once the answer above is no |
| Do you provide a way for users to request that their data is deleted? | Not asked; in the app, deleting an account deletes everything it posted |

Data that stays on the device isn't "collected" in Play's terms, and the app has no network access at all.

### One thing to decide before submitting

The manifest has `android:allowBackup="true"` and no backup rules, so Android's Auto Backup copies the database and
the photos into the user's Google account backup when the user has device backup on. Play doesn't count that as data
the developer collects, and the privacy policy says it can happen. But the app's terms of use, the landing page and
the listing all say the data never leaves the phone and is gone for good after an uninstall. Either:

- set `android:allowBackup="false"` (and `android:dataExtractionRules` with nothing included, for Android 12 and
  later), which makes those sentences true as written, or
- keep backup and reword the terms of use and the "gone for good" lines.

Whichever it is, update [PRIVACY.md](../../PRIVACY.md) to match.

## Regenerating the images

The images come from the HTML in [../templates/](../templates/), the screenshots in [docs/showcase/](../../docs/showcase/)
and the app's logo. After changing any of them:

```sh
node branding/render.mjs
```

It needs Node 22 or later and Chrome or Edge (`BROWSER` can point to either). The screenshot captions are in
[../templates/screenshot.html](../templates/screenshot.html).
