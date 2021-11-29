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
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.system.exitProcess

/**
 * A simple proxy server to authenticate to another proxy server.
 *
 * @author Zeckie
 */
object Main {
    /**
     * ProxyAuth entry point
     */
    fun main(args: Array<String>) {
        val argErrs = parseArgs(args)
        if (!OPT_QUIET.set) {
            println(COPYRIGHT)
        }
        if (OPT_HELP.set) {
            println("\nProxyAuth supports the following command line switches:\n")
            for (opt in OPTIONS) {
                println(opt.helpMsg)
            }
            println(LONG_HELP)
            exitProcess(if (argErrs) -1 else 0)
        } else {
            println(SHORT_HELP)
        }
        if (OPT_LICENCE.set) {
            println("-- Start: LICENCE --")
            Main::class.java.getResourceAsStream("LICENSE").use { licence -> licence.transferTo(System.out) }
            println("-- End: LICENCE --")
            exitProcess(0)
        }
        if (OPT_QUIET.set && OPT_WIZARD.set) {
            System.err.println("WARN: wizard option will be ignored as quiet option specified")
            OPT_WIZARD.set = false
        }
        val con = System.console()
        if (con == null && !OPT_NO_CONSOLE.set && !OPT_QUIET.set) {
            System.err.println(
                """
    No console detected.
    ProxyAuth should be run from command prompt / interactive console, or with /quiet switch.
                """.trimIndent()
            )
            if (launchInteractive()) return
        }
        val configuration = Configuration
        configuration.init(!OPT_RESET.set, !OPT_NO_SAVE.set, OPT_WIZARD.set, OPT_QUIET.set, con)
        ProxyListener(configuration).run()
    }

    /**
     * Try to launch ProxyAuth in an interactive console
     */
    private fun launchInteractive(): Boolean {
        val os = System.getProperty("os.name")
        if (os.lowercase().startsWith("windows")) {
            // On Windows, try launching in a new command prompt
            val java = System.getProperty("java.home")
            val javaExe = Paths.get(java, "bin", "java.exe")
            val comspec: String = System.getenv("comspec")
            if (javaExe.exists() && Paths.get(comspec).exists()) {
                // Get classpath from path to main class
                val main = Main::class.java.canonicalName
                val mainFilename = main.replace(".", "/") + ".class"
                val classPath: String =
                    ClassLoader.getSystemResource(main.replace(".", "/") + ".class")
                        .path.replace("^/|^file:/|!?/?$mainFilename$".toRegex(), "")
                val cmd = arrayOf(
                    comspec, "/c", "start", "\"ProxyAuth\"", comspec, "/k", javaExe.normalize().absolutePathString(),
                    "-cp", classPath, main, "noconsole"
                )
                println("\nLaunching:${cmd.contentToString()}")
                Runtime.getRuntime().exec(cmd)
                return true
            }
        }
        return false
    }

    private fun parseArgs(args: Array<String>): Boolean {
        var err = false
        for (arg in args) {
            var found = false
            val cleaned = Option.clean(arg)
            for (opt in OPTIONS) {
                if (cleaned in opt.args) {
                    opt.set()
                    found = true
                    break
                }
            }
            if (!found) {
                System.err.println("ERROR: Unrecognised switch: $arg")
                OPT_HELP.set()
                err = true
            }
        }
        return err
    }

    private const val COPYRIGHT = """---
ProxyAuth (https://github.com/Zeckie/ProxyAuth) is a simple http proxy
server to authenticate to and forward requests to an upstream proxy server.
ProxyAuth is Copyright (c) 2021 Zeckie

ProxyAuth is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free 
Software Foundation, version 3.

ProxyAuth is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.

You should have received a copy of the GNU General Public License along
with ProxyAuth. If you have the source code, this is in a file called
LICENSE. If you have the built jar file, the licence can be viewed by
running "java -jar ProxyAuth-<version>.jar licence".
Otherwise, see <https://www.gnu.org/licenses/>.
---
"""
    private const val SHORT_HELP = "For help (including configuration and licence) run with command line switch /? or --help"
    private const val LONG_HELP = "\nFor more, see https://github.com/Zeckie/ProxyAuth\n"
    private val OPT_HELP = Option("Displays this list of commands (then quits)", "help", "" /* eg. for "-?" or "/?" */, "h")
    private val OPT_LICENCE = Option(
        "Display copyright and licence (then quits)", "licence", "license", "l",
        "copyright", "copyleft", "gpl"
    )
    private val OPT_WIZARD = Option("Runs configuration wizard", "wizard", "config", "w")
    private val OPT_RESET = Option("Skips loading of configuration file", "reset", "noload", "r")
    private val OPT_NO_SAVE = Option("Skips saving of configuration file", "nosave", "n")
    private val OPT_QUIET = Option(
        "Disables console input and most console output. Will quit if required settings not configured.",
        "quiet",
        "q"
    )
    private val OPT_NO_CONSOLE =
        Option("Skips attempt to launch interactive console if launched non-interactively", "noconsole")

    // TODO: implement OPT_LICENCE
    // TODO: use OPT_QUIET more
    private val OPTIONS = listOf(OPT_HELP, OPT_LICENCE, OPT_WIZARD, OPT_RESET, OPT_NO_SAVE, OPT_QUIET, OPT_NO_CONSOLE)
}

/**
 * @param helpMsg description of this option to be displayed when user requests help
 * @param args    clean versions of command line arguments that trigger this option
 */
class Option(helpMsg: String, vararg args: String) {
    val args: Set<String> = args.toSet()
    val helpMsg: String = args[0] + " - " + helpMsg
    var set = false

    fun set() {
        set = true
    }

    companion object {
        /**
         * Clean up argument by converting to lowercase and remove non-alphanumeric chars,
         * so windows ("/foo"), *nix ("--foo") or bare ("foo") format can be used
         *
         * @param arg
         * @return cleaned version
         */
        fun clean(arg: String): String {
            return arg.lowercase().replace("\\W".toRegex(), "")
        }
    }
}
