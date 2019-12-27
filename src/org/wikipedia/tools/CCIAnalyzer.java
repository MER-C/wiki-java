/**
 *  @(#)CCIAnalyzer.java 0.02 27/12/2019
 *  Copyright (C) 2013 - 20xx MER-C
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

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.*;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import org.wikipedia.*;

/**
 *  Identifies trivial diffs in a contributor copyright investigation. Usage:
 * 
 *  <pre>
 *  Wiki enWiki = Wiki.newSession("en.wikipedia.org");
 *  CCIAnalyzer analyzer = new CCIAnalyzer();
 *  analyzer.loadWikiPage(enWiki, "Wikipedia:Contributor copyright investigations/Example");
 *  analyzer.setCullingFunction(diff -> CCIAnalyzer.whitelistCull(diff) 
 *      && CCIAnalyzer.wordCountCull(diff, wordcount, norefs));
 *  analyzer.analyzeDiffs();
 *  analyzer.writeOutput();
 *  </pre>
 * 
 *  @author MER-C
 *  @version 0.02
 */
public class CCIAnalyzer
{
    private static Wiki enWiki = Wiki.newSession("en.wikipedia.org");
    private int diffcount = 0;
    private List<String> diffshort = new ArrayList<>(1000);
    private List<String> diffs = new ArrayList<>(1000);
    private List<String> minoredits = new ArrayList<>(500);
    private Predicate<String> cullingfn;
    private String cci;
    
    /**
     *  Runs this program.
     *  @param args the command line arguments
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        Map<String, String> parsedargs = new CommandLineParser()
            .addBooleanFlag("--references", "Remove all references (aggressive)")
            .addSingleArgumentFlag("--numwords", "int", "Strings with more than this number of consecutive words are major edits.")
            .parse(args);
        
        CCIAnalyzer analyzer = new CCIAnalyzer();
        int wordcount = Integer.parseInt(parsedargs.getOrDefault("--numwords", "9"));
        boolean norefs = parsedargs.containsKey("--references");

        enWiki.setLogLevel(Level.WARNING);
        String ccipage = parsedargs.get("default");
        if (ccipage == null)
        {
            // read in from file
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
                System.exit(0);      
            List<String> lines = Files.readAllLines(fc.getSelectedFile().toPath());
            StringBuilder temp = new StringBuilder(1000000);
            for (String line : lines)
            {
                temp.append(line);
                temp.append("\n");
            }
            analyzer.loadString(temp.toString());
        }
        else
            analyzer.loadWikiPage(enWiki, ccipage);
        
        analyzer.setCullingFunction(diff -> whitelistCull(diff) && wordCountCull(diff, wordcount, norefs));
        analyzer.analyzeDiffs();
        analyzer.writeOutput();
    }
    
    public CCIAnalyzer()
    {
        
    }
    
    /**
     *  Sets the function used by this analyzer that determines whether edits
     *  are major or minor. This class defines several static functions that may
     *  be useful. Culling functions should expect entirely lower case strings only.
     * 
     *  @param culler a function String &#8594; boolean, that indicates whether 
     *  a block of text added should be counted as major
     */
    public void setCullingFunction(Predicate<String> culler)
    {
        this.cullingfn = culler;
    }
    
    /**
     *  Loads diffs to analyze from the given wiki page. Diffs must be of the
     *  format [[Special:Diff/123456]].
     *  @param wiki the wiki the page is on
     *  @param page the page to load from
     *  @throws IOException if a network error occurs
     */
    public void loadWikiPage(Wiki wiki, String page) throws IOException
    {
        cci = wiki.getPageText(List.of(page)).get(0);
        loadDiffs();
    }
    
    /**
     *  Loads diffs from the supplied String. Diffs must be of the format 
     *  [[Special:Diff/123456]].
     *  @param cci the diffs to load
     */
    public void loadString(String cci)
    {
        this.cci = cci;
        loadDiffs();
    }
    
