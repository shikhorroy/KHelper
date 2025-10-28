import ktorapp.server.ProblemParserServer

/**
 * Main entry point for the Problem Parser Server.
 * This application receives competitive programming problems from Competitive Companion
 * browser extension and saves test cases to local files for testing.
 */
fun main() {
    val server = ProblemParserServer()
    server.start()
}