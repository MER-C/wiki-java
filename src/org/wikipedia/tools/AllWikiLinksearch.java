/**
 *  @(#)AllWikiLinksearch.java 0.03 26/12/2016
 *  Copyright (C) 2011 - 2017 MER-C
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.*;
import javax.swing.*;
import org.wikipedia.*;

/**
 *  Searches all Wikimedia wikis for a given external link. Consider:
 *  <ul>
 *      <li>Running the <a href="https://wikipediatools.appspot.com">Cross-wiki
 *      linksearch tool</a>
 *      <li>COIBot poking the domain AND
 *      <li>Using Luxo's cross-wiki contributions to undo revisions by spammers
 *  </ul>
 *  before running this program. This also provides the backend for the above
 *  cross-wiki linksearch tool.
 * 
 *  @author MER-C
 *  @version 0.03
 */
public class AllWikiLinksearch
{    
    // predefined wiki sets
    
    /**
     *  The top 20 Wikimedia projects, namely { "en", "de", "fr", "nl", "it", 
     *  "pl", "es", "ru", "ja", "pt", "zh", "sv", "vi", "uk", "ca", "no", "fi", 
     *  "cs", "hu", "fa" }.wikipedia.org.
     */
    public static final Wiki[] TOP20 = new Wiki[20];
    
    /**
     *  The top 40 Wikimedia projects, namely everything in {@link #TOP20}, plus
     *  { "ro", "ko", "ar", "tr", "id", "sk", "eo", "da", "sr", "kk", "lt", 
     *  "ms", "he", "bg", "eu", "sl", "vo", "hr", "war", "hi" }.wikipedia.org.
     */
    public static final Wiki[] TOP40 = new Wiki[40];
    
    /**
     *  Major Wikimedia projects prone to spam, namely { "en", "de", "fr" }.
     *  { "wikipedia", "wiktionary", "wikibooks", "wikiquote", "wikivoyage" }
     *  .org, plus Wikimedia Commons, Meta, mediawiki.org and WikiData.
     */
    public static final Wiki[] MAJOR_WIKIS = new Wiki[19];
    
    /**
     *  Initializes wiki groups.
     */
    static
    {
        String[] temp = { 
            // top 20 Wikipedias
            "en", "de", "fr", "nl", "it", "pl", "es", "ru", "ja",  "pt",
            "zh", "sv", "vi", "uk", "ca", "no", "fi", "cs", "hu",  "fa",
            // 20-40
            "ro", "ko", "ar", "tr", "id", "sk", "eo", "da", "sr",  "kk",
            "lt", "ms", "he", "bg", "eu", "sl", "vo", "hr", "war", "hi" };
        
        for (int i = 0; i < temp.length; i++)
        {
            TOP40[i] = Wiki.createInstance(temp[i] + ".wikipedia.org");
            TOP40[i].setMaxLag(-1);
        }
        System.arraycopy(TOP40, 0, TOP20, 0, 20);

        temp = new String[] { "en", "de", "fr" };
        for (int i = 0; i < temp.length; i++)
        {
            MAJOR_WIKIS[5 * i    ] = Wiki.createInstance(temp[i] + ".wikipedia.org");
            MAJOR_WIKIS[5 * i + 1] = Wiki.createInstance(temp[i] + ".wiktionary.org");
            MAJOR_WIKIS[5 * i + 2] = Wiki.createInstance(temp[i] + ".wikibooks.org");
            MAJOR_WIKIS[5 * i + 3] = Wiki.createInstance(temp[i] + ".wikiquote.org");
            MAJOR_WIKIS[5 * i + 4] = Wiki.createInstance(temp[i] + ".wikivoyage.org");
        }
        MAJOR_WIKIS[15] = Wiki.createInstance("meta.wikimedia.org");
        MAJOR_WIKIS[16] = Wiki.createInstance("commons.wikimedia.org");
        MAJOR_WIKIS[17] = Wiki.createInstance("mediawiki.org");
        MAJOR_WIKIS[18] = Wiki.createInstance("wikidata.org");
        for (Wiki tempwiki : MAJOR_WIKIS)
            tempwiki.setMaxLag(-1);
    }
    
