/**
 *  @(#)ArrayUtilsUnitTest.java 0.01 31/10/2017
 *  Copyright (C) 2017 - 2018 MER-C and contributors
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

import java.util.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit tests for {@link ArrayUtils}.
 *  @author MER-C
 */
public class ArrayUtilsTest
{  
    @Test
    public void intersection()
    {
        String[] a = { "1", "2", "3", "4", "5" };
        String[] b1 = { "1", "2", "3" };
        String[] b2 = { "3", "4", "5", "x" };
        assertArrayEquals(new String[] { "3" }, ArrayUtils.intersection(a, b1, b2));
    }
    
    @Test
    public void relativeComplement()
    {
        String[] a = { "1", "2", "3", "4", "5" };
        String[] b1 = { "1", "2" };
        String[] b2 = { "3", "4", "x" };
        assertArrayEquals(new String[] { "5" }, ArrayUtils.relativeComplement(a, b1, b2));
    }
    
    @Test
    public void sortByValue()
    {
        Map<String, String> test = Map.of("AAA", "Entry2", "BBB", "Entry4", "CCC", "Entry1", "DDD", "Entry3");
        Map<String, String> sorted = ArrayUtils.sortByValue(test, Comparator.reverseOrder());
        assertEquals(test.size(), sorted.size());
        int count = 0;
        for (var entry : sorted.entrySet())
        {
            switch (count++)
            {
                case 0: assertEquals(Map.entry("BBB", "Entry4"), entry); break;
                case 1: assertEquals(Map.entry("DDD", "Entry3"), entry); break;
                case 2: assertEquals(Map.entry("AAA", "Entry2"), entry); break;
                case 3: assertEquals(Map.entry("CCC", "Entry1"), entry); break;
            }
        }
    }
}
