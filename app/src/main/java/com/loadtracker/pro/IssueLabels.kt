package com.loadtracker.pro

object IssueLabels {
    const val TESTER_REPORT = "tester-report"

    fun versionLabel(versionName: String = BuildConfig.VERSION_NAME): String =
        if (versionName.startsWith("2.")) "version:v2.0" else "version:v1.x"
}
