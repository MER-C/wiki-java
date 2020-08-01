/**
 *  @(#)CCIAnalyzer.java 0.05 25/07/2020
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
import java.util.function.*;
import java.util.logging.*;
import java.util.regex.*;
import javax.swing.JFileChooser;
import org.wikipedia.*;

/**
 *  Identifies trivial diffs in a contributor copyright investigation. Usage:
 * 
 *  <pre>
 *  Wiki enWiki = Wiki.newSession("en.wikipedia.org");
 *  CCIAnalyzer analyzer = new CCIAnalyzer();
 *  analyzer.loadWikiPage(enWiki, "Wikipedia:Contributor copyright investigations/Example");
 *  analyzer.setCullingFunction(diff -&gt; CCIAnalyzer.whitelistCull(diff) 
 *      &amp;&amp; CCIAnalyzer.wordCountCull(diff, wordcount));
 *  analyzer.analyzeDiffs();
 *  analyzer.writeOutput();
 *  </pre>
 * 
 *  @author MER-C
 *  @version 0.05
 */
public class CCIAnalyzer
{
    private Wiki wiki;
    private int diffcount = 0;
    private int baseremovedarticles = 0;
    private int baseremoveddiffs = 0;
    private List<String> diffshort, diffs, minoredits;
    private Predicate<String> cullingfn;
    private Function<String, String> filterfn;
    private Predicate<String> titlefn;
    private String cci;
    
    // Default size addition limit for CCIs
    private static int MAX_EDIT_SIZE = 150;
    
    // lazy initialized stuff
    private static Pattern targs_pattern;
       
    /**
     *  Runs this program.
     *  @param args the command line arguments
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        Map<String, String> parsedargs = new CommandLineParser()
            .synopsis("CCIAnalyzer", "[options] \"[CCI page]\"")
            .description("Filters minor edits from [[Wikipedia:Contributor copyright investigations]].")
            .addBooleanFlag("--references", "Remove all references (aggressive)")
            .addBooleanFlag("--lists", "Remove all list items (aggressive)")
            .addBooleanFlag("--files", "Remove all file additions (aggressive)")
            .addBooleanFlag("--extlinks", "Remove all external links")
            .addBooleanFlag("--targs", "Remove short template arguments")
            .addBooleanFlag("--comments", "Remove all HTML comments (aggressive)")
            .addBooleanFlag("--listpages", "Removes all list pages (aggressive)")
            .addSingleArgumentFlag("--numwords", "int", "Strings with more than this number of consecutive words are major edits.")
            .addVersion("CCIAnalyzer v0.05\n" + CommandLineParser.GPL_VERSION_STRING)
            .addHelp()
            .parse(args);
        
        CCIAnalyzer analyzer = new CCIAnalyzer();
        int wordcount = Integer.parseInt(parsedargs.getOrDefault("--numwords", "9"));

        Wiki enWiki = Wiki.newSession("en.wikipedia.org");
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
            analyzer.loadString(enWiki, temp.toString());
        }
        else
            analyzer.loadWikiPage(enWiki, ccipage);

        Predicate<String> cullingfn = diff -> whitelistCull(diff) && 
            analyzer.wordCountCull(diff, wordcount) && tableCull(diff);
        if (parsedargs.containsKey("--lists"))
            cullingfn = cullingfn.and(CCIAnalyzer::listItemCull);
        if (parsedargs.containsKey("--files"))
            cullingfn = cullingfn.and(CCIAnalyzer::fileAdditionCull);
        analyzer.setCullingFunction(cullingfn);

        Function<String, String> filterfn = Function.identity();
        if (parsedargs.containsKey("--references"))
            filterfn = filterfn.andThen(CCIAnalyzer::removeReferences);
        if (parsedargs.containsKey("--targs"))
            filterfn = filterfn.andThen(CCIAnalyzer::removeTemplateArguments);
        if (parsedargs.containsKey("--extlinks"))
            filterfn = filterfn.andThen(CCIAnalyzer::removeExternalLinks);
        if (parsedargs.containsKey("--comments"))
            filterfn = filterfn.andThen(WikitextUtils::removeComments);
        analyzer.setFilteringFunction(filterfn);

        Predicate<String> titlefn = CCIAnalyzer::removeDisambiguationPages;
        if (parsedargs.containsKey("--listpages"))
            titlefn = titlefn.and(CCIAnalyzer::removeListPages);
        analyzer.setTitleFunction(titlefn);
        
        analyzer.analyzeDiffs();
        analyzer.writeOutput();
    }
    
    /**
     *  Constructs a new analyzer object. The default functions are:
     *  
     *  <ul>
     *  <li>title: {@link #removeDisambiguationPages(String) }
     *  <li>filter: {@code Function.identity() }
     *  <li>culling: {@code {@link #whitelistCull(String) whitelistCull(diff)} && 
     *      {@link #wordcountCull(String, int) wordcountCull(diff, 10) } && 
     *      {@link #tableCull(String) tableCull(diff)}}
     *  </ul>
     */
    public CCIAnalyzer()
    {
        titlefn = CCIAnalyzer::removeDisambiguationPages;
        filterfn = Function.identity();
        cullingfn = diff -> whitelistCull(diff) && wordCountCull(diff, 10) && tableCull(diff);
        
        diffshort = new ArrayList<>(1000);
        diffs = new ArrayList<>(1000);
        minoredits = new ArrayList<>(500);
    }
    
