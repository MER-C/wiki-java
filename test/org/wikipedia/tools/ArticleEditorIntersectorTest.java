/**
 *  @(#)ArticleEditorIntesectorUnitTest.java 0.02 28/01/2018
 *  Copyright (C) 2017 - 20xx MER-C
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
package org.wikipedia.tools;

import java.time.OffsetDateTime;
import java.util.*;
import org.wikipedia.Wiki;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit tests for {@link ArticleEditorIntersector}.
 *  @author MER-C
 */
public class ArticleEditorIntersectorTest
{
    private final Wiki enWiki, testWiki;
    private final ArticleEditorIntersector intersector, intersector_enWiki;

    /**
     *  Constructs a tool object and wiki connections for every test so that
     *  tests are independent.
     */
    public ArticleEditorIntersectorTest()
    {
        testWiki = Wiki.createInstance("test.wikipedia.org");
        testWiki.setMaxLag(-1);

        enWiki = Wiki.createInstance("en.wikipedia.org");
        enWiki.setMaxLag(-1);

        intersector = new ArticleEditorIntersector(testWiki);
        intersector_enWiki = new ArticleEditorIntersector(enWiki);
    }

    @Test
    public void getWiki()
    {
        assertEquals("test.wikipedia.org", intersector.getWiki().getDomain());
        assertEquals("en.wikipedia.org", intersector_enWiki.getWiki().getDomain());
    }

    @Test
    public void intersetArticles() throws Exception
    {
        // need two articles to get a meaningful intersection
        assertThrows(IllegalArgumentException.class,
            () -> intersector.intersectArticles(Collections.emptyList(), false, false, false),
            "page count = 0, at least two are required");
        assertThrows(IllegalArgumentException.class,
            () -> intersector.intersectArticles(Arrays.asList("Main Page"), false, false, false),
            "page count = 1, at least two are required");
        // check if duplicates are removed
        assertThrows(IllegalArgumentException.class,
            () -> intersector.intersectArticles(Arrays.asList("Main Page", "Main Page"), false, false, false),
            "exact duplicates not removed before going online");
        // check if Special: and Media: pages are removed
        assertThrows(IllegalArgumentException.class,
            () -> intersector.intersectArticles(Arrays.asList("Special:Recentchanges", "Media:Example.png", "Main Page"), false, false, false),
            "Special/Media pages not removed before going online");
        
        // non-existing pages
        List<String> articles = Arrays.asList("This page does not exist", "This page also does not exist");
        Map<String, List<Wiki.Revision>> results = intersector.intersectArticles(articles, false, false, false);
        assertTrue(results.isEmpty(), "non-existing pages");
        results = intersector.intersectArticles(articles, true, true, true);
        assertTrue(results.isEmpty(), "non-existing pages with noadmin/bot/IP flags");

        // no intersection
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/pagetext
        // https://test.wikipedia.org/wiki/User:NikiWiki/EmptyPage
        articles = Arrays.asList("User:NikiWiki/EmptyPage", "User:MER-C/UnitTests/pagetext");
        results = intersector.intersectArticles(articles, false, false, false);
        assertTrue(results.isEmpty(), "Pages with no intersection");

        // page history contains usernames that have been RevisionDeleted or suppressed
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        // otherwise no intersection
        articles = Arrays.asList("User:NikiWiki/EmptyPage", "User:MER-C/UnitTests/Delete");
        results = intersector.intersectArticles(articles, false, false, false);
        assertTrue(results.isEmpty(), "Page with deleted/suppressed username");

        // check noadmin, expect no intersection
        articles = Arrays.asList("User:MER-C/UnitTests/pagetext", "User:MER-C/UnitTests/Delete");
        results = intersector.intersectArticles(articles, true, false, false);
        assertTrue(results.isEmpty(), "Check exclusion of admins");
    }

    @Test
    public void setDateRange() throws Exception
    {
        // first, verify get/set works
        assertThrows(IllegalArgumentException.class,
            () -> intersector_enWiki.setDateRange(OffsetDateTime.now(), OffsetDateTime.MIN));
        OffsetDateTime earliest = OffsetDateTime.parse("2010-01-01T00:00:00Z");
        OffsetDateTime latest = OffsetDateTime.parse("2013-03-01T00:00:00Z");
        intersector_enWiki.setDateRange(earliest, latest);
        assertEquals(earliest, intersector_enWiki.getEarliestDateTime());
        assertEquals(latest, intersector_enWiki.getLatestDateTime());

        // These articles have an intersection, but if we restrict the date range
        // we can get zero results.
        // https://en.wikipedia.org/w/index.php?title=Sainpasela&action=history
        // https://en.wikipedia.org/w/index.php?title=Qihe_County&action=history
        List<String> articles = Arrays.asList("Sainpasela", "Qihe County");
        Map<String, List<Wiki.Revision>> results = intersector_enWiki.intersectArticles(articles, false, false, false);
        assertTrue(results.isEmpty(), "check date range functionality");
    }
}
