/**
 *  @(#)WikiTest.java 0.39 12/08/2023
 *  Copyright (C) 2014-2023 MER-C
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
import javax.security.auth.login.FailedLoginException;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit tests for Wiki.java
 *  @author MER-C
 *  @version 0.39
 */
public class WikiTest
{
    private final Wiki enWiki, deWiki, arWiki, testWiki, enWikt, commons;
    private final MessageDigest sha256;

    /**
     *  Construct wiki objects for each test so that tests are independent.
     *  Don't put network requests here.
     *  @throws Exception if a network error occurs
     */
    public WikiTest() throws Exception
    {
        enWiki = Wiki.newSession("en.wikipedia.org");
        enWiki.setMaxLag(-1);
        deWiki = Wiki.newSession("de.wikipedia.org");
        deWiki.setMaxLag(-1);
        arWiki = Wiki.newSession("ar.wikipedia.org");
        arWiki.setMaxLag(-1);
        testWiki = Wiki.newSession("test.wikipedia.org");
        testWiki.setMaxLag(-1);
        enWikt = Wiki.newSession("en.wiktionary.org");
        enWikt.setMaxLag(-1);
        commons = Wiki.newSession("commons.wikimedia.org");
        commons.setMaxLag(-1);

        sha256 = MessageDigest.getInstance("SHA-256");
    }

    @Test
    public void testUrls()
    {
        Wiki dummy = Wiki.newSession("example.com", "/scriptpath", "http://");
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
        Wiki dummy = Wiki.newSession("en.wikipedia.org");
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

        Wiki dummy = Wiki.newSession("en.wikipedia.org", "/example", "https://");
        int result_5 = enWiki.compareTo(dummy);
        assertTrue(result_5 > 0, "multiple instances on same domain");
    }
    
    @Test
    public void login() throws Exception
    {
        assertThrows(FailedLoginException.class, () -> 
            enWiki.login("MER-C@Fake", "ObviouslyWrongPassword"), "Failed login must throw exception.");
    }
    
