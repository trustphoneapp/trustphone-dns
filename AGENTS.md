# Shield DNS — Agent Rules

## Build
- Run `./gradlew assembleDebug` after major changes.
- Run `./gradlew test` after logic changes in `filter/`, `vpn/`, `dns/`.
- Fix compile errors before finishing a task.

## Code
- Keep packages modular: `ui`, `vpn`, `filter`, `dns`, `firewall`, `rules`, `data`, `billing`.
- No real AdMob or Billing product IDs — use stubs and placeholders.
- No copyrighted assets; use Material icons and Compose graphics.
- Comments only for non-obvious DNS/VPN packet logic.

## Privacy
- Query logs stay on-device (Room). Never upload blocked domains.
- Privacy policy must state: local VPN for DNS only, no traffic inspection.

## Version scope
- v3.0.0: full DNS blocker + per-app rules + schedules + DoH + quick tile.
