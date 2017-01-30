/**
 *  @(#)AllWikiLinksearch.java 0.02 26/12/2016
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

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.*;
import org.wikipedia.*;

/**
 *  Searches all Wikimedia wikis for a given external link. Consider:
 *  <ul>
 *      <li>Running the <a href="http://wikipediatools.appspot.com">top 20 WPs
 *      linksearch tool</a>
 *      <li>COIBot poking the domain AND
 *      <li>Using Luxo's cross-wiki contributions to undo revisions by spammers
 *  </ul>
 *  before running this program. This will never be a servlet, as it takes about
 *  6 minutes to run.
 *  @author MER-C
 *  @version 0.02
 */
public class AllWikiLinksearch
{
    private static final Queue<Wiki> queue = new ConcurrentLinkedQueue();
    private static FileWriter out = null;
    private static ProgressMonitor monitor;
    private static int progress = 0;
    
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
            TOP40[i] = new Wiki(temp[i] + ".wikipedia.org");
            TOP40[i].setMaxLag(-1);
        }
        System.arraycopy(TOP40, 0, TOP20, 0, 20);
        
        temp = new String[] { "en", "de", "fr" };
        for (int i = 0; i < temp.length; i++)
        {
            MAJOR_WIKIS[5 * i    ] = new Wiki(temp[i] + ".wikipedia.org");
            MAJOR_WIKIS[5 * i + 1] = new Wiki(temp[i] + ".wiktionary.org");
            MAJOR_WIKIS[5 * i + 2] = new Wiki(temp[i] + ".wikibooks.org");
            MAJOR_WIKIS[5 * i + 3] = new Wiki(temp[i] + ".wikiquote.org");
            MAJOR_WIKIS[5 * i + 4] = new Wiki(temp[i] + ".wikivoyage.org");
        }
        MAJOR_WIKIS[15] = new Wiki("meta.wikimedia.org");
        MAJOR_WIKIS[16] = new Wiki("commons.wikimedia.org");
        MAJOR_WIKIS[17] = new Wiki("mediawiki.org");
        MAJOR_WIKIS[18] = new Wiki("wikidata.org");
        for (Wiki tempwiki : MAJOR_WIKIS)
            tempwiki.setMaxLag(-1);
    }
    

    private static class LinksearchThread extends Thread
    {
        private final String domain;
        private final boolean httponly;

        public LinksearchThread(String domain, boolean httponly)
        {
            this.domain = domain;
            this.httponly = httponly;
        }

        /**
         *  The real meat of this program.
         */
        @Override
        public void run()
        {
            while(!queue.isEmpty())
            {
                // only write when there are results
                int linknumber = 0;
                // buffer so we don't get the output all mixed up
                StringBuilder builder = new StringBuilder(1000);
                try
                {
                    Wiki wiki = queue.poll();
                    wiki.setMaxLag(0);
                    builder.append("=== Results for ");
                    builder.append(wiki.getDomain());
                    builder.append(" ===\n");
                    List[] links = crossWikiLinksearch(domain, new Wiki[] { wiki }, !httponly, false).get(wiki);
                    linknumber = links[0].size();
                    if (linknumber != 0)
                        builder.append(ParserUtils.linksearchResultsToWikitext(links, domain));
                }
                catch (IOException ex)
                {
                    builder.append("<font color=red>An error occurred: ");
                    linknumber = -1;
                    builder.append(ex.getMessage());
                }
                finally
                {
                    builder.append("\n\n");
                    if (linknumber != 0)
                        writeOutput(builder.toString());
                    updateProgress();
                }
            }
        }
    }

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
        
        // retrieve site matrix
        ArrayList<WMFWiki> temp = new ArrayList<>(Arrays.asList(WMFWiki.getSiteMatrix()));
        for (Wiki wiki : temp)
        {
            String wikidomain = wiki.getDomain();
            // bad wikis: everything containing wikimania
            if (!wikidomain.contains("wikimania"))
                queue.add(wiki);
        }

        // initialize progress monitor
        if (domain != null)
            domain = JOptionPane.showInputDialog(null, "Enter domain to search", "All wiki linksearch", JOptionPane.QUESTION_MESSAGE);
        monitor = new ProgressMonitor(null, "Searching for links to " + domain, null, 0, queue.size());
        monitor.setMillisToPopup(0);

        // do the searching
        out = new FileWriter(domain + ".wiki");
        writeOutput("*{{LinkSummary|" + domain + "}}\nSearching " + queue.size() + " wikis at "
            + new Date().toString() + ".\n\n");
        for (int i = 0; i < threads; i++)
            new LinksearchThread(domain, httponly).start();
    }

    /**
     *  Writes output to the results file.
     *  @param output the output to write
     */
    public static synchronized void writeOutput(String output)
    {
        try
        {
            out.write(output);
            out.flush();
        }
        catch (IOException ex)
        {
            // shouldn't happen
            JOptionPane.showMessageDialog(null, "Error writing to file!", "All wiki linksearch", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     *  Update progress. (Shouldn't cause Swing corruption).
     */
    public static synchronized void updateProgress()
    {
        progress++;
        monitor.setProgress(progress);
    }
    
    /**
     *  Performs a cross-wiki linksearch (see xwikilinksearch.jsp).
     *  @param domain the domain to search
     *  @param wikis the wikis to search
     *  @param https include HTTPS links?
     *  @param mailto include mailto links?
     *  @param ns restrict to the given namespaces
     *  @return the linksearch results, as in wiki => results
     *  @throws IOException if a network error occurs
     */
    public static Map<Wiki, List[]> crossWikiLinksearch(String domain, Wiki[] 
        wikis, boolean https, boolean mailto, int... ns) throws IOException
    {
        // TODO: integrate this with the above
        
        Map<Wiki, List[]> ret = new LinkedHashMap<>();
        for (Wiki wiki : wikis)
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
            ret.put(wiki, temp);
        }
        return ret;
    }
}