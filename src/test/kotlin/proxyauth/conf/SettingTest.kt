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

/**
 * @author Zeckie
 */
class SettingTest {
    @Test
    fun `setting int`() {
        val setting = Setting(5, Converter.INTEGER, false, "Test number", null, 0, 100)
        assertEquals(setting.currentValue as Int, 5)
        setting.setString("3")
        assertEquals(setting.currentValue as Int, 3)
        assertEquals("3", setting.toUserString())
    }

    @Test
    fun `invalid int`() {
        assertFailsWith<InvalidSettingException> {
            val setting = Setting(5, Converter.INTEGER, false, "Test number", min = 0, max = 100)
            setting.setString("Foo")
        }
    }

    @Test
    fun `invalid yes no`() {
        assertFailsWith<InvalidSettingException> {
            val setting = Setting(null, Converter.YES_NO, false, "Test bool")
            setting.setString("Foo")
        }
    }
}
