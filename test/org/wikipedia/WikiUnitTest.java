package org.wikipedia;

import java.util.Arrays;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *  Unit tests for Wiki.java
 *  @author MER-C
 */
public class WikiUnitTest
{
    private static Wiki enWiki, deWiki, arWiki;
    
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
    }
    
    @Test
    public void namespace() throws Exception
    {
        assertEquals("NS: en, category", Wiki.CATEGORY_NAMESPACE, enWiki.namespace("Category:CSD"));
        assertEquals("NS: main ns fail", Wiki.MAIN_NAMESPACE, enWiki.namespace("Star Wars: The Old Republic"));
        assertEquals("NS: main ns fail2", Wiki.MAIN_NAMESPACE, enWiki.namespace("Some Category: Blah"));
        assertEquals("NS: i18n fail", Wiki.CATEGORY_NAMESPACE, deWiki.namespace("Kategorie:Begriffsklärung"));
        // assertEquals("NS: mixed i18n", Wiki.CATEGORY_NAMESPACE, deWiki.namespace("Category:Begriffsklärung"));
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
        assertNull("Non-existent page", enWiki.getFirstRevision("dgfhdfjklg"));
    }
    
    @Test
    public void getLastRevision() throws Exception
    {
        assertNull("Non-existent page", enWiki.getTopRevision("dgfhdfjklg"));
    }
    
    @Test
    public void getTemplates() throws Exception
    {
        assertArrayEquals("getTemplates: non-existent page", new String[0], enWiki.getTemplates("sdkfhsdklj"));
        assertArrayEquals("getTemplates: page with no templates", new String[0], enWiki.getTemplates("User:MER-C/monobook.js"));
    }
    
    @Test
    public void exists() throws Exception
    {
        String[] titles = new String[] { "Main Page", "Tdkfgjsldf", "User:MER-C", "Wikipedia:Skfjdl", "Main Page" };
        boolean[] expected = new boolean[] { true, false, true, false, true };
        assertTrue("exists", Arrays.equals(expected, enWiki.exists(titles)));
    }
    
    @Test
    public void resolveRedirect() throws Exception
    {
        String[] titles = new String[] { "Main page", "Main Page", "sdkghsdklg", "Hello.jpg", "Main page" };
        String[] expected = new String[] { "Main Page", null, null, "Goatse.cx", "Main Page" };
        assertArrayEquals("resolveRedirects", expected, enWiki.resolveRedirect(titles)); 
    }
    
    @Test
    public void getLinksOnPage() throws Exception
    {
        assertArrayEquals("getLinksOnPage: non-existent page", new String[0], enWiki.getLinksOnPage("Skflsjdkfs"));
        assertArrayEquals("getLinksOnPage: page with no links", new String[0], enWiki.getLinksOnPage("User:MER-C/monobook.js"));
    }
    
    @Test
    public void getImagesOnPage() throws Exception
    {
        assertArrayEquals("getImagesOnPage: non-existent page", new String[0], enWiki.getImagesOnPage("Skflsjdkfs"));
        assertArrayEquals("getImagesOnPage: page with no links", new String[0], enWiki.getImagesOnPage("User:MER-C/monobook.js"));
    }
    
    @Test
    public void getImageHistory() throws Exception
    {
        assertArrayEquals("getImageHistory: non-existent page", new Wiki.LogEntry[0], enWiki.getImageHistory("File:Sdfjghsld.jpg"));
    }
}
