/**
 *  @(#)WikiUnitTest.java 0.31 29/08/2015
 *  Copyright (C) 2014-2016 MER-C
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
        enWiki = new Wiki("en.wikipedia.org");
        enWiki.setMaxLag(-1);
        deWiki = new Wiki("de.wikipedia.org");
        deWiki.setMaxLag(-1);
        arWiki = new Wiki("ar.wikipedia.org");
        arWiki.setMaxLag(-1);
        testWiki = new Wiki("test.wikipedia.org");
        testWiki.setMaxLag(-1);
        enWikt = new Wiki("en.wiktionary.org");
        enWikt.setMaxLag(-1);
        enWikt.getSiteInfo();
        
        sha256 = MessageDigest.getInstance("SHA-256");
    }
    
    @Test
    public void assertionMode() throws Exception
    {
        enWiki.setAssertionMode(Wiki.ASSERT_USER);
        assertEquals("assertion mode", Wiki.ASSERT_USER, enWiki.getAssertionMode());
        try
        {
            enWiki.getPageText("Main Page");
            fail("assertion mode: failed login assertion");
        }
        catch (AssertionError ex)
        {
            // Test passed. Assertion tests whether we are logged in, which we
            // are not.
        }
        enWiki.setAssertionMode(Wiki.ASSERT_BOT);
        try
        {
            enWiki.getPageText("Main Page");
            fail("assertion mode: failed bot assertion");
        }
        catch (AssertionError ex)
        {
            // Test passed. Assertion tests whether we have a bot flag. As we
            // are not logged in, we cannot have one.
        }
        enWiki.setAssertionMode(Wiki.ASSERT_SYSOP);
        try
        {
            enWiki.getPageText("Main Page");
            fail("assertion mode: failed admin assertion");
        }
        catch (AssertionError ex)
        {
            // Test passed. Assertion tests whether we are an admin. No sane
            // wiki owner would allow IPs to be admins.
        }
        enWiki.setAssertionMode(Wiki.ASSERT_NONE);
    }
    
    @Test
    public void setResolveRedirects() throws Exception
    {
        testWiki.setResolveRedirects(true);
        assertTrue("resolving redirects", testWiki.isResolvingRedirects());
        // https://test.wikipedia.org/w/index.php?title=User:MER-C/UnitTests/redirect&redirect=no
        // redirects to https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        // FIXME: currently broken, because getPageText and other vectorized
        // methods look for the redirected page title
        assertEquals("getPageText", "This revision is not deleted!", 
            testWiki.getPageText("User:MER-C/UnitTests/redirect"));
        testWiki.setResolveRedirects(false);
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
    }
    
    @Test
    public void supportsSubpages() throws Exception
    {
        assertFalse("supportsSubpages: main", enWiki.supportsSubpages(Wiki.MAIN_NAMESPACE));
        assertTrue("supportsSubpages: talk", enWiki.supportsSubpages(Wiki.TALK_NAMESPACE));
        try
        {
            enWiki.supportsSubpages(-4444);
            fail("supportsSubpages: obviously invalid namespace");
        }
        catch (IllegalArgumentException ex)
        {
            // test passed
        }
    }
    
    @Test
    public void queryLimits() throws Exception
    {
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
        try
        {
            enWiki.getTalkPage("Talk:Hello");
            fail("getTalkPage: tried to get talk page of a talk page");
        }
        catch (IllegalArgumentException ex)
        {
            // test passed
        }
        try
        {
            enWiki.getTalkPage("Special:Newpages");
            fail("getTalkPage: tried to get talk page of a special page");
        }
        catch (IllegalArgumentException ex)
        {
            // test passed
        }
        try
        {
            enWiki.getTalkPage("Media:Wiki.png");
            fail("getTalkPage: tried to get talk page of a media page");
        }
        catch (IllegalArgumentException ex)
        {
            // test passed
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
        assertNull("Non-existent page", enWiki.getFirstRevision("dgfhdf&jklg"));
    }
    
    @Test
    public void getLastRevision() throws Exception
    {
        assertNull("Non-existent page", enWiki.getTopRevision("dgfhd&fjklg"));
    }
    
    @Test
    public void getTemplates() throws Exception
    {
        assertArrayEquals("getTemplates: non-existent page", new String[0], enWiki.getTemplates("sdkf&hsdklj"));
        assertArrayEquals("getTemplates: page with no templates", new String[0], enWiki.getTemplates("User:MER-C/monobook.js"));
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
        String[] expected = new String[] { "Main Page", null, null, "Goatse.cx", "Main Page", "Fish and chips" };
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
    public void getCategories() throws Exception
    {
        assertArrayEquals("getCategories: non-existent page", new String[0], enWiki.getImagesOnPage("Skfls&jdkfs"));
        assertArrayEquals("getCategories: page with no images", new String[0], enWiki.getImagesOnPage("User:MER-C/monobook.js"));
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
        assertArrayEquals("getPageHistory: non-existent page", new Wiki.Revision[0], enWiki.getPageHistory("EOTkd&ssdf"));
        assertArrayEquals("getPageHistory: special page", new Wiki.Revision[0], enWiki.getPageHistory("Special:Specialpages"));
        
        // test for RevisionDeleted revisions
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
    public void getIPBlockList() throws Exception
    {
        // https://en.wikipedia.org/wiki/Special:Blocklist/Nimimaan
        // see also getLogEntries() below
        Wiki.LogEntry[] le = enWiki.getIPBlockList("Nimimaan");
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
        le = enWiki.getIPBlockList("0.0.0.0");
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
        Map<String, Object>[] pageinfo = enWiki.getPageInfo(new String[] { "Main Page", "IPod", "Main_Page" });
        
        // Main Page
        Map<String, Object> protection = (Map<String, Object>)pageinfo[0].get("protection");
        assertEquals("getPageInfo: Main Page edit protection level", Wiki.FULL_PROTECTION, protection.get("edit"));
        assertNull("getPageInfo: Main Page edit protection expiry", protection.get("editexpiry"));
        assertEquals("getPageInfo: Main Page move protection level", Wiki.FULL_PROTECTION, protection.get("move"));
        assertNull("getPageInfo: Main Page move protection expiry", protection.get("moveexpiry"));
        assertTrue("getPageInfo: Main Page cascade protection", (Boolean)protection.get("cascade"));
        assertEquals("getPageInfo: Main Page display title", "Main Page", pageinfo[0].get("displaytitle"));
        
        // different display title
        assertEquals("getPageInfo: iPod display title", "iPod", pageinfo[1].get("displaytitle"));
        
        // Main_Page (duplicate, should be removed)
        assertEquals("getPageInfo: duplicate", pageinfo[0], pageinfo[2]);
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
        for (int i = 0; i < 3; i++)
        {
            String random = enWiki.random();
            assertEquals("random: main namespace", Wiki.MAIN_NAMESPACE, enWiki.namespace(random));
            random = enWiki.random(Wiki.PROJECT_NAMESPACE, Wiki.USER_NAMESPACE);
            int temp = enWiki.namespace(random);
            if (temp != Wiki.PROJECT_NAMESPACE && temp != Wiki.USER_NAMESPACE)
                fail("random: multiple namespaces");
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
        // FIXME: only works because we have called getSiteInfo above
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
        // https://en.wikipedia.org/w/index.php?title=Wikipedia_talk%3AWikiProject_Spam&diff=597454682&oldid=597399794
        Wiki.Revision rev = enWiki.getRevision(597454682L);
        assertEquals("getRevision: page", "Wikipedia talk:WikiProject Spam", rev.getPage());
        assertEquals("getRevision: timestamp", "2014-02-28T00:40:31Z", rev.getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertEquals("getRevision: user", "Lowercase sigmabot III", rev.getUser());
        assertEquals("getRevision: summary", "Archiving 3 discussion(s) to [[Wikipedia talk:WikiProject Spam/2014 Archive Feb 1]]) (bot",
            rev.getSummary());
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
        assertNull("diff: dummy edit", enWiki.getRevision(738178354L).diff(Wiki.PREVIOUS_REVISION));
    }
    
    @Test
    public void contribs() throws Exception
    {
        // should really be null, but the API returns zero
        assertEquals("contribs: non-existent user", testWiki.contribs("Dsdlgfkjsdlkfdjilgsujilvjcl").length, 0);
        
        // RevisionDeleted content
        Wiki.Revision[] contribs = enWiki.contribs("Allancake");
        for (Wiki.Revision rev : contribs)
        {
            if (rev.getRevid() == 724989913L)
            {
                assertTrue("contribs: summary deleted", rev.isSummaryDeleted());
                assertTrue("contribs: content deleted", rev.isContentDeleted());
            }
        }
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
        Map<String, Object>[] info = testWiki.getUserInfo(new String[]
        {
            "127.0.0.1", // IP address
            "MER-C", 
            "DKdsf;lksd" // should be non-existent...
        });
        assertNull("getUserInfo: IP address", info[0]);
        assertNull("getUserInfo: non-existent user", info[2]);
        
        // editcount omitted because it is dynamic
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
    }
    
    @Test
    public void getPageText() throws Exception
    {
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
        enWiki.constructNamespaceString(temp, "blah", new int[] { 3, 2, 1, 2 });
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
        String[] actual = enWiki.constructTitleString(0, titles, false);
        assertArrayEquals("constructTitleString", expected, actual);
    }
    
    @Test
    public void constructTitleStringUrlLimit() throws Exception
    {
        String[] titles = new String[]
        {
            "File:Булыжниковая мостовая на Автопарковом проезде в Мурманске.jpg", "File:漁火温泉 おと姫の湯.jpg",
            "File:Лебеді на зимівлі в Чорноморському біосферному заповіднику.jpg",
            "File:Второй корпус Мурманского гуманитарного института.jpg", "File:國立臺灣工藝研究發展中心Logo.jpg",
            "File:Герои Российской Федерации. Стела в Сквере Воинов-интернационалистов.jpg",
            "File:Марьяна Наумова и легендарный спортсмен, актер и экс-губернатор Арнольд Шварценеггер..jpg",
            "File:東京表現高等学院MIICA校舎ナナメ.jpg",
            "File:Выступление Д. Медведева на открытии нового здания Марийского государственного театра оперы и балета им. Э. Сапаева.jpg",
            "File:Інструментарій для гіпнозу Клініки активної терапії особливих станів(м.Київ). Автор - Сергій БОЛТІВЕЦЬ.jpg",
            "File:Дипломная работа С.В. Клиндухова по воссозданию дореволюционной ватной игрушки.jpg",
            "File:Герб Общественной молодежной палаты Ярославской области.jpg",
            "File:Вимірювання глибини гіпнотичного трансу каталепсією лівої руки. Автор - Сергій БОЛТІВЕЦЬ.jpg",
            "File:Мурманский областной наркологический диспансер.jpg",
            "File:Левостороннее движение в Мурманске. Высадка пассажиров троллейбуса на дорогу.jpg",
            "File:Тест гіпнабельності з розплющеними очима за технікою каталепсії доктора Є.Гливи(Сідней, Австралія). Автор - Сергій БОЛТІВЕЦЬ.jpg",
            "File:Новое здание Марийского государственного театра оперы и балета имени Эрика Сапаева.jpg",
            "File:Мемориальная доска железнодорожникам мурманского узла.jpg",
            "File:Закладной камень мемориала участникам союзнических конвоев.jpg",
            "File:Мемориальная доска учреждению медали За оборону Советского Заполярья.jpg",
            "File:国際航空宇宙展 ベル412EPI発展型 40%スケールモデル.jpg",
            "File:Ганаев Камиль Гаджимурадович - Победитель ХVIII Московского международного салона изобретений и инновационных технологий.jpg",
            "File:Доска на закладном камне Спасо-Преображенского морского кафедрального собора.jpg",
            "File:Енцефалографічне відтворення початку плато(рівнини) перебігу гіпнотичного стану. Автор - Сергій БОЛТІВЕЦЬ.jpg",
            "File:บริษัท โตโยต้าบ้านโพธิ์ - panoramio.jpg",
            "File:Апаратурна індикація глибини гіпнотичного трансу в Клініці активної терапії особливих станів(м.Київ).jpg",
            "File:Часовня в честь благоверных князей Александра Невского, Вячеслава Чешского и Владислава Сербского.jpg",
            "File:基隆市政府 建築物公共安全檢查不合格場所 20161022.jpg", "File:呂學淵塗鴉作品，《犬》，2001。.JPG",
            "File:Мемориальная доска в память о сотрудниках Мурманского управления гидрометеослужбы.jpg",
            "File:新板特區與萬坪都會公園-1.jpg", "File:মুজিবনগর ১৪.jpg", "File:粟島浦村・内浦集落.jpg",
            "File:大观楼-号称中国四大名楼之一，盛名之下其实难符！ - panoramio.jpg",
            "File:Співробітник Сумського державного університету Роман Москаленко працює на атомному силовому мікроскопі.jpg",
            "File:Лазеропунктурна регуляція психофізіологічних станів українським лазеропунктурним апаратом. Автор - Сергій БОЛТІВЕЦЬ.jpg",
            "File:รปภ บริษัท โตโยต้าบ้านโพธิ์ - panoramio.jpg", "File:รถรางใต้ดิน สถานีเพชรบุรี - panoramio.jpg",
            "File:প্রথম শহীদ মিনার 2.jpg", "File:西門徒步區廣告。武昌街二段與中華路一段交岔口。 - panoramio.jpg",
            "File:കൊട്ടിയൂര്‍ പഠനശിബിരം2016.jpg.jpg", "File:ธงชาติไทยที่บริษัท โตโยต้า - panoramio.jpg",
            "File:大間港旧フェリー埠頭.jpg", "File:รถไฟฟ้าใต้ดิน สถานีเพชรบุรี - panoramio.jpg", "File:প্রথম শহীদ মিনার .jpg",
            "File:สถาบันเทคโนโลยีแห่งเอเชีย - panoramio.jpg",
            "File:2016 맥스큐 머슬마니아 피트니스 세계대회 선발전 미즈비키니 1라운드 톨부문 (9).jpg",
            "File:เจดีย์วัดเครือวัลย์วรวิหาร - panoramio.jpg", "File:東勢區林管處雙崎工作站.JPG",
            "File:ถนนหน้าบริษัท โตโยต้าบ้านโพธิ์ - panoramio.jpg", "File:超音速輸送機 国際航空宇宙展.jpg",
            "File:คลื่นในสปริง.gif", "File:姫新線平均通過人員（2015年度時点改訂版）.png", "File:शेरपुर शहीद स्मारक.jpg",
            "File:水戸市植物公園熱帯果樹温室.jpg", "File:小型超音速旅客機 国際航空宇宙展.jpg", "File:香港中國婦女會丘佐榮学校内部.jpg",
            "File:專訪《殺破狼2》張晉 如今帥哥才能演反派.jpg", "File:อัครจิต พนมวัน ณ อยุธยา.jpg", "File:沈阳市人民政府（浑南规划大厦3号楼）.jpg",
            "File:水戸市植物公園における薬草園の瓦.jpg", "File:শিধলকুড়া উচ্চ বিদ্যালয়.jpeg",
            "File:রংপুর বিভাগ, বাংলাদেশ এর ফেসবুক লেগো -২.png",
            "File:आप सभी को मेरी और से न्यू ईयर की शुभकामनाएं.gif", "File:鶴橋駅 3・4番のりば 大阪環状線乗り換え階段 (駅ナンバリング導入前).jpg",
            "File:水戸市植物公園観賞大温室カクタス室.jpg", "File:तहसील कार्यालय में रखा तिरंगा.jpg",
            "File:রংপুর বিভাগ, বাংলাদেশ এর ফেসবুক লেগো.png", "File:রংপুর বিভাগ, বাংলাদেশ এর ফেসবুক প্রফাইল.png",
            "File:หาติวเตอร์ เตรียมตัวเข้ามหาลัย ติวSAT ติว.jpg",
            "File:இலங்கை தகவல் தொழில்நுட்ப நிறுவனம்(சின்னம்).png",
            "File:முக்குறுணிப் பிள்ளையார் கோவில் கோபுரம்.jpg", "File:พระนางมณีจันทร์(เจ้าขรัวมณีจันทร์).jpg",
            "File:இலங்கை தகவல் தொழில்நுட்ப நிறுவனத்தின் சின்னம்.png",
            "File:กำลังหาครูสอนพิเศษ และหาติวเตอร์เพื่$.jpg",
            "File:முக்குறுணிப் பிள்ளையார் கோவில் முகப்புத் தோற்றம்.jpg",
            "File:முக்குறுணிப் பிள்ளையார் கோவில் உட்ப்புறத் தோற்றம்.jpg",
            "File:พระบามสมเด็จพระเจ้าอยู่หัวเปิดศาลาเหรียญ.jpg",
            "File:พระบาทสมเด็จพระเจ้าอยู่หัวเปิดศาลาเครื่องราช.jpg"
        };
        int lengthBaseUrl = (int)(Math.random() * 400);
        String[] titleStrings = enWiki.constructTitleString(lengthBaseUrl, titles, false);
        for (String ts : titleStrings)
        {
            assertTrue("constructTitleStringUrlLimit", ts.length() <= enWiki.URL_LENGTH_LIMIT - lengthBaseUrl);
        }
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
}
