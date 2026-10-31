# Copilot Instructions for load_tracker_pro

This file provides guidance for GitHub Copilot when working on the load_tracker_pro repository.

---

## 🚦 Mandatory Operating Rules

### Communication & Execution
- **Answer first, act later:** Address the user's question or idea directly first, with analysis or a proposed design.
- **Wait for confirmation:** Do NOT modify code, commit, push, or make GitHub changes (issues, labels, comments, milestones) until the user explicitly says "go ahead" / "do it" / "make the changes".
- **Allowed without permission:** Reading files, searching, and compiling/running tests.
- If the user says "stop", stop immediately and revert any unapproved changes.

### Tooling
- Edit and read files with the IDE/built-in file tools, not shell commands (`sed`, `echo >`, `cat`, etc.). Re-read a file before editing it.

### GitHub Issue Workflow
- After a fix or feature is implemented, pushed, and verified by tests/build, comment on the issue with the changes.
- Leave the issue **OPEN** and apply the **`needs-verification`** label.
- Never close an issue unless the user explicitly says "close it" or "verified".

### Branches & Versions
- State the current branch before starting work and after any branch switch.
- **Bug fixes and new features go to both `v1.x` and `v2` by default**, unless the user explicitly names a different version scope. If the target version is unclear or the change may not fit one version, ask which version(s).
- Apply to the second branch via cherry-pick, resolve conflicts, rebuild, and test before pushing.
- `v2` has code `v1.x` lacks (e.g., load model, backup manager). On conflicts, never copy v1.x-only calls; adapt to what the target branch actually has.
- After branch-hopping, switch back to the branch the user named.

### Verification & Reporting
- Run the focused unit test or build before any push. State plainly if validation failed or could not run.
- Be honest about status: say clearly if something is unpushed, unverified, or partly done.

### Device Testing
- Any testing that interacts with the app or its data (tapping, entering loads, running flows, clearing data) must be done on an **emulator only**, never on the user's physical phone. This protects real load history and avoids test records.
- If no emulator is running, tell the user so they can start one. Do not fall back to the phone.
- Before using `adb`, confirm the target serial starts with `emulator-` and pin it with `-s`.
- Read-only inspection on the phone (e.g., `dumpsys`) is fine. Install to the phone only when the user asks, and never uninstall (always update in place).

### Scope & Behavior
- Fix only what the issue covers. Raise unrelated problems as a question or a new issue; do not change them.
- One-time instructions (e.g., "no commit message needed") apply to that action only, unless the user makes them permanent.
- If a request could mean two things, ask one short question instead of guessing.
- Keep reports short unless the user asks for detail.

---

## 📋 Project Overview

**load_tracker_pro** is a Kotlin-based Android application for dispatch, payroll, and auditing workflows for flatbed trucking operations. The app processes dispatch screenshots via OCR, tracks loads, calculates payroll, and maintains detailed audit logs.

---

## 🏗️ Architecture Guidelines

### UI Layer
- **Framework:** Jetpack Compose with Material 3 components
- **State Management:** MVVM pattern with Compose state holders
- **Navigation:** Use Compose Navigation framework
- **Layouts:** Keep the main map interface clean; use compact floating bars or bottom sheets for HUD elements

### Data Layer
- **Database:** SQLite via Room ORM
- **Migrations:** All schema changes must use explicit `Migration` objects in `AppDatabase.kt`
- **Data Classes:** Use Kotlin data classes with immutable fields where practical
- **Queries:** Prefer Flow-based queries for reactive UI updates

### Business Logic
- **Separation:** Keep domain logic out of Compose files
- **Calculations:** Payroll math, odometer tracking, and monetary values require clear, testable functions
- **Validation:** Validate user input early and provide clear error messages

---

## 🧠 Domain-Specific Rules

### OCR & Dispatch Processing
- When parsing dispatch screenshots with ML Kit, use flexible regex patterns
- Exclude dynamic headers (`Origin`, `Drop`, `unload #`) from required fields
- Make optional fields truly optional—do not crash if a field is missing
- Prioritize graceful degradation over strict parsing

### Payroll & Odometer Calculations
- All monetary and mileage calculations must have corresponding unit tests
- Keep calculations deterministic and easy to audit
- Store raw values; format for display in UI only

### Database Integrity
- Always verify migrations before pushing changes
- Test with both fresh installs and upgraded databases
- Document any breaking schema changes

---

## 📚 Code Quality Standards

### Kotlin Style
- Use idiomatic Kotlin: null safety, extension functions, scope functions where appropriate
- Prefer `val` over `var`; use immutable data structures
- Keep functions small and single-purpose
- Use meaningful names for variables and functions

### Resources & Strings
- Store all user-facing strings in `strings.xml` (not hardcoded)
- Use color, dimension, and style resources consistently
- Keep layout files clean and readable

### Dependencies
- Avoid unnecessary library additions without discussion
- Keep the dependency tree lean
- Use AndroidX and Jetpack libraries as the primary source

---

## ✅ Testing Requirements

### When to Write Tests
- **Must test:** All business logic, payroll calculations, odometer math, data transformations
- **Should test:** Core features that affect user data or financial calculations
- **Nice to have:** UI state transitions and error handling

### Test Practices
- Use JUnit 4 with Mockito or equivalent for unit tests
- Use Compose testing utilities for UI tests
- Keep tests fast and isolated
- Run tests locally before pushing: `./gradlew test`

---

## 🌿 Git & Branching

### Active Branches
- **`v1.x`:** Stable v1.x production release
- **`v2`:** Version 2.0 UI/UX redesign and Owner-Operator features (current main development branch)

### Before Starting Work
- Confirm the active branch with the user
- Verify your local branch matches the intended version
- After switching branches, rebuild/sync Gradle to ensure BuildConfig fields and package IDs are current

### Commit Messages
- Write clear, descriptive commit messages
- Reference GitHub issues when applicable (e.g., "Fixes #42")
- Keep commits atomic and focused on a single change

---

## 🗂️ Project Boundaries

### In Scope
- Dispatch, payroll, and auditing features for flatbed trucking
- OCR processing and document handling
- Load tracking and odometer management
- Database schema and persistence logic
- Android app UI/UX improvements

### Out of Scope
- Commercial LVR (Logistics/Routing) features belong in the separate `TruckRoutePro` repository
- Do not mix code or features between `load_tracker_pro` and `TruckRoutePro`
- Do not introduce unrelated features into this repo

---

## 🔧 Common Workflows

### Adding a New Feature
1. Discuss the feature scope with the user
2. Design the data model and database changes (if any)
3. Implement business logic with tests
4. Build the UI in Compose
5. Test end-to-end with real or mock data

### Fixing a Bug
1. Understand and reproduce the bug
2. Write a test that captures the bug
3. Fix the code to pass the test
4. Verify no regressions in related features

### Database Schema Changes
1. Create a new `Migration` object in `AppDatabase.kt`
2. Test with both fresh installs and version upgrades
3. Document the migration in commit message
4. Verify Room compiler generates correct classes

---

## 🤝 Communication

- **Ask clarifying questions** before making large changes
- **Explain your approach** for non-trivial edits
- **Link relevant issues** when you reference them
- **Test and verify** locally before requesting review

---

## 📝 Summary

Write clean, idiomatic Kotlin code. Test business logic thoroughly. Keep the UI simple and consistent. Respect project boundaries. When in doubt, ask.
