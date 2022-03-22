class LoxFunction(
    private val declaration: Stmt.Function,
    private val closure: Environment,
    private val isInitializer: Boolean
) : LoxCallable {
    fun bind(instance: LoxInstance): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", instance)
        return LoxFunction(declaration, environment, isInitializer)
    }

    override val arity: Int = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        declaration.params.zip(arguments).forEach { (param, argument) ->
            environment.define(param.lexeme, argument)
        }
        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            return if (isInitializer) closure.getAt(0, "this") else returnValue.value
        }
        return if (isInitializer) closure.getAt(0, "this") else null
    }

    override fun toString(): String = "<fn ${declaration.name.lexeme}>"
}