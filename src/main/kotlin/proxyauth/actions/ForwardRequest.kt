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
package proxyauth.actions

import proxyauth.ASCII
import proxyauth.PassThrough
import proxyauth.ProxyRequest
import proxyauth.StatusListener
import proxyauth.ascii
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Base64
import kotlin.system.exitProcess

/**
 * Forwards a request to a proxy server
 *
 * @Param proxyRequest The request being forwarded
 * @author Zeckie
 */
class ForwardRequest(
    private val proxyRequest: ProxyRequest,
    private val action: ForwardAction,
) : StatusListener<PassThrough> {
    /**
     * Connection to upstream proxy server
     */
    private var upstreamSocket: Socket? = null

    @Volatile
    private var anyErrors = false

    /**
     * @param headers HTTP request headers
     * @return a copy of headers, with the configured proxy authorization
     */
    private fun processAuthHeaders(headers: List<String>): List<String> =
        headers.filterNot { it.lowercase().startsWith("proxy-authorization:") } + (
            "Proxy-Authorization: Basic " + Base64.getEncoder()
                .encode((action.username + ":" + action.password).ascii()).toString(ASCII)
            )

    /**
     * @param headers
     * @return a copy of headers, modified to stop keep-alive
     */
    private fun processKeepAlive(headers: List<String>): List<String> = headers
        .filterNot { it.lowercase().startsWith("connection:") }
        .filterNot { it.lowercase().startsWith("keep-alive:") } + "Connection: Close"

    fun go(): Boolean {
        val upload: PassThrough
        val download: PassThrough
        val config = proxyRequest.parent.config
        Socket().use { upstream ->
            upstream.soTimeout = config.SOCKET_TIMEOUT.value!!
            upstream.connect(
                InetSocketAddress(action.host, action.port),
                config.SOCKET_TIMEOUT.value!!
            )
            upstreamSocket = upstream
            val outputStream =
                BufferedOutputStream(upstream.getOutputStream(), config.BUF_SIZE.value!!)
            if (config.DEBUG.value!!) println("upstream socket = $upstream")
            var headers = processAuthHeaders(proxyRequest.requestHeaders!!)
            if (config.CONNECTION_CLOSE.value!!) {
                headers = processKeepAlive(headers)
            }
            upload = PassThrough(
                this, proxyRequest.incomingSocket.getInputStream(), outputStream, upstream,
                true, headers, config
            )
            upload.start()
            proxyRequest.responseHeaders = proxyRequest.processHeaders(upstream.getInputStream())
            if (config.STOP_ON_PROXY_AUTH_ERROR.value!!) {
                val line = proxyRequest.responseHeaders!![0]
                if (PROXY_AUTH_ERROR matches line) {
                    System.err.println("STOPPING due to proxy auth error: $line")
                    exitProcess(5) // magic number 5 often = access denied
                    /*
                     * TODO: change to respond to all requests with error page,
                     * instead of quitting
                     */
                }
            }
            val respHeaders =
                if (config.CONNECTION_CLOSE.value!!) processKeepAlive(proxyRequest.responseHeaders!!)
                else proxyRequest.responseHeaders
            download = PassThrough(
                this, upstream.getInputStream(),
                BufferedOutputStream(
                    proxyRequest.incomingSocket.getOutputStream(), config.BUF_SIZE.value!!
                ),
                proxyRequest.incomingSocket, false, respHeaders, config
            )
            download.start()
            try {
                // Wait for streams to be closed
                upload.join()
                download.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        synchronized(this) {
            println(Thread.currentThread().toString() + " Finished")
            if (proxyRequest.parent.config.DEBUG.value!!) println(
                """--Finished--
                 - any errors: $anyErrors
                 - request: ${proxyRequest.requestHeaders!![0]}
                 - upload: ${upload.bytesTransferred.get()}
                 - download: ${download.bytesTransferred.get()}
                 - elapsed: ${System.currentTimeMillis() - proxyRequest.started.time}
                """.trimIndent()
            )
            return !anyErrors
        }
    }

    @Synchronized
    override fun finished(source: PassThrough, succeeded: Boolean) {
        if (!succeeded) {
            anyErrors = true

            // Close both sockets
            upstreamSocket?.let {
                try {
                    it.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            try {
                proxyRequest.incomingSocket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}

private val PROXY_AUTH_ERROR = Regex("^HTTP/\\d.\\d 407 .*")
