/**
 *  @(#)PagesUnitTest.java 0.01 31/03/2018
 *  Copyright (C) 2018 - 20xx MER-C
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
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *  Tests for {@link org.wikipedia.Pages}.
 *  @author MER-C
 */
public class PagesTest
{
    @Test
    public void toWikitextList()
    {
        // Wiki-markup breaking titles should not make it to this method
        List<String> articles = Arrays.asList("File:Example.png", "Main Page", 
            "Category:Example", "*-algebra");
        
        String expected = "*[[:File:Example.png]]\n*[[:Main Page]]\n"
            + "*[[:Category:Example]]\n*[[:*-algebra]]\n";
        assertEquals("toWikitextList, unnumbered", expected, Pages.toWikitextList(articles, false));
        expected = "#[[:File:Example.png]]\n#[[:Main Page]]\n#[[:Category:Example]]\n"
            + "#[[:*-algebra]]\n";
        assertEquals("toWikitextList, numbered", expected, Pages.toWikitextList(articles, true));
    }
    
    @Test
    public void parseWikitextList()
    {
        // Again, wiki-markup breaking titles should not make it to this method.
        // In particular, titles must not contain [.
        String list = "*[[:File:Example.png]]\n*[[Main Page]] -- annotation\n"
            + "*[[*-algebra]]\n*:Not a list item."
            + "*[[Cape Town#Economy]]\n**[[Nested list]]";
        List<String> expected = Arrays.asList("File:Example.png", "Main Page", 
            "*-algebra", "Cape Town#Economy", "Nested list");        
        assertEquals("parseList, unnumbered", expected, Pages.parseWikitextList(list));
        
        list = "#[[:File:Example.png]]\n#[[*-algebra]]\n#[[Cape Town#Economy]]";
        expected = Arrays.asList("File:Example.png", "*-algebra", "Cape Town#Economy");
        assertEquals("parseList, numbered", expected, Pages.parseWikitextList(list));
    }
}
