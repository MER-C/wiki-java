/**
 *  @(#)StringSimilarityFinder.java 0.01 31/10/2025
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

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

import org.wikipedia.HTMLUtils;

/**
 *  Finds similarities between two strings and generates an HTML report 
 *  highlighting the matches.
 *  @author MER-C (with assistance from Gemini Pro 2.5)
 *  @version 0.01
 */
public class StringSimilarityFinder
{
    private int min_consecutive_words = 3;
    
    public StringSimilarityFinder()
    {
        
    }
    
    /**
     *  Sets the minimum number of words required to comprise a match. The 
     *  default is three.
     *  @param words the new minimum number of words
     *  @see #getMinimumMatchLength() 
     */
    public void setMinimumMatchLength(int words)
    {
        min_consecutive_words = words;
    }
    
    /**
     *  Returns the current minimum number of words required to comprise a match.
     *  @return (see above)
     *  @see #setMinimumMatchLength(int) 
     */
    public int getMinimumMatchLength()
    {
        return min_consecutive_words;
    }

    /**
     *  Represents a single match found between the two texts. The indices are 
     *  character-based and inclusive.
     *
     *  @param start1 The starting character index of the match in the first string.
     *  @param end1   The ending character index of the match in the first string.
     *  @param start2 The starting character index of the match in the second string.
     *  @param end2   The ending character index of the match in the second string.
     */
    public record Match(int start1, int end1, int start2, int end2)
    {
        @Override
        public String toString()
        {
            return String.format("Match[text1(%d-%d), text2(%d-%d)]", start1, end1, start2, end2);
        }
    }

    /**
     *  A private helper record to hold a word and its original character position.
     *
     *  @param text  The word string, converted to lowercase.
     *  @param start The original starting character index.
     *  @param end   The original ending character index.
     */
    private record Word(String text, int start, int end) {}
    
    /**
     *  A private helper record for Pass 1 (Find All). Stores matches by their
     *  <u>word index</u> for easy overlap checking.
     *
     *  @param wordIndex1 The starting *word index* of the match in text1.
     *  @param wordIndex2 The starting *word index* of the match in text2.
     *  @param wordLength The number of words in the match.
     */
    private record PotentialMatch(int wordIndex1, int wordIndex2, int wordLength) {}

    /**
     *  A private helper record to represent one side of a match (for a single text)
     *  and its match ID. Implements Comparable to allow sorting by start index.
     *
     * @param start   The starting character index.
     * @param end     The ending character index (inclusive).
     * @param matchId The 1-based index of the match (for tooltips).
     */
    private record SubMatch(int start, int end, int matchId) implements Comparable<SubMatch>
    {
        @Override
        public int compareTo(SubMatch other)
        {
            return Integer.compare(this.start, other.start);
        }
    }

    /**
     *  Compares two strings to find sequences of at least the minimum number of 
     *  identical consecutive words. The comparison is case-insensitive and 
     *  ignores punctuation at the start and end of words. The method identifies 
     *  the longest possible non-overlapping matches.
     *
     *  @param text1 The first string to compare. Must not be null.
     *  @param text2 The second string to compare. Must not be null.
     *  @return A list of {@link Match} objects, each containing the start and end
     *  character indices for a match in both strings. The list will be empty
     *  if no such matches are found.
     */
    public List<Match> findConsecutiveWordMatches(String text1, String text2)
    {
        Objects.requireNonNull(text1, "Input text1 cannot be null.");
        Objects.requireNonNull(text2, "Input text2 cannot be null.");

        List<Word> words1 = extractWordsWithIndices(text1);
        List<Word> words2 = extractWordsWithIndices(text2);
        if (words1.size() < min_consecutive_words || words2.size() < min_consecutive_words)
            return Collections.emptyList();

        // --- PASS 1: Find All Potential Matches ---
        List<PotentialMatch> allMatches = new ArrayList<>();
        for (int i = 0; i <= words1.size() - min_consecutive_words; i++)
        {
            for (int j = 0; j <= words2.size() - min_consecutive_words; j++)
            {
                // Find the length of the match starting at (i, j)
                int matchLength = 0;
                while (i + matchLength < words1.size() &&
                       j + matchLength < words2.size() &&
                       words1.get(i + matchLength).text().equals(words2.get(j + matchLength).text())) {
                    matchLength++;
                }

                if (matchLength >= min_consecutive_words)
                {
                    allMatches.add(new PotentialMatch(i, j, matchLength));
                    
                    // Optimization: We found a match of 'matchLength' starting at j.
                    // We can skip checking j+1 through j+matchLength-1, as they
                    // cannot be the start of a *new* match.
                    j += matchLength - 1;
                }
            }
        }

        // --- PASS 2: Rank and Filter ---
        // Sort: Longest first.
        // Secondary sort by start index in text1 for stable, deterministic results.
        allMatches.sort((m1, m2) -> {
            int lenCompare = Integer.compare(m2.wordLength(), m1.wordLength());
            if (lenCompare != 0)
                return lenCompare;
            return Integer.compare(m1.wordIndex1(), m2.wordIndex1());
        });

        List<Match> finalMatches = new ArrayList<>();
        boolean[] word1Used = new boolean[words1.size()];
        boolean[] word2Used = new boolean[words2.size()];

        for (PotentialMatch pm : allMatches)
        {
            // Check if any word in this potential match has already been
            // claimed by a *better* (longer) match.
            if (isOverlapping(pm, word1Used, word2Used))
                continue;

            // This is a valid, non-overlapping, "best" match. Accept it.
            // Mark all words in both texts as used.
            for (int k = 0; k < pm.wordLength(); k++)
            {
                word1Used[pm.wordIndex1() + k] = true;
                word2Used[pm.wordIndex2() + k] = true;
            }

            // Convert word-index-based PotentialMatch to char-index-based Match
            Word startWord1 = words1.get(pm.wordIndex1());
            Word endWord1 = words1.get(pm.wordIndex1() + pm.wordLength() - 1);
            Word startWord2 = words2.get(pm.wordIndex2());
            Word endWord2 = words2.get(pm.wordIndex2() + pm.wordLength() - 1);

            finalMatches.add(new Match(
                startWord1.start(), endWord1.end(),
                startWord2.start(), endWord2.end()
            ));
        }
        
        // Sort the final results by their start position in text1 for
        // a predictable and easy-to-read final list.
        finalMatches.sort((m1, m2) -> Integer.compare(m1.start1(), m2.start1()));

        return finalMatches;
    }
    
