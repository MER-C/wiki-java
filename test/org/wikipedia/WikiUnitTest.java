/**
 *  @(#)WikiUnitTest.java 0.34 13/01/2018
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
import java.net.URLEncoder;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.security.auth.login.CredentialNotFoundException;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *  Unit tests for Wiki.java
 *  @author MER-C
 */
public class WikiUnitTest
{
    private static Wiki enWiki, deWiki, arWiki, testWiki, enWikt;
    private static MessageDigest sha256;
    
    public WikiUnitTest()
    {
    }
    
    /**
     *  Initialize wiki objects.
     *  @throws Exception if a network error occurs
     */
    @BeforeClass
    public static void setUpClass() throws Exception
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
        enWiki.setAssertionMode(Wiki.ASSERT_NONE);
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
        testWiki.setResolveRedirects(false);
        enWiki.setResolveRedirects(false);
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
        enWiki.setQueryLimit(Integer.MAX_VALUE);
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
        assertEquals("getFirstRevision", 217080L, first.getRevid());
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
        tempfile.delete();
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
            if (rev.getRevid() == 275553L)
            {
                assertTrue("revdeled history: content", rev.isContentDeleted());
                assertTrue("revdeled history: user", rev.isUserDeleted());
                assertTrue("revdeled history: summary", rev.isSummaryDeleted());
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
        catch (CredentialNotFoundException expected)
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
        catch (CredentialNotFoundException expected)
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
        catch (CredentialNotFoundException expected)
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
        catch (CredentialNotFoundException expected)
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
        catch (CredentialNotFoundException expected)
        {
        }
    }
    
