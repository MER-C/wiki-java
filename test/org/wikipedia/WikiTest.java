/**
 *  @(#)WikiTest.java 0.34 13/01/2018
 *  Copyright (C) 2014-2018 MER-C
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

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *  Unit tests for Wiki.java
 *  @author MER-C
 */
public class WikiTest
{
    private final Wiki enWiki, deWiki, arWiki, testWiki, enWikt;
    private final MessageDigest sha256;

    /**
     *  Construct wiki objects for each test so that tests are independent.
     *  @throws Exception if a network error occurs
     */
    public WikiTest() throws Exception
    {
        enWiki = Wiki.createInstance("en.wikipedia.org");
        enWiki.setMaxLag(-1);
        deWiki = Wiki.createInstance("de.wikipedia.org");
        deWiki.setMaxLag(-1);
        arWiki = Wiki.createInstance("ar.wikipedia.org");
        arWiki.setMaxLag(-1);
        testWiki = Wiki.createInstance("test.wikipedia.org");
        testWiki.setMaxLag(-1);
        enWikt = Wiki.createInstance("en.wiktionary.org");
        enWikt.setMaxLag(-1);
        enWikt.getSiteInfo();
        
        sha256 = MessageDigest.getInstance("SHA-256");
    }

    @Test
    public void testURLs()
    {
        Wiki dummy = Wiki.createInstance("example.com", "/scriptpath", "http://");
        assertEquals("protocol", "http://", dummy.getProtocol());
        assertEquals("domain", "example.com", dummy.getDomain());
        assertEquals("scriptPath", "/scriptpath", dummy.getScriptPath());
    }

    @Test
    public void equalsAndhashCode()
    {
        Wiki dummy = Wiki.createInstance("en.wikipedia.org");
        assertEquals(enWiki, dummy);
        assertEquals(enWiki.hashCode(), dummy.hashCode());
    }

    @Test
    public void compareTo()
    {
        int result_1 = enWiki.compareTo(arWiki);
        int result_2 = arWiki.compareTo(enWiki);
        int result_3 = testWiki.compareTo(enWiki);
        int result_4 = testWiki.compareTo(arWiki);
        assertTrue("compareTo", result_1 > 0);
        assertEquals("compareTo: symmetric", -1*result_1, result_2);
        assertTrue("compareTo: transitivity", result_3 > 0 && result_4 > 0);

        Wiki dummy = Wiki.createInstance("en.wikipedia.org", "/example", "https://");
        int result_5 = enWiki.compareTo(dummy);
        assertTrue("compareTo: multiple instances on same domain", result_5 > 0);
    }

    @Test
    public void getIndexPHPURL()
    {
        assertEquals("getIndexPHPURL", "https://en.wikipedia.org/w/index.php", enWiki.getIndexPHPURL());
        Wiki dummy = Wiki.createInstance("example.com", "/scriptpath", "http://");
        assertEquals("getIndexPHPURL", "http://example.com/scriptpath/index.php", dummy.getIndexPHPURL());
    }

    @Test
    public void assertionMode() throws Exception
    {
        enWiki.setAssertionMode(Wiki.ASSERT_USER);
        assertEquals("assertion mode", Wiki.ASSERT_USER, enWiki.getAssertionMode());
        // This test runs logged out. The following assertions are expected to fail.
        try
        {
            enWiki.getPageText("Main Page");
            fail("assertion mode: logged out, but ASSERT_USER passed");
        }
        catch (AssertionError expected)
        {
        }
        enWiki.setAssertionMode(Wiki.ASSERT_BOT);
        try
        {
            enWiki.getPageText("Main Page");
            fail("assertion mode: logged out, but ASSERT_BOT passed");
        }
        catch (AssertionError expected)
        {
        }
        enWiki.setAssertionMode(Wiki.ASSERT_SYSOP);
        try
        {
            enWiki.getPageText("Main Page");
            fail("assertion mode: logged out, but ASSERT_SYSOP passed");
        }
        catch (AssertionError expected)
        {
        }
    }

