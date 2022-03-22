class Parser(private val tokens: List<Token>) {
    private var current: Int = 0

    fun parse(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }
        return statements
    }

    private fun declaration(): Stmt? = try {
        when {
            match(TokenType.CLASS) -> classDeclaration()
            match(TokenType.FUN) -> function("function")
            match(TokenType.VAR) -> varDeclaration()
            else -> statement()
        }
    } catch (e: ParseError) {
        synchronize()
        null
    }

    private fun classDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect class name.")
        val superclass = if (match(TokenType.LESS)) {
            consume(TokenType.IDENTIFIER, "Expect superclass name.")
            Expr.Variable(previous())
        } else {
            null
        }
        consume(TokenType.LEFT_BRACE, "Expect '{' before class body.")
        val methods = mutableListOf<Stmt.Function>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"))
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.")
        return Stmt.Class(name, superclass, methods)
    }

    private fun function(kind: String): Stmt.Function {
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        consume(TokenType.LEFT_PAREN, "Expect '(' after $kind name.")
        val parameters = mutableListOf<Token>()
        if (!check(TokenType.RIGHT_PAREN)) {
           do {
               if (parameters.size >= 255) {
                   parseError(peek(), "Can't have more than 255 parameters.")
               }
               parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
           } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")
        consume(TokenType.LEFT_BRACE, "Expect '{' before $kind body.")
        val body = block()
        return Stmt.Function(name, parameters, body)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")
        val initializer = if (match(TokenType.EQUAL)) expression() else null
        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt = when {
        match(TokenType.FOR) -> forStatement()
        match(TokenType.IF) -> ifStatement()
        match(TokenType.PRINT) -> printStatement()
        match(TokenType.RETURN) -> returnStatement()
        match(TokenType.WHILE) -> whileStatement()
        match(TokenType.LEFT_BRACE) -> Stmt.Block(block())
        else -> expressionStatement()
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.")
        val initializer = when {
            match(TokenType.SEMICOLON) -> null
            match(TokenType.VAR) -> varDeclaration()
            else -> expressionStatement()
        }
        val condition = if (!check(TokenType.SEMICOLON)) expression() else Expr.Literal(true)
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.")
        val increment = if (!check(TokenType.RIGHT_PAREN)) expression() else null
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.")
        var body = statement()
        if (increment != null) {
            body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
        }
        body = Stmt.While(condition, body)
        if (initializer != null) {
            body = Stmt.Block(listOf(initializer, body))
        }
        return body
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.")

        val thenBranch = statement()
        val elseBranch = if (match(TokenType.ELSE)) statement() else null
        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = if (!check(TokenType.SEMICOLON)) expression() else null
        consume(TokenType.SEMICOLON, "Expect ';' after return value.")
        return Stmt.Return(keyword, value)
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after condition.")
        val body = statement()

        return Stmt.While(condition, body)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun block(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) declaration()?.let { statements.add(it) }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after block.")
        return statements
    }

    private fun expression(): Expr = assignment()

    private fun assignment(): Expr {
        val expr = or()
        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment()
            when (expr) {
                is Expr.Variable -> return Expr.Assign(expr.name, value)
                is Expr.Get -> return Expr.Set(expr.obj, expr.name, value)
            }
            parseError(equals, "Invalid assignment target.")
        }
        return expr
    }

    private fun or(): Expr = binary(::and, TokenType.OR)

    private fun and(): Expr = binary(::equality, TokenType.AND)

    private fun equality(): Expr = binary(::comparison, TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)

    private fun comparison(): Expr =
        binary(::term, TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)

    private fun term(): Expr = binary(::factor, TokenType.MINUS, TokenType.PLUS)

    private fun factor(): Expr = binary(::unary, TokenType.SLASH, TokenType.STAR)

    private fun binary(f: () -> Expr, vararg types: TokenType): Expr {
        var expr = f()
        while (match(*types)) {
            expr = Expr.Binary(expr, previous(), f())
        }
        return expr
    }

    private fun unary(): Expr = if (match(TokenType.BANG, TokenType.MINUS)) {
        Expr.Unary(previous(), unary())
    } else {
        call()
    }

    private fun call(): Expr {
        var expr = primary()
        while (true) {
            expr = when {
                match(TokenType.LEFT_PAREN) -> finishCall(expr)
                match(TokenType.DOT) ->
                    Expr.Get(expr, consume(TokenType.IDENTIFIER, "Expected property name after '.'."))
                else -> break
            }
        }

        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    parseError(peek(), "Can't have more than 255 arguments.")
                }
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        }
        val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")

        return Expr.Call(callee, paren, arguments)
    }

    private fun primary(): Expr = when {
        match(TokenType.FALSE) -> Expr.Literal(false)
        match(TokenType.TRUE) -> Expr.Literal(true)
        match(TokenType.NIL) -> Expr.Literal(null)
        match(TokenType.NUMBER, TokenType.STRING) -> Expr.Literal(previous().literal)
        match(TokenType.SUPER) -> {
            val keyword = previous()
            consume(TokenType.DOT, "Expect '.' after 'supers'.")
            val method = consume(TokenType.IDENTIFIER, "Expect superclass method name.")
            Expr.Super(keyword, method)
        }
        match(TokenType.THIS) -> Expr.This(previous())
        match(TokenType.IDENTIFIER) -> Expr.Variable(previous())
        match(TokenType.LEFT_PAREN) ->
            Expr.Grouping(expression().also { consume(TokenType.RIGHT_PAREN, "Expect ')' after Expression.") })
        else -> throw parseError(peek(), "Expected expression.")
    }

    private fun match(vararg types: TokenType): Boolean = types.any { check(it) }.also { if (it) advance() }

    private fun check(type: TokenType): Boolean = !isAtEnd() && peek().type == type

    private fun advance(): Token = tokens[current].also { if (!isAtEnd()) current++ }

    private fun peek(): Token = tokens[current]

    private fun previous(): Token = tokens[current-1]

    private fun isAtEnd() = peek().type == TokenType.EOF

    private fun consume(type: TokenType, message: String) = if (check(type)) advance()
        else throw parseError(peek(), message)

    private fun synchronize() {
        val statementStarts = listOf(
            TokenType.CLASS,
            TokenType.FUN,
            TokenType.VAR,
            TokenType.FOR,
            TokenType.IF,
            TokenType.WHILE,
            TokenType.PRINT,
            TokenType.RETURN
        )
        do {
            advance()
        } while (!isAtEnd() && previous().type != TokenType.SEMICOLON && peek().type !in statementStarts)
    }

    private fun parseError(token: Token, message: String): ParseError {
        Lox.error(token, message)
        return ParseError()
    }

    private class ParseError : RuntimeException()
}