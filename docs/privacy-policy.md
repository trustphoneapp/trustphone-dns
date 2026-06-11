# TrustPhone DNS Privacy Policy

**Last updated:** June 11, 2026  
**App:** TrustPhone DNS (`com.trustphone.dns`)

## Summary

TrustPhone DNS is a local DNS blocker. It uses Android's VPN API to process DNS
queries on your device. Your DNS queries and browsing history are **never** sent
to our servers or sold, and we do not require an account. The only data that can
leave your device is **optional, off-by-default** crash and usage diagnostics
that you control (see "Optional diagnostics").

## What TrustPhone DNS does

- Intercepts DNS queries on your device via a local VPN tunnel
- Matches domain names against on-device blocklists (ads, trackers, malware)
- Forwards allowed queries to your chosen DNS resolver (e.g. Cloudflare)
- Stores blocked-query logs and settings **only on your device**

## What we do NOT do

- We do not inspect HTTPS page content
- We do not log your browsing history to the cloud
- We do not sell data to advertisers
- We do not require an account
- We do not collect analytics or crash data unless you explicitly opt in

## Data stored on your device

| Data | Purpose |
|------|---------|
| Settings (allowlist, DNS choice) | App functionality |
| Blocked/allowed query log | Show activity in the app |
| App rules (per-app bypass) | Firewall features |

This data is held in the app's private, sandboxed storage. It is excluded from
Android cloud backup and is not transferred off-device by us. You can clear app
data at any time in Android Settings.

## Optional diagnostics (off by default)

TrustPhone DNS includes Google Firebase **Crashlytics** (crash reports) and
**Analytics** (aggregate, non-advertising usage events). **Both are disabled by
default.** They are only activated if you turn on **"Share anonymous telemetry"**
in Privacy Center, and you can turn them off again at any time. When enabled:

- Advertising ID (AAID) and SSAID collection remain disabled.
- We do not send your DNS queries, resolved domains, or query logs to analytics.
- Diagnostics are limited to crash stack traces and aggregate counts used to
  improve stability and resolver health.

We also use Firebase **Remote Config** to deliver DNS resolver and blocklist
configuration. Remote Config does not transmit app-supplied personal data;
as with any network request, Google may process your IP address to serve the
configuration response.

## Permissions

| Permission | Why |
|------------|-----|
| VPN | Required to filter DNS system-wide |
| Internet | Forward DNS to upstream resolver, update blocklists |
| Notifications | Show when protection is active |
| Boot completed | Optional auto-start |
| Installed apps (queries) | Per-app rules only; the app list stays on-device |

## Third-party services

- **Upstream DNS** (e.g. Cloudflare 1.1.1.1): receives DNS queries you allow through
- **Blocklist sources** (download): public domain lists from the internet
- **Google Firebase** (Crashlytics, Analytics, Remote Config): see "Optional
  diagnostics" — collection is opt-in except Remote Config delivery

## Children's privacy

TrustPhone DNS is not directed at children under 13. The Family/Child profiles
are tools for parents and provide best-effort category filtering, not a guarantee
that all unsafe content is blocked.

## Changes

We may update this policy. The date above will change when we do.

## Contact

For privacy questions: [your-email@example.com]