    /**
     *  Pass 2 helper: Checks if a potential match overlaps with any words that 
     *  have already been "used" by a better match.
     */
    private static boolean isOverlapping(PotentialMatch pm, boolean[] word1Used, boolean[] word2Used)
    {
        for (int k = 0; k < pm.wordLength(); k++)
            if (word1Used[pm.wordIndex1() + k] || word2Used[pm.wordIndex2() + k])
                return true;
        return false;
    }

    /**
     *  Splits a string into words and captures their original start and end 
     *  indices. A "word" is considered to be any sequence of one or more 
     *  non-whitespace characters, ignoring any punctuation at the start and end
     *  and is case-insensitive.
     *
     *  @param text The string to process.
     *  @return A list of {@link Word} objects.
     */
    private List<Word> extractWordsWithIndices(String text)
    {
        List<Word> words = new ArrayList<>();
        // This pattern finds sequences of non-whitespace characters.
        Pattern pattern = Pattern.compile("\\S+");
        Matcher matcher = pattern.matcher(text);

        // Pattern for leading punctuation/symbols. \p{P}=Punctuation, \p{S}=Symbol
        Pattern leadingPunctuation = Pattern.compile("^[\\p{P}\\p{S}]+");
        // Pattern for trailing punctuation/symbols.
        Pattern trailingPunctuation = Pattern.compile("[\\p{P}\\p{S}]+$");

        while (matcher.find())
        {
            String fullBlock = matcher.group();
            int blockStart = matcher.start();
            int blockEnd = matcher.end() - 1; // inclusive

            String coreWord = fullBlock;
            int coreStart = blockStart;
            int coreEnd = blockEnd;

            // trim trailing punctuation
            Matcher tailMatcher = trailingPunctuation.matcher(coreWord);
            if (tailMatcher.find())
            {
                coreWord = coreWord.substring(0, tailMatcher.start());
                coreEnd = blockStart + tailMatcher.start() - 1;
            }

            // trim leading punctuation
            Matcher headMatcher = leadingPunctuation.matcher(coreWord);
            if (headMatcher.find())
            {
                // Found leading punctuation, adjust coreWord and coreStart
                // headMatcher.end() gives the length of the leading punctuation
                coreWord = coreWord.substring(headMatcher.end());
                coreStart = coreStart + headMatcher.end();
            }

            // Only add if the coreWord is not empty (e.g., if the block was just "---")
            if (!coreWord.isEmpty())
            {
                words.add(new Word(
                    coreWord.toLowerCase(),
                    coreStart,
                    coreEnd
                ));
            }
        }
        return words;
    }

