/**
 *  @(#)AllWikiLinksearch.java 0.04 06/07/2023
 *  Copyright (C) 2011 - 2023 MER-C
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

import java.awt.GraphicsEnvironment;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.*;
import javax.swing.*;
import org.wikipedia.*;

/**
 *  Searches Wikimedia wikis for a given external link. Back-end for the 
 *  <a href="https://wikipediatools.appspot.com">Cross-wiki linksearch tool</a>.
 *  Consider:
 *  <ul>
 *      <li>COIBot poking the domain AND
 *      <li>Using Luxo's cross-wiki contributions to undo revisions by spammers
 *  </ul>
 *  before running this program. 
 * 
 *  @author MER-C
 *  @version 0.04
 */
public class AllWikiLinksearch
{    
    // predefined wiki sets
    private static final WMFWikiFarm sessions = WMFWikiFarm.instance();
    
    /**
     *  The top 25 Wikipedias (by number of admins), namely { "en", "de", "fr", 
     *  "it", "pl", "ru", "sv", "zh", "es", "pt", "uk", "id", "no", "ja", "fa", 
     *  "ta", "nl", "et", "fi", "he", "cs", "ca", "ko", "hu", "da" }.wikipedia.org.
     */
    public static final List<WMFWiki> TOP25;
    
    /**
     *  The top 50 Wikipedias (by number of admins), namely everything in 
     *  {@link #TOP25}, plus { "bg", "ar", "el", "sl", "tr", "th", "is", "vi", 
     *  "simple", "sw", "sr", "ro", "uz", "la", "eo", "cy", "ms", "az", "ml", 
     *  "kk", "nn", "bn", "lv", "hr", "af" }.wikipedia.org.
     */
    public static final List<WMFWiki> TOP50;
    
    /**
     *  Major Wikimedia projects prone to spam, namely { "en", "de", "fr", "it" }.
     *  { "wikipedia", "wiktionary", "wikibooks", "wikiquote", "wikivoyage" } .org, 
     *  plus Wikimedia Commons, Meta, mediawiki.org and WikiData.
     */
    public static final List<WMFWiki> MAJOR_WIKIS;
    
    /**
     *  Initializes wiki groups.
     */
    static
    {
        List<String> temp = List.of(
            // top 25 Wikipedias (by number of admins, see 
            "en", "de", "fr", "it", "pl", "ru", "sv", "zh", "es", "pt", "uk", 
            "id", "no", "ja", "fa", "ta", "nl", "et", "fi", "he", "cs", "ca", 
            "ko", "hu", "da",
            // 25-50
            "bg", "ar", "el", "sl", "tr", "th", "is", "vi", "simple", "sw", "sr", 
            "ro", "uz", "la", "eo", "cy", "ms", "az", "ml", "kk", "nn", "bn", 
            "lv", "hr", "af");
        
        List<WMFWiki> wikilist = new ArrayList<>();
        for (String lang : temp)
            wikilist.add(sessions.sharedSession(lang + ".wikipedia.org"));
        TOP50 = Collections.unmodifiableList(wikilist);
        TOP25 = TOP50.subList(0, 25);

        wikilist = new ArrayList<>();
        for (String lang : temp.subList(0, 4))
        {
            wikilist.add(sessions.sharedSession(lang + ".wikipedia.org"));
            wikilist.add(sessions.sharedSession(lang + ".wiktionary.org"));
            wikilist.add(sessions.sharedSession(lang + ".wikibooks.org"));
            wikilist.add(sessions.sharedSession(lang + ".wikiquote.org"));
            wikilist.add(sessions.sharedSession(lang + ".wikivoyage.org"));
        }
        wikilist.add(sessions.sharedSession("meta.wikimedia.org"));
        wikilist.add(sessions.sharedSession("commons.wikimedia.org"));
        wikilist.add(sessions.sharedSession("www.mediawiki.org"));
        wikilist.add(sessions.sharedSession("www.wikidata.org"));
        MAJOR_WIKIS = Collections.unmodifiableList(wikilist);
    }
    