    @Test
    public void requiresExtension()
    {
        // https://en.wikipedia.org/wiki/Special:Version
        enWiki.requiresExtension("SpamBlacklist");
        enWiki.requiresExtension("CheckUser");
        enWiki.requiresExtension("Abuse Filter");
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.requiresExtension("This extension does not exist."),
            "required a non-existing extension");        
    }

    @Test
    @DisplayName("Wiki.setAssertionMode (logged out)")
    public void assertionMode() throws Exception
    {
        enWiki.setAssertionMode(Wiki.ASSERT_USER);
        assertEquals(Wiki.ASSERT_USER, enWiki.getAssertionMode(), "check assertion mode set");
        // This test runs logged out. The following assertions are expected to fail.
        List<String> pages = List.of("Main Page");
        assertThrows(AssertionError.class, () -> enWiki.getPageText(pages).get(0), "ASSERT_USER");
        enWiki.setAssertionMode(Wiki.ASSERT_BOT);
        assertThrows(AssertionError.class, () -> enWiki.getPageText(pages).get(0), "ASSERT_BOT");
        // This only trips on write requests.
        // enWiki.setAssertionMode(Wiki.ASSERT_SYSOP);
        // assertThrows(AssertionError.class, () -> enWiki.getPageText("Main Page"), "ASSERT_SYSOP");
        enWiki.setAssertionMode(Wiki.ASSERT_NONE);
        enWiki.getPageText(pages); // no exception
    }

    @Test
    public void setResolveRedirects() throws Exception
    {
        List<String> pages = List.of("User:MER-C/UnitTests/redirect",
            "User:MER-C/UnitTests/Delete");
        
        testWiki.setResolveRedirects(true);
        enWiki.setResolveRedirects(true);
        assertTrue(testWiki.isResolvingRedirects());
        // https://test.wikipedia.org/w/index.php?title=User:MER-C/UnitTests/redirect&redirect=no
        // redirects to https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        assertEquals("This revision is not deleted!", testWiki.getPageText(pages).get(0),
            "resolveredirects/getPageText");
        List<Map<String, Object>> x = testWiki.getPageInfo(pages);
        x.get(0).remove("inputpagename");
        x.get(1).remove("inputpagename");
        assertEquals(x.get(1), x.get(0), "resolveredirects/getPageInfo");
        List<List<String>> y = enWiki.getTemplates(List.of("Main page", "Main Page"));
        assertEquals(y.get(1), y.get(0), "resolveredirects/getTemplates");
    }

    @Test
    public void namespace() throws Exception
    {
        assertEquals(Wiki.CATEGORY_NAMESPACE, enWiki.namespace("Category:CSD"), "en category");
        assertEquals(Wiki.CATEGORY_NAMESPACE, enWiki.namespace("category:CSD"), "en category lower case");
        assertEquals(Wiki.CATEGORY_NAMESPACE, enWiki.namespace("CaTeGoRy:CSD"), "en category random inner case");
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
        assertEquals(500, enWiki.listPages("", null, Wiki.MAIN_NAMESPACE).size(), "listPages override");
        assertEquals(530, enWiki.getPageHistory("Main Page", null).size(), "after listPages override");
    }

    @ParameterizedTest
    @CsvSource({
        "Hello, Talk:Hello",
        "User:Hello, User talk:Hello",
        "Talk:Hello, EXCEPTION",       // talk page of a talk page
        "Special:Newpages, EXCEPTION", // special pages don't have talk pages
        "Media:Wiki.png, EXCEPTION"})  // media pages don't have talk pages    
    public void getTalkPage(String page, String talkpage) throws Exception
    {
        if (talkpage.equals("EXCEPTION"))
            assertThrows(IllegalArgumentException.class, () -> enWiki.getTalkPage(page));
        else
            assertEquals(talkpage, enWiki.getTalkPage(page));
    }

    @ParameterizedTest
    @CsvSource({
        "Talk:Hello, Hello",
        "User talk:Hello, User:Hello",
        "Hello, EXCEPTION",       // content page of a content page
        "Special:Newpages, EXCEPTION", // special pages don't have content pages
        "Media:Wiki.png, EXCEPTION"})  // media pages don't have content pages    
    public void getContentPage(String page, String contentpage) throws Exception
    {
        if (contentpage.equals("EXCEPTION"))
            assertThrows(IllegalArgumentException.class, () -> enWiki.getContentPage(page));
        else
            assertEquals(contentpage, enWiki.getContentPage(page));
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
        boolean[] temp = testWiki.userExists(List.of("MER-C", "127.0.0.1", "Djskgh;jgsd", "::/1"));
        assertTrue(temp[0], "user that exists");
        assertFalse(temp[1], "IP address");
        assertFalse(temp[2], "nonsense input");
        assertFalse(temp[3], "IPv6 range");
    }

    @Test
    public void changeUserPrivileges() throws Exception
    {
        // check expiry
        Wiki.User user = enWiki.getUsers(List.of("Example")).get(0);
        List<String> granted = List.of("autopatrolled");
        assertThrows(IllegalArgumentException.class,
            () -> enWiki.changeUserPrivileges(user, granted, List.of(OffsetDateTime.MIN), Collections.emptyList(), "dummy reason"),
            "attempted to set user privilege expiry in the past");
        // check supply of correct amount of expiry dates
        OffsetDateTime now = OffsetDateTime.now();
        List<OffsetDateTime> expiries = List.of(now.plusYears(1), now.plusYears(2));
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
        assertEquals(List.of("HotCat", "MyStupidTestTag"), first.getTags());
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
        List<String> titles = List.of("Main Page", "Tdkfgjsldf", "User:MER-C", 
            "Wikipedia:Skfjdl", "Main Page", "Fish & chips", "[[illegal title]]");
        boolean[] expected = new boolean[] { true, false, true, false, true, true, false };
        assertArrayEquals(expected, enWiki.exists(titles));
    }

    @Test
    public void resolveRedirects() throws Exception
    {
        List<String> titles = List.of("Main page", "Main Page", "sdkghsdklg",
            "Hello.jpg", "main page", "Fish & chips", "Fish&nbsp;&&nbsp;chips");
        List<String> expected = List.of("Main Page", "Main Page", "Sdkghsdklg",
            "Goatse.cx", "Main Page", "Fish and chips", "Fish and chips");
        assertEquals(expected, enWiki.resolveRedirects(titles));
        assertEquals(List.of("الصفحة الرئيسية"), arWiki.resolveRedirects(List.of("الصفحه الرئيسيه")), "rtl");
    }

    @Test
    public void getLinksOnPage() throws Exception
    {
        assertTrue(enWiki.getLinksOnPage("Skfls&jdkfs").isEmpty(), "non-existent page");
        // User:MER-C/monobook.js has one link... despite it being preformatted (?!)
        assertTrue(enWiki.getLinksOnPage("User:MER-C/monobook.css").isEmpty(), "page with no links");
    }

    @Test
    public void getImagesOnPage() throws Exception
    {
        // https://en.wikipedia.org/wiki/Template:POTD/2018-11-01
        List<String> pages = List.of("Skflsj&dkfs", "Template:POTD/2018-11-01", "User:MER-C/monobook.js");
        List<List<String>> images = enWiki.getImagesOnPage(pages);
        assertTrue(images.get(0).isEmpty(), "non-existent page");
        assertEquals(List.of("File:Adelie Penguins on iceberg.jpg", "File:Eagle 01.svg"), images.get(1));
        assertTrue(images.get(2).isEmpty(), "page with no images");
    }

    @Test
    public void getTemplates() throws Exception
    {
        List<String> pages = List.of(
            "sdkf&hsdklj", // non-existent
            // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/templates_test
            "User:MER-C/UnitTests/templates_test",
            // https://test.wikipedia.org/wiki/User:MER-C/monobook.js (no templates)
            "User:MER-C/monobook.js",
            "user:MER-C/UnitTests/templates test"); // same as [1]
        List<List<String>> results = testWiki.getTemplates(pages);
        assertTrue(results.get(0).isEmpty(), "non-existent page");
        assertEquals(1, results.get(1).size());
        assertEquals("Template:La", results.get(1).get(0));
        assertTrue(results.get(2).isEmpty(), "page with no templates");
        assertEquals(results.get(1), results.get(3), "duplicate");

        pages = List.of(pages.get(1));
        assertTrue(testWiki.getTemplates(pages, Wiki.MAIN_NAMESPACE).get(0).isEmpty(), "namespace filter");
        assertEquals(List.of("Template:La"), testWiki.getTemplates(pages, Wiki.TEMPLATE_NAMESPACE).get(0), "namespace filter");
    }

    @Test
    public void getCategories() throws Exception
    {
        List<String> pages = List.of("sdkf&hsdklj", "User:MER-C/monobook.js",
            "Category:Wikipedia articles with undisclosed paid content");
        List<String> expected2 = List.of("Category:Hidden categories",            
            "Category:Wikipedia articles with paid content",
            "Category:Wikipedia maintenance categories sorted by month");
        List<List<String>> actual = enWiki.getCategories(pages, null, false);
        assertTrue(actual.get(0).isEmpty(), "non-existent page");
        assertTrue(actual.get(1).isEmpty(), "page with no categories");
        assertEquals(expected2, actual.get(2), "page with three categories");

        Wiki.RequestHelper rh = enWiki.new RequestHelper()
            .filterBy(Map.of("hidden", Boolean.FALSE));
        actual = enWiki.getCategories(List.of("Category:Wikipedia articles with undisclosed paid content"), rh, false);
        assertTrue(actual.get(0).isEmpty(), "filter hidden categories");
    }

    @Test
    public void getImageHistory() throws Exception
    {
        assertTrue(enWiki.getImageHistory("File:Sdfjgh&sld.jpg").isEmpty(), "non-existent file");
        assertTrue(enWiki.getImageHistory("File:WikipediaSignpostIcon.svg").isEmpty(), "commons image");
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
        assertEquals("c0538b43b2a84b0b0caee667b17aa8d311300efd56252d972b6ce20bde6dd758",
            "%064x".formatted(new BigInteger(1, hash)));
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
            () -> enWiki.delete("Special:SpecialPages", "Not a reason", false));
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.delete("Media:Example.png", "Not a reason", false));
        // Test runs without logging in, therefore expect failure.
        assertThrows(SecurityException.class, () -> enWiki.delete("User:MER-C", "Not a reason", false),
            "attempted to delete while logged out");
    }

    @Test
    public void undelete() throws Exception
    {
        // No special namespaces
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.undelete("Special:SpecialPages", "Not a reason", false),
            "Attempted to undelete a special page.");
        assertThrows(UnsupportedOperationException.class,
            () -> enWiki.undelete("Media:Example.png", "Not a reason", false),
            "Attempted to undelete a special page.");
        // Test runs without logging in, therefore expect failure.
        assertThrows(SecurityException.class, () -> enWiki.undelete("User:MER-C", "Not a reason", false),
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
        List<Wiki.Event> events = List.of(revision, logs.get(0));
        assertThrows(IllegalArgumentException.class,
            () -> testWiki.revisionDelete(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, "Not a reason", Boolean.FALSE, events),
            "can't mix revisions and log entries in RevisionDelete");
        // Test runs without logging in, therefore expect failure.
        assertThrows(SecurityException.class,
            () -> testWiki.revisionDelete(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, "Not a reason", Boolean.FALSE, List.of(revision)),
            "attempted to RevisionDelete while logged out");
    }

    @Test
    public void getBlockList() throws Exception
    {
        // Must specify users in unit tests because otherwise it is a dynamic list.
        List<String> users = List.of("Nimimaan", "Jimbo Wales", "Bodiadub");
        List<Wiki.LogEntry> le = enWiki.getBlockList(users, null);
        assertEquals(2, le.size());
        
        assertEquals("2019-05-22T03:28:04Z", le.get(0).getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertEquals("TonyBallioni", le.get(0).getUser());
        assertEquals("User:Bodiadub", le.get(0).getTitle());
        assertEquals("{{checkuserblock-account}}", le.get(0).getComment());
        
        // https://en.wikipedia.org/wiki/Special:Blocklist/Nimimaan
        // see also getLogEntries() below
        assertEquals(-1, le.get(1).getID());
        assertEquals("2016-06-21T13:14:54Z", le.get(1).getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertEquals("MER-C", le.get(1).getUser());
        assertEquals(Wiki.BLOCK_LOG, le.get(1).getType());
        assertEquals("block", le.get(1).getAction());
        assertEquals("User:Nimimaan", le.get(1).getTitle());
        assertEquals("spambot", le.get(1).getComment());
//        assertEquals(new Object[] {
//            false, true, // hard block (not anon only), account creation disabled,
//            false, true, // autoblock enabled, email disabled
//            true, "indefinite" // talk page access revoked, expiry
//        }, le[0].getDetails(), "block parameters");

        // What happens if there are no blocked users in the list? 
        le = enWiki.getBlockList(List.of("0.0.0.0"), null); // Reserved IPs should never be blocked. 
        assertTrue(le.isEmpty());
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
        Map<String, String> details = le.get(0).getDetails();
        assertTrue(details.containsKey("nocreate"));
        assertTrue(details.containsKey("noemail"));
        assertTrue(details.containsKey("nousertalk"));
        assertEquals("infinity", details.get("expiry"));

        // New user log
        assertEquals("Nimimaan", le.get(1).getUser());
        assertEquals(Wiki.USER_CREATION_LOG, le.get(1).getType());
        assertEquals("create", le.get(1).getAction());
        assertEquals("", le.get(1).getComment());
        assertEquals("", le.get(1).getParsedComment());
        assertTrue(le.get(1).getDetails().isEmpty());

        // https://en.wikipedia.org/w/api.php?action=query&list=logevents&letitle=Talk:96th%20Test%20Wing/Temp

        // Move log
        rh = enWiki.new RequestHelper()
            .byTitle("Talk:96th Test Wing/Temp")
            .withinDateRange(null, c);
        le = enWiki.getLogEntries(Wiki.ALL_LOGS, null, rh);
        assertEquals(Wiki.MOVE_LOG, le.get(0).getType());
        assertEquals("move", le.get(0).getAction());
        assertEquals("96th Test Wing", le.get(0).getDetails().get("target_title"));
        // TODO: test for redirect suppression - on hold pending https://phabricator.wikimedia.org/T152346
        
        // protection log
        rh = enWiki.new RequestHelper()
            .byTitle("Wikipedia:Contact us - Licensing");
        le = enWiki.getLogEntries(Wiki.PROTECTION_LOG, null, rh);
        assertEquals("protect", le.get(0).getAction());
        assertEquals("‎[edit=sysop] (indefinite) ‎[move=sysop] (indefinite)", le.get(0).getDetails().get("protection string"));
        
        // user rights log
        rh = enWiki.new RequestHelper().byUser("MER-C").byTitle("User:Siddiqsazzad001");
        le = enWiki.getLogEntries(Wiki.USER_RIGHTS_LOG, null, rh);
        assertEquals(Wiki.USER_RIGHTS_LOG, le.get(0).getType());
        assertEquals("rights", le.get(0).getAction());
        details = le.get(0).getDetails();
        assertEquals("extendedconfirmed,patroller,reviewer,rollbacker", details.get("oldgroups"));
        assertEquals("extendedconfirmed", details.get("newgroups"));

        // RevisionDeleted log entries, no access
        // https://test.wikipedia.org/w/api.php?action=query&list=logevents&letitle=User%3AMER-C%2FTest
        rh = testWiki.new RequestHelper().byTitle("User:MER-C/Test");
        le = testWiki.getLogEntries(Wiki.ALL_LOGS, null, rh);
        assertEquals(Wiki.Event.COMMENT_DELETED, le.get(0).getComment(), "reason hidden");
        assertEquals(Wiki.Event.COMMENT_DELETED, le.get(0).getParsedComment(), "reason hidden");
        assertTrue(le.get(0).isCommentDeleted(), "reason hidden");
        assertEquals(Wiki.Event.USER_DELETED, le.get(0).getUser(), "user hidden");
        assertTrue(le.get(0).isUserDeleted(), "user hidden");

        // tags (not related to the LogEntry being RevisionDeleted)
        assertEquals(List.of("HotCat", "MyStupidTestTag"), le.get(0).getTags());
        
        // back to RevisionDeleted log entries, no access
        // https://test.wikipedia.org/w/api.php?action=query&list=logevents&leuser=MER-C
        //     &lestart=20161002050030&leend=20161002050000&letype=delete
        rh = testWiki.new RequestHelper()
            .byUser("MER-C")
            .withinDateRange(OffsetDateTime.parse("2016-10-02T05:00:00Z"), OffsetDateTime.parse("2016-10-02T05:30:00Z"));
        le = testWiki.getLogEntries(Wiki.DELETION_LOG, null, rh);
        assertEquals(Wiki.Event.CONTENT_DELETED, le.get(1).getTitle(), "target hidden");
        assertTrue(le.get(1).isContentDeleted(), "target hidden");
    }

    @Test
    public void getPageInfo() throws Exception
    {
        List<String> pages = List.of("Main Page", "IPod", "Main_Page", "Special:Specialpages", "HomePage", "1&nbsp;000", "[invalid]");
        List<Map<String, Object>> pageinfo = enWiki.getPageInfo(pages);
        assertEquals(pages.size(), pageinfo.size());

        // Main Page
        Map<String, Object> protection = (Map<String, Object>)pageinfo.get(0).get("protection");
        assertEquals(Wiki.FULL_PROTECTION, protection.get("edit"), "Main Page edit protection level");
        assertNull(protection.get("editexpiry"), "Main Page edit protection expiry");
        assertEquals(Wiki.FULL_PROTECTION, protection.get("move"), "Main Page move protection level");
        assertNull(protection.get("moveexpiry"), "Main Page move protection expiry");
        assertTrue((Boolean)protection.get("cascade"), "Main Page cascade protection");
        assertEquals("Main Page", pageinfo.get(0).get("displaytitle"), "Main Page display title");
        assertEquals(pages.get(0), pageinfo.get(0).get("inputpagename"), "inputpagename");
        assertEquals(pages.get(0), pageinfo.get(0).get("pagename"), "normalized identity");

        // different display title
        assertEquals("iPod", pageinfo.get(1).get("displaytitle"), "iPod display title");

        // Main_Page (duplicate, should be identical except for inputpagename)
        assertEquals(pages.get(2), pageinfo.get(2).get("inputpagename"), "identity");
        pageinfo.get(0).remove("inputpagename");
        pageinfo.get(2).remove("inputpagename");
        assertEquals(pageinfo.get(0), pageinfo.get(2), "duplicate");
        
        // Special page = return null
        assertNull(pageinfo.get(3), "special page");
        
        // redirect
        assertTrue((Boolean)pageinfo.get(4).get("redirect"));
        
        // HTML entities in title (special normalization case)
        assertEquals("1 000", pageinfo.get(5).get("pagename"), "normalized HTML entities");

        // invalid title = return null
        assertNull(pageinfo.get(6));
        
        List<String> userpages = List.of("User:Beispielnutzer", "User:Sicherlich");
        List<Map<String, Object>> userpageinfo = deWiki.getPageInfo(userpages);
        
        // namespace prefix normalization on gender-aware wiki
        assertEquals(userpageinfo.get(0).get("pagename"), "Benutzer:Beispielnutzer"); // male/default prefix
        assertEquals(userpageinfo.get(1).get("pagename"), "Benutzerin:Sicherlich"); // female prefix alias
    }

    @Test
    public void getPageProperties() throws Exception
    {
        List<String> pages = new ArrayList<>();
        pages.add(null);
        assertNull(enWiki.getPageProperties(pages).get(0));
        
        pages.addAll(List.of("Main Page", "IPod", "Main_Page", "Special:Specialpages", "Portal talk:Mesozoic", "1&nbsp;000", "[invalid]"));
        List<Map<String, String>> pageprops = enWiki.getPageProperties(pages);
        assertEquals(pages.size(), pageprops.size());

        // null in = null out
        assertNull(pageprops.get(0));
        
        // Main Page
        assertEquals("Q5296", pageprops.get(1).get("wikibase_item"), "Main Page wikibase item");
        assertTrue(pageprops.get(1).containsKey("notoc"), "Main Page has no toc");
        assertTrue(pageprops.get(1).containsKey("noeditsection"), "Main Page has no edit section");
        
        // IPod
        assertEquals("iPod", pageprops.get(2).get("displaytitle"), "iPod display title");
        assertEquals("Q9479", pageprops.get(2).get("wikibase_item"), "iPod wikibase item");
        assertTrue(pageprops.get(2).containsKey("wikibase-shortdesc"), "iPod has wikibase description");
        
        // Main_Page (duplicate, should be identical)
        assertEquals(pageprops.get(1).hashCode(), pageprops.get(3).hashCode(), "identity");
        
        // Special page = return null
        assertNull(pageprops.get(4), "special page");
        
        // page with no properties
        assertTrue(pageprops.get(5).isEmpty(), "page with no properties");
        
        // HTML entities in title (special normalization case, we don't expect any props here either)
        assertTrue(pageprops.get(6).isEmpty(), "normalization");

        // invalid title = return null
        assertNull(pageprops.get(7), "invalid title");
    }

    @Test
    public void getCategoryMemberCounts() throws Exception
    {
        // highly volatile content, so not amenable to unit testing
        // but can check clear zero categories and title rewrites
        List<int[]> results = testWiki.getCategoryMemberCounts(List.of("Category:Testssss",
            "Wikipedia noticeboards", "Category:Wikipedia noticeboards", "Wikipedia&nbsp;noticeboards"));
        assertArrayEquals(new int[] {0, 0, 0, 0}, results.get(0), "non-existent category");
        assertArrayEquals(results.get(2), results.get(1), "check title rewrite");
        assertArrayEquals(results.get(3), results.get(1), "check title normalization");
    }

    @Test
    public void getFileMetadata() throws Exception
    {
        List<String> files = new ArrayList<>();
        files.add(null);
        assertNull(enWiki.getFileMetadata(files).get(0));
        
        files.addAll(List.of("File:Tankman_new_longshot_StuartFranklin.jpg", 
            "File:Lweo&pafd.blah", "File:Phra Phuttha Chinnarat (II).jpg", 
            "File:WikipediaSignpostIcon.svg", "File:Mandelbrotzoom 20191023.webm",
            "File:Mandelbrotzoom&nbsp;20191023.webm"));
        List<Map<String, Object>> results = enWiki.getFileMetadata(files);
        assertNull(results.get(0));
        
        // https://en.wikipedia.org/wiki/File:Tankman_new_longshot_StuartFranklin.jpg
        Map<String, Object> tankman = results.get(1);
        assertEquals("image/jpeg", tankman.get("mime"));
        assertEquals(261, tankman.get("width"));
        assertEquals(381, tankman.get("height"));
        assertEquals(24310L, tankman.get("size"));
        
        assertNull(results.get(2), "non-existent file");
        
        // Commons files return results
        // https://en.wikipedia.org/wiki/File:Phra_Phuttha_Chinnarat_(II).jpg   
        Map<String, Object> wat = results.get(3);
        assertEquals("image/jpeg", wat.get("mime"));
        assertEquals(5395, wat.get("width"));
        assertEquals(3596, wat.get("height"));
        assertEquals(19125101L, wat.get("size"));
        
        // EXIF or other metadata parsing (subset)
        assertEquals("Canon", wat.get("Make"));
        assertEquals("Canon EOS 5D Mark II", wat.get("Model"));
        assertEquals("70/1", wat.get("FocalLength"));
        assertEquals("1/80", wat.get("ExposureTime"));
        assertEquals("4/1", wat.get("FNumber"));
        assertEquals("250", wat.get("ISOSpeedRatings"));
        
        // slightly exotic file type: SVG
        // https://en.wikipedia.org/wiki/File:WikipediaSignpostIcon.svg
        Map<String, Object> signpost = results.get(4);
        assertEquals("image/svg+xml", signpost.get("mime"));
        assertEquals(46, signpost.get("width"));
        assertEquals(55, signpost.get("height"));
        assertEquals(2809L, signpost.get("size"));
        
        // large file that busts Java integer size
        // https://en.wikipedia.org/wiki/File:Mandelbrotzoom_20191023.webm
        Map<String, Object> fractal = results.get(5);
        assertEquals("video/webm", fractal.get("mime"));
        assertEquals(2703768090L, fractal.get("size"));
        
        // server-side normalization
        assertEquals(fractal, results.get(6));
        
        // further tests blocked on MediaWiki API rewrite
        // see https://phabricator.wikimedia.org/T89971
    }
    
    @Test
    public void listDeletedFiles() throws Exception
    {
        List<Wiki.LogEntry> files = enWiki.listDeletedFiles("fdb4e6b0e934c02e52cd732508247b895ac6a805", null, null);
        // test is unstable, so filter by date range to make it stable
        OffsetDateTime start = OffsetDateTime.parse("2007-08-07T00:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2007-08-08T00:00:00Z");
        for (Wiki.LogEntry file : files)
        {
            OffsetDateTime ts = file.getTimestamp();
            if (ts.isAfter(start) && ts.isBefore(end))
            {
                // [[Special:Undelete/File:Example.png]]
                assertEquals("2007-08-07T08:35:39Z", ts.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                assertEquals("Ilmari Karonen", file.getUser());
                assertNull(file.getComment()); // no privileges => no content
                assertNull(file.getParsedComment());
                assertEquals(Wiki.UPLOAD_LOG, file.getType());
            }
        }
    }

    @Test
    public void getDuplicates() throws Exception
    {
        List<String> files = List.of("File:Sdfj&ghsld.jpg");
        List<List<String>> dupes = enWiki.getDuplicates(files);
        assertTrue(dupes.get(0).isEmpty(), "non-existent file");
    }

    @Test
    public void getInterWikiLinks() throws Exception
    {
        List<String> inputs = List.of("Test", "Gkdfkkl&djfdf", "Perth (disambiguation)", "Test", "Albert Einstein", "1000&nbsp;(number)");        
        var result = enWiki.getInterWikiLinks(inputs);
        
        assertTrue(result.get(1).isEmpty(), "non-existing page");
        assertEquals(result.get(0), result.get(3), "check duplicate removal");
        assertNotNull(result.get(5), "check title normalization");
        // quick functionality verification
        assertEquals(result.get(0).get("ja"), "テスト");
        assertEquals(result.get(2).get("pl"), "Perth (ujednoznacznienie)");
        assertEquals(result.get(4).get("zh"), "阿尔伯特·爱因斯坦");
        assertEquals(result.get(5).get("it"), "1000 (numero)");
    }

    @Test
    public void getExternalLinksOnPages() throws Exception
    {
        List<List<String>> links = enWiki.getExternalLinksOnPage(List.of("Gdkgfskl&dkf", "User:MER-C/monobook.js"));
        assertTrue(links.get(0).isEmpty(), "non-existent page");
        assertTrue(links.get(1).isEmpty(), "page with no links");

        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Linkfinder
        links = testWiki.getExternalLinksOnPage(List.of("User:MER-C/UnitTests/Linkfinder"));
        List<String> expected = List.of("http://spam.example.com/", "https://en.wikipedia.org/", 
            "http://www.example.net/", "https://example.com/protocol_relative");
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
        boolean[] b = enWiki.pageHasTemplate(List.of(
            "Wikipedia:Articles for deletion/Log/2016 September 20",
            "Main Page",
            "dsigusodgusdigusd" // non-existent, should be false
        ), "Wikipedia:Articles for deletion/FMJAM");
        assertTrue(b[0]);
        assertFalse(b[1]);
        assertFalse(b[2], "non-existent page");
    }
    
    @Test
    public void whatLinksHere() throws Exception
    {
        // general test (generally too non-deterministic for functionality testing)
        List<List<String>> results = enWiki.whatLinksHere(List.of("Empty page title 1234"), false, false);
        assertEquals(1, results.size());
        assertTrue(results.get(0).isEmpty());
        
        // check namespace filtering (can test functionality here)
        List<String> titles = List.of("Main Page");
        results = enWiki.whatLinksHere(titles, false, false, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(0).containsAll(List.of("Home page", "Wikipedia", "MainPage")), "namespace filter");
        assertFalse(results.get(0).contains("Portal:Main page"));
        
        // check redirect filtering
        results = enWiki.whatLinksHere(titles, true, false, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(0).containsAll(List.of("MainPage")), "redirect filter");
        assertFalse(results.get(0).removeAll(List.of("Portal:Main page", "Home page", "Wikipedia")));
        
        // check adding redirects to results
        results = testWiki.whatLinksHere(List.of("Main Page"), false, false, Wiki.HELP_NAMESPACE);
        assertFalse(results.get(0).contains("Help:Wiki markup"));
        results = testWiki.whatLinksHere(List.of("Main Page"), false, true, Wiki.HELP_NAMESPACE);
        assertTrue(results.get(0).contains("Help:Wiki markup"));
    }

    @Test
    public void whatTranscludesHere() throws Exception
    {
        List<String> titles = List.of("Wikipedia:Articles for deletion/MegaMeeting.com", "Empty page title 1234",
            "Wikipedia:Articles for deletion/PolymerUpdate", "Wikipedia:Articles for deletion/Mobtown Studios");
        List<List<String>> results = enWiki.whatTranscludesHere(titles);
        assertEquals(4, results.size());
        assertEquals(List.of("Wikipedia:Articles for deletion/Log/2018 April 23"), results.get(0));
        assertTrue(results.get(1).isEmpty());
        assertEquals(List.of("Wikipedia:Articles for deletion/Log/2018 April 22"), results.get(2));
        assertEquals(List.of("Wikipedia:Articles for deletion/Log/2018 April 24"), results.get(3));
        titles = List.of("Wikipedia:Articles for deletion/MegaMeeting.com");
        assertTrue(enWiki.whatTranscludesHere(titles, Wiki.MAIN_NAMESPACE).get(0).isEmpty(), "namespace filter");
    }

    @Test
    public void getUploads() throws Exception
    {
        List<String> users = List.of(
            "Stanton00T", // blocked spambot
            "Charlesjsharp", // https://commons.wikimedia.org/wiki/Special:ListFiles/Charlesjsharp
            "127.0.0.1"); // no uploads for IPs on WMF sites
        assertTrue(commons.getUploads(users.get(0), null).isEmpty(), "no uploads");
        assertTrue(commons.getUploads(users.get(2), null).isEmpty(), "no uploads for IPs");
        
        OffsetDateTime odt = OffsetDateTime.parse("2020-03-13T17:00:00Z");
        Wiki.RequestHelper rh = commons.new RequestHelper().withinDateRange(odt, odt.plusMinutes(10));
        List<Wiki.LogEntry> results = commons.getUploads(users.get(1), rh);
        assertEquals(3, results.size());
        assertEquals("File:Vervain hummingbird (Mellisuga minima) feeding.jpg", results.get(0).getTitle());
        assertEquals("File:Red-billed streamertail (Trochilus polytmus) feeding.jpg", results.get(1).getTitle());
        assertEquals("File:Red-billed streamertail( Trochilus polytmus) adult male 2.jpg", results.get(2).getTitle());
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
        assertTrue(rev.getTags().isEmpty());
        
        // tags
        rev = enWiki.getRevision(925243214L);
        assertEquals(List.of("huggle", "mw-rollback"), rev.getTags());

        // revdel, logged out
        // https://en.wikipedia.org/w/index.php?title=Imran_Khan_%28singer%29&oldid=596714684
        rev = enWiki.getRevision(596714684L);
        assertTrue(rev.isUserDeleted(), "user revdeled flag");
        assertTrue(rev.isCommentDeleted(), "summary revdeled flag");
        assertTrue(rev.isContentDeleted(), "content revdeled flag");
        assertEquals(Wiki.Event.COMMENT_DELETED, rev.getComment(), "summary revdeled");
        assertEquals(Wiki.Event.COMMENT_DELETED, rev.getParsedComment(), "summary revdeled");
        assertEquals(Wiki.Event.USER_DELETED, rev.getUser(), "user revdeled");
        assertEquals(Wiki.Event.CONTENT_DELETED, rev.getSha1(), "sha1/content revdeled");
        
        // Revision has been deleted (not RevisionDeleted)
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        assertNull(testWiki.getRevision(217078L), "page deleted");

        assertNull(testWiki.getRevision(1L << 62), "large revid");
    }

    @Test
    public void diff() throws Exception
    {
        // must specify from/to content
        try
        {
            enWiki.diff(Map.of("xxx", "yyy"), Map.of("revid", 738178354L), "unified");
            fail("Failed to specify from content.");
        }
        catch (IllegalArgumentException | NoSuchElementException expected)
        {
        }
        try
        {
            enWiki.diff(Map.of("revid", 738178354L), Map.of("xxx", "yyy"), "unified");
            fail("Failed to specify to content.");
        }
        catch (IllegalArgumentException | NoSuchElementException expected)
        {
        }

        // https://en.wikipedia.org/w/index.php?title=Dayo_Israel&oldid=738178354&diff=prev
        String diff = enWiki.diff(Map.of("revid", 738178354L), Map.of("revid", Wiki.PREVIOUS_REVISION), "unified")
            .replaceAll("<!--.*-->", "").trim();
        assertEquals("", diff, "dummy edit");
        // https://en.wikipedia.org/w/index.php?title=Source_Filmmaker&diff=804972897&oldid=803731343
        // The MediaWiki API does not distinguish between a dummy edit and no
        // difference. Both are now set to the empty string.
        diff = enWiki.diff(Map.of("revid", 803731343L), Map.of("revid", 804972897L), "unified")
            .replaceAll("<!--.*-->", "").trim();
        assertEquals("", diff, "no difference");
        // no deleted pages allowed
        assertNull(enWiki.diff(Map.of("title", "Create a page"), Map.of("revid", 804972897L), "unified"), "from deleted");
        assertNull(enWiki.diff(Map.of("revid", 804972897L), Map.of("title", "Create a page"), "unified"), "to deleted");
        // no RevisionDeleted revisions allowed (also broken)
        // https://en.wikipedia.org/w/index.php?title=Imran_Khan_%28singer%29&oldid=596714684
        assertNull(enWiki.diff(Map.of("revid", 596714684L), Map.of("revid", Wiki.NEXT_REVISION), "unified"), "from deleted revision");

        // bad revids
        assertNull(enWiki.diff(Map.of("revid", 1L << 62), Map.of("revid", 803731343L), "unified"), "bad from revid");
        assertNull(enWiki.diff(Map.of("revid", 803731343L), Map.of("revid", 1L << 62), "unified"), "bad to revid");
        
        // new article
        diff = enWiki.diff(Map.of("revid", 154400451L), Map.of("revid", Wiki.PREVIOUS_REVISION), "unified");
        assertTrue(diff.contains("'''Urmitz''' is a municipality in the [[Mayen-Koblenz|district of Mayen-Koblenz]] "
            + "in [[Rhineland-Palatinate]], western [[Germany]]"));
    }

    @Test
    public void contribs() throws Exception
    {
        List<String> users = List.of(
            "Frank234234",
            "Dsdlgfkjsdlkfdjilgsujilvjcl", // should not exist
            "0.0.0.0", // IP address
            "Allancake" // revision deleted
        );
        List<List<Wiki.Revision>> edits = enWiki.contribs(users, null, null);

        // functionality test
        // https://en.wikipedia.org/wiki/Special:Contributions/Frank234234
        List<Wiki.Revision> contribs = edits.get(0);
        assertEquals(921259981L, contribs.get(0).getID());
        assertEquals(918474023L, contribs.get(1).getID());
        assertEquals(List.of("visualeditor"), contribs.get(0).getTags());
        
        // edge cases
        assertTrue(edits.get(1).isEmpty(), "non-existent user");
        assertTrue(edits.get(2).isEmpty(), "IP address with no edits");
        for (Wiki.Revision rev : edits.get(3))
        {
            if (rev.getID() == 724989913L)
            {
                assertTrue(rev.isContentDeleted(), "summary deleted");
                assertTrue(rev.isContentDeleted(), "content deleted");
            }
        }

        // check rcoptions and namespace filter
        // https://test.wikipedia.org/wiki/Blah_blah_2
        Wiki.RequestHelper rh = testWiki.new RequestHelper()
            .inNamespaces(Wiki.MAIN_NAMESPACE)
            .filterBy(Map.of("new", Boolean.TRUE, "top", Boolean.TRUE))
            .withinDateRange(null, OffsetDateTime.parse("2020-01-01T00:00:00Z"));
        edits = testWiki.contribs(List.of("MER-C"), null, rh);
        assertEquals(120919L, edits.get(0).get(0).getID(), "filtered");
        // not implemented in MediaWiki API
        // assertEquals("bcdb66a63846bacdf39f5c52a7d2cc5293dbde3e", edits.get(0).get(0).getSha1());
        for (Wiki.Revision rev : edits.get(0))
            assertEquals(Wiki.MAIN_NAMESPACE, testWiki.namespace(rev.getTitle()), "namespace");
    }

    @Test
    public void getUsers() throws Exception
    {
        // empty/null check
        assertTrue(enWiki.getUsers(Collections.emptyList()).isEmpty());
        List<String> usernames = new ArrayList<>();
        usernames.add(null);
        assertNull(enWiki.getUsers(usernames).get(0));
        
        usernames.add("127.0.0.1"); // IP address
        usernames.add("MER-C");
        usernames.add("DKdsf;lksd"); // should be non-existent...
        usernames.add("ZZRBrenda08"); // blocked spambot with 2 edits
        usernames.add("127.0.0.0/24"); // IP range
        List<Wiki.User> users = enWiki.getUsers(usernames);
        assertNull(users.get(0), "null input");
        assertNull(users.get(1), "IP address");
        assertNull(users.get(3), "non-existent user");
        assertNull(users.get(5), "IP address range");

        assertEquals(usernames.get(2), users.get(2).getUsername(), "normalized username");
        assertNull(users.get(2).getBlockDetails());
        assertEquals(Wiki.Gender.unknown, users.get(2).getGender());
        assertEquals("2006-07-07T10:52:41Z",
            users.get(2).getRegistrationDate().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        // should be privileged information, but isn't?
        // assertFalse(users[2].canBeEmailed());

        List<String> groups = users.get(2).getGroups();
        List<String> temp = List.of("*", "autoconfirmed", "user", "sysop");
        assertTrue(groups.containsAll(temp));

        // check (subset of) rights
        List<String> rights = users.get(2).getRights();
        temp = List.of("apihighlimits", "delete", "block", "editinterface");
        assertTrue(rights.containsAll(temp));

        assertEquals(usernames.get(4), users.get(4).getUsername());
        assertEquals(2, users.get(4).countEdits());
        Wiki.LogEntry entry = users.get(4).getBlockDetails();
        assertNotNull(entry);
        assertEquals("MER-C", entry.getUser());
        assertEquals("2018-04-18T18:46:05Z", entry.getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertEquals("spammer", entry.getComment());
        Map<String, String> details = entry.getDetails();
        assertEquals("infinite", details.get("expiry"));
        assertFalse(details.containsKey("noautoblock"));
        assertTrue(details.containsKey("nocreate"));
        // https://phabricator.wikimedia.org/T329426
        // assertTrue(details.containsKey("noemail"));
        // assertTrue(details.containsKey("notalk"));
    }

    @Test
    public void getPageText() throws Exception
    {
        assertThrows(UnsupportedOperationException.class,
            () -> testWiki.getPageText(List.of("Special:Specialpages")),
            "Tried to get page text for a special page!");
        assertThrows(UnsupportedOperationException.class,
            () -> testWiki.getPageText(List.of("Media:Example.png")),
            "Tried to get page text for a media page!");

        List<String> text = testWiki.getPageText(List.of(
            "User:MER-C/UnitTests/Delete", // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
            "User:MER-C/UnitTests/pagetext", // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/pagetext
            "Gsgdksgjdsg", // https://test.wikipedia.org/wiki/Gsgdksgjdsg (does not exist)
            "User:NikiWiki/EmptyPage" // https://test.wikipedia.org/wiki/User:NikiWiki/EmptyPage (empty)
        ));
        // API result does not include the terminating new line
        assertEquals("This revision is not deleted!", text.get(0));
        assertEquals("&#039;&#039;italic&#039;&#039;\n'''&amp;'''\n&&\n&lt;&gt;\n<>\n&quot;",
            text.get(1), "decoding");
        assertNull(text.get(2), "non-existent page");
        assertEquals("", text.get(3), "empty page");
        
        List<String> text2 = deWiki.getText(List.of("User:Beispielnutzer", "User:Sicherlich"), null, -1);
        assertNotNull(text2.get(0), "resolve default namespace prefix");
        assertNotNull(text2.get(1), "resolve namespace prefix alias");
    }

    @Test
    public void getText() throws Exception
    {
        assertTrue(testWiki.getText(Collections.emptyList(), null, -1).isEmpty());
        assertTrue(testWiki.getText(null, new long[0], -1).isEmpty());

        long[] ids =
        {
            230472L, // https://test.wikipedia.org/w/index.php?oldid=230472 (decoding test)
            322889L, // https://test.wikipedia.org/w/index.php?oldid=322889 (empty revision)
            275553L, // https://test.wikipedia.org/w/index.php?oldid=275553 (RevisionDeleted)
            316531L, // https://test.wikipedia.org/w/index.php?oldid=316531 (first revision on same page)
            316533L  // https://test.wikipedia.org/w/index.php?oldid=316533 (second revision on same page)
        };
        List<String> text = testWiki.getText(null, ids, -1);
        assertEquals("&#039;&#039;italic&#039;&#039;\n'''&amp;'''\n&&\n&lt;&gt;\n<>\n&quot;",
            text.get(0), "decoding");
        assertEquals("", text.get(1), "empty revision");
        assertEquals(Wiki.Event.CONTENT_DELETED, text.get(2), "Content RevisionDeleted");
        assertEquals("Testing 0.2786153173518522", text.get(3), "same page");
        assertEquals("Testing 0.28713760508426645", text.get(4), "same page");
    }

    @Test
    public void parse() throws Exception
    {
        // currently broken: error is passed silently through
        // assertNull(enWiki.parse(Map.of("title", "Hello"), 1000000, true), "no such section");
        assertNull(enWiki.parse(Map.of("title", "Create a page"), -1, true), "deleted page");
        assertNull(enWiki.parse(Map.of("revid", 1L << 62), -1, true), "bad revid");
        // https://en.wikipedia.org/w/index.php?oldid=596714684
        assertThrows(SecurityException.class, () -> enWiki.parse(Map.of("revid", 596714684L), -1, true),
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
        List<Map<String, Object>> results = testWiki.search("dlgsjdglsjdgljsgljsdlg", Wiki.MEDIAWIKI_NAMESPACE);
        assertTrue(results.isEmpty(), "no results");
        // https://test.wikipedia.org/w/api.php?action=query&list=search&srsearch=User:%20subpageof:MER-C/UnitTests%20revision%20delete
        results = testWiki.search("User: subpageof:MER-C/UnitTests revision delete", Wiki.USER_NAMESPACE);
        assertEquals(2, results.size(), "result count");
        Map<String, Object> result = results.get(0);
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
        rh = rh.filterBy(Map.of("minor", Boolean.FALSE, "bot", Boolean.TRUE));
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
        assertTrue(testWiki.allUsersInGroup("sdfkd|&").isEmpty(), "invalid input");
        assertEquals(List.of("Jimbo Wales"), enWiki.allUsersInGroup("founder"));
    }

    @Test
    public void allUserswithRight() throws Exception
    {
        assertTrue(testWiki.allUsersWithRight("sdfkd|&").isEmpty(), "invalid input");
    }

    @Test
    public void constructNamespaceString() throws Exception
    {
        assertEquals("1|2|3", enWiki.constructNamespaceString(new int[] { 3, 2, 1, 2, -10 }));
    }

    @Test
    public void constructTitleString() throws Exception
    {
        List<String> titles = new ArrayList<>();
        for (int i = 0; i < 101; i++)
            titles.add("A" + i);
        // duplicates should be removed
        for (int i = 0; i < 101; i++)
            titles.add("A" + i);
        List<String> expected = new ArrayList<>();
        // slowmax == 50 for Wikimedia wikis if not logged in
        expected.add("""
            A0|A1|A10|A100|A11|A12|A13|A14|A15|A16|A17|A18|A19|A2|\
            A20|A21|A22|A23|A24|A25|A26|A27|A28|A29|A3|A30|A31|A32|A33|A34|\
            A35|A36|A37|A38|A39|A4|A40|A41|A42|A43|A44|A45|A46|A47|A48|A49|\
            A5|A50|A51|A52""");
        expected.add("""
            A53|A54|A55|A56|A57|A58|A59|A6|A60|A61|A62|A63|A64|A65|\
            A66|A67|A68|A69|A7|A70|A71|A72|A73|A74|A75|A76|A77|A78|A79|A8|\
            A80|A81|A82|A83|A84|A85|A86|A87|A88|A89|A9|A90|A91|A92|A93|A94|\
            A95|A96|A97|A98""");
        expected.add("A99");
        List<String> actual = enWiki.constructTitleString(titles);
        assertEquals(expected, actual);
        
        // Determine whether the correct number of items is returned.
        titles.clear();
        for (int i = 0; i < 50; i++)
            titles.add("A" + i);
        actual = enWiki.constructTitleString(titles);
        assertEquals(1, actual.size());
        
        // should behave well with nulls if one gets fed in from a revdel somewhere
        titles.clear();
        titles.add(null);
        assertTrue(enWiki.constructTitleString(titles).isEmpty());
        titles.add("A");
        assertEquals(List.of("A"), enWiki.constructTitleString(titles));
    }
    
    // INNER CLASS TESTS

    @Test
    public void revisionCompareTo() throws Exception
    {
        // https://en.wikipedia.org/w/index.php?title=Azerbaijan&offset=20180125204500&action=history
        long[] oldids = { 822342083L, 822341440L };
        List<Wiki.Revision> revisions = enWiki.getRevisions(oldids);
        assertEquals(0, revisions.get(0).compareTo(revisions.get(0)), "self");
        assertTrue(revisions.get(1).compareTo(revisions.get(0)) < 0, "before");
        assertTrue(revisions.get(0).compareTo(revisions.get(1)) > 0, "after");
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
        Wiki.User user = enWiki.getUsers(List.of("LornaIln046035")).get(0); // spambot
        assertTrue(user.isAllowedTo("read"));
        assertFalse(user.isAllowedTo("checkuser"));
        assertFalse(user.isAllowedTo("sdlkghsdlkgsd"));
    }

    @Test
    public void userIsA() throws Exception
    {
        Wiki.User me = testWiki.getUsers(List.of("MER-C")).get(0);
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
