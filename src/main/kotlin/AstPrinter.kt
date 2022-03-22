class AstPrinter : Expr.Visitor<String> {
    fun print(expr: Expr) = expr.accept(this)

    override fun visitAssign(expr: Expr.Assign): String =
        parenthesize("assign ${expr.name.lexeme}", expr.value)

    override fun visitBinary(expr: Expr.Binary): String =
        parenthesize(expr.operator.lexeme, expr.left, expr.right)

    override fun visitGrouping(expr: Expr.Grouping): String =
        parenthesize("group", expr.expression)

    override fun visitLiteral(expr: Expr.Literal): String =
        expr.value?.toString() ?: "nil"

    override fun visitSet(expr: Expr.Set): String =
        parenthesize("set", expr.obj.accept(this), expr.name.lexeme, expr.value.accept(this))

    override fun visitSuper(expr: Expr.Super): String = parenthesize(expr.keyword.lexeme, expr.method.lexeme)

    override fun visitThis(expr: Expr.This): String = expr.keyword.lexeme

    override fun visitUnary(expr: Expr.Unary): String =
        parenthesize(expr.operator.lexeme, expr.right)

    override fun visitCall(expr: Expr.Call): String =
        parenthesize(expr.callee.accept(this), *expr.arguments.toTypedArray())

    override fun visitGet(expr: Expr.Get): String =
        parenthesize("get", expr.obj.accept(this), expr.name.lexeme)

    override fun visitVariable(expr: Expr.Variable): String =
        expr.name.lexeme

    override fun visitLogical(expr: Expr.Logical): String =
        parenthesize(expr.operator.lexeme, expr.left, expr.right)

    private fun parenthesize(name: String, vararg exprs: Expr) =
        (listOf(name) + exprs.map { it.accept(this) }).joinToString(" ", "(", ")")

    private fun parenthesize(vararg values: String) = values.joinToString(" ", "(", ")")
}