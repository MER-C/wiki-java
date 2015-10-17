/**
 *  @(#)WikiUnitTest.java 0.31 29/08/2015
 *  Copyright (C) 2014-2015 MER-C
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
import java.net.URLEncoder;
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
    private static Wiki enWiki, deWiki, arWiki, testWiki;
    
    public WikiUnitTest()
    {
    }
    
    /**
     *  Initialize wiki objects.
     */
    @BeforeClass
    public static void setUpClass()
    {
        enWiki = new Wiki("en.wikipedia.org");
        enWiki.setMaxLag(-1);
        deWiki = new Wiki("de.wikipedia.org");
        deWiki.setMaxLag(-1);
        arWiki = new Wiki("ar.wikipedia.org");
        arWiki.setMaxLag(-1);
        testWiki = new Wiki("test.wikipedia.org");
        testWiki.setMaxLag(-1);
    }
    
    @Test
    public void namespace() throws Exception
    {
        assertEquals("NS: en, category", Wiki.CATEGORY_NAMESPACE, enWiki.namespace("Category:CSD"));
        assertEquals("NS: en, alias", Wiki.PROJECT_NAMESPACE, enWiki.namespace("WP:CSD"));
        assertEquals("NS: main ns fail", Wiki.MAIN_NAMESPACE, enWiki.namespace("Star Wars: The Old Republic"));
        assertEquals("NS: main ns fail2", Wiki.MAIN_NAMESPACE, enWiki.namespace("Some Category: Blah"));
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
    public void userExists() throws Exception
    {
        assertTrue("I should exist!", enWiki.userExists("MER-C"));
        assertFalse("Anon should not exist", enWiki.userExists("127.0.0.1"));
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
        tempfile.deleteOnExit();
        assertFalse("getImage: non-existent file", enWiki.getImage("File:Sdkjf&sdlf.blah", tempfile));
    }
    
    @Test
    public void getPageHistory() throws Exception
    {
        assertArrayEquals("getPageHistory: non-existent page", new Wiki.Revision[0], enWiki.getPageHistory("EOTkd&ssdf"));
        assertArrayEquals("getPageHistory: special page", new Wiki.Revision[0], enWiki.getPageHistory("Special:Specialpages"));
    }
    
    @Test
    public void getPageInfo() throws Exception
    {
        Map<String, Object>[] pageinfo = enWiki.getPageInfo(new String[] { "Main Page", "IPod" });
        
        // Main Page
        Map<String, Object> protection = (Map<String, Object>)pageinfo[0].get("protectionstate");
        assertEquals("getPageInfo: Main Page edit protection level", Wiki.FULL_PROTECTION, protection.get("edit"));
        assertNull("getPageInfo: Main Page edit protection expiry", protection.get("editexpiry"));
        assertEquals("getPageInfo: Main Page move protection level", Wiki.FULL_PROTECTION, protection.get("move"));
        assertNull("getPageInfo: Main Page move protection expiry", protection.get("moveexpiry"));
        assertTrue("getPageInfo: Main Page cascade protection", (Boolean)protection.get("cascade"));
        assertEquals("getPageInfo: Main Page display title", "Main Page", pageinfo[0].get("displaytitle"));
        
        // different display title
        assertEquals("getPageInfo: iPod display title", "iPod", pageinfo[1].get("displaytitle"));
    }
    
    @Test
    public void getFileMetadata() throws Exception
    {
        assertNull("getFileMetadata: non-existent file", enWiki.getFileMetadata("File:Lweo&pafd.blah"));
        assertNull("getFileMetadata: commons image", enWiki.getFileMetadata("File:WikipediaSignpostIcon.svg"));
    }
    
    @Test
    public void getDuplicates() throws Exception
    {
        assertArrayEquals("getDuplicates: non-existent file", new String[0], enWiki.getImageHistory("File:Sdfj&ghsld.jpg"));
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
        catch (Throwable ex)
        {
            ex.printStackTrace();
            fail("getSectionText: should throw IllegalArgumentException");
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
        info = new Wiki("en.wiktionary.org").getSiteInfo();
        assertFalse("siteinfo: caplinks false", (Boolean)info.get("usingcapitallinks"));
    }
    
    @Test
    public void normalize() throws Exception
    {
        assertEquals("normalize", "Blah", enWiki.normalize("Blah"));
        assertEquals("normalize", "Blah", enWiki.normalize("blah"));
        assertEquals("normalize", "File:Blah.jpg", enWiki.normalize("File:Blah.jpg"));
        assertEquals("normalize", "File:Blah.jpg", enWiki.normalize("File:blah.jpg"));
        assertEquals("normalize", "Category:Wikipedia:blah", enWiki.normalize("Category:Wikipedia:blah"));
        assertEquals("normalize", "Hilfe Diskussion:Glossar", deWiki.normalize("Help talk:Glossar"));
    }
    
    @Test
    public void getRevision() throws Exception
    {
        // https://en.wikipedia.org/w/index.php?title=Wikipedia_talk%3AWikiProject_Spam&diff=597454682&oldid=597399794
        Wiki.Revision rev = enWiki.getRevision(597454682L);
        assertEquals("getRevision: page", "Wikipedia talk:WikiProject Spam", rev.getPage());
        assertEquals("getRevision: timestamp", "20140228004031", enWiki.calendarToTimestamp(rev.getTimestamp()));
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
        // NOT IMPLEMENTED:
        // assertTrue("getRevision: content revdeled", rev.isContentDeleted());
    }
    
    @Test
    public void diff() throws Exception
    {
        assertNull("diff: no previous revision", enWiki.getRevision(586849481L).diff(Wiki.PREVIOUS_REVISION));
    }
    
    @Test
    public void contribs() throws Exception
    {
        // should really be null, but the API returns zero
        assertEquals("contribs: non-existent user", testWiki.contribs("Dsdlgfkjsdlkfdjilgsujilvjcl").length, 0);
    }
    
    @Test
    public void getPageText() throws Exception
    {
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
        String text = testWiki.getPageText("User:MER-C/UnitTests/Delete");
        assertEquals("getPageText", text, "This revision is not deleted!\n");
        // https://test.wikipedia.org/wiki/User:MER-C/UnitTests/pagetext
        text = testWiki.getPageText("User:MER-C/UnitTests/pagetext");
        assertEquals("page text: decoding", text, "&#039;&#039;italic&#039;&#039;" +
            "\n'''&amp;'''\n&&\n&lt;&gt;\n<>\n&quot;\n");
    }
    
    @Test
    public void constructNamespaceString() throws Exception
    {
        StringBuilder temp = new StringBuilder();
        enWiki.constructNamespaceString(temp, "blah", 1, 2, 3);
        assertEquals("constructNamespaceString", "&blahnamespace=1%7C2%7C3", temp.toString());
    }
    
    @Test
    public void constructTitleString() throws Exception
    {
        String[] titles = new String[101];
        for (int i = 0; i < titles.length; i++)
            titles[i] = "a" + i;
        String[] expected = new String[]
        {
            // slowmax == 50 for Wikimedia wikis if not logged in
            URLEncoder.encode("A0|A1|A2|A3|A4|A5|A6|A7|A8|A9|A10|A11|A12|A13|A14|" +
                "A15|A16|A17|A18|A19|A20|A21|A22|A23|A24|A25|A26|A27|A28|A29|A30|" +
                "A31|A32|A33|A34|A35|A36|A37|A38|A39|A40|A41|A42|A43|A44|A45|A46|" +
                "A47|A48|A49", "UTF-8"),
            URLEncoder.encode("A50|A51|A52|A53|A54|A55|A56|A57|A58|A59|A60|A61|A62|" +
                "A63|A64|A65|A66|A67|A68|A69|A70|A71|A72|A73|A74|A75|A76|A77|A78|A79|" +
                "A80|A81|A82|A83|A84|A85|A86|A87|A88|A89|A90|A91|A92|A93|A94|A95|A96|" + 
                "A97|A98|A99", "UTF-8"),
            URLEncoder.encode("A100", "UTF-8")
        };
        String[] actual = enWiki.constructTitleString(titles);
        assertArrayEquals("constructTitleString", expected, actual);
    }
    
    // INNER CLASS TESTS
    
    @Test
    public void revisionGetText() throws Exception
    {
        // https://test.wikipedia.org/w/index.php?oldid=230472
        Wiki.Revision rev = testWiki.getRevision(230472);
        String text = rev.getText();
        assertEquals("revision text: decoding", text, "&#039;&#039;italic&#039;&#039;" +
            "\n'''&amp;'''\n&&\n&lt;&gt;\n<>\n&quot;\n");
    }
}
