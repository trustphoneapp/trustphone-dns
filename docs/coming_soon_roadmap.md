# Architecture & Feature Roadmap — Shield DNS

This document details the upcoming development cycles (Phase 2 and Phase 3) to scale the Shield DNS engine to enterprise-grade functionality.

---

## Phase 2 — High-Performance Native Execution & Threat Intel

*   **Double-Array Trie (DAT) Compilation:**
    Migrate from on-device Bloom Filters to a flat Double-Array Trie (DAT) binary structure. This compiles 1.5M+ domains into a structured binary table where lookup complexity is strictly $O(L)$ (domain characters length) using array offset arithmetic, with zero false positives.
*   **Memory-Mapped File Matching (`mmap`):**
    Instead of loading blocklists into the Android JVM heap, the pre-compiled binary DAT file will be memory-mapped (`mmap`) directly into memory. The OS handles virtual memory paging, reducing JVM RAM overhead to **0 bytes**.
*   **Rust Native Matcher (JNI integration):**
    Re-write the packet interception parser and lookup matching engine in **Rust** (compiled to native binaries via JNI). This bypasses JVM Garbage Collector triggers entirely, maximizing CPU efficiency and cutting lookup decision overhead to sub-microsecond levels.
*   **Real-time Reputation Sync (Cloud Queries):**
    Implement local caching of popular allowed domains combined with real-time cloud lookup checks (Google Safe Browsing or URLhaus API queries) for rare/unseen domains.
*   **AI-Powered Scam Explainer:**
    An on-device localized micro-LLM/model or lightweight API prompt that explains the origin and classification of scam/phishing blocks in natural, simple language for users.

---

## Phase 3 — Enterprise Security & Centralized Management

*   **NextDNS-style Profile Sync:**
    Sync user configurations, allowlists, and filter toggles across multiple devices (macOS, iOS, Android, Windows) using Firebase Firestore or Realtime Database.
*   **Family & Profile Restrictions:**
    Create age-restricted profiles (e.g. Parental Controls) with scheduled block windows (e.g. "no social media after 8 PM") managed under a master pin lock.
*   **Zero-Trust Managed DNS Firewall:**
    Integrate with corporate enterprise portals to push company-wide custom block rules, dynamic threat feeds, and telemetry logs directly to worker devices.
*   **SIEM Log Export Integration:**
    Export encrypted aggregate logs in standard syslog format or directly to cloud logs (Splunk, Datadog, or Google Cloud Operations suite) for security team compliance monitoring.
