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
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.lang.reflect.Modifier
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.file.Paths
import java.util.Properties
import kotlin.collections.HashMap
import kotlin.io.path.notExists

/**
 * Manages all user configurable settings for ProxyAuth
 *
 * @author Zeckie
 */
object Configuration {
    /* Tuning */
    val BUF_SIZE = Setting(
        1024, Converter.INTEGER,
        false, "Size of each buffer, in bytes.", min = 100
    )
    val DEBUG = Setting(
        true, Converter.YES_NO,
        false, "Print debug details?"
    )
    val SOCKET_TIMEOUT = Setting(
        180000, Converter.INTEGER,
        false, "Timeout in milliseconds used when connecting, reading and writing to TCP sockets",
        min = 0,
    )
    val LISTEN_BACKLOG = Setting(
        50,
        Converter.INTEGER,
        false,
        "Number of incoming connections that can be queued. Setting this too low will result in connections being refused",
        min = 0,
    )
    val STOP_ON_PROXY_AUTH_ERROR = Setting(
        true,
        Converter.YES_NO,
        false,
        "Immediately stop on http error 407, to prevent account from being locked due to multiple attempts with wrong password",
    )
    val MAX_ACTIVE_REQUESTS = Setting(
        20,
        Converter.INTEGER,
        false,
        "The number of concurrent requests that can be processed. Higher values will use more resources.",
        min = 1,
    )
    val CONNECTION_CLOSE = Setting(
        true, Converter.YES_NO, false,
        "Add headers to indicate the connection needs to be closed. Should be set to Yes to work around issue 23.",
    )

    /* Addresses */
    val LISTEN_ADDRESS = Setting(
        "127.0.0.127",
        Converter.STRING,
        false,
        "Local IP address to listen on. Use a loopback address 127.* to make proxy only accessible to processes running locally",
        { value ->
            try {
                // Check that the specified address is local
                ServerSocket(0, 0, InetAddress.getByName(value))
            } catch (e: IOException) {
                throw InvalidSettingException("Not able to listen on specified address - $e")
            }
        },
    )
    val LISTEN_PORT = Setting(
        8080, Converter.INTEGER,
        false, "TCP port to listen on. Port 8080 is often used.", min = 0, max = 65535
    )
    val UPSTREAM_PROXY_HOST = Setting(
        null, Converter.STRING,
        false, "Name or IP address of the upstream proxy server to send requests to"
    )
    val UPSTREAM_PROXY_PORT = Setting(
        8080, Converter.INTEGER,
        false, "TCP Port of upstream proxy server to send requests to", min = 1, max = 65535
    )

    /* Authentication - these 3 are handled slightly differently */
    val USERNAME = Setting(
        System.getenv("USERNAME"), Converter.STRING,
        true, "Username for authenticating to upstream proxy server"
    )
    val PASSWORD = Setting(
        null, Converter.STRING,
        true, "Password for authenticating to upstream proxy server"
    )
    val SAVE_PASS = Setting(
        false, Converter.YES_NO,
        true, "Save proxy username and password to configuration file?"
    )
    val FILE_NAME = "proxyauth.properties"

    /**
     * Intended for internal use only, such as loading configuration.
     * Lists all configuration fields (i.e. all final fields of type Configuration)
     */
    val allConfigFields: Map<String, Setting<*>>
        get() {
            val allProps: MutableMap<String, Setting<*>> = HashMap()
            for (f in Configuration::class.java.declaredFields) {
                val modifiers = f.modifiers
                if (Modifier.isFinal(modifiers) && f.type == Setting::class.java) {
                    try {
                        val c = f[this] as? Setting<*>
                            ?: throw IllegalStateException("Configuration field " + f.name + " is null")
                        allProps[f.name] = c
                    } catch (e: IllegalAccessException) {
                        throw IllegalStateException(e)
                    }
                }
            }
            return allProps
        }

    private fun load(required: Boolean) {
        val configFile = Paths.get(FILE_NAME)
        if (configFile.notExists() && required) {
            throw FileNotFoundException("Configuration file $FILE_NAME not found")
        }
        val props = Properties().apply { load(FileReader(FILE_NAME)) }
        val allConfigFields = allConfigFields
        for (key in props.stringPropertyNames()) {
            try {
                allConfigFields[key]?.setString(props.getProperty(key))
                    ?: System.err.println("Discarding unknown setting from properties file: $key")
            } catch (ex: Exception) {
                System.err.println("Unable to load $key: $ex")
            }
        }
    }

    private fun save() {
        val allConfigFields = allConfigFields
        val props = Properties()
        for ((key, setting) in allConfigFields) {
            if (setting.currentValue != null && (!setting.special || SAVE_PASS.value!!)) {
                props.setProperty(key, setting.toUserString())
            } else {
                println("Skip save: $key")
            }
        }
        props.store(FileWriter(FILE_NAME), "")
    }

    fun init(doLoad: Boolean, doSave: Boolean, doWizard: Boolean, quiet: Boolean, con: Console) {
        val allConfigFields = allConfigFields
        if (doLoad) load(false)
        if (!quiet) {
            run {
                for ((key, config) in allConfigFields) {
                    if ((config.currentValue == null || doWizard) && !config.special) {
                        config.prompt(key, con)
                    }
                }
                if (USERNAME.value == null || PASSWORD.value == null || doWizard) {
                    val currentUser = USERNAME.currentValue
                    if (currentUser == null) {
                        println("Username:")
                    } else {
                        println("Username (press ENTER for '$currentUser'):")
                    }
                    USERNAME.value = con.readLine().ifEmpty { currentUser ?: "" }
                    println("Password (masked)")
                    PASSWORD.value = String(con.readPassword())
                    if (doSave) {
                        println("Configuration file location: " + File(FILE_NAME).canonicalPath)
                        SAVE_PASS.prompt("SAVE_PASS", con)
                    }
                }
                if (doSave) save()
            }
        }

        // Check that all setting have values (e.g. if running in quiet mode)
        for ((key, value) in allConfigFields) {
            value.value ?: throw InvalidSettingException("Setting $key is not set")
        }
    }
}
