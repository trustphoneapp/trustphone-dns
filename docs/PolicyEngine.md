# Policy Engine Rules Specification

The `PolicyEngine` enforces hierarchical policy control over DNS resolutions based on selected user profiles, risk tolerances, and the requesting application's identity.

## Rules Evaluation Order
To ensure safety and respect user overrides, domains are processed sequentially:
1. **Security Guard**: Blocks immediately if debugger presence or signature mismatch is detected.
2. **App-specific Firewall Override**: If the user set a per-app rule to `BYPASS` or `BLOCK` (via settings/firewall panel), it is honored immediately.
3. **Custom Allowlist**: Allowed immediately.
4. **Custom Deny-list**: Blocked immediately.
5. **Policy Engine Rules**: Evaluates active profile restrictions, app-specific category overrides, and risk threshold limits.
6. **Consolidated Blocklists**: Fallback check against the loaded Bloom filter database.

## Profile Specifications

### 1. Child Profile
* **Purpose**: Absolute safety.
* **Blocks**: Ads, Trackers, Telemetry, Malware, Phishing, Scam, Cryptoscam, Adult content, Gambling.
* **App Override**: Blocks all categories for kids learning/gaming applications.

### 2. Teen Profile
* **Purpose**: Age-appropriate safety.
* **Blocks**: Adult, Gambling, Scam, Malware, Phishing, Cryptoscam. Allows trackers/ads.

### 3. Family Profile
* **Purpose**: Standard household defense.
* **Blocks**: Adult content, Gambling, Scam, Malware, Phishing, Cryptoscam.

### 4. Work Profile
* **Purpose**: Productivity.
* **Blocks**: Malware, Phishing, Scam, Cryptoscam, Adult, Gambling.

### 5. Travel Profile
* **Purpose**: Bandwidth conservation.
* **Blocks**: Ads, Trackers, Telemetry, Malware, Phishing, Scam, Cryptoscam.

### 6. Default Profile
* **Purpose**: User preference.
* **Blocks**: Follows the selected blocklist toggles (Ads, Trackers, Malware) checked in the application settings.

## App-Specific Filtering
The engine inspects the package name of the querying app to apply context-aware blocks:
* **Browsers**: Forces adult and gambling blocklists strictly when family-focused profiles (Child/Teen/Family) are active.
* **Kid Apps**: Enforces zero-tracking policies by stripping all ads and telemetry analytics domains regardless of profile settings.
