fun main() {
    val texts = listOf(
        "POC JOHN SMITH 8005551234",
        "Call: 800-555-1234",
        "Job site contact (800) 555-1234",
        "Just a formatted number 800-555-1234 here",
        "Just an unformatted number 8005551234 here"
    )

    // Regex 1: Keyword based
    val keywordRegex = Regex(""(?:call|contact|poc|phone|tel)\b.{0,20}?\(?\b(\d{3})\)?[-.\s]?(\d{3})[-.\s]?(\d{4})\b"", RegexOption.IGNORE_CASE)
    
    // Regex 2: Standard formatted number (no keyword needed)
    val standardRegex = Regex("""\(?\b(\d{3})\)?[-.\s]+(\d{3})[-.\s]+(\d{4})\b""")

    for (text in texts) {
        val keywordMatch = keywordRegex.find(text)
        val standardMatch = standardRegex.find(text)
        
        val finalMatch = keywordMatch ?: standardMatch
        if (finalMatch != null) {
            println("? FOUND in '$text' -> \-\-\")
        } else {
            println("? MISSED: '$text'")
        }
    }
}