    /**
     *  Loads and parses diffs from a loaded CCI.
     */
    private void loadDiffs()
    {
        // some HTML strings we are looking for
        // see https://en.wikipedia.org/w/api.php?action=compare&fromrev=77350972&torelative=prev
        String deltabegin = "<ins class=\"diffchange diffchange-inline\">";
        String deltaend = "</ins>";
        diffs.clear();
        diffshort.clear();
        
        // count number of diffs
        diffcount = 0;
        for (int i = cci.indexOf("[[Special:Diff/"); i >= 0; i = cci.indexOf("[[Special:Diff/", ++i))
            diffcount++;
        
        // parse the list of diffs
        int parsed = 0;
        long start = System.currentTimeMillis();
        boolean exception = false;
        for (int i = cci.indexOf("[[Special:Diff/"); i >= 0; i = cci.indexOf("[[Special:Diff/", ++i))
        {
            int xx = cci.indexOf("/", i);
            int yy = cci.indexOf("|", xx);
            int zz = cci.indexOf("]]", xx) + 2;
            String edit = cci.substring(i, zz);
            long oldid = Long.parseLong(cci.substring(xx + 1, yy));

            // Fetch diff. No plain text diffs for performance reasons, see
            // https://phabricator.wikimedia.org/T15209
            try 
            {
                String diff = enWiki.diff(Map.of("revid", oldid), Map.of("revid", Wiki.PREVIOUS_REVISION));
                parsed++;
                if (diff == null) // RevisionDeleted revision
                    continue;
                // Condense deltas to avoid problems like https://en.wikipedia.org/w/index.php?title=&diff=prev&oldid=486611734
                diff = diff.toLowerCase(enWiki.locale());
                diff = diff.replace(deltaend + " " + deltabegin, " ");
                diffshort.add(edit);
                diffs.add(diff);
                exception = false;
            }
            catch (IOException ex)
            {
                System.err.println();
                System.out.flush();
                ex.printStackTrace();
                
                // bail if two IOExceptions are thrown consecutively
                if (exception)
                {
                    System.err.printf("\033[31;1mBailing due to consecutive IOExceptions!\033[0m\n");
                    break;
                }
                System.err.printf("\033[31;1mSkipping oldid %s\033[0m\n", oldid);
                exception = true;
                continue;
            }
            catch (UnknownError err)
            {
                // HACK: RevisionDeleted revision or deleted article.
                continue;
            }
            
            long now = System.currentTimeMillis();
            double percent = 100.0 * parsed / diffcount;
            int elapsed = (int)((now - start) / 1000.0);
            int projected = elapsed * diffcount / parsed;
            int eta = projected - elapsed;
            System.err.printf("\r\033[K%d of %d diffs loaded (%2.2f%%, %d:%02d remaining)", 
                parsed, diffcount, percent, eta / 60, eta % 60);
        }
    }
    
    /**
     *  Culls diffs using the currently set culling function. This method does 
     *  not modify the diff text held by this object and therefore multiple 
     *  culling functions may be experimented with to see which works best. 
     *  Culling is not cumulative unless you save the wikipage in between and 
     *  reload the diffs.
     */
    public void analyzeDiffs()
    {
        minoredits.clear();
        
        // some HTML strings we are looking for
        // see https://en.wikipedia.org/w/api.php?action=compare&fromrev=77350972&torelative=prev
        String diffaddedbegin = "<td class=\"diff-addedline\">";
        String diffaddedend = "</td>";
        String deltabegin = "<ins class=\"diffchange diffchange-inline\">";
        String deltaend = "</ins>";
        
        for (int i = 0; i < diffs.size(); i++)
        {
            String diff = diffs.get(i);
            // If the diff is empty (see https://en.wikipedia.org/w/index.php?diff=343490272)
            // it will not contain diffaddedbegin -> default major to true.
            boolean major = true;
            // It is easy to strip the HTML.
            for (int j = diff.indexOf(diffaddedbegin); j >= 0; j = diff.indexOf(diffaddedbegin, j))
            {
                int y2 = diff.indexOf(diffaddedend, j);
                String addedline = diff.substring(j + diffaddedbegin.length(), y2);
                addedline = addedline.replaceFirst("^<div>", "");
                addedline = addedline.replace("</div>;", "");
                if (addedline.contains(deltabegin))
                {
                    for (int k = addedline.indexOf(deltabegin); k >= 0; k = addedline.indexOf(deltabegin, k))
                    {
                        int y3 = addedline.indexOf(deltaend, k);
                        String delta = addedline.substring(k + deltabegin.length(), y3);
                        major = cullingfn.test(delta);
                        if (major)
                            break;
                        k = y3;
                    }
                }
                else
                    major = cullingfn.test(addedline);
                if (major)
                    break;
                j = y2;
            }
            if (!major)
                minoredits.add(diffshort.get(i));
            
        }
    }
    
    /**
     *  Returns the list of edits flagged as minor.
     *  @return (see above)
     */
    public List<String> getMinorEdits()
    {
        return new ArrayList<>(minoredits);
    }
    
    /**
     *  Writes the CCI with trivial diffs removed to standard output.
     */
    public void writeOutput()
    {
        System.err.println();
        StringBuilder ccib = new StringBuilder(cci);
        
        // remove all minor edits from the CCI
        for (String minoredit : minoredits)
        {
            int x = ccib.indexOf(minoredit);
            ccib.delete(x, x + minoredit.length());
            
            // we don't care about minor edits that add less than 500 chars
            int yy = minoredit.indexOf('|') + 2;
            int zz = minoredit.indexOf(")]]");
            int size = Integer.parseInt(minoredit.substring(yy, zz));
            if (size > 499)
                System.out.println(minoredit);
        }
        System.out.println("----------------------");
        
        // clean up output CCI listing
        String[] articles = ccib.toString().split("\\n");
        StringBuilder cleaned = new StringBuilder();
        int removedarticles = 0;
        Pattern pattern = Pattern.compile(".*edits?\\):\\s+");
        for (String article : articles)
        {
            // remove articles where all diffs are trivial
            if (pattern.matcher(article).matches())
            {
                removedarticles++;
                continue;
            }
            cleaned.append(article);
            cleaned.append("\n");
        }
        System.out.println(cleaned);
        System.err.printf("%d of %d diffs and %d articles removed.", minoredits.size(), 
            diffcount, removedarticles);
    }
    
