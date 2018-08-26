/**
 *  @(#)ServletUtilsTest.java 0.01 26/08/2018
 *  Copyright (C) 2018-20XX MER-C and contributors
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

import org.junit.Test;
import static org.junit.Assert.*;

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
        try
        {
            ServletUtils.sanitizeForAttributeOrDefault(null, null);
        }
        catch (NullPointerException expected)
        {
        }
    }
    
    @Test
    public void sanitizeForHTML()
    {
        assertEquals("", ServletUtils.sanitizeForHTML(null));
    }
}
