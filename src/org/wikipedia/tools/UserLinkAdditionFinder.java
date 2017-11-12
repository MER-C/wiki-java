/**
 *  @(#)UserLinkAdditionFinder.java 0.02 05/11/2017
 *  Copyright (C) 2015-2017 MER-C
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
import java.util.regex.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.time.OffsetDateTime;
import javax.swing.JFileChooser;
import org.wikipedia.*;

/**
 *  Finds links added by a user in the main namespace.
 *  @author MER-C
 *  @version 0.02
 */
public class UserLinkAdditionFinder
{
    /**
     *  Runs this program.
     *  @param args the command line arguments (see "--help" below)
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        WMFWiki enWiki = WMFWiki.createInstance("en.wikipedia.org");
        enWiki.setQueryLimit(500);
        
        // parse command line args
        boolean linksearch = false, removeblacklisted = false;
        String filename = null;
        String datestring = null;
        for (int i = 0; i < args.length; i++)
        {
            switch (args[i])
            {
                case "--help":
                    System.out.println("SYNOPSIS:\n\t java org.wikipedia.tools.UserLinkAdditionFinder [options] [file]\n\n"
                        + "DESCRIPTION:\n\tFinds the set of links added by a list of users.\n\n"
                        + "\t--help\n\t\tPrints this screen and exits.\n"
                        + "\t--linksearch\n\t\tConduct a linksearch to filter commonly used links.\n"
                        + "\t--fetchafter\n\t\tFetch only edits after this date.\n"
                        + "If a file is not specified, a dialog box will prompt for one.");
                    System.exit(0);
                case "--linksearch":
                    linksearch = true;
                    break;
                case "--removeblacklisted":
                    removeblacklisted = true;
                    break;
                case "--fetchafter":
                    datestring = args[++i];
                    break;
                default:
                    filename = args[i];
                    break;
            }
        }
        final OffsetDateTime date = datestring == null ? null : OffsetDateTime.parse(datestring);
        
        // read in from file
        Path fp = null;
        if (filename == null)
        {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
                System.exit(0);
            fp = fc.getSelectedFile().toPath();
        }
        else
            fp = Paths.get(filename);
        
        // fetch and parse edits
        Map<Wiki.Revision, List<String>> results = new HashMap<>();
        Files.lines(fp, Charset.forName("UTF-8"))
            .flatMap(user -> {
                try 
                {
                    return Arrays.stream(enWiki.contribs(user, "", null, date, Wiki.MAIN_NAMESPACE));
                }
                catch (IOException ex)
                {
                    System.err.println("IOException for fetching contribs of user " + user);
                    return Arrays.stream(new Wiki.Revision[0]);
                }
            }).forEach(revision -> {
                try
                {
                    // remove all sets { revision, links... } where no links are added
                    Map<Wiki.Revision, List<String>> temp = parseDiff(revision);
                    if (!temp.get(revision).isEmpty())
                        results.putAll(temp);
                }
                catch (IOException ex)
                {
                }
            });
        if (results.isEmpty())
        {
            System.out.println("No links found.");
            System.exit(0);
        }
        
        // transform to wikitable
        System.out.println("{| class=\"wikitable\"\n");
        results.forEach((revision, links) ->
        {
            StringBuilder temp = new StringBuilder("|-\n|| [[Special:Diff/");
            temp.append(revision.getRevid());
            temp.append("]]\n|| ");
            for (int i = 0; i < links.size(); i++)
            {
                temp.append(links.get(i));
                temp.append("\n");
            }
            System.out.println(temp.toString());
        });
        System.out.println("|}");
        
        // then transform to a map with domain -> spammers
        Map<String, Set<String>> domains = new HashMap<>();
        results.forEach((revision, links) ->
        {
            for (int i = 0; i < links.size(); i++)
            {
                String link = links.get(i);
                // get domain name
                String[] temp2 = link.split("/");
                String domain = temp2[2].replace("www.", "");
                if (domains.containsKey(domain))
                    domains.get(domain).add(revision.getUser());
                else
                {
                    HashSet<String> blah = new HashSet<>();
                    blah.add(revision.getUser());
                    domains.put(domain, blah);
                }
            }
        });
        // remove blacklisted domains (if applicable)
        if (removeblacklisted)
        {
            Iterator<Map.Entry<String, Set<String>>> iter = domains.entrySet().iterator();
            while (iter.hasNext())
            {
                Map.Entry<String, Set<String>> entry = iter.next();
                if (enWiki.isSpamBlacklisted(entry.getKey()))
                    iter.remove();
            }
        }
        // perform a linksearch to remove frequently used domains
        if (linksearch)
        {
            Iterator<Map.Entry<String, Set<String>>> iter = domains.entrySet().iterator();
            while (iter.hasNext())
            {
                Map.Entry<String, Set<String>> entry = iter.next();
                int linkcount = enWiki.linksearch("*." + entry.getKey()).size()
                    + enWiki.linksearch("*." + entry.getKey(), "https").size();
                if (linkcount > 14)
                    iter.remove();
            }
        }
        
        System.out.println("== Domain list ==");
        for (String domain : domains.keySet())
            System.out.println("*{{spamlink|" + domain + "}}");
        System.out.println();
        
        System.out.println("== Blacklist log ==");
        for (Map.Entry<String, Set<String>> entry : domains.entrySet())
        {
            String domain = entry.getKey().replace(".", "\\.");
            System.out.print(" \\b" + domain + "\\b");
            for (int i = domain.length(); i < 35; i++)
                System.out.print(' ');
            System.out.print(" # ");
            for (String spammer : entry.getValue())
                System.out.print("{{user|" + spammer + "}} ");
            System.out.println();
        }
        System.out.flush();
    }
    
    /**
     *  Returns a list of external links added by a particular revision.
     *  @param revision the revision to check of added external links.
     *  @return a map: revision &#8594; list of added URLs.
     *  @throws IOException if a network error occurs
     */
    public static Map<Wiki.Revision, List<String>> parseDiff(Wiki.Revision revision) throws IOException
    {
        // fetch the diff
        String diff;
        Map<Wiki.Revision, List<String>> ret = new HashMap<>();
        List<String> links = new ArrayList<>();
        if (revision.isNew())
            diff = revision.getText();
        else
            diff = revision.diff(Wiki.PREVIOUS_REVISION);
        // filter dummy edits
        if (diff == null)
        {
            ret.put(revision, links);
            return ret;
        }

        // some HTML strings we are looking for
        // see https://en.wikipedia.org/w/api.php?action=query&prop=revisions&revids=77350972&rvdiffto=prev
        String diffaddedbegin = "<td class=\"diff-addedline\">";
        String diffaddedend = "</td>";
        String deltabegin = "<ins class=\"diffchange diffchange-inline\">";
        String deltaend = "</ins>";
        // link regex
        Pattern pattern = Pattern.compile("https?://.+?\\..{2,}?(?:\\s|]|<|$)");
        
        // Condense deltas to avoid problems like https://en.wikipedia.org/w/index.php?title=&diff=prev&oldid=486611734
        diff = diff.toLowerCase();
        diff = diff.replace(deltaend + " " + deltabegin, " ");
        diff = diff.replace("&lt;", "<");
        for (int j = diff.indexOf(diffaddedbegin); j >= 0; j = diff.indexOf(diffaddedbegin, j))
        {
            int y2 = diff.indexOf(diffaddedend, j);
            String addedline = diff.substring(j + diffaddedbegin.length(), y2);
            addedline = addedline.replaceFirst("^<div>", "");
            addedline = addedline.replace("</div>", "");
            if (addedline.contains(deltabegin))
            {
                for (int k = addedline.indexOf(deltabegin); k >= 0; k = addedline.indexOf(deltabegin, k))
                {
                    int y3 = addedline.indexOf(deltaend, k);
                    String delta = addedline.substring(k + deltabegin.length(), y3);
                    // extract links
                    Matcher matcher = pattern.matcher(delta);
                    while (matcher.find())
                        links.add(matcher.group().split("[\\|<\\]\\s]")[0]);
                        
                    k = y3;
                }
            }
            else
            {
                Matcher matcher = pattern.matcher(addedline);
                while (matcher.find())
                    links.add(matcher.group().split("[\\|<\\]\\s]")[0]);
            }
            j = y2;
        }
        
        ret.put(revision, links);
        return ret;
    }
}