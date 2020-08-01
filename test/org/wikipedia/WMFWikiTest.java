/**
 *  @(#)WMFWikiUnitTest.java 0.01 12/11/2017
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

package org.wikipedia;

import java.util.*;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit tests for {@link org.wikipedia.WMFWiki}.
 *  @author MER-C
 */
public class WMFWikiTest
{
    private final WMFWiki enWiki;
    
    /**
     *  Construct wiki objects for each test so that tests are independent.
     */
    public WMFWikiTest()
    {
        enWiki = WMFWiki.newSession("en.wikipedia.org");
        enWiki.setMaxLag(-1);
    }
    
    @ParameterizedTest
    @CsvSource({"enwiki, en.wikipedia.org",  "wikidatawiki, www.wikidata.org",
            "zhwikiquote, zh.wikiquote.org", "commonswiki, commons.wikimedia.org",
            "metawiki, meta.wikimedia.org"})
    public void newSessionFromDBName(String dbname, String domain)
    {
        WMFWiki wiki = WMFWiki.newSessionFromDBName(dbname);
        assertEquals(domain, wiki.getDomain());
    }
    
    @Test
    public void isSpamBlacklisted() throws Exception
    {
        assertFalse(enWiki.isSpamBlacklisted("example.com"));
        assertTrue(enWiki.isSpamBlacklisted("youtu.be"));
    }
    
