/**
 *  @(#)ThrowingFunction.java 0.01 10/12/2023
 *  Copyright (C) 2023 - 20xx MER-C
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

import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Tests for ThrowingFunction.
 *  @author MER-C
 *  @version 0.01
 */
public class ThrowingFunctionTest
{
    @Test
    public void testApply() throws Exception
    {
        ThrowingFunction fn = a -> a;
        assertEquals("Test", fn.apply("Test"));
        fn = a -> { throw new IOException(); };
        assertNull(fn.apply("Test"));
    }
}