    @Test
    public void setResolveRedirects() throws Exception
    {
        String[] pages = {
            "User:MER-C/UnitTests/redirect",
            "User:MER-C/UnitTests/Delete"
        };

        testWiki.setResolveRedirects(true);
        enWiki.setResolveRedirects(true);
        assertTrue("resolving redirects", testWiki.isResolvingRedirects());
        // https://test.wikipedia.org/w/index.php?title=User:MER-C/UnitTests/redirect&redirect=no
        // redirects to https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        assertEquals("resolveredirects/getPageText", "This revision is not deleted!",
            testWiki.getPageText(pages[0]));
        Map<String, Object>[] x = testWiki.getPageInfo(pages);
        x[0].remove("inputpagename");
        x[1].remove("inputpagename");
        assertEquals("resolveredirects/getPageInfo", x[1], x[0]);
        pages = new String[] { "Main page", "Main Page" };
        List<String>[] y = enWiki.getTemplates(pages);
        assertEquals("resolveredirects/getTemplates", y[1], y[0]);
    }

    @Test
    public void namespace() throws Exception
    {
        assertEquals("NS: en category", Wiki.CATEGORY_NAMESPACE, enWiki.namespace("Category:CSD"));
        assertEquals("NS: en category lower case", Wiki.CATEGORY_NAMESPACE, enWiki.namespace("category:CSD"));
        assertEquals("NS: en help talk", Wiki.HELP_TALK_NAMESPACE, enWiki.namespace("Help talk:About"));
        assertEquals("NS: en help talk lower case", Wiki.HELP_TALK_NAMESPACE, enWiki.namespace("help talk:About"));
        assertEquals("NS: en help talk lower case underscore", Wiki.HELP_TALK_NAMESPACE, enWiki.namespace("help_talk:About"));
        assertEquals("NS: en alias", Wiki.PROJECT_NAMESPACE, enWiki.namespace("WP:CSD"));
        // Undocumented on mediawiki.org, better comment out for now
        // assertEquals("NS: not case sensitive", Wiki.PROJECT_NAMESPACE, enWiki.namespace("wp:csd"));
        assertEquals("NS: main ns fail", Wiki.MAIN_NAMESPACE, enWiki.namespace("Star Wars: The Old Republic"));
        assertEquals("NS: main ns fail2", Wiki.MAIN_NAMESPACE, enWiki.namespace("Some Category: Blah"));
        assertEquals("NS: leading colon", Wiki.FILE_NAMESPACE, enWiki.namespace(":File:Blah.jpg"));
        assertEquals("NS: i18n fail", Wiki.CATEGORY_NAMESPACE, deWiki.namespace("Kategorie:Begriffsklärung"));
        assertEquals("NS: mixed i18n", Wiki.CATEGORY_NAMESPACE, deWiki.namespace("Category:Begriffsklärung"));
        assertEquals("NS: rtl fail", Wiki.CATEGORY_NAMESPACE, arWiki.namespace("تصنيف:صفحات_للحذف_السريع"));
    }

    @Test
    public void namespaceIdentifier() throws Exception
    {
        assertEquals("NSIdentifier: wrong identifier", "Category", enWiki.namespaceIdentifier(Wiki.CATEGORY_NAMESPACE));
        assertEquals("NSIdentifier: i18n fail", "Kategorie", deWiki.namespaceIdentifier(Wiki.CATEGORY_NAMESPACE));
        assertEquals("NSIdentifier: custom namespace", "Portal", enWiki.namespaceIdentifier(100));
        assertEquals("NSIdentifier: obviously invalid", "", enWiki.namespaceIdentifier(-100));
    }

    @Test
    public void removeNamespace() throws Exception
    {
        assertEquals("removeNamespace: main namespace", "Hello World", enWiki.removeNamespace("Hello World"));
        assertEquals("removeNamespace: pretend namespace", "Hello:World", enWiki.removeNamespace("Hello:World"));
        assertEquals("removeNamespace: custom namespace", "Hello", enWiki.removeNamespace("Portal:Hello"));
        // something funky going on with RTL, can't tell whether it's Netbeans, Firefox or me failing at copy/paste
        // assertEquals("removeNamespace: rtl fail", "صفحات للحذف السريع", arWiki.removeNamespace("تصنيف:صفحات_للحذف_السريع"));
    }

    @Test
    public void supportsSubpages() throws Exception
    {
        assertFalse("supportsSubpages: main", enWiki.supportsSubpages(Wiki.MAIN_NAMESPACE));
        assertTrue("supportsSubpages: talk", enWiki.supportsSubpages(Wiki.TALK_NAMESPACE));
        // Slashes in special pages denote arguments, not subpages
        assertFalse("supportsSubpages: special", enWiki.supportsSubpages(Wiki.SPECIAL_NAMESPACE));
        try
        {
            enWiki.supportsSubpages(-4444);
            fail("supportsSubpages: obviously invalid namespace");
        }
        catch (IllegalArgumentException expected)
        {
        }
    }

    @Test
    public void queryLimits() throws Exception
    {
        try
        {
            enWiki.setQueryLimit(-1);
            fail("Negative query limits don't make sense.");
        }
        catch (IllegalArgumentException expected)
        {
        }
        enWiki.setQueryLimit(530);
        assertEquals("querylimits", 530, enWiki.getQueryLimit());
        assertEquals("querylimits: length", 530, enWiki.getPageHistory("Main Page").length);
        // check whether queries that set a separate limit function correctly
        assertEquals("querylimits: recentchanges", 10, enWiki.recentChanges(10).length);
        assertEquals("querylimits: after recentchanges", 530, enWiki.getPageHistory("Main Page").length);
        assertEquals("querylimits: getLogEntries", 10, enWiki.getLogEntries(Wiki.DELETION_LOG, "delete", 10).length);
        assertEquals("querylimits: after getLogEntries", 530, enWiki.getPageHistory("Main Page").length);
        assertEquals("querylimits: listPages", 500, enWiki.listPages("", null, Wiki.MAIN_NAMESPACE).length);
        assertEquals("querylimits: after listPages", 530, enWiki.getPageHistory("Main Page").length);
    }

    @Test
    public void getTalkPage() throws Exception
    {
        assertEquals("getTalkPage: main", "Talk:Hello", enWiki.getTalkPage("Hello"));
        assertEquals("getTalkPage: user", "User talk:Hello", enWiki.getTalkPage("User:Hello"));
        try
        {
            enWiki.getTalkPage("Talk:Hello");
            fail("getTalkPage: tried to get talk page of a talk page");
        }
        catch (IllegalArgumentException expected)
        {
        }
        try
        {
            enWiki.getTalkPage("Special:Newpages");
            fail("getTalkPage: tried to get talk page of a special page");
        }
        catch (IllegalArgumentException expected)
        {
        }
        try
        {
            enWiki.getTalkPage("Media:Wiki.png");
            fail("getTalkPage: tried to get talk page of a media page");
        }
        catch (IllegalArgumentException expected)
        {
        }
    }

    @Test
    public void getRootPage() throws Exception
    {
        assertEquals("getRootPage: main ns", "Aaa/Bbb/Ccc", enWiki.getRootPage("Aaa/Bbb/Ccc"));
        assertEquals("getRootPage: talk ns", "Talk:Aaa", enWiki.getRootPage("Talk:Aaa/Bbb/Ccc"));
        assertEquals("getRootPage: talk ns, already root", "Talk:Aaa", enWiki.getRootPage("Talk:Aaa"));
        assertEquals("getRootPage: rtl", "ويكيبيديا:نقاش الحذف",
            arWiki.getRootPage("ويكيبيديا:نقاش الحذف/كأس الخليج العربي لكرة القدم 2014 المجموعة ب"));
    }

    @Test
    public void getParentPage() throws Exception
    {
        assertEquals("getParentPage: main ns", "Aaa/Bbb/Ccc", enWiki.getParentPage("Aaa/Bbb/Ccc"));
        assertEquals("getParentPage: talk ns", "Talk:Aaa/Bbb", enWiki.getParentPage("Talk:Aaa/Bbb/Ccc"));
        assertEquals("getRootPage: talk ns, already root", "Talk:Aaa", enWiki.getParentPage("Talk:Aaa"));
        assertEquals("getParentPage: rtl", "ويكيبيديا:نقاش الحذف",
            arWiki.getParentPage("ويكيبيديا:نقاش الحذف/كأس الخليج العربي لكرة القدم 2014 المجموعة ب"));
    }

    @Test
    public void getPageURL() throws Exception
    {
        assertEquals("getPageURL", "https://en.wikipedia.org/wiki/Hello_World", enWiki.getPageURL("Hello_World"));
        assertEquals("getPageURL", "https://en.wikipedia.org/wiki/Hello_World", enWiki.getPageURL("Hello World"));
    }

    @Test
    public void userExists() throws Exception
    {
        assertTrue("I should exist!", enWiki.userExists("MER-C"));
        assertFalse("Anon should not exist", enWiki.userExists("127.0.0.1"));
        boolean[] temp = testWiki.userExists(new String[] { "Jimbo Wales", "Djskgh;jgsd", "::/1" });
        assertTrue("user exists: Jimbo", temp[0]);
        assertFalse("user exists: nonsense", temp[1]);
        assertFalse("user exists: IPv6 range", temp[2]);
    }

    @Test
    public void changeUserPrivileges() throws Exception
    {
        // check expiry
        Wiki.User user = enWiki.getUser("Example");
        List<String> granted = Arrays.asList("autopatrolled");
        try
        {
            enWiki.changeUserPrivileges(user, granted, Arrays.asList(OffsetDateTime.MIN), Collections.emptyList(), "dummy reason");
            fail("Attempted to set user privilege expiry in the past.");
        }
        catch (IllegalArgumentException expected)
        {
        }
        // check supply of correct amount of expiry dates
        try
        {
            OffsetDateTime now = OffsetDateTime.now();
            List<OffsetDateTime> expiries = Arrays.asList(now.plusYears(1), now.plusYears(2));
            enWiki.changeUserPrivileges(user, granted, expiries, Collections.emptyList(), "dummy reason");
            fail("Attempted to set too many expiry dates.");
        }
        catch (IllegalArgumentException expected)
        {
        }
        // Test runs without logging in, therefore expect failure.
        try
        {
            enWiki.changeUserPrivileges(user, granted, Collections.emptyList(), Collections.emptyList(), "dummy reason");
            fail("Attempted to set user privileges when logged out.");
        }
        catch (SecurityException expected)
        {
        }
    }

    @Test
    public void getFirstRevision() throws Exception
    {
        try
        {
            enWiki.getFirstRevision("Special:SpecialPages");
            fail("Attempted to get the page history of a special page.");
        }
        catch (UnsupportedOperationException expected)
        {
        }
        try
        {
            enWiki.getFirstRevision("Media:Example.png");
            fail("Attempted to get the page history of a special page.");
        }
        catch (UnsupportedOperationException expected)
        {
        }
        assertNull("getFirstRevision: Non-existent page", enWiki.getFirstRevision("dgfhdf&jklg"));
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        Wiki.Revision first = testWiki.getFirstRevision("User:MER-C/UnitTests/Delete");
        assertEquals("getFirstRevision", 217080L, first.getID());
    }

    @Test
    public void getTopRevision() throws Exception
    {
        try
        {
            enWiki.getTopRevision("Special:SpecialPages");
            fail("Attempted to get the page history of a special page.");
        }
        catch (UnsupportedOperationException expected)
        {
        }
        try
        {
            enWiki.getTopRevision("Media:Example.png");
            fail("Attempted to get the page history of a special page.");
        }
        catch (UnsupportedOperationException expected)
        {
        }
        assertNull("Non-existent page", enWiki.getTopRevision("dgfhd&fjklg"));
    }

    @Test
    public void exists() throws Exception
    {
        String[] titles = new String[] { "Main Page", "Tdkfgjsldf", "User:MER-C", "Wikipedia:Skfjdl", "Main Page", "Fish & chips" };
        boolean[] expected = new boolean[] { true, false, true, false, true, true };
        assertTrue("exists", Arrays.equals(expected, enWiki.exists(titles)));
    }

    @Test
    public void resolveRedirects() throws Exception
    {
        String[] titles = new String[] { "Main page", "Main Page", "sdkghsdklg", "Hello.jpg", "Main page", "Fish & chips" };
        String[] expected = new String[] { "Main Page", "Main Page", "sdkghsdklg", "Goatse.cx", "Main Page", "Fish and chips" };
        assertArrayEquals("resolveRedirects", expected, enWiki.resolveRedirects(titles));
        assertEquals("resolveRedirects: RTL", "الصفحة الرئيسية", arWiki.resolveRedirect("الصفحه الرئيسيه"));
    }

    @Test
    public void getLinksOnPage() throws Exception
    {
        assertArrayEquals("getLinksOnPage: non-existent page", new String[0], enWiki.getLinksOnPage("Skfls&jdkfs"));
        // User:MER-C/monobook.js has one link... despite it being preformatted (?!)
        assertArrayEquals("getLinksOnPage: page with no links", new String[0], enWiki.getLinksOnPage("User:MER-C/monobook.css"));
    }

    @Test
    public void getImagesOnPage() throws Exception
    {
        assertArrayEquals("getImagesOnPage: non-existent page", new String[0], enWiki.getImagesOnPage("Skflsj&dkfs"));
        assertArrayEquals("getImagesOnPage: page with no images", new String[0], enWiki.getImagesOnPage("User:MER-C/monobook.js"));
    }

    @Test
    public void getTemplates() throws Exception
    {
        String[] pages =
        {
            "sdkf&hsdklj", // non-existent
            // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/templates_test
            "User:MER-C/UnitTests/templates_test",
            // https://test.wikipedia.org/wiki/User:MER-C/monobook.js (no templates)
            "User:MER-C/monobook.js",
            "user:MER-C/UnitTests/templates test", // same as [1]
        };
        List<String>[] results = testWiki.getTemplates(pages);
        assertTrue("getTemplates: non-existent page", results[0].isEmpty());
        assertEquals("getTemplates", 1, results[1].size());
        assertEquals("getTemplates", "Template:La", results[1].get(0));
        assertTrue("getTemplates: page with no templates", results[2].isEmpty());
        assertEquals("getTemplates: duplicate", results[1], results[3]);

        assertEquals("getTemplates: namespace filter", 0, testWiki.getTemplates(pages[1], Wiki.MAIN_NAMESPACE).length);
        assertEquals("getTemplates: namespace filter", "Template:La", testWiki.getTemplates(pages[1], Wiki.TEMPLATE_NAMESPACE)[0]);
    }

    @Test
    public void getCategories() throws Exception
    {
        assertArrayEquals("getCategories: non-existent page", new String[0], enWiki.getCategories("sdkf&hsdklj"));
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        assertArrayEquals("getCategories: page with no categories", new String[0], testWiki.getCategories("User:MER-C/UnitTests/Delete"));
    }

    @Test
    public void getImageHistory() throws Exception
    {
        assertArrayEquals("getImageHistory: non-existent file", new Wiki.LogEntry[0], enWiki.getImageHistory("File:Sdfjgh&sld.jpg"));
        assertArrayEquals("getImageHistory: commons image", new Wiki.LogEntry[0], enWiki.getImageHistory("File:WikipediaSignpostIcon.svg"));
    }

    @Test
    public void getImage() throws Exception
    {
        File tempfile = File.createTempFile("wiki-java_getImage", null);
        assertFalse("getImage: non-existent file", enWiki.getImage("File:Sdkjf&sdlf.blah", tempfile));

        // non-thumbnailed Commons file
        // https://commons.wikimedia.org/wiki/File:Portrait_of_Jupiter_from_Cassini.jpg
        enWiki.getImage("File:Portrait of Jupiter from Cassini.jpg", tempfile);
        byte[] imageData = Files.readAllBytes(tempfile.toPath());
        byte[] hash = sha256.digest(imageData);
        assertEquals("getImage", "fc63c250bfce3f3511ccd144ca99b451111920c100ac55aaf3381aec98582035",
            String.format("%064x", new BigInteger(1, hash)));
        Files.delete(tempfile.toPath());
    }

    @Test
    public void getPageHistory() throws Exception
    {
        try
        {
            enWiki.getPageHistory("Special:SpecialPages");
            fail("Attempted to get the page history of a special page.");
        }
        catch (UnsupportedOperationException expected)
        {
        }
        try
        {
            enWiki.getPageHistory("Media:Example.png");
            fail("Attempted to get the page history of a media page.");
        }
        catch (UnsupportedOperationException expected)
        {
        }

        assertArrayEquals("getPageHistory: non-existent page", new Wiki.Revision[0], enWiki.getPageHistory("EOTkd&ssdf"));

        // test for RevisionDeleted revisions
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        Wiki.Revision[] history = testWiki.getPageHistory("User:MER-C/UnitTests/Delete");
        for (Wiki.Revision rev : history)
        {
            if (rev.getID() == 275553L)
            {
                assertTrue("revdeled history: content", rev.isContentDeleted());
                assertTrue("revdeled history: user", rev.isUserDeleted());
                assertTrue("revdeled history: summary", rev.isCommentDeleted());
                break;
            }
        }
    }

    @Test
    public void getDeletedHistory() throws Exception
    {
        try
        {
            enWiki.getPageHistory("Special:SpecialPages");
            fail("Attempted to get the deleted history of a special page.");
        }
        catch (UnsupportedOperationException expected)
        {
        }
        try
        {
            enWiki.getDeletedHistory("Media:Example.png");
            fail("Attempted to get the deleted history of a special page.");
        }
        catch (UnsupportedOperationException expected)
        {
        }
        // Test runs without logging in, therefore expect failure.
        try
        {
            enWiki.getDeletedHistory("Main Page");
            fail("Attempted to view deleted revisions while logged out.");
        }
        catch (SecurityException expected)
        {
        }
    }

    @Test
    public void move() throws Exception
    {
        try
        {
            enWiki.move("Special:SpecialPages", "New page name", "Not a reason");
            fail("Attempted to move a special page.");
        }
        catch (UnsupportedOperationException expected)
        {
        }
        try
        {
            enWiki.move("Media:Example.png", "New page name", "Not a reason");
            fail("Attempted to delete a special page.");
        }
        catch (UnsupportedOperationException expected)
        {
        }
    }

    @Test
    public void delete() throws Exception
    {
        try
        {
            enWiki.delete("Special:SpecialPages", "Not a reason");
            fail("Attempted to delete a special page.");
        }
        catch (UnsupportedOperationException expected)
        {
        }
        try
        {
            enWiki.delete("Media:Example.png", "Not a reason");
            fail("Attempted to delete a special page.");
        }
        catch (UnsupportedOperationException expected)
        {
        }
        // Test runs without logging in, therefore expect failure.
        try
        {
            enWiki.delete("User:MER-C", "Not a reason");
            fail("Attempted to delete while logged out.");
        }
        catch (SecurityException expected)
        {
        }
    }

    @Test
    public void undelete() throws Exception
    {
        try
        {
            enWiki.undelete("Special:SpecialPages", "Not a reason");
            fail("Attempted to delete a special page.");
        }
        catch (UnsupportedOperationException expected)
        {
        }
        try
        {
            enWiki.undelete("Media:Example.png", "Not a reason");
            fail("Attempted to delete a special page.");
        }
        catch (UnsupportedOperationException expected)
        {
        }
        // Test runs without logging in, therefore expect failure.
        try
        {
            enWiki.undelete("User:MER-C", "Not a reason");
            fail("Attempted to undelete while logged out.");
        }
        catch (SecurityException expected)
        {
        }
    }

    @Test
    public void block() throws Exception
    {
        try
        {
            enWiki.block("MER-C", "Not a reason", OffsetDateTime.MIN, null);
            fail("Attempted to block with an expiry time in the past.");
        }
        catch (IllegalArgumentException expected)
        {
        }
        // Test runs without logging in, therefore expect failure.
        try
        {
            enWiki.block("MER-C", "Not a reason", null, null);
            fail("Attempted to block while logged out.");
        }
        catch (SecurityException expected)
        {
        }
    }

    @Test
    public void unblock() throws Exception
    {
        // Test runs without logging in, therefore expect failure.
        try
        {
            enWiki.unblock("MER-C", "Not a reason");
            fail("Attempted to unblock while logged out.");
        }
        catch (SecurityException expected)
        {
        }
    }

    @Test
    public void revisionDelete() throws Exception
    {
        Wiki.Revision revision = testWiki.getRevision(349877L);
        Wiki.LogEntry[] logs = testWiki.getLogEntries(Wiki.DELETION_LOG, "delete", "MER-C",
            "File:Wiki.java test5.jpg", OffsetDateTime.parse("2018-03-18T00:00:00Z"),
            OffsetDateTime.parse("2018-03-16T00:00:00Z"), 50, Wiki.ALL_NAMESPACES);
        try
        {
            List<Wiki.Event> events = Arrays.asList(revision, logs[0]);
            testWiki.revisionDelete(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, "Not a reason", Boolean.FALSE, events);
            fail("Can't mix revisions and log entries in RevisionDelete.");
        }
        catch (IllegalArgumentException expected)
        {
        }
        // Test runs without logging in, therefore expect failure.
        try
        {
            testWiki.revisionDelete(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, "Not a reason", Boolean.FALSE, Arrays.asList(revision));
            fail("Attempted to RevisionDelete while logged out.");
        }
        catch (SecurityException expected)
        {
        }
    }

    @Test
    public void getIPBlockList() throws Exception
    {
        // https://en.wikipedia.org/wiki/Special:Blocklist/Nimimaan
        // see also getLogEntries() below
        Wiki.LogEntry[] le = enWiki.getBlockList("Nimimaan");
        assertEquals("getIPBlockList: ID not available", -1, le[0].getID());
        assertEquals("getIPBlockList: timestamp", "2016-06-21T13:14:54Z", le[0].getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertEquals("getIPBlockList: user", "MER-C", le[0].getUser());
        assertEquals("getIPBlockList: log", Wiki.BLOCK_LOG, le[0].getType());
        assertEquals("getIPBlockList: action", "block", le[0].getAction());
        assertEquals("getIPBlockList: target", "User:Nimimaan", le[0].getTitle());
        assertEquals("getIPBlockList: reason", "spambot", le[0].getComment());
//        assertEquals("getLogEntries/block: parameters", new Object[] {
//            false, true, // hard block (not anon only), account creation disabled,
//            false, true, // autoblock enabled, email disabled
//            true, "indefinite" // talk page access revoked, expiry
//        }, le[0].getDetails());

        // This IP address should not be blocked (it is reserved)
        le = enWiki.getBlockList("0.0.0.0");
        assertEquals("getIPBlockList: not blocked", 0, le.length);
    }

    @Test
    public void getLogEntries() throws Exception
    {
        // https://en.wikipedia.org/w/api.php?action=query&list=logevents&letitle=User:Nimimaan&format=xmlfm

        // Block log
        OffsetDateTime c = OffsetDateTime.parse("2016-06-30T23:59:59Z");
        Wiki.LogEntry[] le = enWiki.getLogEntries(Wiki.ALL_LOGS, null, null, "User:Nimimaan", c,
            null, 5, Wiki.ALL_NAMESPACES);
        assertEquals("getLogEntries: ID", 75695806L, le[0].getID());
        assertEquals("getLogEntries: timestamp", "2016-06-21T13:14:54Z", le[0].getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertEquals("getLogEntries/block: user", "MER-C", le[0].getUser());
        assertEquals("getLogEntries/block: log", Wiki.BLOCK_LOG, le[0].getType());
        assertEquals("getLogEntries/block: action", "block", le[0].getAction());
        assertEquals("getLogEntries: target", "User:Nimimaan", le[0].getTitle());
        assertEquals("getLogEntries: reason", "spambot", le[0].getComment());
        assertEquals("getLogEntries: parsed reason", "spambot", le[0].getParsedComment());
//        assertEquals("getLogEntries/block: parameters", new Object[] {
//            false, true, // hard block (not anon only), account creation disabled,
//            false, true, // autoblock enabled, email disabled
//            true, "indefinite" // talk page access revoked, expiry
//        }, le[0].getDetails());

        // New user log
        assertEquals("getLogEntries/newusers: user", "Nimimaan", le[1].getUser());
        assertEquals("getLogEntries/newusers: log", Wiki.USER_CREATION_LOG, le[1].getType());
        assertEquals("getLogEntries/newusers: action", "create", le[1].getAction());
        assertEquals("getLogEntries/newusers: reason", "", le[1].getComment());
        assertEquals("getLogEntries/newusers: reason", "", le[1].getParsedComment());
//        assertNull("getLogEntries/newusers: parameters", le[1].getDetails());

        // https://en.wikipedia.org/w/api.php?action=query&list=logevents&letitle=Talk:96th%20Test%20Wing/Temp&format=xmlfm

        // Move log
        le = enWiki.getLogEntries(Wiki.ALL_LOGS, null, null, "Talk:96th Test Wing/Temp",
            c, null, 5, Wiki.ALL_NAMESPACES);
        assertEquals("getLogEntries/move: log", Wiki.MOVE_LOG, le[0].getType());
        assertEquals("getLogEntries/move: action", "move", le[0].getAction());
        // TODO: test for new title, redirect suppression

        // RevisionDeleted log entries, no access
        // https://test.wikipedia.org/w/api.php?format=xmlfm&action=query&list=logevents&letitle=User%3AMER-C%2FTest
        le = testWiki.getLogEntries(Wiki.ALL_LOGS, null, null, "User:MER-C/Test");
        assertNull("getLogEntries: reason hidden", le[0].getComment());
        assertNull("getLogEntries: reason hidden", le[0].getParsedComment());
        assertTrue("getLogEntries: reason hidden", le[0].isCommentDeleted());
        assertNull("getLogEntries: user hidden", le[0].getUser());
        assertTrue("getLogEntries: user hidden", le[0].isUserDeleted());
        // https://test.wikipedia.org/w/api.php?format=xmlfm&action=query&list=logevents&leuser=MER-C
        //     &lestart=20161002050030&leend=20161002050000&letype=delete
        le = testWiki.getLogEntries(Wiki.DELETION_LOG, null, "MER-C", null,
            OffsetDateTime.parse("2016-10-02T05:00:30Z"),
            OffsetDateTime.parse("2016-10-02T05:00:00Z"),
            Integer.MAX_VALUE, Wiki.ALL_NAMESPACES);
        assertNull("getLogEntries: action hidden", le[0].getTitle());
        assertTrue("getLogEntries: action hidden", le[0].isContentDeleted());
    }

    @Test
    public void getPageInfo() throws Exception
    {
        String[] pages = new String[] { "Main Page", "IPod", "Main_Page", "Special:Specialpages" };
        Map<String, Object>[] pageinfo = enWiki.getPageInfo(pages);

        // Main Page
        Map<String, Object> protection = (Map<String, Object>)pageinfo[0].get("protection");
        assertEquals("getPageInfo: Main Page edit protection level", Wiki.FULL_PROTECTION, protection.get("edit"));
        assertNull("getPageInfo: Main Page edit protection expiry", protection.get("editexpiry"));
        assertEquals("getPageInfo: Main Page move protection level", Wiki.FULL_PROTECTION, protection.get("move"));
        assertNull("getPageInfo: Main Page move protection expiry", protection.get("moveexpiry"));
        assertTrue("getPageInfo: Main Page cascade protection", (Boolean)protection.get("cascade"));
        assertEquals("getPageInfo: Main Page display title", "Main Page", pageinfo[0].get("displaytitle"));
        assertEquals("getPageInfo: identity", pages[0], pageinfo[0].get("inputpagename"));
        assertEquals("getPageInfo: normalized identity", pages[0], pageinfo[0].get("pagename"));

        // different display title
        assertEquals("getPageInfo: iPod display title", "iPod", pageinfo[1].get("displaytitle"));

        // Main_Page (duplicate, should be identical except for inputpagename)
        assertEquals("getPageInfo: identity (2)", pages[2], pageinfo[2].get("inputpagename"));
        pageinfo[0].remove("inputpagename");
        pageinfo[2].remove("inputpagename");
        assertEquals("getPageInfo: duplicate", pageinfo[0], pageinfo[2]);

        // Special page = return null
        assertNull("getPageInfo: special page", pageinfo[3]);
    }

    @Test
    public void getFileMetadata() throws Exception
    {
        assertNull("getFileMetadata: non-existent file", enWiki.getFileMetadata("File:Lweo&pafd.blah"));
        assertNull("getFileMetadata: commons image", enWiki.getFileMetadata("File:WikipediaSignpostIcon.svg"));

        // further tests blocked on MediaWiki API rewrite
        // see https://phabricator.wikimedia.org/T89971
    }

    @Test
    public void getDuplicates() throws Exception
    {
        assertArrayEquals("getDuplicates: non-existent file", new String[0], enWiki.getDuplicates("File:Sdfj&ghsld.jpg"));
    }

    @Test
    public void getInterWikiLinks() throws Exception
    {
        Map<String, String> temp = enWiki.getInterWikiLinks("Gkdfkkl&djfdf");
        assertTrue("getInterWikiLinks: non-existent page", temp.isEmpty());
    }

    @Test
    public void getExternalLinksOnPages() throws Exception
    {
        List<List<String>> links = enWiki.getExternalLinksOnPage(Arrays.asList("Gdkgfskl&dkf", "User:MER-C/monobook.js"));
        assertTrue("getExternalLinksOnPage: non-existent page", links.get(0).isEmpty());
        assertTrue("getExternalLinksOnPage: page with no links", links.get(1).isEmpty());

        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Linkfinder
        links = testWiki.getExternalLinksOnPage(Arrays.asList("User:MER-C/UnitTests/Linkfinder"));
        List<String> expected = Arrays.asList("http://spam.example.com", "http://www.example.net", "https://en.wikipedia.org");
        assertEquals("getExtenalLinksOnPage", expected, links.get(0));
    }

    @Test
    public void getSectionText() throws Exception
    {
        assertEquals("getSectionText(): section 0", "This is section 0.", testWiki.getSectionText("User:MER-C/UnitTests/SectionTest", 0));
        assertEquals("getSectionText(): section 2", "===Section 3===\nThis is section 2.",
            testWiki.getSectionText("User:MER-C/UnitTests/SectionTest", 2));
        assertNull("getSectionText(): non-existent section", enWiki.getSectionText("User:MER-C/monobook.css", 4920));
        try
        {
            enWiki.getSectionText("User:MER-C/monobook.css", -50);
            fail("getSectionText: negative section number, should have thrown an exception.");
        }
        catch (IllegalArgumentException expected)
        {
        }
    }

    @Test
    public void random() throws Exception
    {
        // The results of this query are obviously non-deterministic, but we can
        // check whether we get the right namespace.
        for (int i = 0; i < 3; i++)
        {
            String random = enWiki.random();
            assertEquals("random: main namespace", Wiki.MAIN_NAMESPACE, enWiki.namespace(random));
            random = enWiki.random(Wiki.PROJECT_NAMESPACE, Wiki.USER_NAMESPACE);
            int temp = enWiki.namespace(random);
            if (temp != Wiki.PROJECT_NAMESPACE && temp != Wiki.USER_NAMESPACE)
                fail("random: multiple namespaces");
            random = enWiki.random(Wiki.PROJECT_NAMESPACE, Wiki.MEDIA_NAMESPACE, Wiki.SPECIAL_NAMESPACE, -1245, 999999);
            assertEquals("random: ignoring invalid namespaces", Wiki.PROJECT_NAMESPACE, enWiki.namespace(random));
        }
    }

    @Test
    public void timezone()
    {
        assertEquals(ZoneId.of("UTC"), enWiki.timezone());
    }
    
    @Test
    public void usesCapitalLinks()
    {
        assertTrue("capital links: en.wp", enWiki.usesCapitalLinks());
        assertFalse("capital links: en.wikt", enWikt.usesCapitalLinks());
    }
    
    @Test
    public void getLocale()
    {
        assertEquals("locale: en.wp", Locale.ENGLISH, enWiki.locale());
        assertEquals("locale: de.wp", Locale.GERMAN, deWiki.locale());
    }
    
    @Test
    public void getInstalledExtensions()
    {
        List<String> extensions = enWiki.installedExtensions();
        assertTrue(extensions.contains("Math"));
        assertTrue(extensions.contains("SpamBlacklist"));
        assertFalse(extensions.contains("NotAnExtension"));
    }

    @Test
    public void normalize() throws Exception
    {
        assertEquals("normalize", "Blah", enWiki.normalize("Blah"));
        assertEquals("normalize", "Blah", enWiki.normalize("blah"));
        assertEquals("normalize", "File:Blah.jpg", enWiki.normalize("File:Blah.jpg"));
        assertEquals("normalize", "File:Blah.jpg", enWiki.normalize("file:blah.jpg"));
        assertEquals("normalize", "Category:Wikipedia:blah", enWiki.normalize("Category:Wikipedia:blah"));
        assertEquals("normalize: namespace i18n", "Hilfe Diskussion:Glossar", deWiki.normalize("Help talk:Glossar"));
        assertEquals("normalize: namespace alias", "Wikipedia:V", enWiki.normalize("WP:V"));
        // variants with different cases undocumented

        // capital links = false
        assertEquals("normalize", "blah", enWikt.normalize("blah"));
        assertEquals("normalize", "Wiktionary:main page", enWikt.normalize("Wiktionary:main page"));
        assertEquals("normalize", "Wiktionary:main page", enWikt.normalize("wiktionary:main page"));
    }

    @Test
    public void pageHasTemplate() throws Exception
    {
        boolean[] b = enWiki.pageHasTemplate(new String[]
        {
            "Wikipedia:Articles for deletion/Log/2016 September 20",
            "Main Page",
            "dsigusodgusdigusd" // non-existent, should be false
        }, "Wikipedia:Articles for deletion/FMJAM");
        assertTrue("pageHasTemplate: true", b[0]);
        assertFalse("pageHasTemplate: false", b[1]);
        assertFalse("pageHasTemplate: non-existent", b[2]);
    }
    
    @Test
    public void whatTranscludesHere() throws Exception
    {
        String title = "Wikipedia:Articles for deletion/MegaMeeting.com";
        String[] results = enWiki.whatTranscludesHere(title);
        assertArrayEquals("transclusions", new String[] { "Wikipedia:Articles for deletion/Log/2018 April 23" }, results);
        assertEquals("transclusions: namespace filter", 0, enWiki.whatTranscludesHere(title, Wiki.MAIN_NAMESPACE).length);
    }
    
    @Test
    public void getUploads() throws Exception
    {
        Wiki.User[] users = enWiki.getUsers(new String[]
        {
            "LakeishaDurham0", // blocked spambot
            "Mifter" // https://en.wikipedia.org/wiki/Special:ListFiles/Mifter
        }); 
        assertEquals("getUploads: no uploads", 0, enWiki.getUploads(users[0]).length);
        OffsetDateTime odt = OffsetDateTime.parse("2017-03-05T17:59:00Z");
        try
        {
            enWiki.getUploads(users[1], odt, odt.minusMinutes(20));
        }
        catch (IllegalArgumentException expected)
        {
        }
        Wiki.LogEntry[] results = enWiki.getUploads(users[1], odt, odt.plusMinutes(20));
        assertEquals("getUploads: functionality check (0)", 3, results.length);
        assertEquals("getUploads: functionality check (1)", "File:Padlock-blue.svg", results[0].getTitle());
        assertEquals("getUploads: functionality check (2)", "File:Padlock-silver-light.svg", results[1].getTitle());
        assertEquals("getUploads: functionality check (3)", "File:Padlock-pink.svg", results[2].getTitle());
    }

    @Test
    public void getRevision() throws Exception
    {
        // https://en.wikipedia.org/w/index.php?title=Wikipedia_talk%3AWikiProject_Spam&oldid=597454682
        Wiki.Revision rev = enWiki.getRevision(597454682L);
        assertEquals("getRevision: title", "Wikipedia talk:WikiProject Spam", rev.getTitle());
        assertEquals("getRevision: timestamp", "2014-02-28T00:40:31Z", rev.getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertEquals("getRevision: user", "Lowercase sigmabot III", rev.getUser());
        assertEquals("getRevision: summary", "Archiving 3 discussion(s) to [[Wikipedia talk:WikiProject Spam/2014 Archive Feb 1]]) (bot",
            rev.getComment());
        String parsedcomment = "Archiving 3 discussion(s) to <a href=\"https://en.wikipedia.org/wiki/Wikipedia_talk:WikiProject_Spam/2014_Archive_Feb_1\" "
            + "title=\"Wikipedia talk:WikiProject Spam/2014 Archive Feb 1\">Wikipedia talk:WikiProject Spam/2014 Archive Feb 1</a>) (bot";
        assertEquals("getRevision: parsed summary with link", parsedcomment, rev.getParsedComment());
        assertEquals("getRevision: sha1", "540a2b3501e4d15729ea25ec3238da9ad0dd6dc4", rev.getSha1());
        assertEquals("getRevision: size", 4286, rev.getSize());
        assertEquals("getRevision: revid", 597454682L, rev.getID());
        assertEquals("getRevision: previous", 597399794L, rev.getPrevious().getID());
        // assertEquals("getRevision: next", 597553957L, rev.getNext().getRevid());
        assertTrue("getRevision: minor", rev.isMinor());
        assertFalse("getRevision: new", rev.isNew());
        assertFalse("getRevison: bot", rev.isBot());
        assertFalse("getRevision: user not revdeled", rev.isUserDeleted());
        assertFalse("getRevision: summary not revdeled", rev.isCommentDeleted());
        assertFalse("getRevision: content not deleted", rev.isContentDeleted());
        assertFalse("getRevision: page not deleted", rev.isPageDeleted());

        // revdel, logged out
        // https://en.wikipedia.org/w/index.php?title=Imran_Khan_%28singer%29&oldid=596714684
        rev = enWiki.getRevision(596714684L);
        assertNull("getRevision: summary revdeled", rev.getComment());
        assertNull("getRevision: summary revdeled", rev.getParsedComment());
        assertNull("getRevision: user revdeled", rev.getUser());
        assertNull("getRevision: sha1/content revdeled", rev.getSha1());
        assertTrue("getRevision: user revdeled", rev.isUserDeleted());
        assertTrue("getRevision: summary revdeled", rev.isCommentDeleted());
        assertTrue("getRevision: content revdeled", rev.isContentDeleted());

        // Revision has been deleted (not RevisionDeleted)
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        assertNull("getRevision: page deleted", testWiki.getRevision(217078L));

        assertNull("getRevision: large revid", testWiki.getRevision(1L << 62));
    }

    @Test
    public void diff() throws Exception
    {
        // must specify from/to content
        Map<String, Object> from = new HashMap<>();
        Map<String, Object> to = new HashMap<>();
        try
        {
            from.put("xxx", "yyy");
            to.put("revid", 738178354L);
            enWiki.diff(from, -1, to, -1);
            fail("Failed to specify from content.");
        }
        catch (IllegalArgumentException | NoSuchElementException expected)
        {
        }
        from.clear();
        to.clear();
        try
        {
            from.put("revid", 738178354L);
            to.put("xxx", "yyy");
            enWiki.diff(from, -1, to, -1);
            fail("Failed to specify to content.");
        }
        catch (IllegalArgumentException | NoSuchElementException expected)
        {
            to.clear();
        }

        // https://en.wikipedia.org/w/index.php?title=Dayo_Israel&oldid=738178354&diff=prev
        from.put("revid", 738178354L);
        to.put("revid", Wiki.PREVIOUS_REVISION);
        assertEquals("diff: dummy edit", "", enWiki.diff(from, -1, to, -1));
        // https://en.wikipedia.org/w/index.php?title=Source_Filmmaker&diff=804972897&oldid=803731343
        // The MediaWiki API does not distinguish between a dummy edit and no
        // difference. Both are now set to the empty string.
        from.put("revid", 803731343L);
        to.put("revid", 804972897L);
        assertEquals("diff: no difference", "", enWiki.diff(from, -1, to, -1));
        // no deleted pages allowed
        // FIXME: broken because makeHTTPRequest() swallows the API error
        // actual = enWiki.diff("Create a page", 0L, null, -1, null, 804972897L, null, -1);
        // assertNull("diff: to deleted", actual);
        // actual = enWiki.diff(null, 804972897L, null, -1, "Create a page", 0L, null, -1);
        // no RevisionDeleted revisions allowed (also broken)
        // https://en.wikipedia.org/w/index.php?title=Imran_Khan_%28singer%29&oldid=596714684
        // actual = enWiki.diff(null, 596714684L, null, -1, null, Wiki.NEXT_REVISION, null, -1);
        // assertNull("diff: from deleted revision", actual);

        // check for sections that don't exist
        from.put("revid", 803731343L);
        to.put("revid", 804972897L);
        assertNull("diff: no such from section", enWiki.diff(from, 4920, to, -1));
        assertNull("diff: no such to section", enWiki.diff(from, -1, to, 4920));
        // bad revids
        from.put("revid", 1L << 62);
        to.put("revid", 803731343L);
        assertNull("diff: bad from revid", enWiki.diff(from, -1, to, -1));
        from.put("revid", 803731343L);
        to.put("revid", 1L << 62);
        assertNull("diff: bad to revid", enWiki.diff(from, -1, to, -1));
    }

    @Test
    public void contribs() throws Exception
    {
        String[] users = new String[]
        {
            "Dsdlgfkjsdlkfdjilgsujilvjcl", // should not exist
            "0.0.0.0", // IP address
            "Allancake" // revision deleted
        };
        List<Wiki.Revision>[] edits = enWiki.contribs(users, "", null, null, null);

        assertTrue("contribs: non-existent user", edits[0].isEmpty());
        assertTrue("contribs: IP address with no edits", edits[1].isEmpty());
        edits[2].forEach(rev ->
        {
            if (rev.getID() == 724989913L)
            {
                assertTrue("contribs: summary deleted", rev.isContentDeleted());
                assertTrue("contribs: content deleted", rev.isContentDeleted());
            }
        });

        // check rcoptions and namespace filter
        // https://test.wikipedia.org/wiki/Blah_blah_2
        users = new String[] { "MER-C" };
        Map<String, Boolean> options = new HashMap<>();
        options.put("new", Boolean.TRUE);
        options.put("top", Boolean.TRUE);
        edits = testWiki.contribs(users, "", null, null, options, Wiki.MAIN_NAMESPACE);
        assertEquals("contribs: filtered", 120919L, edits[0].get(0).getID());
        // not implemented in MediaWiki API
        // assertEquals("contribs: sha1 present", "bcdb66a63846bacdf39f5c52a7d2cc5293dbde3e", edits[0].get(0).getSha1());
        for (Wiki.Revision rev : edits[0])
            assertEquals("contribs: namespace", Wiki.MAIN_NAMESPACE, testWiki.namespace(rev.getTitle()));
    }

    @Test
    public void getUsers() throws Exception
    {
        String[] usernames = new String[]
        {
            "127.0.0.1", // IP address
            "MER-C",
            "DKdsf;lksd", // should be non-existent...
            "ZZRBrenda08", // blocked spambot with 2 edits
            "127.0.0.0/24" // IP range
        };
        Wiki.User[] users = enWiki.getUsers(usernames);
        assertNull("getUsers: IP address", users[0]);
        assertNull("getUsers: non-existent user", users[2]);
        assertNull("getUsers: IP address range", users[4]);

        assertEquals("getUsers: normalized username", usernames[1], users[1].getUsername());
        assertFalse("getUsers: blocked", users[1].isBlocked());
        assertEquals("getUsers: gender", Wiki.Gender.unknown, users[1].getGender());
        assertEquals("getUsers: registration", "2006-07-07T10:52:41Z",
            users[1].getRegistrationDate().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertTrue("getUsers: email", users[1].canBeEmailed());

        List<String> groups = users[1].getGroups();
        List<String> temp = Arrays.asList("*", "autoconfirmed", "user", "sysop");
        assertTrue("getUsers: groups", groups.containsAll(temp));

        // check (subset of) rights
        List<String> rights = users[1].getRights();
        temp = Arrays.asList("apihighlimits", "delete", "block", "editinterface");
        assertTrue("getUsers: groups", rights.containsAll(temp));

        assertEquals("getUsers: username(2)", usernames[3], users[3].getUsername());
        assertEquals("getUsers: editcount", 2, users[3].countEdits());
        assertTrue("getUsers: actually blocked", users[3].isBlocked());
    }

    @Test
    public void getPageText() throws Exception
    {
        try
        {
            testWiki.getPageText("Special:Specialpages");
            fail("Tried to get page text for a special page!");
        }
        catch (UnsupportedOperationException expected)
        {
        }
        try
        {
            testWiki.getPageText("Media:Example.png");
            fail("Tried to get page text for a media page!");
        }
        catch (UnsupportedOperationException expected)
        {
        }

        String[] text = testWiki.getPageText(new String[]
        {
            "User:MER-C/UnitTests/Delete", // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
            "User:MER-C/UnitTests/pagetext", // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/pagetext
            "Gsgdksgjdsg", // https://test.wikipedia.org/wiki/Gsgdksgjdsg (does not exist)
            "User:NikiWiki/EmptyPage" // https://test.wikipedia.org/wiki/User:NikiWiki/EmptyPage (empty)

        });
        // API result does not include the terminating new line
        assertEquals("getPageText", text[0], "This revision is not deleted!");
        assertEquals("page text: decoding", text[1], "&#039;&#039;italic&#039;&#039;" +
            "\n'''&amp;'''\n&&\n&lt;&gt;\n<>\n&quot;");
        assertNull("getPageText: non-existent page", text[2]);
        assertEquals("getPageText: empty page", text[3], "");
    }

    @Test
    public void getText() throws Exception
    {
        long[] ids =
        {
            230472L, // https://test.wikipedia.org/w/index.php?oldid=230472 (decoding test)
            322889L, // https://test.wikipedia.org/w/index.php?oldid=322889 (empty revision)
            275553L, // https://test.wikipedia.org/w/index.php?oldid=275553 (RevisionDeleted)
            316531L, // https://test.wikipedia.org/w/index.php?oldid=316531 (first revision on same page)
            316533L  // https://test.wikipedia.org/w/index.php?oldid=316533 (second revision on same page)
        };
        String[] text = testWiki.getText(null, ids, -1);
        assertEquals("revision text: decoding", "&#039;&#039;italic&#039;&#039;" +
            "\n'''&amp;'''\n&&\n&lt;&gt;\n<>\n&quot;", text[0]);
        assertEquals("revision text: empty revision", text[1], "");
        assertNull("revision text: content deleted", text[2]);
        assertEquals("revision text: same page", "Testing 0.2786153173518522", text[3]);
        assertEquals("revision text: same page", "Testing 0.28713760508426645", text[4]);
    }

    @Test
    public void parse() throws Exception
    {
        HashMap<String, Object> content = new HashMap<>();
        content.put("title", "Hello");
        assertNull("parse: no such section", enWiki.parse(content, 50, true));
        // FIXME: currently broken because makeHTTPRequest swallows the API error
        // content.put("title", "Create a page");
        // assertNull("parse: deleted page", enWiki.parse(content, -1, true));
        content.clear();
        content.put("revid", 1L << 62);
        assertNull("parse: bad revid", enWiki.parse(content, -1, true));
        // https://en.wikipedia.org/w/index.php?oldid=596714684
        try
        {
            content.put("revid", 596714684L);
            assertNull("parse: revisiondeleted revision", enWiki.parse(content, -1, true));
        }
        catch (SecurityException expected)
        {
        }
        try
        {
            enWiki.parse(Collections.emptyMap(), -1, true);
            fail("Parse: did not specify content to parse.");
        }
        catch (IllegalArgumentException | NoSuchElementException expected)
        {
        }
    }

    @Test
    public void search() throws Exception
    {
        Map<String, Object>[] results = testWiki.search("dlgsjdglsjdgljsgljsdlg", Wiki.MEDIAWIKI_NAMESPACE);
        assertEquals("search: no results", 0, results.length);
        https://test.wikipedia.org/w/api.php?action=query&list=search&srsearch=User:%20subpageof:MER-C/UnitTests%20delete
        results = testWiki.search("User: subpageof:MER-C/UnitTests delete", Wiki.USER_NAMESPACE);
        assertEquals("search: results", 2, results.length);
        Map<String, Object> result = results[0];
        assertEquals("search: title", "User:MER-C/UnitTests/Delete", result.get("title"));
        assertEquals("search: snippet", "This revision is not <span class=\"searchmatch\">deleted</span>!", result.get("snippet"));
        assertEquals("search: size", 29, result.get("size"));
        assertEquals("search: word count", 5, result.get("wordcount"));
        assertEquals("search: lastedittime", OffsetDateTime.parse("2016-06-16T08:40:17Z"), result.get("lastedittime"));
    }

    @Test
    public void recentChanges() throws Exception
    {
        // The results of this query will never be known in advance, so this is
        // by necessity an incomplete test. That said, there are a few things we
        // can test for...
        Wiki.Revision[] rc = enWiki.recentChanges(10);
        assertEquals("recentchanges: length", rc.length, 10);
        // Check if the changes are actually recent (i.e. in the last 10 minutes).
        assertTrue("recentchanges: recentness",
            rc[9].getTimestamp().isAfter(OffsetDateTime.now(ZoneId.of("UTC")).minusMinutes(10)));
        // Check namespace filtering
        rc = enWiki.recentChanges(10, new int[] { Wiki.TALK_NAMESPACE });
        for (Wiki.Revision rev : rc)
            assertEquals("recentchanges: namespace filter", Wiki.TALK_NAMESPACE, enWiki.namespace(rev.getTitle()));
        // check options filtering
        Map<String, Boolean> options = new HashMap<>();
        options.put("minor", Boolean.FALSE);
        options.put("bot", Boolean.TRUE);
        rc = enWiki.recentChanges(10, options);
        for (Wiki.Revision rev : rc)
        {
            assertTrue("recentchanges: options", rev.isBot());
            assertFalse("recentchanges: options", rev.isMinor());
        }
    }

    @Test
    public void allUsersInGroup() throws Exception
    {
        assertArrayEquals("allUsersInGroup: nonsense", new String[0], testWiki.allUsersInGroup("sdfkd|&"));
        assertArrayEquals("allUsersInGroup: founder", new String[] { "Jimbo Wales" }, enWiki.allUsersInGroup("founder"));
    }

    @Test
    public void allUserswithRight() throws Exception
    {
        assertArrayEquals("allUsersWithRight: nonsense", new String[0], testWiki.allUsersWithRight("sdfkd|&"));
    }

    @Test
    public void constructNamespaceString() throws Exception
    {
        String actual = enWiki.constructNamespaceString(new int[] { 3, 2, 1, 2, -10 });
        assertEquals("constructNamespaceString", "1|2|3", actual);
    }

    @Test
    public void constructTitleString() throws Exception
    {
        String[] titles = new String[102];
        for (int i = 0; i < titles.length; i++)
            titles[i] = "a" + i;
        titles[101] = "A34"; // should be removed
        List<String> expected = new ArrayList<>();
        // slowmax == 50 for Wikimedia wikis if not logged in
        expected.add("A0|A1|A10|A100|A11|A12|A13|A14|A15|A16|A17|A18|A19|A2|" +
            "A20|A21|A22|A23|A24|A25|A26|A27|A28|A29|A3|A30|A31|A32|A33|A34|" +
            "A35|A36|A37|A38|A39|A4|A40|A41|A42|A43|A44|A45|A46|A47|A48|A49|" +
            "A5|A50|A51|A52");
        expected.add("A53|A54|A55|A56|A57|A58|A59|A6|A60|A61|A62|A63|A64|A65|" +
            "A66|A67|A68|A69|A7|A70|A71|A72|A73|A74|A75|A76|A77|A78|A79|A8|" +
            "A80|A81|A82|A83|A84|A85|A86|A87|A88|A89|A9|A90|A91|A92|A93|A94|" +
            "A95|A96|A97|A98");
        expected.add("A99");
        List<String> actual = enWiki.constructTitleString(titles);
        assertEquals("constructTitleString", expected, actual);
    }

    // INNER CLASS TESTS

    @Test
    public void revisionCompareTo() throws Exception
    {
        // https://en.wikipedia.org/w/index.php?title=Azerbaijan&offset=20180125204500&action=history
        long[] oldids = { 822342083L, 822341440L };
        Wiki.Revision[] revisions = enWiki.getRevisions(oldids);
        assertEquals("Revision.compareTo: self", 0, revisions[0].compareTo(revisions[0]));
        assertTrue("Revision.compareTo: before", revisions[1].compareTo(revisions[0]) < 0);
        assertTrue("Revision.compareTo: after",  revisions[0].compareTo(revisions[1]) > 0);
    }

    @Test
    public void revisionEqualsAndHashCode() throws Exception
    {
        // https://test.wikipedia.org/wiki/Special:Permanentlink/217080
        Wiki.Revision rev1 = testWiki.getFirstRevision("User:MER-C/UnitTests/Delete");
        Wiki.Revision rev2 = testWiki.getRevision(217080L);
        assertEquals("Revision.equals", rev1, rev2);
        assertEquals("Revision.hashCode", rev1.hashCode(), rev2.hashCode());
        // https://test.wikipedia.org/wiki/Special:Permanentlink/275553
        // RevisionDeleted, therefore need to test for NPEs
        rev1 = testWiki.getRevision(275553L);
        rev2 = testWiki.getPageHistory("User:MER-C/UnitTests/Delete", OffsetDateTime.parse("2016-01-01T00:00:00Z"), OffsetDateTime.parse("2016-06-16T08:40:00Z"), false)[0];
        assertEquals("Revision.equals (NPE)", rev1, rev2);
        assertEquals("Revision.hashCode (NPE)", rev1.hashCode(), rev2.hashCode());
    }

    @Test
    public void revisionPermanentURL() throws Exception
    {
        Wiki.Revision rev = enWiki.getRevision(822342083L);
        assertEquals("Revision.permanentURL", "https://en.wikipedia.org/w/index.php?oldid=822342083", rev.permanentURL());
    }

    @Test
    public void userIsAllowedTo() throws Exception
    {
        Wiki.User user = enWiki.getUser("LornaIln046035"); // spambot
        assertTrue("User.isAllowedTo: true", user.isAllowedTo("read"));
        assertFalse("User.isAllowedTo: false", user.isAllowedTo("checkuser"));
        assertFalse("User.isAllowedTo: nonsense input", user.isAllowedTo("sdlkghsdlkgsd"));
    }

    @Test
    public void userIsA() throws Exception
    {
        Wiki.User me = testWiki.getUser("MER-C");
        assertTrue("User.isA: true", me.isA("sysop"));
        assertFalse("User.isA: false", me.isA("templateeditor"));
        assertFalse("User.isA: nonsense input", me.isA("sdlkghsdlkgsd"));
    }

    @Test
    public void userCreatedPages() throws Exception
    {
        Wiki.User me = testWiki.getUser("MER-C");
        String[] pages = me.createdPages(Wiki.MAIN_NAMESPACE);
        assertEquals("createdPages", "Wiki.java Test Page", pages[pages.length - 1]);
    }
}
