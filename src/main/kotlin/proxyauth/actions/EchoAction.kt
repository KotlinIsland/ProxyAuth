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
import proxyauth.ProxyRequest
import java.io.OutputStreamWriter
import java.io.PrintWriter

/**
 * Treat the incoming request as a HTTP get, and reply by echoing the headers
 *
 * @author Zeckie
 */
class EchoAction : Action {
    override fun action(proxyRequest: ProxyRequest): Boolean {
        val pw = PrintWriter(OutputStreamWriter(proxyRequest.incomingSocket.getOutputStream(), ASCII), false)
        val requestLine = proxyRequest.requestHeaders!![0]
        val success = requestLine.startsWith("GET ")
        pw.println(
            if (success)
                """
    HTTP/1.1 200 Echoing your request
    Content-Type: text/plain
    Connection: close
    
    Received request + headers:
                """.trimIndent()
            else
                """
    HTTP/1.1 501 Not Implemented
    Content-Type: text/plain
    Connection: close
    
    
                """.trimIndent()
        )
        proxyRequest.requestHeaders!!.forEach(pw::println)
        pw.flush()
        pw.close()
        return success
    }
}
