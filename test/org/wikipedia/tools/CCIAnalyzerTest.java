/**
 *  @(#)CCIAnalyzerTest.java 0.03 04/01/2020
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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.wikipedia.*;

/**
 *  Unit tests for {@link CCIAnalyzer}.
 *  @author MER-C
 */
public class CCIAnalyzerTest
{
    private final Wiki enWiki;
    private final CCIAnalyzer analyzer;
    
    public CCIAnalyzerTest()
    {
        enWiki = Wiki.newSession("en.wikipedia.org");
        enWiki.setMaxLag(-1);
        analyzer = new CCIAnalyzer();
    }
    
    @Test
    public void removeDisambiguationPages()
    {
        assertFalse(CCIAnalyzer.removeDisambiguationPages("Karpagam (disambiguation)"));
        assertTrue(CCIAnalyzer.removeDisambiguationPages("Finding Dory"));
        
        // INTEGRATION TEST
        // The disambiguation page edit would be culled by normal means
        String cci = """
            *'''N''' [[:Karpagam (disambiguation)]] (1 edit): [[Special:Diff/902209948|(+269)]]
            *[[:Finding Dory]] (1 edit): [[Special:Diff/858890717|(+354)]]""";
        CCIAnalyzer.CCIPage page = analyzer.loadString(enWiki, cci);
        analyzer.loadDiffs(page);
        analyzer.analyzeDiffs(page);
        assertEquals(Collections.emptyList(), page.getMinorEdits());
        
        // the default is disambiguation culling = on
        analyzer.setTitleFunction(title -> true);
        page = analyzer.loadString(enWiki, cci);
        analyzer.loadDiffs(page);
        analyzer.analyzeDiffs(page);
        assertEquals(List.of("[[Special:Diff/902209948|(+269)]]"), page.getMinorEdits());
    }
    
    @Test
    public void removeListPages()
    {
        assertFalse(CCIAnalyzer.removeListPages("List of YouTubers"));
        assertTrue(CCIAnalyzer.removeListPages("Ramanujan (film)"));
        
        // INTEGRATION TEST
        String cci = """
            *[[:List of Tamil films of 2011]] (1 edit): [[Special:Diff/472267771|(+315)]]
            *[[:Ramanujan (film)]] (3 edits): [[Special:Diff/573584256|(+643)]]""";
        analyzer.setTitleFunction(CCIAnalyzer::removeListPages);
        CCIAnalyzer.CCIPage page = analyzer.loadString(enWiki, cci);
        analyzer.loadDiffs(page);
        analyzer.analyzeDiffs(page);
        assertEquals(Collections.emptyList(), page.getMinorEdits());
        
        analyzer.setTitleFunction(title -> true);
        page = analyzer.loadString(enWiki, cci);
        analyzer.loadDiffs(page);
        analyzer.analyzeDiffs(page);
        assertEquals(List.of("[[Special:Diff/472267771|(+315)]]"), page.getMinorEdits());
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
        
        // INTEGRATION TEST
        // the second diff contains references only
        String cci = "*[[:Smiley (1956 film)]] (2 edits): [[Special:Diff/476809081|(+460)]][[Special:Diff/446793589|(+205)]]";
        analyzer.setFilteringFunction(CCIAnalyzer::removeReferences);
        analyzer.setCullingFunction(diff -> analyzer.wordCountCull(diff, 9));
        CCIAnalyzer.CCIPage page = analyzer.loadString(enWiki, cci);
        analyzer.loadDiffs(page);
        analyzer.analyzeDiffs(page);
        assertEquals(List.of("[[Special:Diff/446793589|(+205)]]"), page.getMinorEdits());
    }
    
    @Test
    public void removeExternalLinks()
    {
        assertEquals("Test  Test2", CCIAnalyzer.removeExternalLinks("Test [http://example.com Test link] Test2"));
        assertEquals("*", CCIAnalyzer.removeExternalLinks("*[http://example.com Test link]"));
    }
    
    @Test
    public void whitelistCull()
    {
        // INTEGRATION TEST
        // from [[Wikipedia:Contributor copyright investigations/Kailash29792 02]] - AFD
        String cci = """
            *[[:List of science fiction comedy works]] (1 edit): [[Special:Diff/924018716|(+458)]]
            *[[:Sabash Thambi]] (1 edit): [[Special:Diff/682049136|(+578)]]""";
        CCIAnalyzer.CCIPage page = analyzer.loadString(enWiki, cci);
        analyzer.setCullingFunction(CCIAnalyzer::whitelistCull);
        analyzer.loadDiffs(page);
        analyzer.analyzeDiffs(page);
        assertEquals(List.of("[[Special:Diff/924018716|(+458)]]"), page.getMinorEdits());
    }
    
    @Test
    public void wordCountCull()
    {
        // INTEGRATION TEST
        String cci =
            // 13 words - checks word count threshold
            "*'''N''' [[:Urmitz]] (1 edit): [[Special:Diff/154400451|(+283)]]" + 
            // 15 words, but two of them are just wikitext remnants - should be
            // removed as punctuation
            "*'''N''' [[:SP-354]] (1 edit): [[Special:Diff/255072765|(+286)]]";
        CCIAnalyzer.CCIPage page = analyzer.loadString(enWiki, cci);
        analyzer.setCullingFunction(diff -> analyzer.wordCountCull(diff, 12));
        analyzer.loadDiffs(page);
        analyzer.analyzeDiffs(page);
        assertTrue(page.getMinorEdits().isEmpty());
        analyzer.setCullingFunction(diff -> analyzer.wordCountCull(diff, 13));
        analyzer.analyzeDiffs(page);
        assertEquals(List.of("[[Special:Diff/154400451|(+283)]]", "[[Special:Diff/255072765|(+286)]]"), page.getMinorEdits());
    }
    
