/**
 *  @(#)WikiTest.java 0.35 20/05/2018
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

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

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
    public void testUrls()
    {
        Wiki dummy = Wiki.createInstance("example.com", "/scriptpath", "http://");
        assertEquals("http://", dummy.getProtocol(), "protocol");
        assertEquals("example.com", dummy.getDomain(), "domain");
        assertEquals("/scriptpath", dummy.getScriptPath(), "scriptPath");

        // index.php URL
        assertEquals("https://en.wikipedia.org/w/index.php", enWiki.getIndexPhpUrl(), "getIndexPhpUrl");
        assertEquals("http://example.com/scriptpath/index.php", dummy.getIndexPhpUrl(), "getIndexPhpUrl");

        // API URL
        assertEquals("https://en.wikipedia.org/w/api.php", enWiki.getApiUrl(), "getApiUrl");
        assertEquals("http://example.com/scriptpath/api.php", dummy.getApiUrl(), "getApiUrl");
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
        assertTrue(result_1 > 0);
        assertEquals(-1*result_1, result_2, "symmetric");
        assertTrue(result_3 > 0 && result_4 > 0, "transitivity");

        Wiki dummy = Wiki.createInstance("en.wikipedia.org", "/example", "https://");
        int result_5 = enWiki.compareTo(dummy);
        assertTrue(result_5 > 0, "multiple instances on same domain");
    }

    @Test
    @DisplayName("Wiki.setAssertionMode (logged out)")
    public void assertionMode() throws Exception
    {
        enWiki.setAssertionMode(Wiki.ASSERT_USER);
        assertEquals(Wiki.ASSERT_USER, enWiki.getAssertionMode(), "check assertion mode set");
        // This test runs logged out. The following assertions are expected to fail.
        assertThrows(AssertionError.class, () -> enWiki.getPageText("Main Page"), "ASSERT_USER");
        enWiki.setAssertionMode(Wiki.ASSERT_BOT);
        assertThrows(AssertionError.class, () -> enWiki.getPageText("Main Page"), "ASSERT_BOT");
        // This only trips on write requests.
        // enWiki.setAssertionMode(Wiki.ASSERT_SYSOP);
        // assertThrows(AssertionError.class, () -> enWiki.getPageText("Main Page"), "ASSERT_SYSOP");
        enWiki.setAssertionMode(Wiki.ASSERT_NONE);
        enWiki.getPageText("Main Page"); // no exception
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
        assertTrue(testWiki.isResolvingRedirects());
        // https://test.wikipedia.org/w/index.php?title=User:MER-C/UnitTests/redirect&redirect=no
        // redirects to https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        assertEquals("This revision is not deleted!", testWiki.getPageText(pages[0]),
            "resolveredirects/getPageText");
        Map<String, Object>[] x = testWiki.getPageInfo(pages);
        x[0].remove("inputpagename");
        x[1].remove("inputpagename");
        assertEquals(x[1], x[0], "resolveredirects/getPageInfo");
        pages = new String[] { "Main page", "Main Page" };
        List<String>[] y = enWiki.getTemplates(pages);
        assertEquals(y[1], y[0], "resolveredirects/getTemplates");
    }

    @Test
    public void namespace() throws Exception
    {
        assertEquals(Wiki.CATEGORY_NAMESPACE, enWiki.namespace("Category:CSD"), "en category");
        assertEquals(Wiki.CATEGORY_NAMESPACE, enWiki.namespace("category:CSD"), "en category lower case");
        assertEquals(Wiki.HELP_TALK_NAMESPACE, enWiki.namespace("Help talk:About"), "en help talk");
        assertEquals(Wiki.HELP_TALK_NAMESPACE, enWiki.namespace("help talk:About"), "en help talk lower case");
        assertEquals(Wiki.HELP_TALK_NAMESPACE, enWiki.namespace("help_talk:About"), "en help talk lower case underscore");
        assertEquals(Wiki.PROJECT_NAMESPACE, enWiki.namespace("WP:CSD"), "en alias");
        // Undocumented on mediawiki.org, better comment out for now
        // assertEquals("NS: not case sensitive", Wiki.PROJECT_NAMESPACE, enWiki.namespace("wp:csd"));
        assertEquals(Wiki.MAIN_NAMESPACE, enWiki.namespace("Star Wars: The Old Republic"), "main namespace fail");
        assertEquals(Wiki.MAIN_NAMESPACE, enWiki.namespace("Some Category: Blah"), "main namespace fail");
        assertEquals(Wiki.FILE_NAMESPACE, enWiki.namespace(":File:Blah.jpg"), "leading colon");
        assertEquals(Wiki.CATEGORY_NAMESPACE, deWiki.namespace("Kategorie:Begriffsklärung"), "i18n");
        assertEquals(Wiki.CATEGORY_NAMESPACE, deWiki.namespace("Category:Begriffsklärung"), "mixed i18n");
        assertEquals(Wiki.CATEGORY_NAMESPACE, arWiki.namespace("تصنيف:صفحات_للحذف_السريع"), "rtl fail");
    }

    @Test
    public void namespaceIdentifier() throws Exception
    {
        assertEquals("Category", enWiki.namespaceIdentifier(Wiki.CATEGORY_NAMESPACE));
        assertEquals("Kategorie", deWiki.namespaceIdentifier(Wiki.CATEGORY_NAMESPACE), "i18n");
        assertEquals("Portal", enWiki.namespaceIdentifier(100), "custom namespace");
        assertEquals("", enWiki.namespaceIdentifier(-100), "obviously invalid");
    }

    @Test
    public void removeNamespace() throws Exception
    {
        assertEquals("Hello World", enWiki.removeNamespace("Hello World"), "main namespace");
        assertEquals("Hello:World", enWiki.removeNamespace("Hello:World"), "main namespace with colon");
        assertEquals("Hello", enWiki.removeNamespace("Portal:Hello"), "custom namespace");
        assertEquals("Category:Blah", enWiki.removeNamespace("Category:Blah", Wiki.FILE_NAMESPACE), "select namespaces only");
        assertEquals("Blah", enWiki.removeNamespace("Blah", Wiki.CATEGORY_NAMESPACE), "select namespaces only");
        // something funky going on with RTL, can't tell whether it's Netbeans, Firefox or me failing at copy/paste
        // assertEquals("removeNamespace: rtl fail", "صفحات للحذف السريع", arWiki.removeNamespace("تصنيف:صفحات_للحذف_السريع"));
    }

    @Test
    public void supportsSubpages() throws Exception
    {
        assertFalse(enWiki.supportsSubpages(Wiki.MAIN_NAMESPACE), "main namespace");
        assertTrue(enWiki.supportsSubpages(Wiki.TALK_NAMESPACE), "talk namespace");
        assertFalse(enWiki.supportsSubpages(Wiki.SPECIAL_NAMESPACE),
            "slashes in special pages denote arguments, not subpages");
        assertThrows(IllegalArgumentException.class, () -> enWiki.supportsSubpages(-4444),
            "obviously invalid namespace");
    }

    @Test
    public void queryLimits() throws Exception
    {
        assertThrows(IllegalArgumentException.class, () -> enWiki.setQueryLimit(-1),
            "Negative query limits don't make sense.");
        enWiki.setQueryLimit(530);
        assertEquals(530, enWiki.getQueryLimit());
        assertEquals(530, enWiki.getPageHistory("Main Page", null).size(), "functionality");
        // check RequestHelper local override
        Wiki.RequestHelper rh = enWiki.new RequestHelper().limitedTo(10);
        assertEquals(10, enWiki.recentChanges(rh).size(), "recentchanges override");
        assertEquals(530, enWiki.getPageHistory("Main Page", null).size(), "after recentchanges override");
        assertEquals(10, enWiki.getLogEntries(Wiki.DELETION_LOG, "delete", rh).size(), "getLogEntries override");
        assertEquals(530, enWiki.getPageHistory("Main Page", null).size(), "after getLogEntries override");
        assertEquals(500, enWiki.listPages("", null, Wiki.MAIN_NAMESPACE).length, "listPages override");
        assertEquals(530, enWiki.getPageHistory("Main Page", null).size(), "after listPages override");
    }

    @Test
    public void getTalkPage() throws Exception
    {
        assertEquals("Talk:Hello", enWiki.getTalkPage("Hello"));
        assertEquals("User talk:Hello", enWiki.getTalkPage("User:Hello"));
        assertThrows(IllegalArgumentException.class, () -> enWiki.getTalkPage("Talk:Hello"),
            "tried to get talk page of a talk page");
        assertThrows(IllegalArgumentException.class, () -> enWiki.getTalkPage("Special:Newpages"),
            "tried to get talk page of a special page");
        assertThrows(IllegalArgumentException.class, () -> enWiki.getTalkPage("Media:Wiki.png"),
            "tried to get talk page of a media page");
    }

    @Test
    public void getRootPage() throws Exception
    {
        assertEquals("Aaa/Bbb/Ccc", enWiki.getRootPage("Aaa/Bbb/Ccc"), "main ns");
        assertEquals("Talk:Aaa", enWiki.getRootPage("Talk:Aaa/Bbb/Ccc"), "talk ns");
        assertEquals("Talk:Aaa", enWiki.getRootPage("Talk:Aaa"), "talk ns, already root");
        assertEquals("ويكيبيديا:نقاش الحذف",
            arWiki.getRootPage("ويكيبيديا:نقاش الحذف/كأس الخليج العربي لكرة القدم 2014 المجموعة ب"), "rtl");
    }

    @Test
    public void getParentPage() throws Exception
    {
        assertEquals("Aaa/Bbb/Ccc", enWiki.getParentPage("Aaa/Bbb/Ccc"), "main ns");
        assertEquals("Talk:Aaa/Bbb", enWiki.getParentPage("Talk:Aaa/Bbb/Ccc"), "talk ns");
        assertEquals("Talk:Aaa", enWiki.getParentPage("Talk:Aaa"), "talk ns, already root");
        assertEquals("ويكيبيديا:نقاش الحذف",
            arWiki.getParentPage("ويكيبيديا:نقاش الحذف/كأس الخليج العربي لكرة القدم 2014 المجموعة ب"), "rtl");
    }

    @Test
    public void getPageURL() throws Exception
    {
        assertEquals("https://en.wikipedia.org/wiki/Hello_World", enWiki.getPageUrl("Hello_World"));
        assertEquals("https://en.wikipedia.org/wiki/Hello_World", enWiki.getPageUrl("Hello World"));
    }

    @Test
    public void userExists() throws Exception
    {
        boolean[] temp = testWiki.userExists(new String[] { "MER-C", "127.0.0.1", "Djskgh;jgsd", "::/1" });
        assertTrue(temp[0], "user that exists");
        assertFalse(temp[1], "IP address");
        assertFalse(temp[2], "nonsense input");
        assertFalse(temp[3], "IPv6 range");
    }

    @Test
    public void changeUserPrivileges() throws Exception
    {
        // check expiry
        Wiki.User user = enWiki.getUser("Example");
        List<String> granted = Arrays.asList("autopatrolled");
        assertThrows(IllegalArgumentException.class,
            () -> enWiki.changeUserPrivileges(user, granted, Arrays.asList(OffsetDateTime.MIN), Collections.emptyList(), "dummy reason"),
            "attempted to set user privilege expiry in the past");
        // check supply of correct amount of expiry dates
        OffsetDateTime now = OffsetDateTime.now();
        List<OffsetDateTime> expiries = Arrays.asList(now.plusYears(1), now.plusYears(2));
        assertThrows(IllegalArgumentException.class,
            () -> enWiki.changeUserPrivileges(user, granted, expiries, Collections.emptyList(), "dummy reason"),
            "attempted to set too many expiry dates");
        // Test runs without logging in, therefore expect failure.
        assertThrows(SecurityException.class,
            () -> enWiki.changeUserPrivileges(user, granted, Collections.emptyList(), Collections.emptyList(), "dummy reason"),
            "Attempted to set user privileges when logged out");
    }

    @Test
    public void getFirstRevision() throws Exception
    {
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.getFirstRevision("Special:SpecialPages"),
            "attempted to get the page history of a special page");
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.getFirstRevision("Media:Example.png"),
            "attempted to get the page history of a media page");
        assertNull(enWiki.getFirstRevision("dgfhd&fjklg"), "non-existent page");
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        Wiki.Revision first = testWiki.getFirstRevision("User:MER-C/UnitTests/Delete");
        assertEquals(217080L, first.getID());
    }

    @Test
    public void getTopRevision() throws Exception
    {
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.getTopRevision("Special:SpecialPages"),
            "attempted to get the page history of a special page");
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.getTopRevision("Media:Example.png"),
            "attempted to get the page history of a media page");
        assertNull(enWiki.getTopRevision("dgfhd&fjklg"), "non-existent page");
    }

    @Test
    public void exists() throws Exception
    {
        String[] titles = new String[] { "Main Page", "Tdkfgjsldf", "User:MER-C", "Wikipedia:Skfjdl", "Main Page", "Fish & chips" };
        boolean[] expected = new boolean[] { true, false, true, false, true, true };
        assertArrayEquals(expected, enWiki.exists(titles));
    }

    @Test
    public void resolveRedirects() throws Exception
    {
        String[] titles = new String[] { "Main page", "Main Page", "sdkghsdklg", "Hello.jpg", "Main page", "Fish & chips" };
        String[] expected = new String[] { "Main Page", "Main Page", "sdkghsdklg", "Goatse.cx", "Main Page", "Fish and chips" };
        assertArrayEquals(expected, enWiki.resolveRedirects(titles));
        assertEquals("الصفحة الرئيسية", arWiki.resolveRedirect("الصفحه الرئيسيه"), "rtl");
    }

    @Test
    public void getLinksOnPage() throws Exception
    {
        assertArrayEquals(new String[0], enWiki.getLinksOnPage("Skfls&jdkfs"), "non-existent page");
        // User:MER-C/monobook.js has one link... despite it being preformatted (?!)
        assertArrayEquals(new String[0], enWiki.getLinksOnPage("User:MER-C/monobook.css"), "page with no links");
    }

    @Test
    public void getImagesOnPage() throws Exception
    {
        // https://en.wikipedia.org/wiki/Template:POTD/2018-11-01
        List<String> pages = Arrays.asList("Skflsj&dkfs", "Template:POTD/2018-11-01", "User:MER-C/monobook.js");
        List<List<String>> images = enWiki.getImagesOnPage(pages);
        assertEquals(Collections.emptyList(), images.get(0), "non-existent page");
        assertEquals(Arrays.asList("File:Adelie Penguins on iceberg.jpg"), images.get(1));
        assertEquals(Collections.emptyList(), images.get(2), "page with no images");
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
        assertTrue(results[0].isEmpty(), "non-existent page");
        assertEquals(1, results[1].size());
        assertEquals("Template:La", results[1].get(0));
        assertTrue(results[2].isEmpty(), "page with no templates");
        assertEquals(results[1], results[3], "duplicate");

        assertEquals(0, testWiki.getTemplates(pages[1], Wiki.MAIN_NAMESPACE).length, "namespace filter");
        assertEquals("Template:La", testWiki.getTemplates(pages[1], Wiki.TEMPLATE_NAMESPACE)[0], "namespace filter");
    }

    @Test
    public void getCategories() throws Exception
    {
        List<String> pages = Arrays.asList("sdkf&hsdklj", "User:MER-C/monobook.js", "Category:~genre~ novels");
        List<List<String>> actual = enWiki.getCategories(pages, null, false);
        assertTrue(actual.get(0).isEmpty(), "non-existent page");
        assertTrue(actual.get(1).isEmpty(), "page with no categories");
        assertEquals(actual.get(2), Arrays.asList("Category:Hidden categories"), "page with one category");

        Map<String, Boolean> options = new HashMap<>();
        options.put("hidden", Boolean.FALSE);
        Wiki.RequestHelper rh = enWiki.new RequestHelper().filterBy(options);
        actual = enWiki.getCategories(Arrays.asList("Category:~genre~ novels"), rh, false);
        assertTrue(actual.get(0).isEmpty(), "filter hidden categories");
    }

    @Test
    public void getImageHistory() throws Exception
    {
        assertArrayEquals(new Wiki.LogEntry[0], enWiki.getImageHistory("File:Sdfjgh&sld.jpg"), "non-existent file");
        assertArrayEquals(new Wiki.LogEntry[0], enWiki.getImageHistory("File:WikipediaSignpostIcon.svg"), "commons image");
    }

    @Test
    public void getImage() throws Exception
    {
        File tempfile = File.createTempFile("wiki-java_getImage", null);
        assertFalse(enWiki.getImage("File:Sdkjf&sdlf.blah", tempfile), "non-existent file");

        // non-thumbnailed Commons file
        // https://commons.wikimedia.org/wiki/File:Portrait_of_Jupiter_from_Cassini.jpg
        enWiki.getImage("File:Portrait of Jupiter from Cassini.jpg", tempfile);
        byte[] imageData = Files.readAllBytes(tempfile.toPath());
        byte[] hash = sha256.digest(imageData);
        assertEquals("fc63c250bfce3f3511ccd144ca99b451111920c100ac55aaf3381aec98582035",
            String.format("%064x", new BigInteger(1, hash)));
        Files.delete(tempfile.toPath());
    }

    @Test
    public void getPageHistory() throws Exception
    {
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.getPageHistory("Special:SpecialPages", null),
            "attempted to get the page history of a special page");
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.getPageHistory("Media:Example.png", null),
            "attempted to get the page history of a media page");
        assertTrue(enWiki.getPageHistory("EOTkd&ssdf", null).isEmpty(), "non-existent page");

        // test by user
        Wiki.RequestHelper rh = enWiki.new RequestHelper().byUser("RetiredUser2");
        List<Wiki.Revision> history = enWiki.getPageHistory("Main Page", rh);
        assertEquals(4, history.size(), "by user");
        assertEquals(118014299L, history.get(0).getID(), "by user");
        assertEquals(118014140L, history.get(1).getID(), "by user");
        // test reverse
        rh = rh.reverse(true);
        List<Wiki.Revision> history2 = enWiki.getPageHistory("Main Page", rh);
        Collections.reverse(history);
        assertEquals(history, history2, "reverse");

        // test for RevisionDeleted revisions
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        history = testWiki.getPageHistory("User:MER-C/UnitTests/Delete", null);
        for (Wiki.Revision rev : history)
        {
            if (rev.getID() == 275553L)
            {
                assertTrue(rev.isContentDeleted(), "revdeled history: content");
                assertTrue(rev.isUserDeleted(), "revdeled history: user");
                assertTrue(rev.isCommentDeleted(), "revdeled history: summary");
                break;
            }
        }
    }

    @Test
    public void getDeletedHistory() throws Exception
    {
        // No special namespaces
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.getDeletedHistory("Special:SpecialPages", null));
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.getDeletedHistory("Media:Example.png", null));
        // Test runs without logging in, therefore expect failure.
        assertThrows(SecurityException.class,
            () -> enWiki.getDeletedHistory("Main Page", null),
            "attempted to view deleted revisions while logged out");
    }

    @Test
    public void move() throws Exception
    {
        // No special namespaces
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.move("Special:SpecialPages", "New page name", "Not a reason"));
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.move("Media:Example.png", "New page name", "Not a reason"));
    }

    @Test
    public void delete() throws Exception
    {
        // No special namespaces
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.delete("Special:SpecialPages", "Not a reason"));
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.delete("Media:Example.png", "Not a reason"));
        // Test runs without logging in, therefore expect failure.
        assertThrows(SecurityException.class, () -> enWiki.delete("User:MER-C", "Not a reason"),
            "attempted to delete while logged out");
    }

    @Test
    public void undelete() throws Exception
    {
        // No special namespaces
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.undelete("Special:SpecialPages", "Not a reason"),
            "Attempted to undelete a special page.");
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.undelete("Media:Example.png", "Not a reason"),
            "Attempted to undelete a special page.");
        // Test runs without logging in, therefore expect failure.
        assertThrows(SecurityException.class, () -> enWiki.undelete("User:MER-C", "Not a reason"),
            "Attempted to undelete while logged out.");
    }

    @Test
    public void block() throws Exception
    {
        assertThrows(IllegalArgumentException.class,
            () -> enWiki.block("MER-C", "Not a reason", OffsetDateTime.MIN, null),
            "Attempted to block with an expiry time in the past.");
        // Test runs without logging in, therefore expect failure.
        assertThrows(SecurityException.class,
            () -> enWiki.block("MER-C", "Not a reason", null, null),
            "Attempted to block while logged out.");
    }

    @Test
    public void unblock() throws Exception
    {
        // Test runs without logging in, therefore expect failure.
        assertThrows(SecurityException.class, () -> enWiki.unblock("MER-C", "Not a reason"),
            "Attempted to unblock while logged out.");
    }

    @Test
    public void revisionDelete() throws Exception
    {
        Wiki.Revision revision = testWiki.getRevision(349877L);
        Wiki.RequestHelper rh = testWiki.new RequestHelper()
            .byUser("MER-C")
            .byTitle("File:Wiki.java test5.jpg")
            .withinDateRange(OffsetDateTime.parse("2018-03-16T00:00:00Z"), OffsetDateTime.parse("2018-03-18T00:00:00Z"));
        List<Wiki.LogEntry> logs = testWiki.getLogEntries(Wiki.DELETION_LOG, "delete", rh);
        List<Wiki.Event> events = Arrays.asList(revision, logs.get(0));
        assertThrows(IllegalArgumentException.class,
            () -> testWiki.revisionDelete(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, "Not a reason", Boolean.FALSE, events),
            "can't mix revisions and log entries in RevisionDelete");
        // Test runs without logging in, therefore expect failure.
        assertThrows(SecurityException.class,
            () -> testWiki.revisionDelete(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, "Not a reason", Boolean.FALSE, Arrays.asList(revision)),
            "attempted to RevisionDelete while logged out");
    }

    @Test
    public void getBlockList() throws Exception
    {
        // https://en.wikipedia.org/wiki/Special:Blocklist/Nimimaan
        // see also getLogEntries() below
        List<Wiki.LogEntry> le = enWiki.getBlockList("Nimimaan", null);
        assertEquals(-1, le.get(0).getID());
        assertEquals("2016-06-21T13:14:54Z", le.get(0).getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertEquals("MER-C", le.get(0).getUser());
        assertEquals(Wiki.BLOCK_LOG, le.get(0).getType());
        assertEquals("block", le.get(0).getAction());
        assertEquals("User:Nimimaan", le.get(0).getTitle());
        assertEquals("spambot", le.get(0).getComment());
//        assertEquals(new Object[] {
//            false, true, // hard block (not anon only), account creation disabled,
//            false, true, // autoblock enabled, email disabled
//            true, "indefinite" // talk page access revoked, expiry
//        }, le[0].getDetails(), "block parameters");

        // This IP address should not be blocked (it is reserved)
        le = enWiki.getBlockList("0.0.0.0", null);
        assertTrue(le.isEmpty(), "0.0.0.0 should not be blocked");
    }

    @Test
    public void getLogEntries() throws Exception
    {
        // https://en.wikipedia.org/w/api.php?action=query&list=logevents&letitle=User:Nimimaan

        // Block log
        OffsetDateTime c = OffsetDateTime.parse("2016-06-30T23:59:59Z");
        Wiki.RequestHelper rh = enWiki.new RequestHelper()
            .byTitle("User:Nimimaan")
            .withinDateRange(null, c);
        List<Wiki.LogEntry> le = enWiki.getLogEntries(Wiki.ALL_LOGS, null, rh);
        assertEquals(75695806L, le.get(0).getID());
        assertEquals("2016-06-21T13:14:54Z", le.get(0).getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertEquals("MER-C", le.get(0).getUser());
        assertEquals(Wiki.BLOCK_LOG, le.get(0).getType(), "block log");
        assertEquals("block", le.get(0).getAction(), "block log");
        assertEquals("User:Nimimaan", le.get(0).getTitle());
        assertEquals("spambot", le.get(0).getComment());
        assertEquals("spambot", le.get(0).getParsedComment());
//        assertEquals(new Object[] {
//            false, true, // hard block (not anon only), account creation disabled,
//            false, true, // autoblock enabled, email disabled
//            true, "indefinite" // talk page access revoked, expiry
//        }, le[0].getDetails(), "block log parameters");

        // New user log
        assertEquals("Nimimaan", le.get(1).getUser());
        assertEquals(Wiki.USER_CREATION_LOG, le.get(1).getType());
        assertEquals("create", le.get(1).getAction());
        assertEquals("", le.get(1).getComment());
        assertEquals("", le.get(1).getParsedComment());
//        assertNull(le.get(1).getDetails(), "new user log parameters");

        // https://en.wikipedia.org/w/api.php?action=query&list=logevents&letitle=Talk:96th%20Test%20Wing/Temp

        // Move log
        rh = enWiki.new RequestHelper()
            .byTitle("Talk:96th Test Wing/Temp")
            .withinDateRange(null, c);
        le = enWiki.getLogEntries(Wiki.ALL_LOGS, null, rh);
        assertEquals(Wiki.MOVE_LOG, le.get(0).getType());
        assertEquals("move", le.get(0).getAction());
        // TODO: test for new title, redirect suppression

        // RevisionDeleted log entries, no access
        // https://test.wikipedia.org/w/api.php?action=query&list=logevents&letitle=User%3AMER-C%2FTest
        rh = testWiki.new RequestHelper().byTitle("User:MER-C/Test");
        le = testWiki.getLogEntries(Wiki.ALL_LOGS, null, rh);
        assertNull(le.get(0).getComment(), "reason hidden");
        assertNull(le.get(0).getParsedComment(), "reason hidden");
        assertTrue(le.get(0).isCommentDeleted(), "reason hidden");
        assertNull(le.get(0).getUser(), "user hidden");
        assertTrue(le.get(0).isUserDeleted(), "user hidden");
        // https://test.wikipedia.org/w/api.php?action=query&list=logevents&leuser=MER-C
        //     &lestart=20161002050030&leend=20161002050000&letype=delete
        rh = testWiki.new RequestHelper()
            .byUser("MER-C")
            .withinDateRange(OffsetDateTime.parse("2016-10-02T05:00:00Z"), OffsetDateTime.parse("2016-10-02T05:30:00Z"));
        le = testWiki.getLogEntries(Wiki.DELETION_LOG, null, rh);
        assertNull(le.get(1).getTitle(), "action hidden");
        assertTrue(le.get(1).isContentDeleted(), "action hidden");
    }

    @Test
    public void getPageInfo() throws Exception
    {
        String[] pages = new String[] { "Main Page", "IPod", "Main_Page", "Special:Specialpages" };
        Map<String, Object>[] pageinfo = enWiki.getPageInfo(pages);

        // Main Page
        Map<String, Object> protection = (Map<String, Object>)pageinfo[0].get("protection");
        assertEquals(Wiki.FULL_PROTECTION, protection.get("edit"), "Main Page edit protection level");
        assertNull(protection.get("editexpiry"), "Main Page edit protection expiry");
        assertEquals(Wiki.FULL_PROTECTION, protection.get("move"), "Main Page move protection level");
        assertNull(protection.get("moveexpiry"), "Main Page move protection expiry");
        assertTrue((Boolean)protection.get("cascade"), "Main Page cascade protection");
        assertEquals("Main Page", pageinfo[0].get("displaytitle"), "Main Page display title");
        assertEquals(pages[0], pageinfo[0].get("inputpagename"), "inputpagename");
        assertEquals(pages[0], pageinfo[0].get("pagename"), "normalized identity");

        // different display title
        assertEquals("iPod", pageinfo[1].get("displaytitle"), "iPod display title");

        // Main_Page (duplicate, should be identical except for inputpagename)
        assertEquals(pages[2], pageinfo[2].get("inputpagename"), "identity");
        pageinfo[0].remove("inputpagename");
        pageinfo[2].remove("inputpagename");
        assertEquals(pageinfo[0], pageinfo[2], "duplicate");

        // Special page = return null
        assertNull(pageinfo[3], "special page");
    }

    @Test
    public void getCategoryMemberCounts() throws Exception
    {
        // highly volatile content, so not amenable to unit testing
        // but can check clear zero categories and title rewrites
        List<int[]> results = testWiki.getCategoryMemberCounts(Arrays.asList("Category:Testssss",
            "Wikipedia noticeboards", "Category:Wikipedia noticeboards"));
        assertArrayEquals(new int[] {0, 0, 0, 0}, results.get(0), "non-existent category");
        assertArrayEquals(results.get(2), results.get(1), "check title rewrite");
    }

    @Test
    public void getFileMetadata() throws Exception
    {
        assertNull(enWiki.getFileMetadata("File:Lweo&pafd.blah"), "non-existent file");
        assertNull(enWiki.getFileMetadata("File:WikipediaSignpostIcon.svg"), "commons file");

        // further tests blocked on MediaWiki API rewrite
        // see https://phabricator.wikimedia.org/T89971
    }

    @Test
    public void getDuplicates() throws Exception
    {
        assertArrayEquals(new String[0], enWiki.getDuplicates("File:Sdfj&ghsld.jpg"), "non-existent file");
    }

    @Test
    public void getInterWikiLinks() throws Exception
    {
        Map<String, String> temp = enWiki.getInterWikiLinks("Gkdfkkl&djfdf");
        assertTrue(temp.isEmpty(), "non-existent page");
    }

    @Test
    public void getExternalLinksOnPages() throws Exception
    {
        List<List<String>> links = enWiki.getExternalLinksOnPage(Arrays.asList("Gdkgfskl&dkf", "User:MER-C/monobook.js"));
        assertTrue(links.get(0).isEmpty(), "non-existent page");
        assertTrue(links.get(1).isEmpty(), "page with no links");

        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Linkfinder
        links = testWiki.getExternalLinksOnPage(Arrays.asList("User:MER-C/UnitTests/Linkfinder"));
        List<String> expected = Arrays.asList("http://spam.example.com", "http://www.example.net", "https://en.wikipedia.org");
        assertEquals(expected, links.get(0));
    }

    @Test
    public void getSectionText() throws Exception
    {
        assertEquals("This is section 0.", testWiki.getSectionText("User:MER-C/UnitTests/SectionTest", 0));
        assertEquals("===Section 3===\nThis is section 2.",
            testWiki.getSectionText("User:MER-C/UnitTests/SectionTest", 2));
        assertNull(enWiki.getSectionText("User:MER-C/monobook.css", 4920), "non-existent section");
        assertThrows(IllegalArgumentException.class,
            () -> enWiki.getSectionText("User:MER-C/monobook.css", -50),
            "negative section number");
    }

    @Test
    public void random() throws Exception
    {
        // The results of this query are obviously non-deterministic, but we can
        // check whether we get the right namespace.
        for (int i = 0; i < 3; i++)
        {
            String random = enWiki.random();
            assertEquals(Wiki.MAIN_NAMESPACE, enWiki.namespace(random), "main namespace");
            random = enWiki.random(Wiki.PROJECT_NAMESPACE, Wiki.USER_NAMESPACE);
            int temp = enWiki.namespace(random);
            assertTrue(temp == Wiki.PROJECT_NAMESPACE || temp == Wiki.USER_NAMESPACE, "multiple namespaces");
            random = enWiki.random(Wiki.PROJECT_NAMESPACE, Wiki.MEDIA_NAMESPACE, Wiki.SPECIAL_NAMESPACE, -1245, 999999);
            assertEquals(Wiki.PROJECT_NAMESPACE, enWiki.namespace(random), "ignores invalid namespaces");
        }
    }

    @Test
    public void timezone()
    {
        assertEquals(ZoneId.of("UTC"), enWiki.timezone()); // not the same as ZoneOffset.UTC
    }

    @Test
    public void usesCapitalLinks()
    {
        assertTrue(enWiki.usesCapitalLinks(), "en.wp = true");
        assertFalse(enWikt.usesCapitalLinks(), "en.wikt = false");
    }

    @Test
    public void getLocale()
    {
        assertEquals(Locale.ENGLISH, enWiki.locale());
        assertEquals(Locale.GERMAN, deWiki.locale());
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
        assertEquals("Blah", enWiki.normalize("Blah"));
        assertEquals("Blah", enWiki.normalize("blah"));
        assertEquals("Blah", enWiki.normalize("Blah#Section"), "contains section");
        assertEquals("Blah", enWiki.normalize(":Blah"), "leading colon");
        assertEquals("File:Blah.jpg", enWiki.normalize("File:Blah.jpg"));
        assertEquals("File:Blah.jpg", enWiki.normalize("file:blah.jpg"));
        assertEquals("File:Blah.jpg", enWiki.normalize(":file:Blah.jpg#Copyright"));
        assertEquals("Category:Wikipedia:blah", enWiki.normalize("Category:Wikipedia:blah"));
        assertEquals("Hilfe Diskussion:Glossar", deWiki.normalize("Help talk:Glossar"), "namespace i18n");
        assertEquals("Wikipedia:V", enWiki.normalize("WP:V"), "namespace alias");
        // variants with different cases undocumented

        // capital links = false
        assertEquals("blah", enWikt.normalize("blah"), "capital links = false");
        assertEquals("Wiktionary:main page", enWikt.normalize("Wiktionary:main page"), "capital links = false");
        assertEquals("Wiktionary:main page", enWikt.normalize("wiktionary:main page"), "capital links = false");
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
        assertTrue(b[0]);
        assertFalse(b[1]);
        assertFalse(b[2], "non-existent page");
    }
    
    @Test
    public void whatLinksHere() throws Exception
    {
        // general test (generally too non-deterministic for functionality testing)
        List<String> titles = Arrays.asList("Empty page title 1234");
        List<List<String>> results = enWiki.whatLinksHere(titles, false);
        assertEquals(1, results.size());
        assertEquals(Collections.emptyList(), results.get(0));
        
        // check namespace filtering (can test functionality here)
        titles = Arrays.asList("Wikipedia:Featured picture candidates");
        results = enWiki.whatLinksHere(titles, false, Wiki.MAIN_NAMESPACE);
        assertEquals(Arrays.asList("Wikipedia community", "Featured picture candidates", 
            "Featured picture candidate"), results.get(0), "namespace filter");
        
        // check redirect filtering
        results = enWiki.whatLinksHere(titles, true, Wiki.MAIN_NAMESPACE);
        assertEquals(Arrays.asList("Featured picture candidates", "Featured picture candidate"),
            results.get(0), "namespace filter");        
    }

    @Test
    public void whatTranscludesHere() throws Exception
    {
        List<String> titles = Arrays.asList("Wikipedia:Articles for deletion/MegaMeeting.com", "Empty page title 1234",
            "Wikipedia:Articles for deletion/PolymerUpdate", "Wikipedia:Articles for deletion/Mobtown Studios");
        List<List<String>> results = enWiki.whatTranscludesHere(titles);
        assertEquals(4, results.size());
        assertEquals(Arrays.asList("Wikipedia:Articles for deletion/Log/2018 April 23"), results.get(0));
        assertEquals(Collections.emptyList(), results.get(1));
        assertEquals(Arrays.asList("Wikipedia:Articles for deletion/Log/2018 April 22"), results.get(2));
        assertEquals(Arrays.asList("Wikipedia:Articles for deletion/Log/2018 April 24"), results.get(3));
        titles = Arrays.asList("Wikipedia:Articles for deletion/MegaMeeting.com");
        assertEquals(Collections.emptyList(), enWiki.whatTranscludesHere(titles, Wiki.MAIN_NAMESPACE).get(0), "namespace filter");
    }

    @Test
    public void getUploads() throws Exception
    {
        Wiki.User[] users = enWiki.getUsers(new String[]
        {
            "LakeishaDurham0", // blocked spambot
            "Mifter" // https://en.wikipedia.org/wiki/Special:ListFiles/Mifter
        });
        assertTrue(enWiki.getUploads(users[0], null).isEmpty(), "no uploads");
        OffsetDateTime odt = OffsetDateTime.parse("2017-03-05T17:59:00Z");
        Wiki.RequestHelper rh = enWiki.new RequestHelper().withinDateRange(odt, odt.plusMinutes(20));
        List<Wiki.LogEntry> results = enWiki.getUploads(users[1], rh);
        assertEquals(3, results.size());
        assertEquals("File:Padlock-pink.svg", results.get(0).getTitle());
        assertEquals("File:Padlock-silver-light.svg", results.get(1).getTitle());
        assertEquals("File:Padlock-blue.svg", results.get(2).getTitle());
    }

    @Test
    public void getRevision() throws Exception
    {
        // https://en.wikipedia.org/w/index.php?title=Wikipedia_talk%3AWikiProject_Spam&oldid=597454682
        Wiki.Revision rev = enWiki.getRevision(597454682L);
        assertEquals("Wikipedia talk:WikiProject Spam", rev.getTitle());
        assertEquals("2014-02-28T00:40:31Z", rev.getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertEquals("Lowercase sigmabot III", rev.getUser());
        assertEquals("Archiving 3 discussion(s) to [[Wikipedia talk:WikiProject Spam/2014 Archive Feb 1]]) (bot",
            rev.getComment());
        String parsedcomment = "Archiving 3 discussion(s) to <a href=\"https://en.wikipedia.org/wiki/Wikipedia_talk:WikiProject_Spam/2014_Archive_Feb_1\" "
            + "title=\"Wikipedia talk:WikiProject Spam/2014 Archive Feb 1\">Wikipedia talk:WikiProject Spam/2014 Archive Feb 1</a>) (bot";
        assertEquals(parsedcomment, rev.getParsedComment(), "parsed summary with link");
        assertEquals("540a2b3501e4d15729ea25ec3238da9ad0dd6dc4", rev.getSha1());
        assertEquals(4286, rev.getSize());
        assertEquals(597454682L, rev.getID());
        assertEquals(597399794L, rev.getPrevious().getID());
        // assertEquals(597553957L, rev.getNext().getRevid());
        assertTrue(rev.isMinor());
        assertFalse(rev.isNew());
        assertFalse(rev.isBot());
        assertFalse(rev.isUserDeleted());
        assertFalse(rev.isCommentDeleted());
        assertFalse(rev.isContentDeleted());
        assertFalse(rev.isPageDeleted());

        // revdel, logged out
        // https://en.wikipedia.org/w/index.php?title=Imran_Khan_%28singer%29&oldid=596714684
        rev = enWiki.getRevision(596714684L);
        assertTrue(rev.isUserDeleted(), "user revdeled flag");
        assertTrue(rev.isCommentDeleted(), "summary revdeled flag");
        assertTrue(rev.isContentDeleted(), "content revdeled flag");
        assertNull(rev.getComment(), "summary revdeled");
        assertNull(rev.getParsedComment(), "summary revdeled");
        assertNull(rev.getUser(), "user revdeled");
        assertNull(rev.getSha1(), "sha1/content revdeled");
        
        // Revision has been deleted (not RevisionDeleted)
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        assertNull(testWiki.getRevision(217078L), "page deleted");

        assertNull(testWiki.getRevision(1L << 62), "large revid");
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
            enWiki.diff(from, to);
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
            enWiki.diff(from, to);
            fail("Failed to specify to content.");
        }
        catch (IllegalArgumentException | NoSuchElementException expected)
        {
            to.clear();
        }

        // https://en.wikipedia.org/w/index.php?title=Dayo_Israel&oldid=738178354&diff=prev
        from.put("revid", 738178354L);
        to.put("revid", Wiki.PREVIOUS_REVISION);
        assertEquals("", enWiki.diff(from, to), "dummy edit");
        // https://en.wikipedia.org/w/index.php?title=Source_Filmmaker&diff=804972897&oldid=803731343
        // The MediaWiki API does not distinguish between a dummy edit and no
        // difference. Both are now set to the empty string.
        from.put("revid", 803731343L);
        to.put("revid", 804972897L);
        assertEquals("", enWiki.diff(from, to), "no difference");
        // no deleted pages allowed
        // FIXME: broken because makeHTTPRequest() swallows the API error
        // actual = enWiki.diff("Create a page", 0L, null, null, 804972897L, null);
        // assertNull(actual, "to deleted");
        // actual = enWiki.diff(null, 804972897L, null, "Create a page", 0L, null);
        // no RevisionDeleted revisions allowed (also broken)
        // https://en.wikipedia.org/w/index.php?title=Imran_Khan_%28singer%29&oldid=596714684
        // from.put("revid", 596714684L);
        // to.put("revid", Wiki.NEXT_REVISION);
        // assertNull(enWiki.diff(from, to), "from deleted revision);

        // bad revids
        from.put("revid", 1L << 62);
        to.put("revid", 803731343L);
        assertNull(enWiki.diff(from, to), "bad from revid");
        from.put("revid", 803731343L);
        to.put("revid", 1L << 62);
        assertNull(enWiki.diff(from, to), "bad to revid");
    }

    @Test
    public void contribs() throws Exception
    {
        List<String> users = Arrays.asList(
            "Dsdlgfkjsdlkfdjilgsujilvjcl", // should not exist
            "0.0.0.0", // IP address
            "Allancake" // revision deleted
        );
        List<List<Wiki.Revision>> edits = enWiki.contribs(users, null, null);

        assertTrue(edits.get(0).isEmpty(), "non-existent user");
        assertTrue(edits.get(1).isEmpty(), "IP address with no edits");
        for (Wiki.Revision rev : edits.get(2))
        {
            if (rev.getID() == 724989913L)
            {
                assertTrue(rev.isContentDeleted(), "summary deleted");
                assertTrue(rev.isContentDeleted(), "content deleted");
            }
        }

        // check rcoptions and namespace filter
        // https://test.wikipedia.org/wiki/Blah_blah_2
        Map<String, Boolean> options = new HashMap<>();
        options.put("new", Boolean.TRUE);
        options.put("top", Boolean.TRUE);
        Wiki.RequestHelper rh = testWiki.new RequestHelper()
            .inNamespaces(Wiki.MAIN_NAMESPACE)
            .filterBy(options);
        edits = testWiki.contribs(Arrays.asList("MER-C"), null, rh);
        assertEquals(120919L, edits.get(0).get(0).getID(), "filtered");
        // not implemented in MediaWiki API
        // assertEquals("bcdb66a63846bacdf39f5c52a7d2cc5293dbde3e", edits.get(0).get(0).getSha1());
        for (Wiki.Revision rev : edits.get(0))
            assertEquals(Wiki.MAIN_NAMESPACE, testWiki.namespace(rev.getTitle()), "namespace");
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
        assertNull(users[0], "IP address");
        assertNull(users[2], "non-existent user");
        assertNull(users[4], "IP address range");

        assertEquals(usernames[1], users[1].getUsername(), "normalized username");
        assertFalse(users[1].isBlocked());
        assertEquals(Wiki.Gender.unknown, users[1].getGender());
        assertEquals("2006-07-07T10:52:41Z",
            users[1].getRegistrationDate().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        // should be privileged information, but isn't?
        // assertFalse(users[1].canBeEmailed());

        List<String> groups = users[1].getGroups();
        List<String> temp = Arrays.asList("*", "autoconfirmed", "user", "sysop");
        assertTrue(groups.containsAll(temp));

        // check (subset of) rights
        List<String> rights = users[1].getRights();
        temp = Arrays.asList("apihighlimits", "delete", "block", "editinterface");
        assertTrue(rights.containsAll(temp));

        assertEquals(usernames[3], users[3].getUsername());
        assertEquals(2, users[3].countEdits());
        assertTrue(users[3].isBlocked());
    }

    @Test
    public void getPageText() throws Exception
    {
        assertThrows(UnsupportedOperationException.class,
            () -> testWiki.getPageText("Special:Specialpages"),
            "Tried to get page text for a special page!");
        assertThrows(UnsupportedOperationException.class,
            () -> testWiki.getPageText("Media:Example.png"),
            "Tried to get page text for a media page!");

        String[] text = testWiki.getPageText(new String[]
        {
            "User:MER-C/UnitTests/Delete", // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
            "User:MER-C/UnitTests/pagetext", // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/pagetext
            "Gsgdksgjdsg", // https://test.wikipedia.org/wiki/Gsgdksgjdsg (does not exist)
            "User:NikiWiki/EmptyPage" // https://test.wikipedia.org/wiki/User:NikiWiki/EmptyPage (empty)

        });
        // API result does not include the terminating new line
        assertEquals("This revision is not deleted!", text[0]);
        assertEquals("&#039;&#039;italic&#039;&#039;\n'''&amp;'''\n&&\n&lt;&gt;\n<>\n&quot;",
            text[1], "decoding");
        assertNull(text[2], "non-existent page");
        assertEquals("", text[3], "empty page");
    }

    @Test
    public void getText() throws Exception
    {
        assertEquals(0, testWiki.getText(new String[0], null, -1).length);
        assertEquals(0, testWiki.getText(null, new long[0], -1).length);

        long[] ids =
        {
            230472L, // https://test.wikipedia.org/w/index.php?oldid=230472 (decoding test)
            322889L, // https://test.wikipedia.org/w/index.php?oldid=322889 (empty revision)
            275553L, // https://test.wikipedia.org/w/index.php?oldid=275553 (RevisionDeleted)
            316531L, // https://test.wikipedia.org/w/index.php?oldid=316531 (first revision on same page)
            316533L  // https://test.wikipedia.org/w/index.php?oldid=316533 (second revision on same page)
        };
        String[] text = testWiki.getText(null, ids, -1);
        assertEquals("&#039;&#039;italic&#039;&#039;\n'''&amp;'''\n&&\n&lt;&gt;\n<>\n&quot;",
            text[0], "decoding");
        assertEquals("", text[1], "empty revision");
        assertNull(text[2], "content deleted");
        assertEquals("Testing 0.2786153173518522", text[3], "same page");
        assertEquals("Testing 0.28713760508426645", text[4], "same page");
    }

    @Test
    public void parse() throws Exception
    {
        HashMap<String, Object> content = new HashMap<>();
        content.put("title", "Hello");
        assertNull(enWiki.parse(content, 50, true), "no such section");
        // FIXME: currently broken because makeHTTPRequest swallows the API error
        // content.put("title", "Create a page");
        // assertNull(enWiki.parse(content, -1, true), "deleted page);
        content.clear();
        content.put("revid", 1L << 62);
        assertNull(enWiki.parse(content, -1, true), "bad revid");
        // https://en.wikipedia.org/w/index.php?oldid=596714684
        content.put("revid", 596714684L);
        assertThrows(SecurityException.class, () -> enWiki.parse(content, -1, true),
            "RevisionDeleted revision, no access");

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
        assertEquals(0, results.length, "no results");
        // https://test.wikipedia.org/w/api.php?action=query&list=search&srsearch=User:%20subpageof:MER-C/UnitTests%20revision%20delete
        results = testWiki.search("User: subpageof:MER-C/UnitTests revision delete", Wiki.USER_NAMESPACE);
        assertEquals(2, results.length, "result count");
        Map<String, Object> result = results[0];
        assertEquals("User:MER-C/UnitTests/Delete", result.get("title"), "title");
        assertEquals("This <span class=\"searchmatch\">revision</span> is not <span class=\"searchmatch\">deleted</span>!",
            result.get("snippet"), "snippet");
        assertEquals(29, result.get("size"), "page size");
        assertEquals(5, result.get("wordcount"), "word count");
        assertEquals(OffsetDateTime.parse("2016-06-16T08:40:17Z"), result.get("lastedittime"), "last edit time");
    }

    @Test
    public void recentChanges() throws Exception
    {
        // The results of this query will never be known in advance, so this is
        // by necessity an incomplete test. That said, there are a few things we
        // can test for...
        Wiki.RequestHelper rh = enWiki.new RequestHelper().limitedTo(10);
        List<Wiki.Revision> rc = enWiki.recentChanges(rh);
        assertEquals(10, rc.size(), "length");
        // Check if the changes are actually recent (i.e. in the last 10 minutes).
        assertTrue(rc.get(9).getTimestamp().isAfter(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10)), "recentness");
        // Check namespace filtering
        rh = rh.inNamespaces(Wiki.TALK_NAMESPACE);
        rc = enWiki.recentChanges(rh);
        for (Wiki.Revision rev : rc)
            assertEquals(Wiki.TALK_NAMESPACE, enWiki.namespace(rev.getTitle()), "test namespace filter");
        // check options filtering
        Map<String, Boolean> options = new HashMap<>();
        options.put("minor", Boolean.FALSE);
        options.put("bot", Boolean.TRUE);
        rh = rh.filterBy(options);
        rc = enWiki.recentChanges(rh);
        for (Wiki.Revision rev : rc)
        {
            assertTrue(rev.isBot(), "test rcoptions");
            assertFalse(rev.isMinor(), "test rcoptions");
        }
    }

    @Test
    public void allUsersInGroup() throws Exception
    {
        assertArrayEquals(new String[0], testWiki.allUsersInGroup("sdfkd|&"), "invalid input");
        assertArrayEquals(new String[] { "Jimbo Wales" }, enWiki.allUsersInGroup("founder"));
    }

    @Test
    public void allUserswithRight() throws Exception
    {
        assertArrayEquals(new String[0], testWiki.allUsersWithRight("sdfkd|&"), "invalid input");
    }

    @Test
    public void constructNamespaceString() throws Exception
    {
        assertEquals("1|2|3", enWiki.constructNamespaceString(new int[] { 3, 2, 1, 2, -10 }));
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
        assertEquals(expected, actual);
    }

    // INNER CLASS TESTS

    @Test
    public void revisionCompareTo() throws Exception
    {
        // https://en.wikipedia.org/w/index.php?title=Azerbaijan&offset=20180125204500&action=history
        long[] oldids = { 822342083L, 822341440L };
        Wiki.Revision[] revisions = enWiki.getRevisions(oldids);
        assertEquals(0, revisions[0].compareTo(revisions[0]), "self");
        assertTrue(revisions[1].compareTo(revisions[0]) < 0, "before");
        assertTrue(revisions[0].compareTo(revisions[1]) > 0, "after");
    }

    @Test
    public void revisionEqualsAndHashCode() throws Exception
    {
        // https://test.wikipedia.org/wiki/Special:Permanentlink/217080
        Wiki.Revision rev1 = testWiki.getFirstRevision("User:MER-C/UnitTests/Delete");
        Wiki.Revision rev2 = testWiki.getRevision(217080L);
        assertEquals(rev1, rev2, "equals");
        assertEquals(rev1.hashCode(), rev2.hashCode(), "hashcode");
        // exported revisions
        // https://de.wikipedia.org/w/index.php?title=University_of_Kashmir&diff=prev&oldid=159785234
        // https://en.wikipedia.org/w/index.php?title=University_of_Kashmir&diff=prev&oldid=558893308
        assertFalse(deWiki.getRevision(159785234L).equals(enWiki.getRevision(558893308L)), "equals: exported revisions");
        // https://test.wikipedia.org/wiki/Special:Permanentlink/275553
        // RevisionDeleted, therefore need to test for NPEs
        rev1 = testWiki.getRevision(275553L);
        Wiki.RequestHelper rh = testWiki.new RequestHelper()
            .withinDateRange(OffsetDateTime.parse("2016-01-01T00:00:00Z"), OffsetDateTime.parse("2016-06-16T08:40:00Z"));
        rev2 = testWiki.getPageHistory("User:MER-C/UnitTests/Delete", rh).get(0);
        assertEquals(rev1, rev2, "NPE check - equals");
        assertEquals(rev1.hashCode(), rev2.hashCode(), "NPE check - hashcode");
    }

    @Test
    public void revisionPermanentURL() throws Exception
    {
        Wiki.Revision rev = enWiki.getRevision(822342083L);
        assertEquals("https://en.wikipedia.org/w/index.php?oldid=822342083", rev.permanentUrl());
    }

    @Test
    public void userIsAllowedTo() throws Exception
    {
        Wiki.User user = enWiki.getUser("LornaIln046035"); // spambot
        assertTrue(user.isAllowedTo("read"));
        assertFalse(user.isAllowedTo("checkuser"));
        assertFalse(user.isAllowedTo("sdlkghsdlkgsd"));
    }

    @Test
    public void userIsA() throws Exception
    {
        Wiki.User me = testWiki.getUser("MER-C");
        assertTrue(me.isA("sysop"));
        assertFalse(me.isA("founder"));
        assertFalse(me.isA("sdlkghsdlkgsd"));
    }

    @Test
    @DisplayName("RequestHelper.withinDateRange")
    public void requestHelperDates()
    {
        OffsetDateTime odt = OffsetDateTime.parse("2017-03-05T17:59:00Z");
        assertThrows(IllegalArgumentException.class,
            () -> enWiki.new RequestHelper().withinDateRange(odt, odt.minusMinutes(20)));
    }
}
