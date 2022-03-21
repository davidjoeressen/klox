class Scanner(private val source: String) {
    private val tokens = mutableListOf<Token>()
    private var start: Int = 0
    private var current: Int = 0
    private var startCol: Int = 1
    private var currentCol: Int = 1
    private var line: Int = 1

    private val keywords: Map<String, TokenType> = mapOf(
        "and" to TokenType.AND,
        "class" to TokenType.CLASS,
        "else" to TokenType.ELSE,
        "false" to TokenType.FALSE,
        "for" to TokenType.FOR,
        "fun" to TokenType.FUN,
        "if" to TokenType.IF,
        "nil" to TokenType.NIL,
        "or" to TokenType.OR,
        "print" to TokenType.PRINT,
        "return" to TokenType.RETURN,
        "super" to TokenType.SUPER,
        "this" to TokenType.THIS,
        "true" to TokenType.TRUE,
        "var" to TokenType.VAR,
        "while" to TokenType.WHILE
    )

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            startCol = currentCol
            scanToken()
        }
        tokens.add(Token(TokenType.EOF, "", null, line, startCol, start, current))
        return tokens
    }

    private fun scanToken() {
        when (val c = advance()) {
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON)
            '*' -> addToken(TokenType.STAR)
            '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            '/' -> if (match('/')) {
                while (peek() != '\n' && !isAtEnd()) advance()
            } else {
                addToken(TokenType.SLASH)
            }
            '\n' -> nextLine()
            ' ', '\t', '\r' -> {}
            '"' -> string()
            else -> when {
                isDigit(c) -> number()
                isAlpha(c) -> identifier()
                else -> Lox.error(line, startCol, "Unexpected Character.")
            }
        }
    }

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') nextLine()
            advance()
        }

        if (isAtEnd()) {
            Lox.error(line, currentCol, "Unterminated string.")
            return
        }

        advance()
        addToken(TokenType.STRING, source.substring(start+1, current-1))
    }

    private fun nextLine() {
        line++
        currentCol = 1
    }

    private fun number() {
        while (isDigit(peek())) advance()

        if (peek() == '.' && isDigit(peekNext())) {
            advance()
            while (isDigit(peek())) advance()
        }

        addToken(TokenType.NUMBER, source.substring(start, current).toDouble())
    }

    private fun identifier() {
        while (isAlphaNumeric(peek())) advance()
        val text = source.substring(start, current)
        addToken(keywords[text] ?: TokenType.IDENTIFIER)
    }

    private fun advance(): Char = source[current++].also { currentCol++ }

    private fun match(c: Char): Boolean = (peek() == c).also { if (it) advance() }

    private fun peek(): Char = if (isAtEnd()) '\u0000' else source[current]

    private fun peekNext(): Char = if (current + 1 >= source.length) '\u0000' else source[current+1]

    private fun addToken(type: TokenType, literal: Any? = null) {
        val text: String = source.substring(start, current)
        tokens.add(Token(type, text, literal, line, startCol, start, current))
    }

    private fun isAtEnd(): Boolean = current >= source.length

    private fun isDigit(c: Char) = c in '0'..'9'

    private fun isAlpha(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c == '_'

    private fun isAlphaNumeric(c: Char) = isAlpha(c) || isDigit(c)
}