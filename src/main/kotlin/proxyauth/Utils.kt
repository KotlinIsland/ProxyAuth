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

/**
 * @author Zeckie
 */
val ASCII = Charsets.US_ASCII

/**
 * Convert String to raw ascii bytes
 */
fun String.ascii(): ByteArray = toByteArray(ASCII)

fun Any.notifyAll() {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    (this as Object).notifyAll()
}
fun Any.wait() {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    (this as Object).wait()
}