    /**
     *  Runs this program. In offline mode, this searches all wikis.
     *  @param args command line arguments (see case "--help" below).
     *  @throws IOException if a filesystem error occurs
     */
    public static void main(String[] args) throws IOException
    {
        // parse command line options
        String domain = null;
        boolean httponly = false;
        int threads = 3;
        for (int i = 0; i < args.length; i++)
        {
            switch (args[i])
            {
                case "--help":
                    System.out.println("SYNOPSIS:\n\t java org.wikipedia.tools.AllWikiLinksearch [options] domain\n\n"
                        + "DESCRIPTION:\n\tSearches Wikimedia projects for links.\n\n"
                        + "\t--help\n\t\tPrints this screen and exits.\n"
                        + "\t--httponly\n\t\tSearch for non-secure links only.\n"
                        + "\t--numthreads n\n\t\tUse n threads.\n"
                        + "A dialog box will pop up if domain is not specified.");
                    System.exit(0);
                case "--httponly":
                    httponly = true;
                    break;
                case "--numthreads":
                    threads = Integer.parseInt(args[++i]);
                    break;
                default:
                    domain = args[i];
                    break;
            }
        }
        if (!GraphicsEnvironment.isHeadless() && domain == null)
            domain = JOptionPane.showInputDialog(null, "Enter domain to search", "All wiki linksearch", JOptionPane.QUESTION_MESSAGE);
        if (domain == null)
        {
            System.out.println("No domain specified!");
            System.exit(0);
        }
        
        WMFWiki[] wikis = WMFWiki.getSiteMatrix();
        Map<Wiki, List[]> results = crossWikiLinksearch(false, threads, domain, wikis, !httponly, false);
        
        // output results
        FileWriter out = new FileWriter(domain + ".wiki");
        for (Map.Entry<Wiki, List[]> result : results.entrySet())
        {
            Wiki wiki = result.getKey();
            List[] links = result.getValue();
            StringBuilder temp = new StringBuilder("=== Results for ");
            temp.append(wiki.getDomain());
            temp.append(" ===\n");
            if (links == null)
            {
                temp.append("<span style=\"color: red\">An error occurred!</span>\n\n");
                out.write(temp.toString());
                continue;
            }
            int linknumber = links[0].size();
            if (linknumber != 0)
            {
                temp.append(ParserUtils.linksearchResultsToWikitext(links, domain));
                out.write(temp.toString());
            }
        }
        out.close();
    }
    
    /**
     *  Performs a cross-wiki linksearch.
     * 
     *  @param querylimit sets a query limit (500) for servlets
     *  @param threads use this many threads. If <tt>querylimit</tt> is supplied
     *  this is forced to 1. If greater than 1, this overwrites the system
     *  property <tt>java.util.concurrent.ForkJoinPool.common.parallelism</tt>.
     *  @param domain the domain to search
     *  @param wikis the wikis to search
     *  @param https include HTTPS links?
     *  @param mailto include mailto links?
     *  @param ns restrict to the given namespaces
     *  @return the linksearch results, as in wiki => results, or null if an
     *  IOException occurred
     */
    public static Map<Wiki, List[]> crossWikiLinksearch(boolean querylimit, 
        int threads, String domain, Wiki[] wikis, boolean https, boolean mailto, 
        int... ns)
    {
        Stream<Wiki> stream = Arrays.stream(wikis);
        // set concurrency if desired
        if (!querylimit && threads > 1)
        {
            System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "" + threads);
            stream = stream.parallel();
        }
        Map<Wiki, List[]> ret = stream.collect(Collectors.toMap(Function.identity(), wiki ->
        {
            if (querylimit)
                wiki.setQueryLimit(500);
            try
            {
                List[] temp = wiki.linksearch("*." + domain, "http", ns);
                // silly api designs aplenty here!
                if (https)
                {
                    List[] temp2 = wiki.linksearch("*." + domain, "https", ns);
                    temp[0].addAll(temp2[0]);
                    temp[1].addAll(temp2[1]);
                }
                if (mailto)
                {
                    List[] temp2 = wiki.linksearch("*." + domain, "mailto", ns);
                    temp[0].addAll(temp2[0]);
                    temp[1].addAll(temp2[1]);
                }
                return temp;
            }
            catch (IOException ex)
            {
                return null;
            }
        }));

        return ret;
    }
}