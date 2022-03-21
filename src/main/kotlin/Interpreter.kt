import kotlin.system.exitProcess

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    val globals = Environment().apply {
        define("clock", object : LoxCallable {
            override val arity: Int = 0

            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? =
                System.currentTimeMillis().toDouble() / 1000.0

            override fun toString(): String = "<native fn>"
        })
        define("exit", object : LoxCallable {
            override val arity: Int = 1

            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                exitProcess((arguments.first() as Double).toInt())
            }

            override fun toString(): String = "<native fn>"
        })
    }
    private var environment = globals
    private val locals = mutableMapOf<Expr, Int>()

    fun interpret(statements: List<Stmt>) {
        try {
            statements.forEach(::execute)
        } catch (e: RuntimeError) {
           Lox.runtimeError(e)
        }
    }

    override fun visitAssign(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)

        val distance = locals[expr]
        return if (distance != null) {
            environment.assignAt(distance, expr.name, value)
        } else {
            globals.assign(expr.name, value)
        }
    }

    override fun visitBinary(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)
        return when (expr.operator.type) {
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                left as Double - right as Double
            }
            TokenType.SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                left as Double / right as Double
            }
            TokenType.STAR -> {
                checkNumberOperands(expr.operator, left, right)
                left as Double * right as Double
            }
            TokenType.PLUS -> when {
                left is Double && right is Double -> left + right
                left is String && right is String -> left + right
                else -> throw RuntimeError(expr.operator, "Operands must be two numbers or two strings.")
            }
            TokenType.GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                left as Double > right as Double
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                left as Double >= right as Double
            }
            TokenType.LESS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) < right as Double
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                left as Double <= right as Double
            }
            TokenType.EQUAL_EQUAL -> left == right
            TokenType.BANG_EQUAL -> left != right
            else -> null
        }
    }

    override fun visitGrouping(expr: Expr.Grouping): Any? = evaluate(expr.expression)

    override fun visitLiteral(expr: Expr.Literal): Any? = expr.value

    override fun visitLogical(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)
        return when {
            expr.operator.type == TokenType.OR && isTruthy(left) -> left
            expr.operator.type == TokenType.AND && !isTruthy(left) -> left
            else -> evaluate(expr.right)
        }
    }

    override fun visitUnary(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)
        return when (expr.operator.type) {
            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right)
                -(right as Double)
            }
            TokenType.BANG -> !isTruthy(right)
            else -> null
        }
    }

    override fun visitCall(expr: Expr.Call): Any? {
        val callee = evaluate(expr.callee)

        val arguments = expr.arguments.map(::evaluate)

        if (callee !is LoxCallable) throw RuntimeError(expr.paren, "Can only call function and classes.")
        if (arguments.size != callee.arity) throw RuntimeError(
            expr.paren,
            "Expected ${callee.arity} arguments but got ${arguments.size}."
        )
        return callee.call(this, arguments)
    }

    override fun visitVariable(expr: Expr.Variable): Any? = lookUpVariable(expr.name, expr)

    override fun visitBlock(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    override fun visitExpression(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }

    override fun visitFunction(stmt: Stmt.Function) {
        environment.define(stmt.name.lexeme, LoxFunction(stmt, environment))
    }

    override fun visitIf(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else {
            stmt.elseBranch?.let(::execute)
        }
    }

    override fun visitPrint(stmt: Stmt.Print) {
        println(stringify(evaluate(stmt.expression)))
    }

    override fun visitReturn(stmt: Stmt.Return) {
        val value = stmt.value?.let(::evaluate)
        throw Return(value)
    }

    override fun visitVar(stmt: Stmt.Var) {
        environment.define(stmt.name.lexeme, stmt.initializer?.let(::evaluate))
    }

    override fun visitWhile(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) execute(stmt.body)
    }

    private fun execute(stmt: Stmt) {
        stmt.accept(this)
    }

    fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment
            statements.forEach(::execute)
        } finally {
            this.environment = previous
        }
    }

    private fun evaluate(expr: Expr): Any? = expr.accept(this)

    private fun isTruthy(value: Any?): Boolean = when (value) {
        null -> false
        is Boolean -> value
        else -> true
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand !is Double) throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left !is Double || right !is Double) throw RuntimeError(operator, "Operands must be numbers.")
    }

    private fun stringify(value: Any?): String = when (value) {
        null -> "nil"
        is Double -> value.toString().removeSuffix(".0")
        else -> value.toString()
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr]
        return if (distance != null) environment.getAt(distance, name.lexeme) else globals.get(name)
    }
}