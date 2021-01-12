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
    @Test
    public void parseWikilink()
    {
        assertEquals(List.of("Link", "Link"), WikitextUtils.parseWikilink("[[ Link ]]"));
        assertEquals(List.of("Link", "Link"), WikitextUtils.parseWikilink("[[:Link]]"));
        assertEquals(List.of("Link", "Description"), WikitextUtils.parseWikilink("[[ Link | Description ]]"));
        assertEquals(List.of("Link", "Description"), WikitextUtils.parseWikilink("[[:Link|Description]]"));
    }
    
    @Test
    public void addTableRow()
    {
        List<String> cells = List.of("A", "B", "C");
        assertEquals("|-\n| A || B || C\n", WikitextUtils.addTableRow(cells));
    }
    
    @Test
    public void removeComments()
    {
        assertEquals("A  B",        WikitextUtils.removeComments("A <!-- comment --> B"));
        assertEquals("Blah ",       WikitextUtils.removeComments("Blah <!-- Unbalanced comment"));
        assertEquals("A  B  C",     WikitextUtils.removeComments("A <!-- Two --> B <!-- Comments --> C"));
        assertEquals("A  end2 -->", WikitextUtils.removeComments("A <!-- Two ends --> end2 -->"));
        assertEquals("-->End at 0", WikitextUtils.removeComments("-->End at 0<!--"));
    }
}
