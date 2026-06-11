# TrustPhone DNS — Feature Audit

Last updated: 2026-06-10

## Summary

| Area | UI | Enforced in DNS path | Status |
|------|----|--------------------|--------|
| Connect / Disconnect VPN | Yes | Yes | Working |
| Category blocklists | Yes | Yes (Bloom + SQLite) | Working |
| Remote list updates | Yes | Yes (WorkManager) | Working |
| Policy profiles | Yes | Yes (PolicyEngine) | Working |
| Schedule (Pro) | Yes | Yes (ScheduleManager in handler) | Fixed |
| Wi‑Fi / mobile only (Pro) | Yes | Yes (NetworkConditionWatcher) | Fixed |
| Per-app rules (Pro) | Yes | Yes | Working; UI gated |
| Allowlist / custom deny | Yes | Yes (suffix match) | Fixed |
| Threat / reputation scoring | Yes | Yes | Working |
| Resolver health / DoH | Yes | Partial (metrics + fallback) | Working |
| Privacy CSV export | Yes | N/A (local export) | Fixed |
| Billing / Pro | Yes | ViewModel gates | Working |
| Notifications | Yes | On connect | Working |

## Screen-by-screen

### Home
- **Connect**: Requests VPN via `VpnService.prepare`, starts `DnsBlockerService`.
- **Disconnect**: Stops service via notification action or UI.
- **Stats**: Blocked count from Room query log.

### Lists
- Toggle categories → `BlocklistRepository` + `PolicyEngine`.
- **Update lists**: `BlocklistUpdateWorker` downloads Steven Black, URLhaus, Phishing Army.

### Apps (Pro)
- Lists installed apps via launcher `<queries>`.
- Per-app bypass/block only when `isPro`; button disabled otherwise.

### Policy Center
- Profiles: Child, Teen, Family, Work, Travel.
- Risk threshold and category overrides flow to `PolicyEngine.evaluate`.

### Privacy Center
- Telemetry toggle (opt-in).
- **Export activity log**: Share CSV via system intent.

### More / Advanced
- Resolver selection, engine stats, threat center navigation.

## Known limitations

1. **Samsung Private DNS** must be Off — system DoT bypasses app VPN for DNS.
2. **Play Store** — needs release keystore + hosted privacy policy URL.
3. **Branding** — strings mix "Shield DNS" and "TrustPhone DNS".
4. **QUERY_ALL_PACKAGES** removed; only launcher-visible apps listed.

## Website

Static marketing site: `website/index.html` — open locally or host on GitHub Pages.
