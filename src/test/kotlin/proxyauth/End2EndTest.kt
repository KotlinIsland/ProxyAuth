/*
 * This file is part of ProxyAuth - https://github.com/Zeckie/ProxyAuth
 * ProxyAuth is Copyright (c) 2021 Zeckie
 *
 * ProxyAuth is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * ProxyAuth is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with ProxyAuth. If you have the source code, this is in a file called
 * LICENSE. If you have the built jar file, the licence can be viewed by
 * running "java -jar ProxyAuth-<version>.jar licence".
 * Otherwise, see <https://www.gnu.org/licenses/>.
 */
package proxyauth

import org.junit.jupiter.api.Timeout
import proxyauth.conf.Configuration
import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests multiple components, through use of mock proxy server
 */
class End2EndTest {
    private fun doE2ETest(
        close: Boolean,
        request: String,
        expectedRequest: String,
        response: String,
        expectedResponse: String,
    ) {
        /* Use mostly default configuration, override values important to test */
        val dummy = Configuration
        dummy.UPSTREAM_PROXY_HOST.value = "127.0.1.1"
        dummy.LISTEN_ADDRESS.value = "127.0.1.2"
        dummy.LISTEN_PORT.value = 0 // Ephemeral port
        dummy.USERNAME.value = "foo"
        dummy.PASSWORD.value = "bar"
        dummy.CONNECTION_CLOSE.value = close
        val toClose = ArrayList<Closeable>()
        try {
            // Start mock proxy listening
            val serverSocket = ServerSocket(
                0, 1,
                InetAddress.getByName(dummy.UPSTREAM_PROXY_HOST.value)
            )
            dummy.UPSTREAM_PROXY_PORT.value = serverSocket.localPort
            toClose.add(serverSocket)

            // Start ProxyListener
            val listener = ProxyListener(dummy)
            toClose.add(listener)
            val proxyThread = Thread(listener)
            proxyThread.start()
            while (listener.localPort == null) Thread.sleep(100)

            // Connect to ProxyListener
            val clientSocket = Socket(
                dummy.LISTEN_ADDRESS.value, listener.localPort!!
            )
            toClose.add(clientSocket)
            val clientOutputStream = clientSocket.getOutputStream()
            toClose.add(clientOutputStream)

            // Sent request to ProxyListener
            // TODO: multiple threads to handle blocking, timeouts etc
            clientOutputStream.write(request.toByteArray(ASCII))
            clientOutputStream.flush()
            clientSocket.shutdownOutput()

            // Accept connection using mock proxy
            val acceptedSocket = serverSocket.accept()
            toClose.add(acceptedSocket)
            var received = String(acceptedSocket.getInputStream().readAllBytes(), ASCII)
            assertEquals(expectedRequest, received)

            // Send response back
            val acceptedSocketOutputStream = acceptedSocket.getOutputStream()
            acceptedSocketOutputStream.write(response.toByteArray(ASCII))
            acceptedSocketOutputStream.flush()
            acceptedSocket.shutdownOutput()

            // Verify response
            received = String(clientSocket.getInputStream().readAllBytes(), ASCII)
            assertEquals(expectedResponse, received)
        } finally {
            System.err.println("Cleanup")
            toClose.forEach(Closeable::close)
        }
    }

    @Test
    @Timeout(10000L)
    fun `end to end test with close`() {
        doE2ETest(
            true,
            "FOO http://bar/ HTTP/1.1\r\nBaz: 1\r\n\r\n",
            "FOO http://bar/ HTTP/1.1\r\nBaz: 1\r\nProxy-Authorization: Basic Zm9vOmJhcg==\r\nConnection: Close\r\n\r\n",
            "HTTP/1.1 123 Foo\r\nBar\r\n\r\n",
            "HTTP/1.1 123 Foo\r\nBar\r\nConnection: Close\r\n\r\n"
        )
    }

    @Test
    @Timeout(10000L)
    fun `end to end test without close`() {
        doE2ETest(
            false,
            "FOO http://bar/ HTTP/1.1\r\nBaz: 1\r\n\r\n",
            "FOO http://bar/ HTTP/1.1\r\nBaz: 1\r\nProxy-Authorization: Basic Zm9vOmJhcg==\r\n\r\n",
            "HTTP/1.1 123 Foo\r\nBar\r\n\r\n",
            "HTTP/1.1 123 Foo\r\nBar\r\n\r\n"
        )
    }

    companion object {
        val ASCII = Charsets.US_ASCII
    }
}
