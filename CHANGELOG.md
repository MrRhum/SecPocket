# Changelog

All notable changes to SecPocket are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and releases use [Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-11-19

First public release.

### Added

- 77 field briefs across 14 domains, covering governance, cloud, AI, application security, identity, networking, privacy, payments, OT/ICS, incident response, mobility, supply chain, resilience and cryptography.
- 26 curated essential briefs for the frameworks consultants use most often.
- Accent-insensitive full-text search with AND matching and an "ainsi" alias for ANSSI.
- Domain filters, grouped guide list and local saved briefs.
- Per-guide detail screen with summary, crown jewels, numbered core pillars, client playbook, related domains, official source link and share action.
- About screen with editorial method, privacy posture, catalog date and a clear-saved action.
- Dark navy/teal interface with adaptive launcher icon and 14 domain icons.

### Security

- No Android permissions, including no internet access.
- No accounts, analytics, ads, crash reporting or remote configuration.
- `android:allowBackup="false"` keeps saved briefs off cloud backup.
- APK signed with a self-signed RSA 4096 key using APK Signature Scheme v2 and v3.

### Documentation

- README with screenshots, catalog, install, build, signing and verification instructions.
- Release guide covering web upload, Git CLI and GitHub CLI workflows.
- Published SHA-256 checksums for release assets.

[1.0.0]: ../../releases/tag/v1.0.0
