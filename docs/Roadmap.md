# Architectural & Feature Roadmap — TrustPhone DNS

This document outlines the upcoming development cycles (Phase 2 and Phase 3) to scale the TrustPhone DNS engine to enterprise-grade functionality.

---

## Phase 2 — High-Performance Native Execution & Threat Intel

### 1. Double-Array Trie (DAT) Compilation
* **Goal**: Migrate from on-device Bloom Filters to a flat Double-Array Trie (DAT) binary structure.
* **Details**: Compiles 1.5M+ domains into a structured binary table where lookup complexity is strictly $O(L)$ (domain characters length) using array offset arithmetic, with zero false positives.

### 2. Memory-Mapped File Matching (`mmap`)
* **Goal**: Shift blocklists out of JVM heap storage.
* **Details**: Memory-map (`mmap`) the pre-compiled binary DAT file directly from storage. The OS handles virtual memory paging, reducing JVM RAM overhead to **0 bytes**.

### 3. Rust Native Matcher (JNI integration)
* **Goal**: Zero GC overhead during packet processing.
* **Details**: Rewrite packet interception parsing and domain matching in **Rust** (compiled to native binaries via JNI). This bypasses JVM Garbage Collector triggers entirely, maximizing CPU efficiency and cutting lookup decision overhead to sub-microsecond levels.

### 4. Real-time Cloud Intelligence
* **Goal**: Supplement offline filters with online reputation telemetry.
* **Details**: Query security intelligence APIs (e.g., Google Safe Browsing or URLhaus) for unknown/unseen domains with local caching.

---

## Phase 3 — Enterprise Security & Centralized Management

### 1. NextDNS-style Profile Sync
* **Goal**: Manage settings across multiple client platforms.
* **Details**: Sync user configurations, allowlists, and filter toggles across macOS, iOS, Android, and Windows devices using Firebase Firestore or Realtime Database.

### 2. Centralized DNS Firewall Admin Portal
* **Goal**: Remote configuration management for enterprise IT.
* **Details**: Push corporate custom block rules, dynamic threat feeds, and policy enforcement windows directly to worker devices.

### 3. SIEM Log Export Integration
* **Goal**: Security operations compliance.
* **Details**: Export encrypted aggregate logs in standard syslog format or directly to cloud logs (Splunk, Datadog, or Google Cloud Operations suite) for security team compliance monitoring.