    @Test
    public void getIPBlockList() throws Exception
    {
        // https://en.wikipedia.org/wiki/Special:Blocklist/Nimimaan
        // see also getLogEntries() below
        Wiki.LogEntry[] le = enWiki.getBlockList("Nimimaan");
        assertEquals("getIPBlockList: ID not available", -1, le[0].getLogID());
        assertEquals("getIPBlockList: timestamp", "2016-06-21T13:14:54Z", le[0].getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertEquals("getIPBlockList: user", "MER-C", le[0].getUser().getUsername());
        assertEquals("getIPBlockList: log", Wiki.BLOCK_LOG, le[0].getType());
        assertEquals("getIPBlockList: action", "block", le[0].getAction());
        assertEquals("getIPBlockList: target", "User:Nimimaan", le[0].getTarget());
        assertEquals("getIPBlockList: reason", "spambot", le[0].getReason());
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
        assertEquals("getLogEntries: ID", 75695806L, le[0].getLogID());
        assertEquals("getLogEntries: timestamp", "2016-06-21T13:14:54Z", le[0].getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertEquals("getLogEntries/block: user", "MER-C", le[0].getUser().getUsername());
        assertEquals("getLogEntries/block: log", Wiki.BLOCK_LOG, le[0].getType());
        assertEquals("getLogEntries/block: action", "block", le[0].getAction());
        assertEquals("getLogEntries: target", "User:Nimimaan", le[0].getTarget());
        assertEquals("getLogEntries: reason", "spambot", le[0].getReason());
//        assertEquals("getLogEntries/block: parameters", new Object[] {
//            false, true, // hard block (not anon only), account creation disabled,
//            false, true, // autoblock enabled, email disabled
//            true, "indefinite" // talk page access revoked, expiry
//        }, le[0].getDetails());
        
        // New user log
        assertEquals("getLogEntries/newusers: user", "Nimimaan", le[1].getUser().getUsername());
        assertEquals("getLogEntries/newusers: log", Wiki.USER_CREATION_LOG, le[1].getType());
        assertEquals("getLogEntries/newusers: action", "create", le[1].getAction());
        assertEquals("getLogEntries/newusers: reason", "", le[1].getReason());
//        assertNull("getLogEntries/newusers: parameters", le[1].getDetails());
        
        // https://en.wikipedia.org/w/api.php?action=query&list=logevents&letitle=Talk:96th%20Test%20Wing/Temp&format=xmlfm
        
        // Move log
        le = enWiki.getLogEntries(Wiki.ALL_LOGS, null, null, "Talk:96th Test Wing/Temp",
            c, null, 5, Wiki.ALL_NAMESPACES);
        assertEquals("getLogEntries/move: log", Wiki.MOVE_LOG, le[0].getType());
        assertEquals("getLogEntries/move: action", "move", le[0].getAction());
        // TODO: test for new title, redirect suppression
        
        // Patrol log
        assertEquals("getLogEntries/patrol: log", Wiki.PATROL_LOG, le[1].getType());
        assertEquals("getLogEntries/patrol: action", "autopatrol", le[1].getAction());
        
        // RevisionDeleted log entries, no access
        // https://test.wikipedia.org/w/api.php?format=xmlfm&action=query&list=logevents&letitle=User%3AMER-C%2FTest
        le = testWiki.getLogEntries(Wiki.ALL_LOGS, null, null, "User:MER-C/Test");
        assertNull("getLogEntries: reason hidden", le[0].getReason());
        assertTrue("getLogEntries: reason hidden", le[0].isReasonDeleted());
        assertNull("getLogEntries: user hidden", le[0].getUser());
        assertTrue("getLogEntries: user hidden", le[0].isUserDeleted());
        // https://test.wikipedia.org/w/api.php?format=xmlfm&action=query&list=logevents&leuser=MER-C
        //     &lestart=20161002050030&leend=20161002050000&letype=delete
        le = testWiki.getLogEntries(Wiki.DELETION_LOG, null, "MER-C", null,
            OffsetDateTime.parse("2016-10-02T05:00:30Z"),
            OffsetDateTime.parse("2016-10-02T05:00:00Z"), 
            Integer.MAX_VALUE, Wiki.ALL_NAMESPACES);
        assertNull("getLogEntries: action hidden", le[0].getTarget());
        assertTrue("getLogEntries: action hidden", le[0].isTargetDeleted());
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
    public void getExternalLinksOnPage() throws Exception
    {
        assertArrayEquals("getExternalLinksOnPage: non-existent page", new String[0], enWiki.getExternalLinksOnPage("Gdkgfskl&dkf"));
        assertArrayEquals("getExternalLinksOnPage: page with no links", new String[0], enWiki.getExternalLinksOnPage("User:MER-C/monobook.js"));
    }
    
    @Test
    public void getSectionText() throws Exception
    {
        assertEquals("getSectionText(): section 0", "This is section 0.", testWiki.getSectionText("User:MER-C/UnitTests/SectionTest", 0));
        assertEquals("getSectionText(): section 2", "===Section 3===\nThis is section 2.", 
            testWiki.getSectionText("User:MER-C/UnitTests/SectionTest", 2));
        /*
        try
        {
            enWiki.getSectionText("User:MER-C/monobook.css", 4920);
            fail("getSectionText: non-existent section, should have thrown an exception.");
        }
        catch (IllegalArgumentException ex)
        {
            // the expected result. This is currently broken because fetch
            // intercepts the API error.
        }
        */
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
    public void getSiteInfo() throws Exception
    {
        Map<String, Object> info = enWiki.getSiteInfo();
        assertTrue("siteinfo: caplinks true", (Boolean)info.get("usingcapitallinks"));
        assertEquals("siteinfo: scriptpath", "/w", (String)info.get("scriptpath"));
        assertEquals("siteinfo: timezone", ZoneId.of("UTC"), info.get("timezone"));
        info = enWikt.getSiteInfo();
        assertFalse("siteinfo: caplinks false", (Boolean)info.get("usingcapitallinks"));
    }
    
    @Test
    public void normalize() throws Exception
    {
        assertEquals("normalize", "Blah", enWiki.normalize("Blah"));
        assertEquals("normalize", "Blah", enWiki.normalize("blah"));
        assertEquals("normalize", "File:Blah.jpg", enWiki.normalize("File:Blah.jpg"));
        assertEquals("normalize", "File:Blah.jpg", enWiki.normalize("file:blah.jpg"));
        assertEquals("normalize", "Category:Wikipedia:blah", enWiki.normalize("Category:Wikipedia:blah"));
        assertEquals("normalize", "Hilfe Diskussion:Glossar", deWiki.normalize("Help talk:Glossar"));        

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
    public void getRevision() throws Exception
    {
        // https://en.wikipedia.org/w/index.php?title=Wikipedia_talk%3AWikiProject_Spam&oldid=597454682
        Wiki.Revision rev = enWiki.getRevision(597454682L);
        assertEquals("getRevision: page", "Wikipedia talk:WikiProject Spam", rev.getPage());
        assertEquals("getRevision: timestamp", "2014-02-28T00:40:31Z", rev.getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertEquals("getRevision: user", "Lowercase sigmabot III", rev.getUser());
        assertEquals("getRevision: summary", "Archiving 3 discussion(s) to [[Wikipedia talk:WikiProject Spam/2014 Archive Feb 1]]) (bot",
            rev.getSummary());
        assertEquals("getRevision: sha1", "540a2b3501e4d15729ea25ec3238da9ad0dd6dc4", rev.getSha1());
        assertEquals("getRevision: size", 4286, rev.getSize());
        assertEquals("getRevision: revid", 597454682L, rev.getRevid());
        assertEquals("getRevision: previous", 597399794L, rev.getPrevious().getRevid());
        // assertEquals("getRevision: next", 597553957L, rev.getNext().getRevid());
        assertTrue("getRevision: minor", rev.isMinor());
        assertFalse("getRevision: new", rev.isNew());
        assertFalse("getRevison: bot", rev.isBot());
        assertFalse("getRevision: user not revdeled", rev.isUserDeleted());
        assertFalse("getRevision: summary not revdeled", rev.isSummaryDeleted());
        assertFalse("getRevision: content not deleted", rev.isContentDeleted());
        assertFalse("getRevision: page not deleted", rev.isPageDeleted());
        
        // revdel, logged out
        // https://en.wikipedia.org/w/index.php?title=Imran_Khan_%28singer%29&oldid=596714684
        rev = enWiki.getRevision(596714684L);
        assertNull("getRevision: summary revdeled", rev.getSummary());
        assertNull("getRevision: user revdeled", rev.getUser());
        assertNull("getRevision: sha1/content revdeled", rev.getSha1());
        assertTrue("getRevision: user revdeled", rev.isUserDeleted());
        assertTrue("getRevision: summary revdeled", rev.isSummaryDeleted());
        assertTrue("getRevision: content revdeled", rev.isContentDeleted());
        
        // Revision has been deleted (not RevisionDeleted)
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        assertNull("getRevision: page deleted", testWiki.getRevision(217078L));
    }
    
    @Test
    public void diff() throws Exception
    {
        // https://en.wikipedia.org/w/index.php?title=Dayo_Israel&oldid=738178354&diff=prev
        assertNull("diff: dummy edit", enWiki.getRevision(738178354L).diff(Wiki.PREVIOUS_REVISION));
        // https://en.wikipedia.org/w/index.php?title=Source_Filmmaker&diff=804972897&oldid=803731343
        // should be empty string, but the MediaWiki API does not distinguish 
        // between a dummy edit and no difference
        assertNull("diff: no difference", enWiki.getRevision(803731343L).diff(804972897L));
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
            if (rev.getRevid() == 724989913L)
            {
                assertTrue("contribs: summary deleted", rev.isSummaryDeleted());
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
        assertEquals("contribs: filtered", 120919L, edits[0].get(0).getRevid());
        // not implemented in MediaWiki API
        // assertEquals("contribs: sha1 present", "bcdb66a63846bacdf39f5c52a7d2cc5293dbde3e", edits[0].get(0).getSha1());
        edits[0].forEach(rev ->
        {
            assertEquals("contribs: namespace", Wiki.MAIN_NAMESPACE, testWiki.namespace(rev.getPage()));
        });
    }
    
    @Test
    public void getUser() throws Exception
    {
        assertNull("getUser: IPv4 address", testWiki.getUser("127.0.0.1"));
        assertNull("getUser: IP address range", testWiki.getUser("127.0.0.0/24"));
    }

    @Test
    public void getUserInfo() throws Exception
    {
        String[] users = new String[]
        {
            "127.0.0.1", // IP address
            "MER-C", 
            "DKdsf;lksd", // should be non-existent...
            "Jimbo Wales", 
            "Jimbo_Wales" // duplicate
        };
        Map<String, Object>[] info = testWiki.getUserInfo(users);
        assertNull("getUserInfo: IP address", info[0]);
        assertNull("getUserInfo: non-existent user", info[2]);
        
        // editcount omitted because it is dynamic
        assertEquals("getUserInfo: username", users[1], info[1].get("inputname"));
        assertEquals("getUserInfo: normalized username", users[1], info[1].get("username"));
        assertFalse("getUserInfo: blocked", (Boolean)info[1].get("blocked"));
        assertEquals("getUserInfo: gender", Wiki.Gender.unknown, (Wiki.Gender)info[1].get("gender"));
        assertEquals("getUserInfo: registration", "2007-02-14T11:38:37Z", 
            ((OffsetDateTime)info[1].get("created")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertTrue("getUserInfo: email", (Boolean)info[1].get("emailable"));
        
        // check groups
        String[] temp = (String[])info[1].get("groups");
        List<String> groups = Arrays.asList(temp);
        temp = new String[] { "*", "autoconfirmed", "user", "sysop" };
        assertTrue("getUserInfo: groups", groups.containsAll(Arrays.asList(temp)));
        
        // check (subset of) rights
        temp = (String[])info[1].get("rights");
        List<String> rights = Arrays.asList(temp);
        temp = new String[] { "apihighlimits", "delete", "block", "editinterface", "writeapi" };
        assertTrue("getUserInfo: groups", rights.containsAll(Arrays.asList(temp)));
        
        // check de-duplication
        assertEquals("getUserInfo: username", users[3], info[3].get("inputname"));
        assertEquals("getUserInfo: username", users[4], info[4].get("inputname"));
        info[3].remove("inputname");
        info[4].remove("inputname");
        assertEquals("getUserInfo: duplicates", info[3], info[4]);
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
            // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
            "User:MER-C/UnitTests/Delete", 
            // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/pagetext
            "User:MER-C/UnitTests/pagetext",
            // non-existent -- https://test.wikipedia.org/wiki/Gsgdksgjdsg
            "Gsgdksgjdsg",
            // empty -- https://test.wikipedia.org/wiki/User:NikiWiki/EmptyPage
            "User:NikiWiki/EmptyPage"
            
        });
        // API result does not include the terminating new line
        assertEquals("getPageText", text[0], "This revision is not deleted!");
        assertEquals("page text: decoding", text[1], "&#039;&#039;italic&#039;&#039;" +
            "\n'''&amp;'''\n&&\n&lt;&gt;\n<>\n&quot;");
        assertNull("getPageText: non-existent page", text[2]);
        assertEquals("getPageText: empty page", text[3], "");
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
            assertEquals("recentchanges: namespace filter", Wiki.TALK_NAMESPACE, enWiki.namespace(rev.getPage()));
        // check options filtering
        Map<String, Boolean> options = new HashMap();
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
    }
    
    @Test
    public void allUserswithRight() throws Exception
    {
        assertArrayEquals("allUsersWithRight: nonsense", new String[0], testWiki.allUsersWithRight("sdfkd|&"));
    }
    
    @Test
    public void constructNamespaceString() throws Exception
    {
        StringBuilder temp = new StringBuilder();
        enWiki.constructNamespaceString(temp, "blah", new int[] { 3, 2, 1, 2, -10 });
        assertEquals("constructNamespaceString", "&blahnamespace=1%7C2%7C3", temp.toString());
    }
    
    @Test
    public void constructTitleString() throws Exception
    {
        String[] titles = new String[102];
        for (int i = 0; i < titles.length; i++)
            titles[i] = "a" + i;
        titles[101] = "A34"; // should be removed
        String[] expected = new String[]
        {
            // slowmax == 50 for Wikimedia wikis if not logged in
            URLEncoder.encode("A0|A1|A10|A100|A11|A12|A13|A14|A15|A16|A17|A18|" +
                "A19|A2|A20|A21|A22|A23|A24|A25|A26|A27|A28|A29|A3|A30|A31|" +
                "A32|A33|A34|A35|A36|A37|A38|A39|A4|A40|A41|A42|A43|A44|A45|" +
                "A46|A47|A48|A49|A5|A50|A51|A52", "UTF-8"),
            URLEncoder.encode("A53|A54|A55|A56|A57|A58|A59|A6|A60|A61|A62|A63|" + 
                "A64|A65|A66|A67|A68|A69|A7|A70|A71|A72|A73|A74|A75|A76|A77|" +
                "A78|A79|A8|A80|A81|A82|A83|A84|A85|A86|A87|A88|A89|A9|A90|" +
                "A91|A92|A93|A94|A95|A96|A97|A98", "UTF-8"),
            URLEncoder.encode("A99", "UTF-8")
        };
        String[] actual = enWiki.constructTitleString(titles);
        assertArrayEquals("constructTitleString", expected, actual);
    }

    // INNER CLASS TESTS
    
    @Test
    public void revisionGetText() throws Exception
    {
        // https://test.wikipedia.org/w/index.php?oldid=230472
        Wiki.Revision rev = testWiki.getRevision(230472L);
        String text = rev.getText();
        assertEquals("revision text: decoding", "&#039;&#039;italic&#039;&#039;" +
            "\n'''&amp;'''\n&&\n&lt;&gt;\n<>\n&quot;\n", text);
        
        // RevisionDeleted content (returns 404)
        // https://en.wikipedia.org/w/index.php?title=Imran_Khan_%28singer%29&oldid=596714684
        // rev = enWiki.getRevision(596714684L);
        // text = rev.getText();
        // assertEquals("revision text: content deleted", null, text);
        
        // https://test.wikipedia.org/w/index.php?oldid=322889
        rev = testWiki.getRevision(322889L);
        assertEquals("revision text: empty revision", rev.getText(), "");
    }
    
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
