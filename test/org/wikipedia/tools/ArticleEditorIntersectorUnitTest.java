/**
 *  @(#)ArrayUtilsUnitTest.java 0.01 02/11/2017
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
package org.wikipedia.tools;

import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;
import org.wikipedia.Wiki;
import org.wikipedia.tools.ArticleEditorIntersector;

/**
 *  Unit tests for {@link ArticleEditorIntersector}.
 *  @author MER-C
 */
public class ArticleEditorIntersectorUnitTest
{
    private static ArticleEditorIntersector intersector;
    
    /**
     *  Initializes intersector object.
     *  @throws Exception if a network error occurs
     */
    @BeforeClass
    public static void setUpClass() throws Exception
    {
        Wiki testWiki = Wiki.createInstance("test.wikipedia.org");
        testWiki.setMaxLag(-1);
        intersector = new ArticleEditorIntersector(testWiki);
    }
    
    @Test
    public void intersectArticles() throws Exception
    {
        // Need two articles to get a meaningful intersection
        String[] articles = new String[0];
        try
        {
            intersector.intersectArticles(articles, false, false, false);
            fail("Attempted to intersect edit histories of zero pages, at least two are required.");
        }
        catch (IllegalArgumentException expected)
        {
        }
        articles = new String[] { "Main Page" };
        try
        {
            intersector.intersectArticles(articles, false, false, false);
            fail("Attempted to intersect history of a single page, at least two are required.");
        }
        catch (IllegalArgumentException expected)
        {
        }
        // check if duplicates are removed
        articles = new String[] { "Main Page", "Main Page" };
        try
        {
            intersector.intersectArticles(articles, false, false, false);
            fail("Exact duplicates not removed before going online.");
        }
        catch (IllegalArgumentException expected)
        {
        }
        
        // non-existing pages
        articles = new String[] { "This page does not exist", "This page also does not exist" };
        Map<String, List<Wiki.Revision>> results = intersector.intersectArticles(articles, false, false, false);
        assertTrue("Intersection of non-existing pages", results.isEmpty());
        results = intersector.intersectArticles(articles, true, true, true);
        assertTrue("Intersection of non-existing pages with noadmin/bot/IP flags", results.isEmpty());
        
        // exclude Special: and Media: pages
        articles = new String[] { "Special:Recentchanges", "Media:Example.png", "Main Page" };
        results = intersector.intersectArticles(articles, false, false, false);
        assertTrue("Intersection of Special/Media pages", results.isEmpty());
        
        // no intersection
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/pagetext
        // https://test.wikipedia.org/wiki/User:NikiWiki/EmptyPage
        articles = new String[] { "User:NikiWiki/EmptyPage", "User:MER-C/UnitTests/pagetext" };
        results = intersector.intersectArticles(articles, false, false, false);
        assertTrue("Pages with no intersection", results.isEmpty());
        
        // page history contains usernames that have been RevisionDeleted or suppressed
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        // otherwise no intersection
        articles = new String[] { "User:NikiWiki/EmptyPage", "User:MER-C/UnitTests/Delete" };
        results = intersector.intersectArticles(articles, false, false, false);
        assertTrue("Page with deleted/suppressed username", results.isEmpty());
        
        // check noadmin, expect no intersection
        articles = new String[] { "User:MER-C/UnitTests/pagetext", "User:MER-C/UnitTests/Delete" };
        results = intersector.intersectArticles(articles, true, false, false);
        assertTrue("Check exclusion of admins", results.isEmpty());
    }
}
