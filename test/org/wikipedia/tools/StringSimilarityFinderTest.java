/**
 *  @(#)StringSimilarityFinder.java 0.01 01/11/2025
 *  Copyright (C) 2025 MER-C
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

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Tests for StringSimilarityFinder.
 *  @author MER-C
 */
public class StringSimilarityFinderTest
{
    private StringSimilarityFinder ssf = new StringSimilarityFinder();
    
    /**
     *  Builds test matches. Indices follow substring convention.
     *  @param match the common string
     *  @param text1 the first text that was compared
     *  @param start1 the start index of the common string in text1
     *  @param text2 the second text that was compared
     *  @param start2 the start index of the common string in text2
     *  @return a Match object that is the expected behaviour
     */
    private StringSimilarityFinder.Match buildExpectedMatch(String match, String text1, int start1, String text2, int start2)
    {
        String ml = match.toLowerCase();
        int length = match.length();
        // need to check whether the parameters are correct
        assertEquals(ml, text1.substring(start1, start1 + length).toLowerCase());
        assertEquals(ml, text2.substring(start2, start2 + length).toLowerCase());
        // remember matches are inclusive at both ends but substring isn't
        return new StringSimilarityFinder.Match(start1, start1 + length - 1, start2, start2 + length - 1);
    }

    @Test
    public void findConsecutiveWordMatches()
    {
        assertThrows(NullPointerException.class, () -> ssf.findConsecutiveWordMatches(null, "Test"));
        assertThrows(NullPointerException.class, () -> ssf.findConsecutiveWordMatches("Test", null));
        
        // test short
        String text1 = "Another unrelated";
        String text2 = "Another unrelated sentence to start.";
        assertEquals(Collections.EMPTY_LIST, ssf.findConsecutiveWordMatches(text1, text2));
        text1 = "Another unrelated sentence to start.";
        text2 = "Another unrelated";
        assertEquals(Collections.EMPTY_LIST, ssf.findConsecutiveWordMatches(text1, text2));
        
        text1 = "The quick brown fox jumps over the lazy dog. This is a simple test. " +
                "We are looking for at least three consecutive words that are identical. " +
                "This check should be case-insensitive and find the best matches possible.";
        text2 = "Another unrelated sentence to start. We are looking for at least three " +
                "consecutive words and we found them. The quick brown fox is not here. " +
                "This is a simple test as well, but this check should be case-insensitive.";
        
        // now check the results
        List<StringSimilarityFinder.Match> expected = List.of(
            buildExpectedMatch("The quick brown fox", text1, 0, text2, 108),
            buildExpectedMatch("This is a simple test", text1, 45, text2, 141), // trailing punctuation should be ignored
            buildExpectedMatch("We are looking for at least three consecutive words", text1, 68, text2, 37),
            buildExpectedMatch("This check should be case-insensitive", text1, 140, text2, 176));
        List<StringSimilarityFinder.Match> actual = ssf.findConsecutiveWordMatches(text1, text2);
        assertEquals(expected, actual);
        
        // perturb by adding more spaces
        text1 = "This is a   common string.";
        text2 = "This is a common phrase.";
        expected = List.of(new StringSimilarityFinder.Match(0, 17, 0, 15));
        actual = ssf.findConsecutiveWordMatches(text1, text2);
        assertEquals(expected, actual);
        
        // check leading punctuation too
        text1 = "This is a (common) string.";
        expected = List.of(new StringSimilarityFinder.Match(0, 16, 0, 15));
        actual = ssf.findConsecutiveWordMatches(text1, text2);
        assertEquals(expected, actual);
        
        // common way to write em-dashes
        text1 = "This is a - common string.";
        expected = List.of(new StringSimilarityFinder.Match(0, 17, 0, 15));
        actual = ssf.findConsecutiveWordMatches(text1, text2);
        assertEquals(expected, actual);
        
        // test repeats (text 1 only) - this also tests symmetry
        text1 = "This is a common string. This is a common string.";
        text2 = "Filler. This is a common phrase.";
        expected = List.of(new StringSimilarityFinder.Match(0, 15, 8, 23));
        actual = ssf.findConsecutiveWordMatches(text1, text2);
        assertEquals(expected, actual);
        
        // another repeat, but where the best match doesn't occur first
        text1 = "Filler. This is a common phrase, one of two.";
        text2 = "This is a common string. This is a common phrase as well.";
        expected = List.of(new StringSimilarityFinder.Match(8, 30, 25, 47));
        actual = ssf.findConsecutiveWordMatches(text1, text2);
        assertEquals(expected, actual);
        
        // test repeats (text 2 only) - this also tests symmetry
        text1 = "Filler. This is a common phrase.";
        text2 = "This is a common string. This is a common string.";
        expected = List.of(new StringSimilarityFinder.Match(8, 23, 0, 15));
        actual = ssf.findConsecutiveWordMatches(text1, text2);
        assertEquals(expected, actual);
    }
    
    @Test
    public void setMinimumMatchLength()
    {
        // verify default and get/set
        assertEquals(3, ssf.getMinimumMatchLength());
        ssf.setMinimumMatchLength(4);
        assertEquals(4, ssf.getMinimumMatchLength());
        
        // test functionality
        String text1 = "The quick brown fox jumps over the lazy dog. This is a simple test. " +
                       "We are looking for at least three consecutive words that are identical. " +
                       "This check should be case-insensitive and find the best matches possible.";
        String text2 = "Another unrelated sentence to start. We are looking for at least three " +
                       "consecutive words and we found them. The quick brown fox is not here. " +
                       "This is a simple test as well, but this check should be case-insensitive.";
        
        List<StringSimilarityFinder.Match> expected = List.of(
            buildExpectedMatch("The quick brown fox", text1, 0, text2, 108),
            buildExpectedMatch("This is a simple test", text1, 45, text2, 141),
            buildExpectedMatch("We are looking for at least three consecutive words", text1, 68, text2, 37),
            buildExpectedMatch("This check should be case-insensitive", text1, 140, text2, 176));
        List<StringSimilarityFinder.Match> actual = ssf.findConsecutiveWordMatches(text1, text2);
        assertEquals(expected, actual);
        
        ssf.setMinimumMatchLength(5);
        expected = List.of(
            buildExpectedMatch("This is a simple test", text1, 45, text2, 141),
            buildExpectedMatch("We are looking for at least three consecutive words", text1, 68, text2, 37),
            buildExpectedMatch("This check should be case-insensitive", text1, 140, text2, 176));
        actual = ssf.findConsecutiveWordMatches(text1, text2);
        assertEquals(expected, actual);
    }

    @Test
    public void testGenerateHtmlHighlight()
    {
        assertThrows(NullPointerException.class, () -> ssf.generateHtmlHighlight(null, "", Collections.emptyList()));
        assertThrows(NullPointerException.class, () -> ssf.generateHtmlHighlight("", null, Collections.emptyList()));
        assertThrows(NullPointerException.class, () -> ssf.generateHtmlHighlight("", "", null));
    }
}
