# Play Store Listing — TrustPhone DNS

## App name
**TrustPhone DNS — DNS Firewall**

## Short description (80 chars max)
```
Block ads, trackers & malware at DNS level. On-device. Private by design.
```

## Full description

TrustPhone DNS protects your Android phone by blocking unwanted domains before they load — using a local DNS firewall that never uploads your browsing history.

**One tap to protect.** Enable TrustPhone DNS and approve the local VPN once. Ads, trackers, and malware domains are blocked system-wide in browsers and many apps.

**Private by design.** All blocking happens on your device. DNS query logs stay in local storage on your phone. We do not sell your data or require an account.

**Features (Free):**
- Block ads, trackers, and malware via DNS
- 200,000+ domains from open-source blocklists (auto-updated)
- DNS-over-HTTPS to Cloudflare with UDP fallback
- Allowlist for sites that break
- Searchable activity log
- Quick Settings tile
- Dark theme

**Features (Pro):**
- Per-app bypass and block rules
- Scheduled protection (time windows)
- Wi‑Fi / mobile-only rules
- Custom blocklist URL
- Export/import settings

**What TrustPhone DNS is:** A local DNS filter using Android's VPN API. It routes only DNS traffic to your chosen resolver — not your full internet connection. It does not hide your IP like a traditional VPN.

**What TrustPhone DNS is not:** It cannot block every in-app ad (some apps bypass DNS). For most users it significantly reduces ads and tracking. Set Samsung **Private DNS → Off** for best results.

**Optional telemetry:** Crash and usage diagnostics are off by default. Enable only in Privacy Center if you want to help improve the app.

## Category
Tools

## Tags
dns, ad blocker, privacy, tracker blocker, malware, security, firewall

## Privacy policy URL
`https://trustphoneapp.github.io/trustphone-dns/privacy.html`

## Data safety (Play Console answers)

| Field | Value |
|-------|-------|
| Collects data | Yes — **only when user enables "Share anonymous telemetry"** |
| Default collection | **No data collected** |
| Data types (opt-in) | Crash logs, App interactions (aggregate) |
| Data shared | No |
| Encrypted in transit | Yes (DoH) |
| Data deletion | User clears app data in Android Settings |
| Purpose | Analytics, Crash reporting (opt-in) |

## VPN declaration
This app uses a local VPN solely to filter DNS queries to the user's chosen resolver. No HTTP/HTTPS payload inspection. No remote logging of DNS history. User must approve via Android VPN consent dialog.

## Foreground service (special use)
Subtype: `dns_blocking`. Required to maintain active DNS filtering while the app runs in the background. User sees a persistent notification with Disconnect action.

## Content rating
Everyone / PEGI 3

## Screenshots (capture on device)
1. Dashboard — protection active, shield visual
2. Shield Lists — category toggles + domain counts
3. Activity — blocked domains log
4. Privacy Center — telemetry off, export CSV
5. More — feature grid
6. Notification — "TrustPhone DNS — Protected"

## In-app product IDs (must match Play Console)
- `shield_dns_pro_lifetime`
- `shield_dns_pro_monthly`
- `shield_dns_pro_yearly`
