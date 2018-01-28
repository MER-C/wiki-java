/**
 *  @(#)ArrayUtilsUnitTest.java 0.01 31/10/2017
 *  Copyright (C) 2017 - 2018 MER-C and contributors
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

import java.time.*;
import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

/**
 *  Unit tests for {@link ArrayUtils}.
 *  @author MER-C
 */
public class ArrayUtilsUnitTest
{
    private static Wiki enWiki = Wiki.createInstance("en.wikipedia.org");
    
    /**
     *  Initialize wiki objects.
     *  @throws Exception if a network error occurs
     */
    @BeforeClass
    public static void setUpClass() throws Exception
    {
        enWiki.setMaxLag(-1);
    }
    
    @Test
    public void intersection()
    {
        String[] a = { "1", "2", "3", "4", "5" };
        String[] b1 = { "1", "2", "3" };
        String[] b2 = { "3", "4", "5", "x" };
        assertArrayEquals("intersection", new String[] { "3" }, ArrayUtils.intersection(a, b1, b2));
    }
    
    @Test
    public void relativeComplement()
    {
        String[] a = { "1", "2", "3", "4", "5" };
        String[] b1 = { "1", "2" };
        String[] b2 = { "3", "4", "x" };
        assertArrayEquals("intersection", new String[] { "5" }, ArrayUtils.relativeComplement(a, b1, b2));
    }
    
    @Test
    public void removeReverts() throws Exception
    {
        // https://en.wikipedia.org/w/index.php?title=Azerbaijan&offset=20180125204500&action=history
        Wiki.Revision[] revisions = enWiki.getPageHistory("Azerbaijan", OffsetDateTime.parse("2018-01-16T00:00:00Z"), OffsetDateTime.parse("2018-01-25T20:45:00Z"), false);
        for (int i = 1; i < revisions.length; i++)
            System.out.println(revisions[i].compareTo(revisions[i-1]));
        long[] oldids = new long[] 
        {
            822342083L, //  0: state  3, reverts 822341440L
            822341440L, //  1: state  7, reverts 822339163L
            822339163L, //  2: state  3, reverts 822338691L and 822338419L
            822338691L, //  3: state  7
            822338419L, //  4: state  6
            821798528L, //  5: state  3, reverts 821778837L
            821778837L, //  6: state  5
            821776884L, //  7: state  3, reverts 821776856L
            821776856L, //  8: state  4
            821575229L, //  9: state  3
            821353780L, // 10: state  0, reverts 821353728L
            821353728L, // 11: state NA, content RevisionDeleted
            821171566L, // 12: state  0, reverts 821170526L. Should not be removed, this state does not appear before this revision.
            821171526L, // 13: state  1, reverts 821171155L
            821171155L, // 14: state  2
            821170526L  // 15: state  1
        };
        assertArrayEquals("removereverts: setup", oldids, Arrays.stream(revisions).mapToLong(Wiki.Revision::getRevid).toArray());
        long[] expected = new long[] 
        {
            821170526L, // 14: state  1
            821171155L, // 13: state  2
            821171566L, // 12: state  0, reverts 821170526L. Should not be removed, this is unique.
            821353728L, // 11: state NA, content RevisionDeleted
            821575229L, //  9: state  3
            821776856L, //  8: state  4
            821778837L, //  6: state  5
            822338419L, //  4: state  6
            822338691L  //  3: state  7
        };
        Wiki.Revision[] revertsremoved = ArrayUtils.removeReverts(revisions);
        assertArrayEquals("removereverts", expected, Arrays.stream(revertsremoved).mapToLong(Wiki.Revision::getRevid).toArray());
    }
}
