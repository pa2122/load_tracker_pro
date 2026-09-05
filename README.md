Fix build errors and warnings in DashboardScreen

- Add JitPack repository and Coil dependency
- Correct MarkdownText import and package usage
- Fix trailing brace syntax errors in DashboardScreen.kt
- Enforce Locale.US in String.format calls for decimal consistency
- Clean up unused imports and variables (showHelpDialog, trainerPayDifference)
- 1. Build Configuration & Dependencies
•
 settings.gradle.kts
◦
Added the JitPack repository to dependencyResolutionManagement. This was necessary to resolve the compose-markdown library.
•
 app/build.gradle.kts
◦
Added the Coil dependency (io.coil-kt:coil-compose). The MarkdownText component depends on Coil for image rendering, and the build was failing due to its absence.
2. UI Implementation:  DashboardScreen.kt
•
Library Fixes:
◦
Updated the MarkdownText import to the correct package: dev.jeziellago.compose.markdowntext.MarkdownText.
◦
Removed redundant package qualifiers for LazyColumn.
•
Bug & Syntax Fixes:
◦
Fixed a "top-level declaration expected" error by correcting the nested closing braces at the end of the file.
◦
Renamed the unused exception variable e to _ in the help_guide.md loading logic.
•
Code Cleanup:
◦
Removed the unused showHelpDialog state variable.
◦
Removed unused trainerPayDifference and isCurrentlyTraining variables from the weekly breakdown logic.
◦
Removed several unused import statements that were causing warnings.
•
Best Practices (Locale Safety):
◦
Updated all String.format calls (for currency, miles, and percentages) to use Locale.US. This ensures consistent formatting (e.g., using a dot instead of a comma for decimals) regardless of the user's device settings.
