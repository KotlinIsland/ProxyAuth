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

import proxyauth.conf.Configuration
import java.io.Closeable
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket

/**
 * Listen for requests and create threads to handle them
 *
 * @author Zeckie
 */
class ProxyListener(val config: Configuration) : Runnable, StatusListener<ProxyRequest>, Closeable {
    /**
     * requests that have been accepted but not finished
     */
    private val activeRequests: MutableSet<ProxyRequest> = LinkedHashSet()

    @Volatile
    private var incoming: ServerSocket? = null
    override fun run() {
        try {
            ServerSocket(
                config.LISTEN_PORT.value!!,
                config.LISTEN_BACKLOG.value!!,
                InetAddress.getByName(config.LISTEN_ADDRESS.value)
            ).use { incoming ->
                this.incoming = incoming
                println("Listening $incoming")
                while (true) {
                    val sock = incoming.accept()
                    val proxyRequest = ProxyRequest(sock, this, THREADS)
                    synchronized(activeRequests) {
                        activeRequests.add(proxyRequest)
                        activeRequests.notifyAll()
                        proxyRequest.start()
                        while (activeRequests.size >= config.MAX_ACTIVE_REQUESTS.value!!) {
                            println("Active request limit reached - waiting for a request to finish")
                            activeRequests.wait()
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun finished(source: ProxyRequest, succeeded: Boolean) {
        synchronized(activeRequests) {
            activeRequests.remove(source)
            println(
                """Finished $source success=$succeeded
Active requests:${activeRequests.size}
Active threads:${THREADS.activeCount()}"""
            )
            if (config.DEBUG.value!!) {
                THREADS.list()
            }
            activeRequests.notifyAll()
        }
    }

    /**
     * currently used only for testing - close the listening socket
     */
    override fun close() {
        incoming?.close()
    }

    val localPort: Int?
        get() = incoming?.localPort

    companion object {
        /**
         * ThreadGroup containing threads that we start
         */
        private val THREADS = ThreadGroup("proxyauth")
    }
}
