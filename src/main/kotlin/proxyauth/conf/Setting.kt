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

import java.io.Console

/**
 * Configuration settings.
 *
 * @author Zeckie
 * @param defaultValue  Value that is used unless overridden by user. If null, user will be prompted for a value.
 * @param converter     Converter to convert between Strings and the configuration's class
 * @param special       Is this setting special (different handling for wizard, load, save)
 * @param description   Description that is displayed to the user when prompting for a value
 * @param validator     Validator to check that the (user) supplied string is valid.
 * @param min           Minimum value - values less than this will be rejected
 * @param max           Maximum value - values greater than this will be rejected
*/
class Setting<A : Any>(
    private val defaultValue: A?,
    private val converter: Converter<A>,
    val special: Boolean,
    private val description: String,
    private val validator: ((String?) -> Unit)? = null,
    val min: Comparable<A>? = null,
    val max: Comparable<A>? = null,
) {
    var currentValue: A? = null

    /**
     * Ask the user to supply value for the setting
     */
    fun prompt(name: String, con: Console) {
        while (true) {
            val def = if (currentValue != null) " (press ENTER for " + converter.toString(
                currentValue!!
            ) + ")" else ""
            println("\n\n$name: $description\n\nEnter $name$def:")
            val read: String = con.readLine()

            // Accepted current value
            if (read == "" && currentValue != null) return
            try {
                setString(read)
            } catch (ex: Exception) {
                System.err.println(ex)
                continue
            }
        }
    }

    fun setString(s: String?) {
        validator?.invoke(s)
        value = converter.fromString(s!!)
    }

    /**
     * @return the current value of this setting, or null if it has not been set
     */
    var value: A?
        get() = currentValue
        set(newValue) {
            if (min != null && min > newValue!!) {
                throw InvalidSettingException("less than minimum ($min)")
            }
            if (max != null && max < newValue!!) {
                throw InvalidSettingException("greater than minimum ($max)")
            }
            currentValue = newValue
        }
    init {
        value = defaultValue
    }
    override fun toString(): String {
        return "[Configuration val=$currentValue, default=$defaultValue, description=$description]"
    }

    fun toUserString(): String {
        return converter.toString(currentValue!!)
    }
}

internal class InvalidSettingException : RuntimeException {
    constructor (message: String) : super(message)
    constructor (message: String, cause: Exception) : super(message, cause)
}
