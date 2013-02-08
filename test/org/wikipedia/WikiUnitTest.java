package org.wikipedia;

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
    }
    
    @Test
    public void userExists() throws Exception
    {
        assertTrue("I should exist!", enWiki.userExists("MER-C"));
        assertFalse("Anon should not exist", enWiki.userExists("127.0.0.1"));
    }
}
