/**
 *  @(#)WikitextUtilsTest.java 0.02 23/12/2016
 *  Copyright (C) 2017 - 2018 MER-C
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
 *  Unit tests for org.wikipedia.ParserUtils
 *  @author MER-C
 */
public class WikitextUtilsTest
{
    private static Wiki testWiki;
    
    /**
     *  Initialize wiki objects.
     *  @throws Exception if a network error occurs
     */
    @BeforeAll
    public static void setUpClass() throws Exception
    {
        testWiki = Wiki.newSession("test.wikipedia.org");
        testWiki.setMaxLag(-1);
    }
    
    @Test
    public void parseWikilink()
    {
        assertEquals(Arrays.asList("Link", "Link"), WikitextUtils.parseWikilink("[[ Link ]]"));
        assertEquals(Arrays.asList("Link", "Link"), WikitextUtils.parseWikilink("[[:Link]]"));
        assertEquals(Arrays.asList("Link", "Description"), WikitextUtils.parseWikilink("[[ Link | Description ]]"));
        assertEquals(Arrays.asList("Link", "Description"), WikitextUtils.parseWikilink("[[:Link|Description]]"));
    }
    
    @Test
    public void addTableRow()
    {
        List<String> cells = Arrays.asList("A", "B", "C");
        assertEquals("|-\n| A || B || C\n", WikitextUtils.addTableRow(cells));
    }
}