    /**
     *  Flags known not copyvio strings as minor edits. Currently whitelisted
     *  strings arise from AFDs, RFDs, and (BLP) PRODs.
     *  @param delta the change to check
     *  @return true if the edit is major, false if it matches the whitelist
     */
    public static boolean whitelistCull(String delta)
    {
        for (String commonality : whitelist)
            if (delta.contains(commonality))
                return false;
        return true;
    }
    
    private static final List<String> whitelist = List.of(
        // AFD
        "{{article for deletion/dated|",
        "please do not remove or change this afd message",
        "once discussion is closed, please place on talk page",
        "end of afd message, feel free to edit beyond this point",
        // RFD
        "end of rfd message. don't edit anything above here",
        "don't add anything after this line unless you're drafting",
        "{{proposed deletion/dated|", // {{subst:prod}}
        "{{prod blp/dated|", // {{subst:prod blp}}
        "{{infobox ");
    
    /**
     *  Determines whether a given delta is a major edit. A "major edit" is
     *  defined as something that adds more than the specified number of words.
     * 
     *  <p>
     *  A higher word count is a more aggressive setting. The default of 9 is 
     *  sensible as in it has few false negatives. The use of reference removal
     *  is an aggressive culling option that should not be used unless it has
     *  been verified that the CCIed editor does not dump large quotes into
     *  references.
     * 
     *  @param delta the delta to check
     *  @param wordcount label all edits that add no more than this many words
     *  as minor
     *  @param removerefs remove references before counting words
     *  @return whether this is a major edit
     */
    public static boolean wordCountCull(String delta, int wordcount, boolean removerefs)
    {
        // replace wikilinks, categories and files with their descriptions
        StringBuilder temp = new StringBuilder(delta);
        for (int i = temp.indexOf("[["); i >= 0; i = temp.indexOf("[["))
        {
            int j = temp.indexOf("]]", i);
            if (j < 0) // unbalanced brackets
                break;
            List<String> parsedlink = WikitextUtils.parseWikilink(temp.substring(i, j + 2));
            if (parsedlink.get(0).length() > 100)
                // something has gone wrong here
                break;
            else if (enWiki.namespace(parsedlink.get(0)) == Wiki.CATEGORY_NAMESPACE)
                // I'm not interested in the category sortkey
                temp.delete(i, j + 2);
            else
                temp.replace(i, j + 2, parsedlink.get(1));
        }
        
        // decode() the delta
        String delta2 = temp.toString().replace("&lt;", "<");
        delta2 = delta2.replace("&gt;", ">");
        
        if (removerefs)
            delta2 = removeReferences(delta2);
        StringTokenizer tk = new StringTokenizer(delta2, "<>{}|=");
        while (tk.hasMoreTokens())
        {
            String token =  tk.nextToken();
            if (token.split("\\s").length > wordcount)
                return true;
        }
        return false;
    }
    
    /**
     *  Removes references from the given wikitext.
     *  @param wikitext wikitext for which references should be removed
     *  @return the wikitext with references removed
     */
    public static String removeReferences(String wikitext)
    {
        // Requires extension Cite, and therefore not in WikitextUtils
        for (int refbegin = wikitext.indexOf("<ref"); refbegin >= 0; refbegin = wikitext.indexOf("<ref", refbegin))
        {
            int refend1 = wikitext.indexOf("/>", refbegin); // length: 2
            int refend2 = wikitext.indexOf("</ref>", refbegin); // length: 6
            if (refend1 >= 0 && refend2 >= 0)
            {
                int refend = Math.min(refend1 + 2, refend2 + 6);
                wikitext = wikitext.substring(0, refbegin) + wikitext.substring(refend);
            }
            else if (refend1 >= 0)
                wikitext = wikitext.substring(0, refbegin) + wikitext.substring(refend1 + 2);
            else if (refend2 >= 0)
                wikitext = wikitext.substring(0, refbegin) + wikitext.substring(refend2 + 6);
            else
                refbegin++;
        }
        return wikitext;
    }
    
    /**
     *  This function flags unnumbered list items that begin with an internal
     *  or external link as minor edits. I'm not sure whether this is an 
     *  aggressive option.
     *  @param delta the delta to check
     *  @return whether this is a major edit
     */
    public static boolean listItemCull(String delta)
    {
        return !delta.matches("^\\*\\s?\\[.+");
    }
    
    /**
     *  This function flags the addition of files or categories as minor edits.
     *  This is a slightly aggressive option - I have seen copyvios in image
     *  captions, but this is rare.
     *  @param delta the delta to check
     *  @return whether this is a major edit
     */
    public static boolean fileAdditionCull(String delta)
    {
        return !delta.matches("^\\[\\[(?:file|image|category).+");
    }
}
