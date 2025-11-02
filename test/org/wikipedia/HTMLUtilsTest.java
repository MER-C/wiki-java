/**
 *  @(#)HTMLUtilsTest.java 0.01 26/10/2025
 *  Copyright (C) 2025 - 20xx MER-C
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version. Additionally
 *  this file is subject to the "Classpath" exception.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.wikipedia;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit tests for org.wikipedia.HTMLUtils
 *  @author MER-C
  */
public class HTMLUtilsTest
{
    @Test
    public void sanitizeForHTML()
    {
        assertEquals("", HTMLUtils.sanitizeForHTML(null));
        assertEquals("&lt;p&gt;&quot;&#x27;Test&amp;123&#x27;&quot;&lt;&#x2F;p&gt;",
            HTMLUtils.sanitizeForHTML("<p>\"'Test&123'\"</p>"));
    }
}
