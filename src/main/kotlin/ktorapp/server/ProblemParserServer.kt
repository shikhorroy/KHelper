package ktorapp.server

import io.ktor.http.HttpMethod
import ktorapp.config.ServerConfig
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import ktorapp.routes.ProblemRoutes
import ktorapp.services.FileService
import ktorapp.services.MatchedTestTracker
import ktorapp.services.OutputComparisonService
import ktorapp.services.ProblemService
import ktorapp.services.RequestManager
import ktorapp.services.TestCaseService
import ktorapp.services.ArchiveService

class ProblemParserServer {
    private val requestManager = RequestManager()
    private val fileService = FileService()
    private val matchedTestTracker = MatchedTestTracker()
    private val problemService = ProblemService(fileService, matchedTestTracker)
    private val comparisonService = OutputComparisonService(matchedTestTracker)
    private val testCaseService = TestCaseService(fileService)
    private val archiveService = ArchiveService(fileService)
    private val problemRoutes = ProblemRoutes(requestManager, problemService, comparisonService, testCaseService, archiveService, fileService)

    fun start() {
        embeddedServer(Netty, port = ServerConfig.PORT, host = ServerConfig.LOCALHOST) {
            configureContentNegotiation()
            configureCORS()
            configureRouting()

            monitor.subscribe(ServerReady) {
                printWelcomeBanner()
            }
        }.start(wait = true)
    }

    private fun Application.configureContentNegotiation() {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    private fun Application.configureCORS() {
        install(CORS) {
            anyHost()
            allowNonSimpleContentTypes = true
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Get)
        }
    }

    private fun Application.configureRouting() {
        routing {
            with(problemRoutes) {
                configureProblemRoutes()
            }
        }
    }

    private fun printWelcomeBanner() {
        println("═══════════════════════════════════════════════════════════")
        println("   Competitive Programming Problem Parser Server")
        println("═══════════════════════════════════════════════════════════")
        println("Starting server on ${ServerConfig.LOCALHOST}:${ServerConfig.PORT}")
        println("Waiting for problem data from Competitive Companion...")
        println()
    }
}

