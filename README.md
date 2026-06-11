# TrustPhone DNS

On-device DNS firewall for Android. Blocks ads, trackers, and malware at the DNS layer using a local VPN — your query history never leaves your phone.

**Package:** `com.trustphone.dns` · **Version:** 3.0.1

## Build

```bash
./gradlew assembleDebug
./gradlew test
./gradlew bundleRelease   # requires keystore.properties
```

## Features

- Local VPN DNS filter (DNS-only `/32` route pattern)
- 200K+ domains from open-source blocklists (Steven Black, URLhaus, Phishing Army)
- Bloom filter + SQLite double-check pipeline
- DNS-over-HTTPS (Cloudflare) with UDP fallback
- Per-app rules, schedules, allowlist (Pro)
- Activity log, Quick Settings tile, export/import settings

## Docs

| Doc | Purpose |
|-----|---------|
| [Play Store submission](docs/PLAY_STORE_SUBMISSION.md) | Step-by-step publish guide |
| [Store listing copy](docs/PLAY_STORE_LISTING.md) | Descriptions + Data safety answers |
| [Privacy policy](docs/privacy-policy.md) | Legal text |
| [Website](docs/index.html) | Marketing site (served via GitHub Pages `/docs`) |

## Privacy

DNS queries are processed on-device. Telemetry (Firebase Analytics/Crashlytics) is **off by default** — opt in via Privacy Center.

## License

Proprietary — © TrustPhone DNS