    /**
     *  Generates an HTML fragment that displays the two texts side-by-side
     *  in a table, with matched word sequences highlighted.
     *
     *  @param text1   The first original string.
     *  @param text2   The second original string.
     *  @param matches The list of matches found by {@link #findConsecutiveWordMatches}.
     *  @return An HTML string fragment.
     */
    public String generateHtmlHighlight(String text1, String text2, List<Match> matches)
    {
        Objects.requireNonNull(text1, "Input text1 cannot be null.");
        Objects.requireNonNull(text2, "Input text2 cannot be null.");
        Objects.requireNonNull(matches, "Matches list cannot be null.");

        List<SubMatch> subMatches1 = new ArrayList<>();
        List<SubMatch> subMatches2 = new ArrayList<>();

        for (int i = 0; i < matches.size(); i++)
        {
            Match match = matches.get(i);
            int matchId = i + 1; // 1-based index for tooltips
            subMatches1.add(new SubMatch(match.start1(), match.end1(), matchId));
            subMatches2.add(new SubMatch(match.start2(), match.end2(), matchId));
        }

        // Sort sub-matches by their start index to process the text sequentially
        Collections.sort(subMatches1);
        Collections.sort(subMatches2);

        return String.format(
            """
            <style>
                table.similarity-table { width: 100%%; border-collapse: collapse; table-layout: fixed; }
                th.similarity-header { padding: 12px; border: 1px solid #ddd; background-color: #f4f4f4; }
                td.similarity-cell { width: 50%%; vertical-align: top; padding: 12px; border: 1px solid #ddd; font-family: sans-serif; line-height: 1.6; white-space: pre-wrap; word-wrap: break-word; }
                mark.match-highlight { background-color: #ffff99; padding: 2px 1px; border-radius: 3px; cursor: help; }
            </style>
            <table class="similarity-table">
            <thead>
            <tr>
                <th class="similarity-header">Text 1</th>
                <th class="similarity-header">Text 2</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td class="similarity-cell">%s</td>
                <td class="similarity-cell">%s</td>
            </tr>
            </tbody>
            </table>""", buildHighlightedHtml(text1, subMatches1), buildHighlightedHtml(text2, subMatches2));
    }

    /**
     *  A helper method to build an HTML string for a single text, inserting
     *  <mark> tags for highlighted segments.
     *
     *  @param text              The full original text.
     *  @param sortedSubMatches A list of SubMatch objects, sorted by start index.
     *  @return An HTML string with highlights.
     */
    private String buildHighlightedHtml(String text, List<SubMatch> sortedSubMatches)
    {
        StringBuilder sb = new StringBuilder();
        int currentIndex = 0;

        for (SubMatch match : sortedSubMatches)
        {
            // Append non-matching text before this match, properly escaped
            if (match.start() > currentIndex)
                sb.append(HTMLUtils.sanitizeForHTML(text.substring(currentIndex, match.start())));

            // Append the highlighted match
            sb.append(String.format("<mark class=\"match-highlight\" title=\"Match %d\">", match.matchId()));
            // Ensure end index is within bounds (inclusive)
            int end = Math.min(match.end() + 1, text.length());
            sb.append(HTMLUtils.sanitizeForHTML(text.substring(match.start(), end)));
            sb.append("</mark>");

            currentIndex = match.end() + 1;
        }

        // Append any remaining text after the last match
        if (currentIndex < text.length())
            sb.append(HTMLUtils.sanitizeForHTML(text.substring(currentIndex)));
        return sb.toString();
    }

    /**
     *  Runs this program.
     *  @param args the command line arguments
     *  @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception
    {
        CommandLineParser clp = new CommandLineParser("StringSimilarityFinder")
            .addHelp()
            .addVersion("0.01")
            .addSingleArgumentFlag("--file1", "file.txt", "A text file containing plain text to compare, going to slot 1.")
            .addSingleArgumentFlag("--file2", "file.txt", "A text file containing plain text to compare, going to slot 2.")
            .addSingleArgumentFlag("--numwords", "3", "The number of words that comprise a match.");
        Map<String, String> parsedargs = clp.parse(args);
        
        Path pathA = CommandLineParser.parseFileOption(parsedargs, "--file1", "Select text file 1 to compare", "File 1 not selected", false);
        Path pathB = CommandLineParser.parseFileOption(parsedargs, "--file2", "Select text file 2 to compare", "File 2 not selected", false);
        String textA = Files.readString(pathA);
        String textB = Files.readString(pathB);
        int numwords = Integer.parseInt(parsedargs.getOrDefault("--numwords", "3"));

        System.out.println("Comparing Text A and Text B:\n");
        System.out.println("---Text A---\n" + textA + "\n");
        System.out.println("---Text B---\n" + textB + "\n");

        StringSimilarityFinder ssf = new StringSimilarityFinder();
        ssf.setMinimumMatchLength(numwords);
        List<Match> foundMatches = ssf.findConsecutiveWordMatches(textA, textB);

        if (foundMatches.isEmpty())
            System.out.println("No matches found.");
        else
        {
            System.out.println("Found " + foundMatches.size() + " match(es):");
            for (int i = 0; i < foundMatches.size(); i++)
            {
                Match match = foundMatches.get(i);
                System.out.println("\n--- Match " + (i + 1) + " ---");
                System.out.println(match);

                // Extract and print the matched text from both original strings
                String matchedText1 = textA.substring(match.start1(), match.end1() + 1);
                String matchedText2 = textB.substring(match.start2(), match.end2() + 1);

                System.out.println("  Text A extract: \"" + matchedText1 + "\"");
                System.out.println("  Text B extract: \"" + matchedText2 + "\"");
            }
            
            // Generate and print the HTML report
            System.out.println("\n\n--- HTML HIGHLIGHT REPORT ---");
            String htmlReport = ssf.generateHtmlHighlight(textA, textB, foundMatches);
            System.out.println(htmlReport);
            System.out.println("--- END OF HTML REPORT ---");
        }
    }
}

