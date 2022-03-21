import java.io.File
import kotlin.system.exitProcess

class Lox {
    companion object {
        var hadError = false
        var hadRuntimeError = false

        fun error(line: Int, col: Int, message: String) = report(line, col, "", message)
        fun error(token: Token, message: String) {
            val where = if (token.type == TokenType.EOF) "end" else "'${token.lexeme}'"
            report(token.line, token.col, " at $where", message)
        }
        fun runtimeError(e: RuntimeError) {
            System.err.println("${e.token.line}:${e.token.col}: RuntimeError: ${e.message ?: ""}")
        }
        private fun report(line: Int, col: Int, where: String, message: String) {
            System.err.println("$line:$col: Error$where: $message")
            hadError = true
        }
    }

    fun runFile(fileName: String) {
        val input = File(fileName).readText()
        execute(input)
        if (hadError) exitProcess(65)
        if (hadRuntimeError) exitProcess(70)
    }

    fun runPrompt() {
        while (true) {
            print("> ")
            execute(readLine() ?: break)
            hadError = false
        }
    }

    private fun execute(input: String) {
        val tokens = Scanner(input).scanTokens()
        val statements = Parser(tokens).parse()
        if (hadError) return
        val interpreter = Interpreter()
        Resolver(interpreter).resolve(statements)
        if (hadError) return
        interpreter.interpret(statements)
    }
}