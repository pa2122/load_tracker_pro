# Load Tracker Pro Review – Executive Summary

Repository: `pa2122/load_tracker_pro`  
Date: `2026-09-27`

## Summary

This project has a strong feature set and clear domain focus, but it currently has several production risks that should be addressed before release. The biggest concerns are:
- hard-coded secrets
- destructive database migration behavior
- GPS tracking reliability
- unsafe lifecycle resets for tracking state

## High-priority issues

### 1. Hard-coded Google Maps API key
File: `app/src/main/AndroidManifest.xml`

A Google Maps API key is committed directly to source. This is a security exposure and should be rotated and removed from the repo.

Action:
- Remove the key from source control
- Rotate it
- Restrict it by package name and signing certificate
- Use a secure build-time injection pattern

---

### 2. Hard-coded release signing credentials
File: `app/build.gradle.kts`

The release keystore password and key password are embedded in the Gradle config.

Action:
- Remove all signing secrets from source
- Use encrypted CI secrets
- Store the keystore outside the repo
- Rotate the keystore if it may have been exposed

---

### 3. Destructive migration fallback
File: `app/src/main/java/com/example/tmcloadtracker/AppDatabase.kt`

`fallbackToDestructiveMigration(dropAllTables = true)` is enabled. This can erase user data if a migration fails or a database version mismatch occurs.

Action:
- Remove destructive fallback
- Use explicit, tested migrations
- Add backup/recovery logic before any destructive path is considered

---

### 4. Unsafe tracking state resets
File: `app/src/main/java/com/example/tmcloadtracker/TrackingService.kt`

The service resets tracking state in `onDestroy()`. This is a risky lifecycle choice because service teardown is not equivalent to “trip complete.”

Action:
- Only reset after explicit trip completion
- Persist tracking state during lifecycle transitions
- Do not clear all trip state during generic shutdown

---

### 5. GPS mileage is not validated
File: `app/src/main/java/com/example/tmcloadtracker/TrackingService.kt`

Distance is calculated from all consecutive GPS fixes without visible filtering for:
- inaccurate fixes
- stale locations
- GPS jumps
- mock locations

Action:
- Filter by accuracy and timestamp
- Reject unrealistic jumps
- Audit raw vs filtered mileage separately

---

### 6. Data persistence uses Float for mileage after tracking in Double
File: `app/src/main/java/com/example/tmcloadtracker/TrackingService.kt`

Mileage is tracked as `Double`, then converted to `Float` for storage.

Action:
- Use a higher-precision data type for persisted mileage
- Store integer-based units or another consistent representation

---

## Medium-priority issues

### 7. Replace-based DAO insert can overwrite records
File: `app/src/main/java/com/example/tmcloadtracker/LoadDao.kt`

`OnConflictStrategy.REPLACE` can silently overwrite historic load records.

Action:
- Use explicit update logic
- Validate duplicates before insert
- Use a stable identifier with uniqueness constraints

---

### 8. Payroll math uses floating-point numbers
File: `app/src/main/java/com/example/tmcloadtracker/LoadViewModel.kt`

Money is calculated with `Double`, which can lead to rounding issues.

Action:
- Use integer cents or a decimal money type
- Centralize payment logic in a domain service
- Round only at explicit accounting boundaries

---

### 9. CSV export may not match displayed pay logic
File: `app/src/main/java/com/example/tmcloadtracker/LoadViewModel.kt`

The CSV export can omit logic that is reflected in the UI summary, making exported statements less reliable.

Action:
- Use a single canonical calculation model for UI and export
- Keep exports consistent with displayed statements

---

### 10. Pay-period logic uses mixed timezone assumptions
File: `app/src/main/java/com/example/tmcloadtracker/LoadViewModel.kt`

The app uses UTC in some places and local time in others.

Action:
- Define one business timezone
- Use it consistently for pay grouping, UI display, and exports

---

## CI and release concerns

### 11. Broad workflow permissions
File: `.github/workflows/deploy_release.yml`

The workflow grants `contents: write` and automates releases.

Action:
- Use `contents: read` for build jobs
- Separate build and release jobs
- Require tag or manual approval for production releases
- Pin actions to specific SHAs

---

### 12. Signing isn’t clearly configured in CI
File: `.github/workflows/deploy_release.yml`

The workflow claims to build release bundles but does not clearly show signing secrets or keystore setup.

Action:
- Configure release signing via GitHub secrets
- Verify signing in CI pipeline
- Remove dependence on local keystore assumptions

---

## Maintainability

The app’s large screen/controller files increase complexity and risk:
- `MainActivity.kt`
- `DashboardScreen.kt`
- `LoadEntryScreen.kt`
- `DevNotesScreen.kt`

Action:
- Split into smaller composables
- Use ViewModels and repositories
- Centralize domain logic
- Reduce screen-level state complexity

---

## Final assessment

The project is promising and feature-rich, but it is not yet production-safe from a security and data-integrity standpoint.

Priority order:
1. Remove hard-coded secrets
2. Remove destructive DB fallback
3. Fix service lifecycle and tracking resets
4. Add GPS validation and durable state persistence
5. Centralize payroll logic and pay calculations
6. Tighten CI/release controls

If you want, I can also turn this into:
- a GitHub issue checklist
- a Markdown file for direct upload
- or a prioritized engineering backlog with owners and effort estimates.