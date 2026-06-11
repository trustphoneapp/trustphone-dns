# Platform Security & Environment Integrity

This document outlines the multi-layered security controls implemented to protect TrustPhone DNS against tampering, runtime analysis, and credential extraction.

## Environment Security Checks (`SecurityGuard`)

### 1. Root Detection
The application checks for signs of root access to protect the local sandboxed database and prevent interception of secured DNS traffic:
* Matches common `su` binary locations (e.g. `/sbin/su`, `/system/xbin/su`).
* Verifies build tag signatures (`test-keys` indicating custom or modified ROMs).

### 2. Emulator Verification
To block sandboxed automated malware analysis runs, the engine checks hardware signatures:
* Identifies known emulator properties (`Build.FINGERPRINT`, `Build.MODEL`, `Build.HARDWARE`).
* Flags `goldfish` and `ranchu` virtualization drivers.

### 3. Debugger Hook Listener
An attached debugger allows attackers to bypass security logic or extract encryption keys.
* **Mitigation**: Detects JVM debugger state using `android.os.Debug.isDebuggerConnected()`. If active, the `DnsPacketHandler` triggers immediate DNS fail-stop blocks under the `Security Guard` label.

### 4. Package Signature Verification
To prevent reverse-engineering, repackaging, and side-loading of tampered APKs:
* Computes the SHA-256 hash of the active signing certificate via Android PackageManager.
* Validates matches against hardcoded release certificate hashes.

## Secure Backup Storage
* **Android Keystore**: Future updates will bind settings-export JSON files with AES-256-GCM keys protected by the Android Keystore system, preventing offline payload manipulation.
