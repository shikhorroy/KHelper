package com.roy.khelper.server

import com.roy.khelper.model.Problem
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.json.Json
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A small HTTP server for the Competitive Companion browser extension. It listens on the loopback addresses only,
 * so other machines on the network cannot push files into the project.
 *
 * Browsers send to "localhost", which can resolve to IPv4 127.0.0.1 or IPv6 ::1, so the server listens on both.
 *
 * It uses no IntelliJ classes, so plain unit tests can start it on port 0.
 */
class CompanionHttpServer(
    private val onProblem: (Problem) -> Unit,
    private val onBadRequest: (Exception) -> Unit = {},
) {

    //~ One server per loopback address: IPv4 first, then IPv6 when the machine has it
    private val servers = mutableListOf<HttpServer>()
    private var executor: ExecutorService? = null

    val isRunning: Boolean
        @Synchronized get() = servers.isNotEmpty()

    //~ The bound port. Useful when started on port 0.
    val port: Int
        @Synchronized get() = servers.firstOrNull()?.address?.port ?: -1

    /** Starts the server. Throws [java.io.IOException] when the port is already in use on IPv4. */
    @Synchronized
    fun start(port: Int) {
        check(servers.isEmpty()) { "Server is already running" }

        val pool = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "KHelper Competitive Companion server").apply { isDaemon = true }
        }
        try {
            val ipv4 = bind(InetAddress.getByName("127.0.0.1"), port, pool)
            servers += ipv4
            // IPv6 is optional: a machine without it, or with ::1 taken, still works through IPv4.
            runCatching { bind(InetAddress.getByName("::1"), ipv4.address.port, pool) }
                .onSuccess { servers += it }
        } catch (e: Exception) {
            servers.forEach { it.stop(0) }
            servers.clear()
            pool.shutdownNow()
            throw e
        }
        executor = pool
    }

    private fun bind(address: InetAddress, port: Int, pool: ExecutorService): HttpServer =
        HttpServer.create(InetSocketAddress(address, port), 0).apply {
            executor = pool
            createContext("/") { exchange -> exchange.use { handle(it) } }
            start()
        }

    @Synchronized
    fun stop() {
        servers.forEach { it.stop(0) }
        servers.clear()
        executor?.shutdownNow()
        executor = null
    }

    private fun handle(exchange: HttpExchange) {
        if (exchange.requestMethod != "POST") {
            respond(exchange, 200, """{"status":"healthy"}""")
            return
        }

        val problem = try {
            parse(exchange.requestBody.readBytes().toString(Charsets.UTF_8))
        } catch (e: Exception) {
            respond(exchange, 400, """{"status":"error"}""")
            onBadRequest(e)
            return
        }

        respond(exchange, 200, """{"status":"ok"}""")
        onProblem(problem)
    }

    private fun respond(exchange: HttpExchange, code: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        fun parse(body: String): Problem = json.decodeFromString(Problem.serializer(), body)
    }
}
