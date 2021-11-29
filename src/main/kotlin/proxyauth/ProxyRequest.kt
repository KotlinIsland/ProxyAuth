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

import proxyauth.actions.ForwardAction
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.Socket
import java.util.Arrays
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

/**
 * Handles a single request
 *
 * @author Zeckie
 */
class ProxyRequest(val incomingSocket: Socket, val parent: ProxyListener, threads: ThreadGroup?) :
    Thread(threads, "ProxyRequest-" + THREAD_COUNTER.incrementAndGet()) {
    /**
     * http headers received, including the request line
     * Note that for CONNECT requests (e.g. for https connections), this will contain the
     * target hostname and port, but not much else.
     */
    var requestHeaders: List<String>? = null

    /**
     * http response headers received from upstream proxy, including the response line
     * Note that for CONNECT requests (e.g. for https connections), this will just be headers
     * from the proxy, not the target server
     */
    var responseHeaders: List<String>? = null

    /**
     * Timestamp when this request started (when the incoming connection was accepted)
     */
    var started = Date()
    override fun run() {
        var success = false
        try {
            incomingSocket.use {
                println("Accepted connection from: ${incomingSocket.inetAddress} port ${incomingSocket.port}")
                incomingSocket.soTimeout = parent.config.SOCKET_TIMEOUT.value!!
                requestHeaders = processHeaders(incomingSocket.getInputStream())
                success = ForwardAction(
                    InetAddress.getByName(parent.config.UPSTREAM_PROXY_HOST.value),
                    parent.config.UPSTREAM_PROXY_PORT.value!!,
                    parent.config.USERNAME.value, parent.config.PASSWORD.value
                ).action(this)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            parent.finished(this, success)
        }
    }

    fun processHeaders(inputStream: InputStream): List<String> {
        val bufferSize = parent.config.BUF_SIZE.value!!
        val buf = ByteArray(bufferSize)
        var bytesRead = 0

        // read http request headers (ends with 2x CRLF)
        val endHeaders = "\r\n\r\n".toByteArray()
        while (bytesRead < 4 || !Arrays.equals(buf, bytesRead - 4, bytesRead, endHeaders, 0, 4)) {
            val byteRead = inputStream.read()
            if (byteRead == -1) throw IOException("End of stream reached before http request headers read")
            if (bytesRead == bufferSize) throw IOException("Buffer full before http request headers read")
            buf[bytesRead++] = byteRead.toByte()
        }
        if (parent.config.DEBUG.value!!) {
            println("--- Headers ---")
            System.out.write(buf, 0, bytesRead)
            println("--- End: Headers ---")
            System.out.flush()
        }
        return String(buf, 0, bytesRead, ASCII).trim().split("\r\n").toMutableList()
    }

    companion object {
        /**
         * Counter to give threads unique names
         */
        private val THREAD_COUNTER = AtomicLong()
    }
}
