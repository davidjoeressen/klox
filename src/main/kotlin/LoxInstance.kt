class LoxInstance(private val klass: LoxClass) {
    private val fields = mutableMapOf<String, Any?>()

    fun get(name: Token): Any? =
        fields[name.lexeme]
        ?: klass.findMethod(name.lexeme)?.let { it.bind(this) }
        ?: throw RuntimeError(name, "Undefined property '${name.lexeme}'.")

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }

    override fun toString(): String = "${klass.name} instance"
}