    /**
     *  Attempts to access a privileged log that isn't available in vanilla
     *  MediaWiki.
     *  @throws Exception if a network error occurs
     */
    @Test
    public void getLogEntries() throws Exception
    {
        assertEquals(Collections.emptyList(), enWiki.getLogEntries(WMFWiki.SPAM_BLACKLIST_LOG, null, null));
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
    public void getGlobalUsage() throws Exception
    {
        assertThrows(IllegalArgumentException.class,
            () -> enWiki.getGlobalUsage("Not an image"),
            "tried to get global usage for a non-file page");
        // YARR!
        assertTrue(enWiki.getGlobalUsage("File:Pirated Movie Full HD Stream.mp4").isEmpty(), "unused, non-existing file");
    }
    
    /**
     *  Test fetching the abuse log. This is a semi-privileged action that 
     *  requires the test runner to be not blocked (even though login is not 
     *  required). CI services (Travis, Codeship, etc) run on colocation 
     *  facilities which are blocked as proxies, causing the test to fail.
     *  @throws Exception if a network error occurs
     */
    @Test
    @Disabled
    public void getAbuseFilterLogs() throws Exception
    {
        // https://en.wikipedia.org/wiki/Special:AbuseLog?wpSearchUser=Miniapolis&wpSearchTitle=Catopsbaatar&wpSearchFilter=1
        Wiki.RequestHelper helper = enWiki.new RequestHelper()
            .withinDateRange(OffsetDateTime.parse("2018-04-05T00:00:00Z"), OffsetDateTime.parse("2018-04-06T01:00:00Z"))
            .byUser("Miniapolis")
            .byTitle("Catopsbaatar");
        List<Wiki.LogEntry> afl = enWiki.getAbuseLogEntries(new int[] { 1 }, helper);
        Wiki.LogEntry ale = afl.get(0);
        assertEquals(20838976L, ale.getID());
        assertEquals("Miniapolis", ale.getUser());
        assertEquals("Catopsbaatar", ale.getTitle());
        assertEquals(OffsetDateTime.parse("2018-04-05T22:58:14Z"), ale.getTimestamp());
        assertEquals("edit", ale.getAction());
        Map<String, String> details = ale.getDetails();
        assertEquals("1", details.get("filter_id"));
        assertEquals("834477714", details.get("revid"));
        assertEquals("", details.get("result"));
    }
    
    @Test
    public void getLedeAsPlainText() throws Exception
    {
        List<String> pages = List.of("Java", "Create a page", "Albert Einstein");
        List<String> text = enWiki.getLedeAsPlainText(pages);
        // Cannot assert more than the first two words because the result is
        // non-deterministic. Test is potentially fragile.
        assertTrue(text.get(0).startsWith("Java "));
        assertTrue(text.get(1) == null);
        assertTrue(text.get(2).startsWith("Albert Einstein "));
    }
    
    @Test
    public void getPlainText() throws Exception
    {
        List<String> pages = List.of("Java", "Create a page", "Albert Einstein");
        List<String> shorttext = enWiki.getLedeAsPlainText(pages);
        List<String> text = enWiki.getPlainText(pages);
        assertTrue(text.get(0).startsWith(shorttext.get(0)));
        assertTrue(text.get(1) == null);
        assertTrue(text.get(2).startsWith(shorttext.get(2)));
    }
    
    @Test
    public void getGlobalUserInfo() throws Exception
    {
        // locked account with local block
        // https://meta.wikimedia.org/w/index.php?title=Special:CentralAuth&target=Uruguymma
        Map<String, Object> guserinfo = WMFWiki.getGlobalUserInfo("Uruguymma");
        assertTrue((Boolean)guserinfo.get("locked"));
        assertEquals(38, guserinfo.get("editcount"));
        assertEquals(OffsetDateTime.parse("2016-09-21T13:59:30Z"), guserinfo.get("registration"));
        assertEquals(Collections.emptyList(), guserinfo.get("groups"));
        assertEquals(Collections.emptyList(), guserinfo.get("rights"));
        assertEquals("enwiki", guserinfo.get("home"));
        
        // enwiki
        Map luserinfo = (Map)guserinfo.get("enwiki");
        assertEquals("https://en.wikipedia.org", luserinfo.get("url"));
        assertEquals(23, luserinfo.get("editcount"));
        assertEquals(OffsetDateTime.parse("2016-09-21T13:59:29Z"), luserinfo.get("registration"));
        assertEquals(Collections.emptyList(), luserinfo.get("groups"));
        assertTrue((Boolean)luserinfo.get("blocked"));
        assertNull(luserinfo.get("blockexpiry"));
        assertEquals("Abusing [[WP:Sock puppetry|multiple accounts]]: Please see: "
            + "[[w:en:Wikipedia:Sockpuppet investigations/Japanelemu]]", luserinfo.get("blockreason"));
        
        // meta
        luserinfo = (Map)guserinfo.get("metawiki");
        assertEquals("https://meta.wikimedia.org", luserinfo.get("url"));
        assertEquals(0, luserinfo.get("editcount"));
        assertEquals(OffsetDateTime.parse("2016-09-21T13:59:37Z"), luserinfo.get("registration"));
        assertEquals(Collections.emptyList(), luserinfo.get("groups"));
        assertFalse((Boolean)luserinfo.get("blocked"));
        assertNull(luserinfo.get("blockexpiry"));
        assertNull(luserinfo.get("blockreason"));
        
        // global and local groups set
        // https://meta.wikimedia.org/wiki/Special:CentralAuth?target=Jimbo+Wales
        guserinfo = WMFWiki.getGlobalUserInfo("Jimbo Wales");
        assertEquals(List.of("founder"), guserinfo.get("groups"));
        luserinfo = (Map)guserinfo.get("enwiki");
        assertEquals(List.of("checkuser", "founder", "oversight", "sysop"), luserinfo.get("groups"));
        
        // IP address (throws UnknownError)
        // guserinfo = WMFWiki.getGlobalUserInfo("127.0.0.1");
        // assertNull(guserinfo);
    }
    
    @Test
    public void getWikidataItems() throws Exception
    {
        List<String> input = List.of("Blah", "Albert Einstein", "Create a page", "Test", 
            "Albert_Einstein", "User:MER-C");
        List<String> actual = enWiki.getWikidataItems(input);
        assertEquals("Q527633", actual.get(0));
        assertEquals("Q937", actual.get(1));
        assertNull(actual.get(2)); // local page doesn't exist
        assertEquals("Q224615", actual.get(3));
        assertEquals("Q937", actual.get(4));
        assertNull(actual.get(5)); // local page exists, but no corresponding WD item
    }
}