    @Test
    public void listItemCull()
    {
        assertFalse(CCIAnalyzer.listItemCull("*[http://example.com External link]"));
        assertFalse(CCIAnalyzer.listItemCull("*[[Wikilink]]"));
        assertFalse(CCIAnalyzer.listItemCull("* ''' [[Bold Wikilink]] '''"));
        assertTrue(CCIAnalyzer.listItemCull("Don't cull this! *[[Test]]"));
        
        // INTEGRATION TEST
        // from [[Wikipedia:Contributor copyright investigations/Kailash29792 02]]
        String cci = "*'''N''' [[:We Can Be Heroes (disambiguation)]] (1 edit): [[Special:Diff/895761173|(+486)]]";
        analyzer.setTitleFunction(title -> true);
        analyzer.setCullingFunction(diff -> analyzer.wordCountCull(diff, 11));
        CCIAnalyzer.CCIPage page = analyzer.loadString(enWiki, cci);
        analyzer.loadDiffs(page);
        analyzer.analyzeDiffs(page);
        assertTrue(page.getMinorEdits().isEmpty());
        analyzer.setCullingFunction(diff -> analyzer.wordCountCull(diff, 11) && CCIAnalyzer.listItemCull(diff));
        analyzer.analyzeDiffs(page);
        assertEquals(List.of("[[Special:Diff/895761173|(+486)]]"), page.getMinorEdits());
    }
    
    @Test
    public void fileAdditionCull()
    {
        String filestring = """
            [[File:St Lawrence Jewry, City of London, UK - Diliff.jpg|thumb|right|400px|\
            The interior of St Lawrence Jewry, the official church of the Lord Mayor \
            of London, located next to Guildhall in the City of London.]]""".toLowerCase();
        assertFalse(CCIAnalyzer.fileAdditionCull(filestring));
        
        // INTEGRATION TEST
        // from [[Wikipedia:Contributor copyright investigations/Lightburst]]
        String cci = "*'''N''' [[:7Seventy7]] (2 edits): [[Special:Diff/906034889|(+6049)]][[Special:Diff/906119432|(+164)]]";
        CCIAnalyzer.CCIPage page = analyzer.loadString(enWiki, cci);
        analyzer.setCullingFunction(diff -> analyzer.wordCountCull(diff, 9));
        analyzer.loadDiffs(page);
        analyzer.analyzeDiffs(page);
        assertTrue(page.getMinorEdits().isEmpty());
        analyzer.setCullingFunction(diff -> analyzer.wordCountCull(diff, 9) && CCIAnalyzer.fileAdditionCull(diff));
        analyzer.analyzeDiffs(page);
        assertEquals(List.of("[[Special:Diff/906119432|(+164)]]"), page.getMinorEdits());
    }
    
    @Test
    public void tableCull()
    {
        // INTEGRATION TEST
        // from [[Wikipedia:Contributor copyright investigations/Haikavin1990]]
        String cci = "*[[:Manidhanum Dheivamagalam]] (2 edits): [[Special:Diff/854000150|(+1472)]][[Special:Diff/854001036|(+728)]]";
        CCIAnalyzer.CCIPage page = analyzer.loadString(enWiki, cci);
        analyzer.setCullingFunction(diff -> analyzer.wordCountCull(diff, 9));
        analyzer.loadDiffs(page);
        analyzer.analyzeDiffs(page);
        assertTrue(page.getMinorEdits().isEmpty());
        analyzer.setCullingFunction(diff -> analyzer.wordCountCull(diff, 9) && CCIAnalyzer.tableCull(diff));
        analyzer.analyzeDiffs(page);
        assertEquals(List.of("[[Special:Diff/854001036|(+728)]]"), page.getMinorEdits());
    }
    
    /**
     *  Miscellaneous integration tests.
     */
    @Test
    public void loadString()
    {
        String cci = 
            // WikitextUtils.removeComments
            // from [[Wikipedia:Contributor copyright investigations/Kailash29792 02]]
            "*[[:List of science fiction comedy works]] (1 edit): [[Special:Diff/924018716|(+458)]]" +
            // Copyvio, already revdeled - should be deleted from output.
            // Test only proves this is not problematic.
            // from [[Wikipedia:Contributor copyright investigations/Haikavin1990]]
            "*[[:Amir Garib]] (1 edit): [[Special:Diff/849325539|(+1822)]]" +
            // Deleted article, should be deleted from output (not implemented)
            // Test only proves this is not problematic.
            // from [[Wikipedia:Contributor copyright investigations/20150507 02]]
            "*'''N''' [[:List of Papua New Guinea ODI cricket centurions]] (1 edit): [[Special:Diff/880649594|(+1775)]]";
        CCIAnalyzer.CCIPage page = analyzer.loadString(enWiki, cci);
        analyzer.setCullingFunction(diff -> analyzer.wordCountCull(diff, 9));
        analyzer.loadDiffs(page);
        analyzer.analyzeDiffs(page);
        assertTrue(page.getMinorEdits().isEmpty());
        analyzer.setFilteringFunction(WikitextUtils::removeComments);
        analyzer.analyzeDiffs(page);
        assertEquals(List.of("[[Special:Diff/924018716|(+458)]]"), page.getMinorEdits());
                
        // from [[Wikipedia:Contributor copyright investigations/Dr. Blofeld 40]] - infoboxes
        // cci = "*[[:Ann Thongprasom]] (1 edit): [[Special:Diff/130352114|(+460)]]" +
        // "*[[:Shoma Anand]] (1 edit): [[Special:Diff/130322991|(+460)]]";
    }
}
