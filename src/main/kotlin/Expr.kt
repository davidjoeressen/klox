abstract class Expr {
    class Assign(val name: Token, val value: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitAssign(this)
    }
    class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitBinary(this)
    }
    class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitCall(this)
    }
    class Get(val obj: Expr, val name: Token) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitGet(this)
    }
    class Grouping(val expression: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitGrouping(this)
    }
    class Literal(val value: Any?) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitLiteral(this)
    }
    class Logical(val left: Expr, val operator: Token, val right: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitLogical(this)
    }
    class Set(val obj: Expr, val name: Token, val value: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitSet(this)
    }
    class This(val keyword: Token) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitThis(this)
    }
    class Unary(val operator: Token, val right: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitUnary(this)
    }
    class Variable(val name: Token) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitVariable(this)
    }

    interface Visitor<T> {
        fun visitAssign(expr: Assign): T
        fun visitBinary(expr: Binary): T
        fun visitCall(expr: Call): T
        fun visitGet(expr: Get): T
        fun visitGrouping(expr: Grouping): T
        fun visitLiteral(expr: Literal): T
        fun visitLogical(expr: Logical): T
        fun visitSet(expr: Set): T
        fun visitThis(expr: This): T
        fun visitUnary(expr: Unary): T
        fun visitVariable(expr: Variable): T
    }

    abstract fun <T> accept(visitor: Visitor<T>): T
}