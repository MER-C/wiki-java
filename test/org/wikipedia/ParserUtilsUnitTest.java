/**
 *  @(#)ParserUtilsUnitTest.java 0.02 23/12/2016
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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *  Unit tests for org.wikipedia.ParserUtils
 *  @author MER-C
 */
public class ParserUtilsUnitTest
{
    private static Wiki testWiki = Wiki.createInstance("test.wikipedia.org");
    
    /**
     *  Initialize wiki objects.
     *  @throws Exception if a network error occurs
     */
    @BeforeClass
    public static void setUpClass() throws Exception
    {
        testWiki.setMaxLag(-1);
    }
    
    @Test
    public void formatList()
    {
        // Wiki-markup breaking titles should not make it to this method
        String[] articles = {
            "File:Example.png",
            "Main Page",
            "Category:Example",
            "*-algebra"
        };
        String expected = "*[[:File:Example.png]]\n*[[:Main Page]]\n"
            + "*[[:Category:Example]]\n*[[:*-algebra]]\n";
        assertEquals("formatlist", expected, ParserUtils.formatList(articles));
    }
    
    @Test
    public void parseList()
    {
        // Again, wiki-markup breaking titles should not make it to this method.
        // In particular, titles must not contain [.
        String list = "*[[:File:Example.png]]\n*[[Main Page]]\n"
            + "*[[*-algebra]]\n*:Not a list item."
            + "*[[Cape Town#Economy]]\n**[[Nested list]]";
        String[] expected = {
            "File:Example.png",
            "Main Page",
            "*-algebra",
            "Cape Town#Economy",
            "Nested list"
        };
        assertArrayEquals("parselist", expected, ParserUtils.parseList(list));
        list = "#[[:File:Example.png]]\n#[[*-algebra]]\n#[[Cape Town#Economy]]";
        expected = new String[] {
            "File:Example.png",
            "*-algebra",
            "Cape Town#Economy",
        };
        assertArrayEquals("parselist: numbered", expected, ParserUtils.parseList(list));
    }
    
    @Test
    public void generateUserLinks() throws Exception
    {
        String expected = 
              "<a href=\"//test.wikipedia.org/wiki/User:MER-C\">MER-C</a> ("
            + "<a href=\"//test.wikipedia.org/wiki/User_talk:MER-C\">talk</a> | "
            + "<a href=\"//test.wikipedia.org/wiki/Special:Contributions/MER-C\">contribs</a> | "
            + "<a href=\"//test.wikipedia.org/wiki/Special:DeletedContributions/MER-C\">deleted contribs</a> | "
            + "<a href=\"//test.wikipedia.org/w/index.php?title=Special:Log&user=MER-C\">logs</a> | "
            + "<a href=\"//test.wikipedia.org/wiki/Special:Block/MER-C\">block</a> | "
            + "<a href=\"//test.wikipedia.org/w/index.php?title=Special:Log&type=block&page=User:MER-C\">block log</a>)";
        assertEquals("generateUserLinks", expected, ParserUtils.generateUserLinks(testWiki, "MER-C"));
        
        expected = "<a href=\"//test.wikipedia.org/wiki/User:A_B_%E3%81%AE\">A B の</a> ("
            + "<a href=\"//test.wikipedia.org/wiki/User_talk:A_B_%E3%81%AE\">talk</a> | "
            + "<a href=\"//test.wikipedia.org/wiki/Special:Contributions/A_B_%E3%81%AE\">contribs</a> | "
            + "<a href=\"//test.wikipedia.org/wiki/Special:DeletedContributions/A_B_%E3%81%AE\">deleted contribs</a> | "
            + "<a href=\"//test.wikipedia.org/w/index.php?title=Special:Log&user=A_B_%E3%81%AE\">logs</a> | "
            + "<a href=\"//test.wikipedia.org/wiki/Special:Block/A_B_%E3%81%AE\">block</a> | "
            + "<a href=\"//test.wikipedia.org/w/index.php?title=Special:Log&type=block&page=User:A_B_%E3%81%AE\">block log</a>)";
        assertEquals("generateUserLinks: special characters", expected, ParserUtils.generateUserLinks(testWiki, "A B の"));
    }
    
    @Test
    public void generateUserLinksAsWikitext() throws Exception
    {
        String expected = "* [[User:MER-C|MER-C]] ("
            + "[[User talk:MER-C|talk]] | "
            + "[[Special:Contributions/MER-C|contribs]] | "
            + "[[Special:DeletedContributions/MER-C|deleted contribs]] | "
            + "[{{fullurl:Special:Log|user=MER-C}} logs] | "
            + "[[Special:Block/MER-C|block]] | "
            + "[{{fullurl:Special:Log|type=block&page=User:MER-C}} block log])\n";
        assertEquals("generateUserLinksAsWikitext", expected, ParserUtils.generateUserLinksAsWikitext("MER-C"));
        
        expected = "* [[User:A B の|A B の]] ("
            + "[[User talk:A B の|talk]] | "
            + "[[Special:Contributions/A B の|contribs]] | "
            + "[[Special:DeletedContributions/A B の|deleted contribs]] | "
            + "[{{fullurl:Special:Log|user=A_B_%E3%81%AE}} logs] | "
            + "[[Special:Block/A B の|block]] | "
            + "[{{fullurl:Special:Log|type=block&page=User:A_B_%E3%81%AE}} block log])\n";
        assertEquals("generateUserLinksAsWikitext: special characters", expected, ParserUtils.generateUserLinksAsWikitext("A B の"));
    }
}
