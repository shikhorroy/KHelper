package ktorapp.routes

import ktorapp.config.ServerConfig
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ktorapp.models.PendingRequest
import ktorapp.models.Problem
import ktorapp.services.OutputComparisonService
import ktorapp.services.ProblemService
import ktorapp.services.RequestManager
import ktorapp.services.TestCaseDeleteRequest
import ktorapp.services.TestCaseRequest
import ktorapp.services.TestCaseService
import ktorapp.services.TestCaseUpdateRequest
import java.nio.file.Path
import java.util.*

class ProblemRoutes(
    private val requestManager: RequestManager,
    private val problemService: ProblemService,
    private val comparisonService: OutputComparisonService,
    private val testCaseService: TestCaseService
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun Route.configureProblemRoutes() {

        // Serve main page
        get("/") {
            call.respondText(
                this::class.java.classLoader.getResource("static/index.html")?.readText() ?: "Index not found",
                ContentType.Text.Html
            )
        }

        // Serve other HTML pages
        get("/compare.html") {
            call.respondText(
                this::class.java.classLoader.getResource("static/compare.html")?.readText() ?: "Not found",
                ContentType.Text.Html
            )
        }

        get("/tests.html") {
            call.respondText(
                this::class.java.classLoader.getResource("static/tests.html")?.readText() ?: "Not found",
                ContentType.Text.Html
            )
        }

        get("/test.html") {
            call.respondText(
                this::class.java.classLoader.getResource("static/test.html")?.readText() ?: "Not found",
                ContentType.Text.Html
            )
        }

        // Serve CSS
        get("/styles.css") {
            call.respondText(
                this::class.java.classLoader.getResource("static/styles.css")?.readText() ?: "/* Not found */",
                ContentType.Text.CSS
            )
        }

        // Serve JavaScript files
        get("/main.js") {
            call.respondText(
                this::class.java.classLoader.getResource("static/main.js")?.readText() ?: "// Not found",
                ContentType.Application.JavaScript
            )
        }

        get("/compare.js") {
            call.respondText(
                this::class.java.classLoader.getResource("static/compare.js")?.readText() ?: "// Not found",
                ContentType.Application.JavaScript
            )
        }

        get("/tests.js") {
            call.respondText(
                this::class.java.classLoader.getResource("static/tests.js")?.readText() ?: "// Not found",
                ContentType.Application.JavaScript
            )
        }

        // Health check
        options("/") {
            println("[OPTIONS] Health check received")
            call.respondText("ok")
        }

        // Receive problem data from Competitive Companion
        post("/") {
            val body = call.receiveText()
            val problem = json.decodeFromString<Problem>(body)

            problemService.logProblemReceived(problem)

            // Create a pending request
            val requestId = UUID.randomUUID().toString()
            val deferred = CompletableDeferred<Boolean>()
            val pendingRequest = PendingRequest(requestId, problem, deferred)
            requestManager.addRequest(pendingRequest)

            println("⏳ Waiting for user approval... (Check UI at http://${ServerConfig.LOCALHOST}:${ServerConfig.PORT})")

            // Wait for user decision
            val accepted = deferred.await()

            if (accepted) {
                println("✅ Request ACCEPTED by user")
                problemService.processProblem(problem)
            } else {
                println("❌ Request REJECTED by user")
            }

            println("───────────────────────────────────────────────────────────")
            println("Ready for next problem...\n")

            call.respondText("ok")
        }

        // Accept endpoint
        post("/accept/{id}") {
            val id = call.parameters["id"]
            val pending = requestManager.removeRequest(id ?: "")
            if (pending != null) {
                pending.deferred.complete(true)
                call.respondText("Accepted")
            } else {
                call.respondText("Not found")
            }
        }

        // Reject endpoint
        post("/reject/{id}") {
            val id = call.parameters["id"]
            val pending = requestManager.removeRequest(id ?: "")
            if (pending != null) {
                pending.deferred.complete(false)
                call.respondText("Rejected")
            } else {
                call.respondText("Not found")
            }
        }


        // API endpoint for comparison (JSON)
        get("/api/compare") {
            val result = comparisonService.compareOutputs()
            call.respond(result)
        }


        // Get all test cases (API)
        get("/api/tests") {
            val tests = testCaseService.getAllTestCases()
            call.respond(tests)
        }

        // Add new test case (API)
        post("/api/tests/add") {
            val request = call.receive<TestCaseRequest>()
            val response = testCaseService.addTestCase(request.input, request.output)
            call.respond(HttpStatusCode.OK, response)
        }

        // Update test case (API)
        post("/api/tests/update") {
            val request = call.receive<TestCaseUpdateRequest>()
            val response = testCaseService.updateTestCase(request.testNumber, request.input, request.output)
            call.respond(HttpStatusCode.OK, response)
        }

        // Delete test case (API)
        post("/api/tests/delete") {
            val request = call.receive<TestCaseDeleteRequest>()
            val response = testCaseService.deleteTestCase(request.testNumber)
            call.respond(HttpStatusCode.OK, response)
        }

        // API endpoint for pending requests (for JavaScript frontend)
        get("/api/pending-requests") {
            val requests = requestManager.getAllRequests()
            val serializableRequests = requests.map {
                SerializablePendingRequest(it.id, it.problem)
            }
            val response = PendingRequestsResponse(serializableRequests, requests.size)
            call.respond(response)
        }

        // API endpoint for file content (for comparison page)
        get("/api/file-content/{fileName}") {
            val fileName = call.parameters["fileName"] ?: ""
            try {
                val base = Path.of(System.getProperty("user.dir"))
                val actualFile = base.resolve("output").resolve(fileName)

                if (actualFile.toFile().exists()) {
                    val content = actualFile.toFile().readText()
                    call.respond(FileContentResponse(true, content, ""))
                } else {
                    call.respond(FileContentResponse(false, "", "File not found"))
                }
            } catch (e: Exception) {
                call.respond(FileContentResponse(false, "", e.message ?: "Unknown error"))
            }
        }
    }
}

@Serializable
data class SerializablePendingRequest(
    val id: String,
    val problem: Problem
)

@Serializable
data class PendingRequestsResponse(
    val requests: List<SerializablePendingRequest>,
    val count: Int
)

@Serializable
data class FileContentResponse(
    val success: Boolean,
    val content: String,
    val error: String
)

