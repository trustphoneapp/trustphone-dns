# Launch Status — TrustPhone DNS v3.0.1

**Updated:** June 11, 2026

## Build artifacts

| Artifact | Path | Status |
|----------|------|--------|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` | Ready |
| Release AAB | `app/build/outputs/bundle/release/app-release.aab` | Ready (upload keystore signed) |
| Unit tests | `./gradlew test` | Pass |

## Package

- **Application ID:** `com.trustphone.dns`
- **Version:** 3.0.1 (versionCode 4)

## Play Store readiness

| Task | Status |
|------|--------|
| Upload keystore | Done — `upload-keystore.jks` (gitignored) |
| Signed release AAB | Done |
| Privacy policy (HTML) | Done — `website/privacy.html` |
| Play listing copy | Done — `docs/PLAY_STORE_LISTING.md` |
| Submission guide | Done — `docs/PLAY_STORE_SUBMISSION.md` |
| Firebase consent (opt-in) | Done |
| Stats counter (blocks only) | Done |
| **Privacy policy hosted URL** | **You must deploy `website/`** |
| **Play Console account ($25)** | **You must create** |
| Screenshots (6) | **You must capture** |
| In-app products in Console | **You must create** (IDs in listing doc) |
| Internal testing (14 days) | Recommended before production |

## Next steps (your actions)

1. Pay $25 → [Play Console signup](https://play.google.com/console/signup)
2. Host `website/` → get HTTPS privacy URL (GitHub Pages / Netlify)
3. Follow **`docs/PLAY_STORE_SUBMISSION.md`** step by step
4. Upload `app-release.aab` to Internal testing
5. Create Pro SKUs: `shield_dns_pro_lifetime`, `_monthly`, `_yearly`
