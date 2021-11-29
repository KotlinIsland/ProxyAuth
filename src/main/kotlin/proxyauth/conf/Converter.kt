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
package proxyauth.conf

/**
 * Convert between user readable / supplied string, and another type
 *
 * @author Zeckie
 */
sealed class Converter<A> {
    abstract fun fromString(value: String): A
    open fun toString(value: A): String {
        return value.toString()
    }
    object YES_NO : Converter<Boolean>() {
        override fun fromString(value: String): Boolean = when (value.lowercase()) {
            in YES -> true
            in NO -> false
            else -> throw InvalidSettingException("Value should be 'Yes' or 'No'")
        }

        override fun toString(value: Boolean): String =
            if (value) "Yes" else "No"
    }

    object INTEGER : Converter<Int>() {
        override fun fromString(value: String): Int = try {
            value.toInt()
        } catch (nfe: NumberFormatException) {
            throw InvalidSettingException("Value should be an integer", nfe)
        }
    }

    object STRING : Converter<String>() {
        override fun fromString(value: String): String = value
    }
}

private val YES = setOf("yes", "y", "true")
private val NO = setOf("no", "n", "false")
