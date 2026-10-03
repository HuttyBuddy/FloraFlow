package com.example.support

/** Public support contact, as listed on the Play Store listing. */
const val SUPPORT_EMAIL = "huttybuddy20@gmail.com"

/** Support email intent with a pre-filled subject/body. */
fun buildSupportEmailIntent(subject: String, body: String): android.content.Intent =
    android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
        data = android.net.Uri.parse("mailto:$SUPPORT_EMAIL")
        putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
        putExtra(android.content.Intent.EXTRA_TEXT, body)
    }

/** Body for an AI-answer report email. Carries only the anonymous reference
 * ID, reason, and timestamp — never the user's message or the AI text. */
fun aiReportEmailBody(reportId: String, reason: String, timestampUtc: String): String =
    "AI answer report\n" +
        "Reference ID: $reportId\n" +
        "Reason: $reason\n" +
        "Reported at (UTC): $timestampUtc\n\n" +
        "Details (optional — describe what was wrong):\n"
