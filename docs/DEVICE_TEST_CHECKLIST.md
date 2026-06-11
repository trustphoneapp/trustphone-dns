# Device Test Checklist — Shield DNS

Run on physical device before Play Store submit.

## Install
```bash
./gradlew installDebug
```

## Core flows
- [ ] App opens to Dashboard
- [ ] Connect → VPN permission dialog appears
- [ ] After approve → notification shows "Shield DNS active"
- [ ] Disconnect → notification gone
- [ ] Block counter increases after browsing (open site with ads)

## Navigation
- [ ] Bottom tabs: Dashboard, Shield, Activity, More
- [ ] More → each sub-screen opens
- [ ] Back arrow returns to More
- [ ] Quick Settings tile toggles (add tile manually)

## Shield lists
- [ ] Ads / Trackers / Malware toggles persist after restart
- [ ] Update blocklists → snackbar confirmation

## Allowlist
- [ ] Save allowlist → snackbar
- [ ] Allowed domain resolves when list would block

## Settings
- [ ] Upstream DNS save
- [ ] DoH toggle
- [ ] System / Light / Dark theme
- [ ] Export JSON copies data
- [ ] Import JSON restores settings

## Apps (per-app)
- [ ] App list loads
- [ ] Rule dropdown changes mode

## Rules
- [ ] Schedule toggle + hour sliders
- [ ] Mobile-only / Wi‑Fi-only (mutually exclusive)

## Stability
- [ ] Screen off 5 min — VPN stays connected
- [ ] Reboot with "start on boot" — reconnects
- [ ] No crash on rotate (if rotation enabled)

## Battery
- [ ] 30 min active — acceptable drain vs idle
