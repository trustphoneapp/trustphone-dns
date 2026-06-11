# Client Privacy & Compliance Guidelines

TrustPhone DNS is built on a "Privacy First" foundation. No raw domain query logs or personal browsing habits are ever uploaded or transmitted off-device.

## On-Device Storage Rules
* **Room SQLite Storage**: All resolved and blocked domain logs remain strictly inside the local sandboxed database directory (`/data/data/com.yashwanthsurabhi.shielddns/databases/`).
* **Retention Policy**: Query logs are trimmed automatically to a maximum of 500 recent queries to conserve local disk space and prevent forensic retrieval of long-term history.
* **No Remote Aggregation**: We do not maintain, upload, or sell logs of domains visited by users.

## CCPA & GDPR Compliance
* **Consent Control**: User-guided telemetry share toggle (`SettingsStore.SHARE_TELEMETRY`).
* **Anonymized Event Tracking**: If telemetry is enabled, only aggregate, non-identifiable counts (e.g. `dns_query_blocked_count_daily`, `doh_fallback_used`) are sent.
* **Zero PII**: IP addresses, package identifiers (other than aggregate flags), and query domains are stripped from all outbound telemetry payloads.

## VPN Privacy Policy Statement
The local VPN service operates solely to redirect DNS packets to the local matching loopback address and the user's selected secure DoH endpoints. It does not inspect, modify, or cache general HTTP/HTTPS payload traffic.
