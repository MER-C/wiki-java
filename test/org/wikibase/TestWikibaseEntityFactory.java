package org.wikibase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wikibase.data.Claim;
import org.wikibase.data.Entity;
import org.wikibase.data.Item;
import org.wikibase.data.Property;

public class TestWikibaseEntityFactory {

    private Entity q3 = null;
    private Entity q2 = null;

    @Before
    public void before() throws IOException, WikibaseException {
        q3 = readQFromXMLFile(3);
        q2 = readQFromXMLFile(2);
    }

    private Entity readQFromXMLFile(int no) throws IOException, WikibaseException {

        InputStream q3stream = ClassLoader.getSystemResourceAsStream("q" + no + ".xml");
        BufferedReader q3reader = new BufferedReader(new InputStreamReader(q3stream));
        StringBuilder q3string = new StringBuilder();
        try {
            String line = null;
            while (null != (line = q3reader.readLine())) {
                q3string.append(line).append(System.lineSeparator());
            }
        } finally {
            if (null != q3reader) {
                q3reader.close();
            }
        }

        return WikibaseEntityFactory.getWikibaseItem(q3string.toString());
    }

    @Test
    public void testQ3RoLabel() {
        Assert.assertNotNull(q3);
        Assert.assertNotNull(q3.getLabels());
        Assert.assertEquals(q3.getLabels().get("ro"), "viață");
    }

    @Test
    public void testQ2EnSitelink() {
        Assert.assertNotNull(q2);
        Assert.assertNotNull(q2.getSitelinks());
        Assert.assertEquals(q2.getSitelinks().get("enwiki").getPageName(), "Earth");
    }

    @Test
    public void testQ2HighestPointItem() {
        Assert.assertNotNull(q2);
        Assert.assertNotNull(q2.getClaims());
        Set<Claim> p610claims = q2.getClaims().get(new Property("P610"));
        Assert.assertEquals(1, p610claims.size());
        Claim p610claim = new ArrayList<Claim>(p610claims).get(0);

        Assert.assertNotNull(p610claim.getMainsnak());
        Assert.assertEquals(p610claim.getMainsnak().getDatatype(), "wikibase-item");

        Assert.assertTrue(p610claim.getMainsnak().getData() instanceof Item);
        Assert.assertEquals("513", ((Item) p610claim.getMainsnak().getData()).getEnt().getId());
    }
}
