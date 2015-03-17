package org.wikipedia;

import java.util.*;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *  Unit tests for Wiki.java
 *  @author MER-C
 */

public class AdminUnitTest
{
    private static Wiki testWiki, enWiki;
    
    @BeforeClass
    public static void setUpClass()
    {
        testWiki = new Wiki("test.wikipedia.org");
        org.wikiutils.LoginUtils.guiLogin(testWiki);
        testWiki.setMaxLag(-1);
        enWiki = new Wiki("en.wikipedia.org");
        org.wikiutils.LoginUtils.guiLogin(enWiki);
        enWiki.setMaxLag(-1);
    }
    
    @Test
    public void getRevision() throws Exception
    {
        // revdel
        // https://en.wikipedia.org/w/index.php?title=Imran_Khan_%28singer%29&oldid=596714684
        Wiki.Revision rev = enWiki.getRevision(596714684L);
        assertTrue("getRevision: user revdeled", rev.isUserDeleted());
        assertTrue("getRevision: summary revdeled", rev.isSummaryDeleted());
        assertTrue("getRevision: content revdeled", rev.isContentDeleted());
    }
    
    /**
     *  See https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
     *  @throws Exception if something goes wrong
     */
    @Test
    public void getDeletedText() throws Exception
    {
        String text = testWiki.getDeletedText("User:MER-C/UnitTests/Delete");
        assertEquals("getDeletedText", text, "This revision is also deleted!");
    }
    
    /**
     *  See https://test.wikipedia.org/wiki/User:MER-C/UnitTests/Delete
     *  @throws Exception if something goes wrong
     */
    @Test
    public void RevisionGetText() throws Exception
    {
        Wiki.Revision deleted = testWiki.getRevision(217078L);
        assertEquals(deleted.getText(), "This revision is deleted!");
    }
}
