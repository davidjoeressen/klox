class LoxFunction(private val declaration: Stmt.Function, private val closure: Environment) : LoxCallable {
    override val arity: Int = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        declaration.params.zip(arguments).forEach { (param, argument) ->
            environment.define(param.lexeme, argument)
        }
        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            return returnValue.value
        }
        return null
    }

    override fun toString(): String = "<fn ${declaration.name.lexeme}>"
}