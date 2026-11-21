package com.roy.khelper.server

import com.roy.khelper.model.Problem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class CompanionHttpServerTest {

    private val received = LinkedBlockingQueue<Problem>()
    private val server = CompanionHttpServer(onProblem = { received.add(it) })

    @After
    fun stop() = server.stop()

    @Test
    fun `post with a Competitive Companion payload delivers the problem`() {
        server.start(0)

        val code = request("POST", SAMPLE_PAYLOAD)

        assertEquals(200, code)
        val problem = received.poll(5, TimeUnit.SECONDS)!!
        assertEquals("A. Watermelon", problem.name)
        assertEquals(1000, problem.timeLimit)
        assertEquals(64, problem.memoryLimit)
        assertEquals(listOf("8\n"), problem.tests.map { it.input })
        assertEquals("YES\n", problem.tests.single().output)
    }

    @Test
    fun `invalid json is rejected and delivers nothing`() {
        server.start(0)

        assertEquals(400, request("POST", "not json"))
        assertTrue(received.isEmpty())
    }

    @Test
    fun `get answers as a health check`() {
        server.start(0)
        assertEquals(200, request("GET", null))
    }

    @Test
    fun `answers on both IPv4 and IPv6 loopback, because browsers resolve localhost to either`() {
        server.start(0)

        assertEquals(200, request("GET", null, "127.0.0.1"))
        //~ True when this machine has an IPv6 loopback at all, checked without the server under test
        val ipv6Available = runCatching { ServerSocket(0, 1, InetAddress.getByName("::1")).close() }.isSuccess
        if (ipv6Available) {
            assertEquals(200, request("GET", null, "[::1]"))
        }
    }

    @Test
    fun `stop shuts the server down`() {
        server.start(0)
        assertTrue(server.isRunning)
        server.stop()
        assertFalse(server.isRunning)
    }

    private fun request(method: String, body: String?, host: String = "127.0.0.1"): Int {
        val url = URI("http://$host:${server.port}/").toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = method
        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray()) }
        }
        return connection.responseCode.also { connection.disconnect() }
    }

    private companion object {
        val SAMPLE_PAYLOAD = """
            {
              "name": "A. Watermelon",
              "group": "Codeforces - Codeforces Beta Round 4 (Div. 2 Only)",
              "url": "https://codeforces.com/problemset/problem/4/A",
              "interactive": false,
              "memoryLimit": 64,
              "timeLimit": 1000,
              "tests": [{ "input": "8\n", "output": "YES\n" }],
              "testType": "single",
              "input": { "type": "stdin" },
              "output": { "type": "stdout" },
              "languages": { "java": { "mainClass": "Main", "taskClass": "AWatermelon" } },
              "batch": { "id": "123e67c8-03c6-44a4-a3f9-5918bf3a3e1e", "size": 1 }
            }
        """.trimIndent()
    }
}
