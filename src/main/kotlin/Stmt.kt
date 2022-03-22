abstract class Stmt {
    class Block(val statements: List<Stmt>) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitBlock(this)
    }
    class Class(val name: Token, val superclass: Expr.Variable?, val methods: List<Function>) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitClass(this)
    }
    class Expression(val expression: Expr) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitExpression(this)
    }
    class Function(val name: Token, val params: List<Token>, val body: List<Stmt>) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitFunction(this)
    }
    class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitIf(this)
    }
    class Print(val expression: Expr) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitPrint(this)
    }
    class Return(val keyword: Token, val value: Expr?) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitReturn(this)
    }
    class Var(val name: Token, val initializer: Expr?) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitVar(this)
    }
    class While(val condition: Expr, val body: Stmt) :Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitWhile(this)
    }

    interface Visitor<T> {
        fun visitBlock(stmt: Block): T
        fun visitClass(stmt: Class): T
        fun visitExpression(stmt: Expression): T
        fun visitFunction(stmt: Function): T
        fun visitIf(stmt: If): T
        fun visitPrint(stmt: Print): T
        fun visitReturn(stmt: Return): T
        fun visitVar(stmt: Var): T
        fun visitWhile(stmt: While): T
    }

    abstract fun <T> accept(visitor: Visitor<T>): T
}