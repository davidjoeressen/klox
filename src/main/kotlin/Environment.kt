class Environment(private val enclosing: Environment? = null) {
    private val values = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun get(name: Token): Any? = when {
        name.lexeme in values -> values[name.lexeme]
        enclosing != null -> enclosing.get(name)
        else -> throw RuntimeError(name, "Undefined variable ${name.lexeme}.")
    }

    fun getAt(distance: Int, name: String): Any? = ancestor(distance)?.values?.get(name)

    fun assign(name: Token, value: Any?) {
        when {
            name.lexeme in values -> values[name.lexeme] = value
            enclosing != null -> enclosing.assign(name, value)
            else -> throw RuntimeError(name, "Undefined variable ${name.lexeme}.")
        }
    }

    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance)?.values?.put(name.lexeme, value)
    }

    private fun ancestor(distance: Int): Environment? {
        var environment: Environment? = this
        repeat(distance) { environment = environment?.enclosing }
        return environment
    }
}