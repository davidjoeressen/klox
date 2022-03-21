class Resolver(private val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private enum class FunctionType { NONE, FUNCTION }
    private val scopes = ArrayDeque<MutableMap<String, Boolean>>()
    private var currentFunction = FunctionType.NONE

    override fun visitAssign(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinary(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCall(expr: Expr.Call) {
        resolve(expr.callee)
        expr.arguments.forEach(::resolve)
    }

    override fun visitGrouping(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteral(expr: Expr.Literal) {}

    override fun visitLogical(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitUnary(expr: Expr.Unary) {
        resolve(expr.right)
    }

    override fun visitVariable(expr: Expr.Variable) {
        if (!scopes.isEmpty() && scopes.first()[expr.name.lexeme] == false) {
           Lox.error(expr.name, "Can't read local variable in its own initializer.")
        }
        resolveLocal(expr, expr.name)
    }

    override fun visitBlock(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitExpression(stmt: Stmt.Expression) {
        resolve(stmt.expression)
    }

    override fun visitFunction(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)
        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitIf(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        stmt.elseBranch?.let(::resolve)
    }

    override fun visitPrint(stmt: Stmt.Print) {
        resolve(stmt.expression)
    }

    override fun visitReturn(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE) Lox.error(stmt.keyword, "Can't return from top-level code.")
        stmt.value?.let(::resolve)
    }

    override fun visitVar(stmt: Stmt.Var) {
        declare(stmt.name)
        if (stmt.initializer != null) {
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    override fun visitWhile(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    fun resolve(statements: List<Stmt>) {
        statements.forEach(::resolve)
    }

    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        scopes.forEachIndexed { i, scope ->
            if (name.lexeme in scope) {
                interpreter.resolve(expr, i)
                return
            }
        }
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type
        beginScope()
        function.params.forEach {
            declare(it)
            define(it)
        }
        resolve(function.body)
        endScope()
        currentFunction = enclosingFunction
    }

    private fun beginScope() {
        scopes.addFirst(mutableMapOf())
    }

    private fun endScope() {
        scopes.removeFirst()
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return
        val scope = scopes.first()
        if (name.lexeme in scope) Lox.error(name, "Already a variable with this name in this scope.")
        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        scopes.first()[name.lexeme] = true
    }
}