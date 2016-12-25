/**
 *  @(#)AllWikiLinksearch.java 0.01 29/03/2011
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
 *  @version 0.01
 */
public class AllWikiLinksearch
{
    private static final Queue<Wiki> queue = new ConcurrentLinkedQueue();
    private static FileWriter out = null;
    private static ProgressMonitor monitor;
    private static int progress = 0;

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
                    List[] links = wiki.linksearch("*." + domain);
                    if (!httponly)
                    {
                        List[] temp = wiki.linksearch("https://*." + domain);
                        links[0].addAll(temp[0]);
                        links[1].addAll(temp[1]);
                    }
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
        for (int i = 0; i < args.length; i++)
        {
            switch (args[i])
            {
                case "--httponly":
                    httponly = true;
                    break;
                default:
                    domain = args[i];
                    break;
            }
        }
        
        // retrieve site matrix
        ArrayList<Wiki> temp = new ArrayList<>(Arrays.asList(WMFWiki.getSiteMatrix()));
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
        // TODO: perhaps make number of threads configurable
        for (int i = 0; i < 3; i++)
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
}