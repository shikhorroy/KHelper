package com.roy.khelper.server

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.roy.khelper.model.Problem
import com.roy.khelper.server.listeners.ProblemListener
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

@Service(Service.Level.APP)
class CompetitiveCompanionServer {

    private val logger = logger<CompetitiveCompanionServer>()
    private var server: EmbeddedServer<*, *>? = null
    private val listeners = mutableListOf<ProblemListener>()

    companion object {
        const val DEFAULT_PORT = 10043

        fun getInstance(): CompetitiveCompanionServer {
            return ApplicationManager.getApplication().getService(CompetitiveCompanionServer::class.java)
        }
    }

    fun start(port: Int = DEFAULT_PORT) {
        if (server != null) {
            logger.warn("Server is already running")
            return
        }

        try {
            server = embeddedServer(Netty, port = port) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }

                routing {
                    post {
                        try {
                            val problem = call.receive<Problem>()
                            logger.info("Received problem: ${problem.name}")

                            // Notify all listeners
                            notifyListeners(problem)

                            call.respond(mapOf("status" to "ok"))
                        } catch (e: Exception) {
                            logger.error("Error processing problem", e)
                            call.respond(mapOf("status" to "error", "message" to e.message))
                        }
                    }

                    get("/health") {
                        call.respond(mapOf("status" to "healthy"))
                    }
                }
            }.start(wait = false)

            logger.info("Competitive Companion server started on port $port")
        } catch (e: Exception) {
            logger.error("Failed to start server", e)
            throw e
        }
    }

    @Suppress("unused")
    fun stop() {
        server?.stop(1000, 2000)
        server = null
        logger.info("Competitive Companion server stopped")
    }

    fun addListener(listener: ProblemListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: ProblemListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(problem: Problem) {
        listeners.forEach { listener ->
            try {
                listener.onProblemReceived(problem)
            } catch (e: Exception) {
                logger.error("Error notifying listener", e)
            }
        }
    }

    fun isRunning(): Boolean = server != null
}
