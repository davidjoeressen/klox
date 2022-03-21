data class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any?,
    val line: Int,
    val col: Int,
    val start: Int,
    val end: Int
) {
    override fun toString() = "$line:$col: " + listOfNotNull(type, lexeme.ifBlank { null }, literal).joinToString(", ")
}