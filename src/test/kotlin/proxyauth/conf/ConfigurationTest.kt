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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ConfigurationTest {
    var config = Configuration

    @Test
    fun `get all config fields`() {
        val allConfigFields = config.allConfigFields
        println(allConfigFields)
        assertSame(allConfigFields["PASSWORD"], config.PASSWORD)
    }

    @Test
    fun `valid listen address`() {
        config.LISTEN_ADDRESS.setString("127.0.0.1")
        assertEquals(config.LISTEN_ADDRESS.value, "127.0.0.1")
    }

    @Test
    fun `invalid listen address`() {
        // This should not be a local address
        assertFailsWith<InvalidSettingException> { config.LISTEN_ADDRESS.setString("1.1.1.1") }
    }
}