    /**
     *  Sets the function used by this analyzer that determines whether edits
     *  are major or minor. This class defines several static functions that may
     *  be useful. Culling functions should expect entirely lower case strings only.
     * 
     *  @param culler a function String &#8594; boolean, that indicates whether 
     *  a block of text added should be counted as major; must not be null
     *  @since 0.02
     */
    public void setCullingFunction(Predicate<String> culler)
    {
        this.cullingfn = Objects.requireNonNull(culler);
    }
    
    /**
     *  Sets the function used by this analyzer that removes text from changes
     *  before determining whether they are major or minor. This class defines 
     *  several static functions that may be useful. Filtering functions should
     *  expect entirely lower case strings only.
     * 
     *  <p>
     *  {@link org.wikipedia.WikitextUtils} defines a function {@link 
     *  WikitextUtils#removeComments}. This function is useful for filtering but
     *  it is a semi-aggressive option. Please verify that an editor has not
     *  pasted copyvios in HTML comments before using it - yes, I have seen this 
     *  before!
     * 
     *  @param filter a function String &#8594; String that removes not major
     *  content from a block of text; must not be null
     *  @since 0.03
     */
    public void setFilteringFunction(Function<String, String> filter)
    {
        this.filterfn = Objects.requireNonNull(filter);
    }
    
    /**
     *  Sets the function to be used that filters entire pages from the CCI
     *  before diffs are loaded. Title functions should expect titles as is. If 
     *  you change the title function, you need to reload the page.
     *  @param titlefn a function that returns true if a title is to be retained
     *  @since 0.04
     */
    public void setTitleFunction(Predicate<String> titlefn)
    {
        this.titlefn = Objects.requireNonNull(titlefn);
    }
    
    /**
     *  Loads diffs to analyze from the given wiki page. Diffs must be of the
     *  format [[Special:Diff/123456]].
     *  @param wiki the wiki the page is on
     *  @param page the page to load from
     *  @throws IOException if a network error occurs
     *  @since 0.02
     */
    public void loadWikiPage(Wiki wiki, String page) throws IOException
    {
        cci = wiki.getPageText(List.of(page)).get(0);
        this.wiki = wiki;
        filterPage();
        loadDiffs();
    }
    
    /**
     *  Loads diffs from the supplied String. Diffs must be of the format 
     *  [[Special:Diff/123456]].
     *  @param wiki the wiki to load diffs from
     *  @param cci the diffs to load
     *  @since 0.02
     */
    public void loadString(Wiki wiki, String cci)
    {
        this.cci = cci;
        this.wiki = wiki;
        filterPage();
        loadDiffs();
    }
    
    /**
     *  Filters entire articles from a CCI before diffs are loaded.
     */
    private void filterPage()
    {
        // count number of diffs
        baseremovedarticles = 0;
        baseremoveddiffs = 0;
        diffcount = 0;
        for (int i = cci.indexOf("[[Special:Diff/"); i >= 0; i = cci.indexOf("[[Special:Diff/", ++i))
            diffcount++;

        StringBuilder sb = new StringBuilder();
        for (String line : cci.split("\n"))
        {
            if (!line.startsWith("*[[:") && !line.startsWith("*'''N''' [[:"))
            {
                sb.append(line);
                sb.append("\n");
                continue;
            }
            int a = line.indexOf("[[:");
            int b = line.indexOf("]]");
            if (titlefn.test(line.substring(a + 3, b)))
            {
                sb.append(line);
                sb.append("\n");
            }
            else
            {
                baseremovedarticles++;
                // count number of removed diffs
                for (int i = line.indexOf("[[Special:Diff/"); i >= 0; i = line.indexOf("[[Special:Diff/", ++i))
                    baseremoveddiffs++;
            }
        }
        cci = sb.toString();
    }
    
