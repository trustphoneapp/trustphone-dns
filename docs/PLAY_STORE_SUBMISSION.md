# Play Store Submission Guide — TrustPhone DNS

**Package:** `com.trustphone.dns`  
**Version:** 3.0.1 (versionCode 4)  
**Release AAB:** `app/build/outputs/bundle/release/app-release.aab`  
**Signed with:** `upload-keystore.jks` (Play App Signing recommended)

---

## Before you start

| Requirement | Status | Action |
|-------------|--------|--------|
| Google Play Developer account ($25 one-time) | You | [play.google.com/console/signup](https://play.google.com/console/signup) |
| Signed release AAB | Ready | Rebuild after code changes: `./gradlew bundleRelease` |
| Upload keystore | Ready | `upload-keystore.jks` — **back up securely; never commit** |
| Privacy policy URL (public HTTPS) | Pending | Host `website/` (see Step 2) |
| 2+ screenshots (phone) | Pending | Capture from Samsung device |
| Feature graphic (1024×500) | Optional | Can add later for production |

---

## Step 1 — Create the app in Play Console

1. Open [Google Play Console](https://play.google.com/console)
2. **Create app**
   - App name: **TrustPhone DNS**
   - Default language: English (United States)
   - App or game: **App**
   - Free or paid: **Free** (Pro is in-app purchase)
3. Accept declarations (Play policies, US export laws, etc.)

---

## Step 2 — Host the privacy policy

Play requires a **public HTTPS URL**. Options:

### Option A — GitHub Pages (free)

```bash
cd ~/Projects/shield-dns
git init
git add website/ docs/privacy-policy.md .gitignore
git commit -m "Add TrustPhone DNS marketing site and privacy policy"
# Create repo on GitHub, push, enable Pages → /website folder
# URL: https://YOUR_USERNAME.github.io/shield-dns/privacy.html
```

### Option B — Netlify / Vercel

Drag the `website/` folder to [netlify.com/drop](https://app.netlify.com/drop)  
URL will be like `https://random-name.netlify.app/privacy.html`

### Option C — Your own domain

Point `trustphone.dns` DNS to any static host; upload `website/` contents.

**Paste this URL in Play Console → App content → Privacy policy**

---

## Step 3 — Upload the release bundle

```bash
cd ~/Projects/shield-dns
./gradlew bundleRelease
```

Upload: `app/build/outputs/bundle/release/app-release.aab`

**Play Console path:** Release → Testing → Internal testing → Create new release → Upload AAB

> Use **Internal testing** first (up to 100 testers, no review wait). Promote to Closed → Open → Production after 14 days of real-device testing.

**Enable Play App Signing** when prompted — Google holds the app signing key; you keep the upload key (`upload-keystore.jks`).

---

## Step 4 — Store listing

Copy from `docs/PLAY_STORE_LISTING.md` or use:

**Short description (80 chars):**
```
Block ads, trackers & malware at DNS level. On-device. Private by design.
```

**Full description:** See `PLAY_STORE_LISTING.md`

**Category:** Tools  
**Email:** your support email (required)  
**Privacy policy URL:** your hosted `privacy.html` URL

### Graphics needed

| Asset | Size | Notes |
|-------|------|-------|
| Phone screenshots | 2–8 | 1080×2340 or similar; Dashboard, Lists, Activity |
| App icon | 512×512 | Already in project (`ic_launcher`) |
| Feature graphic | 1024×500 | Optional for v1 |

---

## Step 5 — App content declarations

### Data safety form

Answer honestly based on current code:

| Question | Answer |
|----------|--------|
| Does your app collect or share user data? | **Yes** (only if user opts into telemetry) |
| Is all data encrypted in transit? | **Yes** (DoH when enabled) |
| Can users request data deletion? | **Yes** — Clear app data in Android Settings |
| Data types collected (if telemetry ON) | Crash logs, App interactions (aggregate) |
| Data types collected (default) | **None** — telemetry off by default |
| Is data shared with third parties? | **No** (Firebase only when user opts in) |
| Purpose | Analytics, Crash reporting (opt-in only) |

### VPN declaration

- **Uses VPN:** Yes
- **Purpose:** Local DNS filtering only — intercepts DNS queries to block ads, trackers, and malware domains
- **Does NOT:** Route general internet traffic, hide IP, or inspect HTTP payloads
- **User consent:** Android `VpnService.prepare()` dialog before first connect

### Foreground service (Special use)

- **Type:** `specialUse` / subtype `dns_blocking`
- **Justification:** Persistent DNS firewall requires an ongoing foreground service while protection is active
- **Demo video:** Record 30s screen capture: open app → Connect → show notification → browse → show blocked activity

### Ads

- **Contains ads:** No

### Target audience

- **Age:** 13+ (or Everyone if you remove family marketing emphasis)
- **Not designed for children**

### Content rating

Complete the IARC questionnaire:
- Violence: None
- Sexual content: None (app blocks some adult domains but doesn't display content)
- Result: likely **Everyone** or **PEGI 3**

---

## Step 6 — In-app products (Pro)

Create in Play Console → Monetize → Products → In-app products:

| Product ID | Type | Suggested price |
|------------|------|-----------------|
| `shield_dns_pro_lifetime` | One-time | $4.99 |
| `shield_dns_pro_monthly` | Subscription | $0.99/mo |
| `shield_dns_pro_yearly` | Subscription | $7.99/yr |

These IDs must match `BillingManager.kt` constants exactly.

Activate products and add license testers (your Gmail) under **Setup → License testing**.

---

## Step 7 — Pre-launch report

1. Upload AAB to **Internal testing**
2. Add yourself as tester (email list or opt-in link)
3. Install from Play Store link on your Samsung
4. Run `docs/DEVICE_TEST_CHECKLIST.md`
5. Fix any crashes before promoting

---

## Step 8 — Submit for review

**Internal testing** → no Google review (instant)  
**Production** → 1–7 day review

Checklist before production submit:
- [ ] Privacy policy URL live and matches app behavior
- [ ] Data safety form matches Firebase opt-in behavior
- [ ] VPN + Special use FGS declarations complete
- [ ] Screenshots uploaded
- [ ] Pro SKUs created and tested
- [ ] Tested on Samsung with Private DNS Off
- [ ] No false claims in store listing (no "SafeSearch", no "encrypted database")

---

## Quick reference

| Item | Value |
|------|-------|
| Application ID | `com.trustphone.dns` |
| Version code | 4 |
| Version name | 3.0.1 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| AAB path | `app/build/outputs/bundle/release/app-release.aab` |
| Privacy page | `website/privacy.html` |
| Keystore | `upload-keystore.jks` (gitignored) |

---

## What I cannot do for you

- Create your Play Developer account (requires your Google account + $25)
- Click through Play Console forms
- Host the privacy policy on your domain (you choose GitHub Pages / Netlify / custom)
- Record the FGS demo video (you screen-record on phone)

Everything else — AAB, privacy policy, listing copy, SKU IDs, declarations — is ready in this repo.
