/**
 *  @(#)CCIAnalyzerTest.java 0.01 27/12/2019
 *  Copyright (C) 2019 - 20xx MER-C
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
package org.wikipedia.tools;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit tests for {@link CCIAnalyzer}.
 *  @author MER-C
 */
public class CCIAnalyzerTest
{
    
    public CCIAnalyzerTest()
    {
    }
    
    @Test
    public void removeReferences()
    {
        assertEquals("Test: plain ref.", CCIAnalyzer.removeReferences("Test: plain ref<ref>Test reference</ref>."));
        assertEquals("Test: named ref.", CCIAnalyzer.removeReferences("Test: named ref<ref name=\"Test\">Test reference</ref>."));
        assertEquals("Test: reused ref.", CCIAnalyzer.removeReferences("Test: reused ref<ref name=\"Test\" />."));
        assertEquals("Test: unbalanced ref 1<ref>.", CCIAnalyzer.removeReferences("Test: unbalanced ref 1<ref>."));
        assertEquals("Test: unbalanced ref 2<ref name=\"unbalanced\">.", CCIAnalyzer.removeReferences("Test: unbalanced ref 2<ref name=\"unbalanced\">."));
        assertEquals("Test: combined. Sentence 2.", CCIAnalyzer.removeReferences(
            "Test: combined<ref name=\"Test\">Test reference</ref>. Sentence 2<ref name=\"Test\" />."));
        assertEquals("Test: combined before. Sentence 2.", CCIAnalyzer.removeReferences(
            "Test: combined before<ref name=\"Before\" />. Sentence 2<ref>Test reference</ref>."));
        assertEquals("Test: multiple.", CCIAnalyzer.removeReferences("Test: multiple<ref>Reference 1</ref><ref>Reference 2</ref>."));
    }
    
    @Test
    public void whitelistCull()
    {
        // INTEGRATION TEST
        String cci = 
        // from [[Wikipedia:Contributor copyright investigations/Kailash29792 02]] - AFD
        "*[[:List of science fiction comedy works]] (1 edit): [[Special:Diff/924018716|(+458)]]";
        CCIAnalyzer analyzer = new CCIAnalyzer();
        analyzer.loadString(cci);
        analyzer.setCullingFunction(CCIAnalyzer::whitelistCull);
        analyzer.analyzeDiffs();
        assertEquals(List.of("[[Special:Diff/924018716|(+458)]]"), analyzer.getMinorEdits());
    }
    
    @Test
    public void wordCountCull()
    {
        // INTEGRATION TEST
        
        // check whether references are removed
        // from [[Wikipedia:Contributor copyright investigations/Dutchy85]]
        String cci = 
            "*[[:Smiley (1956 film)]] (3 edits): [[Special:Diff/509191673|(+7148)]][[Special:Diff/476809081|(+460)]]"
            + "[[Special:Diff/446793589|(+205)]]";
        CCIAnalyzer analyzer = new CCIAnalyzer();
        analyzer.loadString(cci);
        analyzer.setCullingFunction(diff -> CCIAnalyzer.wordCountCull(diff, 9, false));
        analyzer.analyzeDiffs();
        assertTrue(analyzer.getMinorEdits().isEmpty());
        analyzer.setCullingFunction(diff -> CCIAnalyzer.wordCountCull(diff, 9, true));
        analyzer.analyzeDiffs();
        assertEquals(List.of("[[Special:Diff/446793589|(+205)]]"), analyzer.getMinorEdits());
    }
    
    @Test
    public void listItemCull()
    {
        assertFalse(CCIAnalyzer.listItemCull("*[http://example.com External link]"));
        assertFalse(CCIAnalyzer.listItemCull("*[[Wikilink]]"));
    }
    
    @Test
    public void fileAdditionCull()
    {
        String filestring = ("[[File:St Lawrence Jewry, City of London, UK - Diliff.jpg"
            + "|thumb|right|400px|The interior of St Lawrence Jewry, the official church of the Lord Mayor "
            + "of London, located next to Guildhall in the City of London.]]").toLowerCase();
        assertFalse(CCIAnalyzer.fileAdditionCull(filestring));
    }
    
    public void loadString()
    {
        String cci = 
        // from [[Wikipedia:Contributor copyright investigations/Dutchy85]] - references
        "*[[:Smiley (1956 film)]] (3 edits): [[Special:Diff/509191673|(+7148)]][[Special:Diff/476809081|(+460)]]"
            + "[[Special:Diff/446793589|(+205)]]" +
        "*[[:Australian cricket team in the West Indies in 1983–84]] (16 edits): [[Special:Diff/601446274|(+7142)]]"
            + "[[Special:Diff/696963536|(+6475)]][[Special:Diff/637576690|(+5366)]][[Special:Diff/601439595|(+4500)]]"
            + "[[Special:Diff/485135430|(+2420)]][[Special:Diff/601474114|(+2369)]][[Special:Diff/601472887|(+1223)]]"
            + "[[Special:Diff/500172000|(+1106)]][[Special:Diff/655598008|(+647)]][[Special:Diff/601497454|(+560)]]"
            + "[[Special:Diff/486364045|(+407)]][[Special:Diff/503566575|(+379)]][[Special:Diff/637574127|(+293)]]"
            + "[[Special:Diff/705087540|(+287)]][[Special:Diff/601471866|(+281)]][[Special:Diff/601471765|(+230)]]" +
        // from [[Wikipedia:Contributor copyright investigations/Dr. Blofeld 40]] - infoboxes
        "*[[:Ann Thongprasom]] (1 edit): [[Special:Diff/130352114|(+460)]]" +
        "*[[:Shoma Anand]] (1 edit): [[Special:Diff/130322991|(+460)]]";
    }
}