    /**
     *  Loads and parses diffs from a loaded CCI.
     *  @since 0.02
     */
    private void loadDiffs()
    {
        // some HTML strings we are looking for
        // see https://en.wikipedia.org/w/api.php?action=compare&fromrev=77350972&torelative=prev
        String deltabegin = "<ins class=\"diffchange diffchange-inline\">";
        String deltaend = "</ins>";
        diffs.clear();
        diffshort.clear();
        
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
                String diff = wiki.diff(Map.of("revid", oldid), Map.of("revid", Wiki.PREVIOUS_REVISION));
                parsed++;
                if (diff == null) // RevisionDeleted revision
                    continue;
                // Condense deltas to avoid problems like https://en.wikipedia.org/w/index.php?title=&diff=prev&oldid=486611734
                diff = diff.toLowerCase(wiki.locale());
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
                parsed, diffcount - baseremoveddiffs, percent, eta / 60, eta % 60);
        }
    }
    
    /**
     *  Culls diffs using the currently set culling function. This method does 
     *  not modify the diff text held by this object and therefore multiple 
     *  culling functions may be experimented with to see which works best. 
     *  Culling is not cumulative unless you save the wikipage in between and 
     *  reload the diffs.
     *  @since 0.02
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
                addedline = addedline.replaceAll("</div>.?$", "");
                if (addedline.contains(deltabegin))
                {
                    for (int k = addedline.indexOf(deltabegin); k >= 0; k = addedline.indexOf(deltabegin, k))
                    {
                        // TODO: should strip all wikilinks here instead of in word count culling
                        int y3 = addedline.indexOf(deltaend, k);
                        String delta = addedline.substring(k + deltabegin.length(), y3);
                        delta = delta.replace("&lt;", "<").replace("&gt;", ">");
                        major = cullingfn.test(filterfn.apply(delta));
                        if (major)
                            break;
                        k = y3;
                    }
                }
                else
                {
                    addedline = addedline.replace("&lt;", "<").replace("&gt;", ">");                
                    major = cullingfn.test(filterfn.apply(addedline));
                }
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
     *  @since 0.02
     */
    public List<String> getMinorEdits()
    {
        return new ArrayList<>(minoredits);
    }
    
    /**
     *  Writes the CCI with trivial diffs removed to standard output.
     *  @since 0.02
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
        System.err.printf("%d of %d diffs and %d articles removed.%n", baseremoveddiffs + minoredits.size(), 
            diffcount, baseremovedarticles + removedarticles);
    }
    
    /**
     *  Flags known not copyvio strings as minor edits. Currently whitelisted
     *  strings arise from AFDs, RFDs, and (BLP) PRODs.
     *  @param delta the change to check
     *  @return true if the edit is major, false if it matches the whitelist
     *  @since 0.02
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
     *  A higher word count is a more aggressive setting. The default of 9 is 
     *  sensible as in it has few false negatives. 
     *
     *  <p>
     *  <b>Warning:</b> this method is very good at removing song lyrics!
     *  Be careful.
     * 
     *  @param delta the delta to check
     *  @param wordcount label all edits that add no more than this many words
     *  as minor
     *  @return whether this is a major edit
     *  @since 0.02
     */
    public boolean wordCountCull(String delta, int wordcount)
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
            else if (wiki.namespace(parsedlink.get(0)) == Wiki.CATEGORY_NAMESPACE)
                // I'm not interested in the category sortkey
                temp.delete(i, j + 2);
            else
                temp.replace(i, j + 2, parsedlink.get(1));
        }
        
        StringTokenizer tk = new StringTokenizer(temp.toString(), "<>{}|=");
        while (tk.hasMoreTokens())
        {
            String token =  tk.nextToken();
            String[] words = token.split("\\s");
            int count = 0;
            // mop up some non-words such as dashes and stray bits of wiki markup e.g. ''' Test '''
            for (String word : words)
                if (!word.matches("^\\p{Punct}*$"))
                    count++;
            if (count > wordcount)
                return true;
        }
        return false;
    }
    
    /**
     *  Removes references from the given wikitext. The use of reference removal
     *  is an aggressive filtering option that should not be used unless it has
     *  been verified that the CCIed editor does not dump large quotes into
     *  references.
     * 
     *  @param wikitext wikitext for which references should be removed
     *  @return the wikitext with references removed
     *  @since 0.02
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
     *  This function removes template arguments that are less than 150 
     *  characters long. (150 characters is the default length of a major 
     *  contribution for CCI purposes). This is a fairly safe culling function. 
     *  The end of the argument is defined by the next instance of "|", "}" or 
     *  the end of the string and therefore may not be the actual end of the 
     *  argument for parsing purposes. The list of template arguments is 
     *  hardcoded.
     *  @param wikitext the wikitext to process
     *  @return the processed wikitext
     *  @since 0.05
     */
    public static String removeTemplateArguments(String wikitext)
    {
        if (targs_pattern == null)
        {
            targs_pattern = Pattern.compile("\\|\\s*(" + 
                // citation templates
                "archiveurl|archive-url|url|title|date|accessdate|access-date|archivedate|" +
                "archive-date|last|first|work|author|publisher|" +
                // infobox officeholder
                "office\\d?|alma_mater|appointer\\d?|death_date)\\s*=.{0," + MAX_EDIT_SIZE + "}?(\\||$|\\})");
        }
        Matcher matcher = targs_pattern.matcher(wikitext);
        while (matcher.find())
        {
            wikitext = matcher.replaceAll("|");
            matcher.reset(wikitext);
        }
        return wikitext;
    }
    
    /**
     *  This function flags unnumbered list items that begin with an internal
     *  or external link as minor edits. Of all the culling options, this one
     *  is probably the most aggressive - links may be followed by long
     *  descriptions.
     *
     *  @param delta the delta to check
     *  @return whether this is a major edit
     *  @since 0.02
     */
    public static boolean listItemCull(String delta)
    {
        // '* bit matches wikitext formatting for bold or italic formatting
        return !delta.matches("^[#\\*]\\s?'*\\s?\\[.+");
    }
    
    /**
     *  This function flags the addition of files or categories as minor edits.
     *  This should be fairly safe - edits are flagged as minor only if file
     *  captions are less than 150 characters long.
     *  @param delta the delta to check
     *  @return whether this is a major edit
     *  @since 0.02
     */
    public static boolean fileAdditionCull(String delta)
    {
        if (delta.matches("^\\[\\[\\s?(?:file|image).+"))
        {
            String[] temp = delta.split("\\|");
            // The first item will always be the filename.
            // The second and third items may be image parameters for MediaWiki 
            // (always short and less than 10 characters). They are optional.
            // The last item(s) should be the caption (although it may be broken up by
            // wikilinks and template arguments).
            int count = 0;
            for (int i = 1; i < temp.length; i++)
            {
                int length = temp[i].length();
                if (count > 0 || length > 15)
                    count += length;
                if (count > MAX_EDIT_SIZE)
                    return true;
            }
            return false;
        }
        if (delta.matches("^\\[\\[\\scategory:.+"))
            return false;
        return true;
    }
    
    /**
     *  This function flags the beginning of tables. Safe, but not seen very often.
     *  Detecting table rows is hard in this context - they start with | which is 
     *  not a very specific pattern.
     *  @param delta the delta to check
     *  @return whether this is a major edit
     *  @since 0.02
     */
    public static boolean tableCull(String delta)
    {
        return !delta.startsWith("{|");
    }
    
    /**
     *  Removes external links and their captions from the supplied string.
     *  Should be fairly safe.
     *  @param delta the string to strip external links from
     *  @return the string removed of external links
     *  @since 0.03 
     */
    public static String removeExternalLinks(String delta)
    {
        return delta.replaceAll("\\[https?://.+\\]", "");
    }
    
    /**
     *  Removes disambiguation pages based on their title only.
     *  @param title the title to test
     *  @return false if it contains {@code " (disambiguation)"}, true otherwise
     *  @since 0.04
     */
    public static boolean removeDisambiguationPages(String title)
    {
        return !title.contains(" (disambiguation)");
    }
    
    /**
     *  Removes list pages based on their title only. Somewhat aggressive, one
     *  needs to check whether the list has extended descriptions in it or is a 
     *  list of TV episodes.
     *  @param title the title to test
     *  @return false if it starts with {@code "List of "}, true otherwise
     *  @since 0.04
     */
    public static boolean removeListPages(String title)
    {
        return !title.startsWith("List of ");
    }
}
