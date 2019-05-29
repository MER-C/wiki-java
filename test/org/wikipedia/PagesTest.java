/**
 *  @(#)PagesTest.java 0.01 31/03/2018
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
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Tests for {@link org.wikipedia.Pages}.
 *  @author MER-C
 */
public class PagesTest
{
    private final Wiki testWiki;
    private final Pages testWikiPages;

    /**
     *  Construct wiki objects for each test so that tests are independent.
     */
    public PagesTest()
    {
        testWiki = Wiki.newSession("test.wikipedia.org");
        testWiki.setMaxLag(-1);
        testWikiPages = Pages.of(testWiki);
    }

    @Test
    public void toWikitextList()
    {
        // Wiki-markup breaking titles should not make it to this method
        List<String> articles = Arrays.asList("File:Example.png", "Main Page",
            "Category:Example", "*-algebra");

        String expected = "*[[:File:Example.png]]\n*[[:Main Page]]\n"
            + "*[[:Category:Example]]\n*[[:*-algebra]]\n";
        assertEquals(expected, Pages.toWikitextList(articles, Pages.LIST_OF_LINKS, false), "unnumbered list");
        expected = "#[[:File:Example.png]]\n#[[:Main Page]]\n#[[:Category:Example]]\n"
            + "#[[:*-algebra]]\n";
        assertEquals(expected, Pages.toWikitextList(articles, Pages.LIST_OF_LINKS, true), "numbered list");

        articles = Arrays.asList("example.com", "example.net");
        expected = "*{{spamlink|1=example.com}}\n*{{spamlink|1=example.net}}\n";
        assertEquals(expected, Pages.toWikitextTemplateList(articles, "spamlink", false), "unnumbered list of templates");
        expected = "#{{TEST|1=Hello world}}\n#{{TEST|1=Just testing}}\n";
        articles = Arrays.asList("Hello world", "Just testing");
        assertEquals(expected, Pages.toWikitextTemplateList(articles, "TEST", true), "numbered list of templates");
    }

    @Test
    public void parseWikitextList()
    {
        // Again, wiki-markup breaking titles should not make it to this method
        // though it is able to tolerate some abuse
        String list =
            "*[[:File:Example.png]]\n" +
            "*[[Main Page]] -- annotation with extra [[wikilink|and description]]\n" +
            "*[[*-algebra]]\n" +
            "*:Not a list item.\n" +
            "*[[Cape Town#Economy]]\n" +
            "**[[Nested list]]\n" +
            "*[[Link|Description]]" +
            "*[[Invalid wiki markup instance #1\n" +
            "*Not a link]]";
        List<String> expected = Arrays.asList("File:Example.png", "Main Page",
            "*-algebra", "Cape Town#Economy", "Nested list", "Link");
        assertEquals(expected, Pages.parseWikitextList(list), "unnumbered list");

        list = "#[[:File:Example.png]]\n#[[*-algebra]]\n#[[Cape Town#Economy]]";
        expected = Arrays.asList("File:Example.png", "*-algebra", "Cape Town#Economy");
        assertEquals(expected, Pages.parseWikitextList(list), "numbered list");
    }

    @Test
    public void parseWikitextTemplateList()
    {
        // Remember, don't attempt to parse meta-templates.
        String list =
            "*{{la|Test}}\n" +
            "*{{la|Test2}} - stuff\n" +
            "*:Not a list item\n" +
            "*{{ la | Test3 }}\n" +
            "*{{template|Test4}}\n" +
            "*{{la|1=Test5}}\n" +
            "*{{la|x=Test6}}\n" +
            "{{la}}";
        List<String> expected = Arrays.asList("Test", "Test2", "Test3", "Test5", 
            "Test6", "");
        assertEquals(expected, Pages.parseWikitextTemplateList(list, "la"));
    }
    
    @Test
    public void generatePageLink()
    {
        assertEquals("<a href=\"https://test.wikipedia.org/wiki/Test\">Test</a>", 
            testWikiPages.generatePageLink("Test"));
        assertEquals("<a href=\"https://test.wikipedia.org/wiki/Test\" class=\"new\">Test</a>", 
            testWikiPages.generatePageLink("Test", false));
        assertEquals("<a href=\"https://test.wikipedia.org/wiki/Test\">Caption</a>", 
            testWikiPages.generatePageLink("Test", "Caption"));
        assertEquals("<a href=\"https://test.wikipedia.org/wiki/Test\" class=\"new\">Caption</a>", 
            testWikiPages.generatePageLink("Test", "Caption", false));
    }

    @Test
    public void generateSummaryLinks()
    {
        String indexPHPURL = testWiki.getIndexPhpUrl();
        String expected =
              "<a href=\"" + testWiki.getPageUrl("Test") + "\">Test</a> ("
            + "<a href=\"" + indexPHPURL + "?title=Test&action=edit\">edit</a> | "
            + "<a href=\"" + testWiki.getPageUrl("Talk:Test") + "\">talk</a> | "
            + "<a href=\"" + indexPHPURL + "?title=Test&action=history\">history</a> | "
            + "<a href=\"" + indexPHPURL + "?title=Special:Log&page=Test\">logs</a>)";
        assertEquals(expected, testWikiPages.generateSummaryLinks("Test"));

        expected = "<a href=\"" + testWiki.getPageUrl("A B の") + "\">A B の</a> ("
            + "<a href=\"" + indexPHPURL + "?title=A+B+%E3%81%AE&action=edit\">edit</a> | "
            + "<a href=\"" + testWiki.getPageUrl("Talk:A B の") + "\">talk</a> | "
            + "<a href=\"" + indexPHPURL + "?title=A+B+%E3%81%AE&action=history\">history</a> | "
            + "<a href=\"" + indexPHPURL + "?title=Special:Log&page=A+B+%E3%81%AE\">logs</a>)";
        assertEquals(expected, testWikiPages.generateSummaryLinks("A B の"), "special characters");
    }

    @Test
    public void containExternalLinks() throws Exception
    {
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Linkfinder
        String page = "User:MER-C/UnitTests/Linkfinder";
        List<String> links = Arrays.asList("http://spam.example.com", "http://absent.example.com");
        Map<String, List<String>> inputs = new HashMap<>();
        inputs.put(page, links);
        Map<String, Map<String, Boolean>> actual = testWikiPages.containExternalLinks(inputs);
        assertTrue(actual.get(page).get(links.get(0)));
        assertFalse(actual.get(page).get(links.get(1)));
    }
}
