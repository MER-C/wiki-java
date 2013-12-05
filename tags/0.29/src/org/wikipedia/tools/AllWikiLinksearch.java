/**
 *  @(#)AllWikiLinksearch.java 0.01 29/03/2011
 *  Copyright (C) 2011 MER-C
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
 *      <li>Running the {@link http://wikipediatools.appspot.com top 20 WPs
 *      linksearch tool}
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
    private static Queue<Wiki> queue = new ConcurrentLinkedQueue();
    private static FileWriter out = null;
    private static ProgressMonitor monitor;
    private static int progress = 0;

    private static class LinksearchThread extends Thread
    {
        private String domain;

        public LinksearchThread(String domain)
        {
            this.domain = domain;
        }

        /**
         *  The real meat of this program.
         */
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
                    ArrayList[] links = wiki.linksearch("*." + domain);
                    linknumber = links[0].size();
                    if (linknumber != 0)
                    {
                        for (int i = 0; i < linknumber; i++)
                        {
                            builder.append("# [http://");
                            builder.append(wiki.getDomain());
                            builder.append("/wiki/");
                            builder.append(((String)links[0].get(i)).replace(' ', '_'));
                            builder.append(" ");
                            builder.append(links[0].get(i));
                            builder.append("] uses link <nowiki>");
                            builder.append(links[1].get(i));
                            builder.append("</nowiki>\n");
                        }
                        builder.append(linknumber);
                        builder.append(" links found. ([http://");
                        builder.append(wiki.getDomain());
                        builder.append("/wiki/Special:Linksearch/*.");
                        builder.append(domain);
                        builder.append(" Linksearch])");
                    }
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
            try
            {
                // flush so output is not truncated
                out.flush();
                sleep(5000);
            }
            catch (Exception ex)
            {
                // bleh
            }
        }
    }

    public static void main(String[] args) throws IOException
    {
        // retrieve site matrix
        ArrayList<Wiki> temp = new ArrayList<Wiki>(Arrays.asList(WMFWiki.getSiteMatrix()));
        for (Wiki wiki : temp)
        {
            String domain = wiki.getDomain();
            // bad wikis: everything containing wikimania
            if (!domain.contains("wikimania"))
                queue.add(wiki);
        }

        // initialize progress monitor
        String domain = JOptionPane.showInputDialog(null, "Enter domain to search", "All wiki linksearch", JOptionPane.QUESTION_MESSAGE);
        monitor = new ProgressMonitor(null, "Searching for links to " + domain, null, 0, queue.size());
        monitor.setMillisToPopup(0);

        // do the searching
        out = new FileWriter(domain + ".wiki");
        writeOutput("*{{LinkSummary|" + domain + "}}\nSearching " + queue.size() + " wikis at "
            + new Date().toString() + ".\n\n");
        // TODO: perhaps make number of threads configurable
        for (int i = 0; i < 3; i++)
            new LinksearchThread(domain).start();
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