    /**
     *  Runs this program. In offline mode, this searches all wikis.
     *  @param args command line arguments (see code for documentation).
     *  @throws IOException if a filesystem error occurs
     */
    public static void main(String[] args) throws IOException
    {
        // parse command line options
        Map<String, String> parsedargs = new CommandLineParser()
            .synopsis("org.wikipedia.tools.AllWikiLinksearch", "[options] domains")
            .description("Searches Wikimedia projects for links.")
            .addHelp()
            .addVersion("AllWikiLinksearch v0.04\n" + CommandLineParser.GPL_VERSION_STRING)
            .addSingleArgumentFlag("--numthreads", "n", "Use n threads.")
            .addSingleArgumentFlag("--domainlist", "domains.txt", "Search for this list of domains")
            .addSingleArgumentFlag("--wikiset", "TOP25", "Search this list of wikis, valid sets are TOP20, TOP40, MAJOR, default: ALL")
            .addSingleArgumentFlag("--outfile", "example.txt", "Write output to example.txt")
            .addSection("A dialog box will pop up if domain is not specified.")
            .parse(args);
        
        int threads = Integer.parseInt(parsedargs.getOrDefault("--numthreads", "1"));
        Path outfile = CommandLineParser.parseFileOption(parsedargs, "--outfile", "Enter output file", "Error: Output file not specified", true);
        
        List<String> domains = new ArrayList<>();
        String domainlist = parsedargs.get("--domainlist");
        String domain = parsedargs.get("default");
        if (domainlist != null)
        {
            Path path = Paths.get(domainlist);
            domains.addAll(Files.readAllLines(path));
        }
        else if (!GraphicsEnvironment.isHeadless())
            domain = JOptionPane.showInputDialog(null, "Enter domain(s) to search", "All wiki linksearch", JOptionPane.QUESTION_MESSAGE);
        if (domain != null)
            domains.addAll(List.of(domain.split(" ")));
        if (domains.isEmpty())
        {
            System.out.println("No domain(s) specified!");
            System.exit(0);
        }
        
        List<WMFWiki> wikis = switch (parsedargs.getOrDefault("--wikiset", "x"))
        {
            case "TOP25" -> TOP25;
            case "TOP50" -> TOP50;
            case "MAJOR" -> MAJOR_WIKIS;
            default -> sessions.getSiteMatrix();
        };
        
        // output results
        try (BufferedWriter out = Files.newBufferedWriter(outfile))
        {
            for (String domain2 : domains)
            {
                Map<Wiki, List<String[]>> results = crossWikiLinksearch(Integer.MAX_VALUE, threads, domain2, wikis, false);
                out.write("==" + domain2 + "==\n");
                for (Map.Entry<Wiki, List<String[]>> result : results.entrySet())
                {
                    Wiki wiki = result.getKey();
                    List<String[]> links = result.getValue();
                    StringBuilder temp = new StringBuilder("=== Results for ");
                    temp.append(wiki.getDomain());
                    temp.append(" ===\n");
                    if (links == null)
                    {
                        temp.append("<span style=\"color: red\">An error occurred!</span>\n\n");
                        out.write(temp.toString());
                        continue;
                    }
                    int linknumber = links.size();
                    if (linknumber != 0)
                    {
                        temp.append(ExternalLinks.of(wiki).linksearchResultsToHTML(links, domain));
                        out.write(temp.toString());
                    }
                }
                out.write("\n");
            }
        }
    }
    
    /**
     *  Performs a cross-wiki linksearch.
     * 
     *  @param querylimit sets a query limit, passed to {@link Wiki#setQueryLimit(int)}
     *  @param threads use this many threads. If <var>querylimit</var> is 
     *  not Integer.MAX_VALUE this is forced to 1. If greater than 1, this overwrites 
     *  the system property <var>java.util.concurrent.ForkJoinPool.common.parallelism</var>.
     *  @param domain the domain to search
     *  @param wikis the wikis to search
     *  @param mailto include mailto links?
     *  @param ns restrict to the given namespaces
     *  @return the linksearch results, as in wiki &#8594; results, or null if an
     *  IOException occurred
     */
    public static Map<Wiki, List<String[]>> crossWikiLinksearch(int querylimit, 
        int threads, String domain, Collection<? extends Wiki> wikis, boolean mailto, 
        int... ns)
    {
        Stream<? extends Wiki> stream = wikis.stream();
        // set concurrency if desired
        if (querylimit == Integer.MAX_VALUE && threads > 1)
        {
            System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "" + threads);
            stream = stream.parallel();
        }
        Map<Wiki, List<String[]>> ret = stream.collect(Collectors.toMap(Function.identity(), wiki ->
        {
            wiki.setMaxLag(-1);
            wiki.setQueryLimit(querylimit);
            try
            {
                List<String[]> temp = wiki.linksearch("*." + domain, null, ns);
                if (mailto)
                {
                    List<String[]> temp2 = wiki.linksearch("*." + domain, "mailto", ns);
                    temp.addAll(temp2);
                }
                return temp;
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
                return null;
            }
        }, (wiki1, wiki2) -> { throw new RuntimeException("Duplicate wikis!"); }, TreeMap::new));

        return ret;
    }
}
