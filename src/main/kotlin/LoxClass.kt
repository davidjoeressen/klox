class LoxClass(
    val name: String,
    private val superclass: LoxClass?,
    private val methods: Map<String, LoxFunction>
) : LoxCallable {
    fun findMethod(name: String): LoxFunction? = methods[name] ?: superclass?.findMethod(name)

    override val arity: Int = findMethod("init")?.arity ?: 0

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? = LoxInstance(this)
        .also { instance -> findMethod("init")?.bind(instance)?.call(interpreter, arguments) }

    override fun toString(): String = name
}