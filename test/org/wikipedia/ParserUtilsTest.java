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

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;

/**
 *  Unit tests for org.wikipedia.ParserUtils
 *  @author MER-C
 */
public class ParserUtilsTest
{
    private static Wiki testWiki;
    
    /**
     *  Initialize wiki objects.
     *  @throws Exception if a network error occurs
     */
    @BeforeClass
    public static void setUpClass() throws Exception
    {
        testWiki = Wiki.createInstance("test.wikipedia.org");
        testWiki.setMaxLag(-1);
    }
    
    @Test
    public void generateUserLinks() throws Exception
    {
        String indexPHPURL = testWiki.getIndexPHPURL();
        String expected = 
              "<a href=\"" + testWiki.getPageURL("User:MER-C") + "\">MER-C</a> ("
            + "<a href=\"" + testWiki.getPageURL("User talk:MER-C") + "\">talk</a> | "
            + "<a href=\"" + testWiki.getPageURL("Special:Contributions/MER-C") + "\">contribs</a> | "
            + "<a href=\"" + testWiki.getPageURL("Special:DeletedContributions/MER-C") + "\">deleted contribs</a> | "
            + "<a href=\"" + indexPHPURL + "?title=Special:Log&user=MER-C\">logs</a> | "
            + "<a href=\"" + testWiki.getPageURL("Special:Block/MER-C") + "\">block</a> | "
            + "<a href=\"" + indexPHPURL + "?title=Special:Log&type=block&page=User:MER-C\">block log</a>)";
        assertEquals("generateUserLinks", expected, ParserUtils.generateUserLinks(testWiki, "MER-C"));
        
        expected = "<a href=\"" + testWiki.getPageURL("User:A_B_の") + "\">A B の</a> ("
            + "<a href=\"" + testWiki.getPageURL("User_talk:A_B_の") + "\">talk</a> | "
            + "<a href=\"" + testWiki.getPageURL("Special:Contributions/A_B_の") + "\">contribs</a> | "
            + "<a href=\"" + testWiki.getPageURL("Special:DeletedContributions/A_B_の") + "\">deleted contribs</a> | "
            + "<a href=\"" + indexPHPURL + "?title=Special:Log&user=A+B+%E3%81%AE\">logs</a> | "
            + "<a href=\"" + testWiki.getPageURL("Special:Block/A_B_の") + "\">block</a> | "
            + "<a href=\"" + indexPHPURL + "?title=Special:Log&type=block&page=User:A+B+%E3%81%AE\">block log</a>)";
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
            + "[{{fullurl:Special:Log|user=A+B+%E3%81%AE}} logs] | "
            + "[[Special:Block/A B の|block]] | "
            + "[{{fullurl:Special:Log|type=block&page=User:A+B+%E3%81%AE}} block log])\n";
        assertEquals("generateUserLinksAsWikitext: special characters", expected, ParserUtils.generateUserLinksAsWikitext("A B の"));
    }
    
    @Test
    public void parseWikilink()
    {
        assertEquals("link", Arrays.asList("Link", "Link"), ParserUtils.parseWikilink("[[ Link ]]"));
        assertEquals("link with colon", Arrays.asList("Link", "Link"), ParserUtils.parseWikilink("[[:Link]]"));
        assertEquals("link with description", Arrays.asList("Link", "Description"), ParserUtils.parseWikilink("[[ Link | Description ]]"));
        assertEquals("link with description and colon", Arrays.asList("Link", "Description"), ParserUtils.parseWikilink("[[:Link|Description]]"));
    }
}
