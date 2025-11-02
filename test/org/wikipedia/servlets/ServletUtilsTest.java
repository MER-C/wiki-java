/**
 *  @(#)ServletUtilsTest.java 0.02 20/04/2025
 *  Copyright (C) 2018-2025 MER-C and contributors
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
package org.wikipedia.servlets;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit tests for {@link org.wikipedia.servlets.ServletUtils}.
 *  @author MER-C
 */
public class ServletUtilsTest
{
    @Test
    public void sanitizeForAttribute()
    {
        assertEquals("", ServletUtils.sanitizeForAttribute(null));
        assertEquals("default", ServletUtils.sanitizeForAttributeOrDefault(null, "default"));
        assertThrows(NullPointerException.class, 
            () -> ServletUtils.sanitizeForAttributeOrDefault(null, null));
    }
    
    @Test
    public void generatePagination()
    {
        // failure states
        String urlbase = "https://example.com/test.jsp?x=1";
        assertThrows(IllegalArgumentException.class, () -> ServletUtils.generatePagination(urlbase, -1, 10, 100));
        assertThrows(IllegalArgumentException.class, () -> ServletUtils.generatePagination(urlbase, 0, 0, 100));
        assertThrows(IllegalArgumentException.class, () -> ServletUtils.generatePagination(urlbase, 0, 0, 0));
        
        // test start from zero
        assertEquals("<p>Previous 50 | <a href=\"" + urlbase + "&offset=50\">Next 50</a>", 
            ServletUtils.generatePagination(urlbase, 0, 50, 149));
        // test intermediate
        assertEquals("<p><a href=\"" + urlbase + "&offset=1\">Previous 50</a> | " 
            + "<a href=\"" + urlbase + "&offset=101\">Next 50</a>", 
            ServletUtils.generatePagination(urlbase, 51, 50, 149));
        // test final
        assertEquals("<p><a href=\"" + urlbase + "&offset=50\">Previous 50</a> | Next 50",
            ServletUtils.generatePagination(urlbase, 100, 50, 149));
    }
}
