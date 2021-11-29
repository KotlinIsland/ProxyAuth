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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicLong

/**
 * Transfers all bytes from input to output, flushing as required to keep things moving
 *
 * @author Zeckie
 * Write the supplied http headers, then copy all bytes from input to output
 *
 * @param listener         Listener to notify when finished
 * @param inputStream      stream to read bytes from
 * @param os               stream to write bytes to
 * @param toShutdownOutput socket to shut down output of when done
 * @param isUp             direction (is this uploading?)
 * @param headers          list of http headers
 * @param config           configuration
 */
class PassThrough(
    var listener: StatusListener<PassThrough>,
    var inputStream: InputStream,
    var os: OutputStream,
    private val toShutdownOutput: Socket?,
    isUp: Boolean,
    val headers: List<String>?,
    val config: Configuration,
) : Thread("PassThrough-" + THREAD_COUNTER.incrementAndGet() + if (isUp) "-up" else "-down") {
    var bytesTransferred = AtomicLong(0)
    override fun run() {
        println(currentThread().toString() + " Started")
        var succeeded = true
        try {
            try {
                // Send the headers, followed by blank line
                headers?.joinToString("\r\n", postfix = "\r\n\r\n")?.ascii()?.let(os::write)

                // transfer remaining bytes (eg. body)
                while (true) {
                    if (inputStream.available() == 0) os.flush()
                    val nxt = inputStream.read()
                    if (nxt == -1) {
                        toShutdownOutput?.shutdownOutput()
                        println(currentThread().toString() + " Finished. Bytes=" + bytesTransferred.get())
                        return
                    }
                    bytesTransferred.incrementAndGet()
                    os.write(nxt)
                }
            } catch (se: SocketException) {
                /* Fairly common - e.g. when either side closes the connection with TCP reset.
                    However, we need to make sure we clean up any resources, such as other sockets.
                 */
                succeeded = false
                os.close()
                inputStream.close()
                println(currentThread().toString() + " SocketException -> closed. Bytes=" + bytesTransferred.get())
                if (config.DEBUG.value!!) se.printStackTrace()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            listener.finished(this, succeeded)
        }
    }

    companion object {
        val THREAD_COUNTER = AtomicLong(0)
    }
}
