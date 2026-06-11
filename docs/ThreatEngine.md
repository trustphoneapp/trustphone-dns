# Threat & Reputation Engine Specification

This document describes the mechanics of the `ReputationEngine` designed to protect users against phishing, scam, and brand-impersonation attacks.

## Normalization Pipeline
Before evaluating any domain name, it is normalized to prevent bypasses:
1. Whitespace trimming.
2. Conversion to absolute lowercase (Locale.US).
3. Suffix dot removal.

## Phishing & Brand Spoofing Analysis

### 1. IDN Homograph Spoofing
Internationalized Domain Names (IDNs) allow non-ASCII characters to represent domains. Attackers exploit this to register domains visually indistinguishable from famous brands (e.g. replacing Latin 'a' with Cyrillic 'a').
* **Detection**: The engine flags any domain starting with `xn--` (Punycode prefix) as a high threat indicator, assigning an automatic risk score penalty of `+40`.

### 2. Typosquatting (Levenshtein Distance)
We compute the Levenshtein distance between domain labels and a list of high-profile target brands (e.g. google, paypal, bankofamerica, chase).
* **Metric**: If the edit distance is `1` or `2` (e.g. `paypa1.com`), it is flagged as typosquatting (`+55` risk penalty).
* **Substring Check**: Substrings containing brands with separating characters (e.g. `secure-paypal-login.com`) are immediately flagged.

### 3. Suspicious Keywords
Phishing domains frequently contain urgent action keywords like `login`, `verify`, `checkout`, `wallet`, or `billing`.
* **Detection**: Matches against the list of target keywords add `+15` risk penalty.

### 4. Suspicious Top-Level Domains (TLDs)
The engine maintains a set of TLDs heavily correlated with spam and malicious actions (`.zip`, `.mov`, `.fit`, `.top`, `.tk`, etc.). Matching domains receive `+25` risk penalty.

## Threat Classifications
The final aggregated score ranges from `0` to `100`:
* **Dangerous (81 - 100)**: Immediate threat, auto-blocked.
* **Risky (51 - 80)**: Suspicious features matching brand-impersonations.
* **Suspicious (21 - 50)**: Minor triggers (e.g. suspicious keyword on generic TLD).
* **Safe (0 - 20)**: Clean domain reputation.
