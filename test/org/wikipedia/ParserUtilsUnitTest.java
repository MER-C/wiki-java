/**
 *  @(#)ParserUtilsUnitTest.java 0.31 05/10/2017
 *  Copyright (C) 2017 MER-C
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
    public void generateUserLinks() throws Exception
    {
        String expected = 
              "<a href=\"//test.wikipedia.org/wiki/User:MER-C\">MER-C</a> ("
            + "<a href=\"//test.wikipedia.org/wiki/User_talk:MER-C\">talk</a> | "
            + "<a href=\"//test.wikipedia.org/wiki/Special:Contributions/MER-C\">contribs</a> | "
            + "<a href=\"//test.wikipedia.org/wiki/Special:DeletedContributions/MER-C\">deleted contribs</a> | "
            + "<a href=\"//test.wikipedia.org/wiki/Special:Block/MER-C\">block</a> | "
            + "<a href=\"//test.wikipedia.org/w/index.php?title=Special:Log&type=block&page=User:MER-C\">block log</a>)";
        assertEquals("generateUserLinks", expected, ParserUtils.generateUserLinks(testWiki, "MER-C"));
        
        expected = "<a href=\"//test.wikipedia.org/wiki/User:A_B_%E3%81%AE\">A B の</a> ("
            + "<a href=\"//test.wikipedia.org/wiki/User_talk:A_B_%E3%81%AE\">talk</a> | "
            + "<a href=\"//test.wikipedia.org/wiki/Special:Contributions/A_B_%E3%81%AE\">contribs</a> | "
            + "<a href=\"//test.wikipedia.org/wiki/Special:DeletedContributions/A_B_%E3%81%AE\">deleted contribs</a> | "
            + "<a href=\"//test.wikipedia.org/wiki/Special:Block/A_B_%E3%81%AE\">block</a> | "
            + "<a href=\"//test.wikipedia.org/w/index.php?title=Special:Log&type=block&page=User:A_B_%E3%81%AE\">block log</a>)";
        assertEquals("generateUserLinks: special characters", expected, ParserUtils.generateUserLinks(testWiki, "A B の"));
    }
    
}
