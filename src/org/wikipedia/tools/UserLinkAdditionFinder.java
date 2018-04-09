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
    private static int threshold = 20;
    private static WMFWiki wiki;
    
    /**
     *  Runs this program.
     *  @param args the command line arguments (see code for documentation)
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        // parse command line args
        Map<String, String> parsedargs = new CommandLineParser()
            .synopsis("org.wikipedia.tools.UserLinkAdditionFinder", "[options] [file]")
            .description("Finds the set of links added by a list of users.")
            .addHelp()
            .addVersion("UserLinkAdditionFinder v0.02\n" + CommandLineParser.GPL_VERSION_STRING)
            .addBooleanFlag("--linksearch", "Conduct a linksearch to filter commonly used links.")
            .addBooleanFlag("--removeblacklisted", "Remove blacklisted links")
            .addSingleArgumentFlag("--fetchafter", "date", "Fetch only edits after this date.")
            .addSection("If a file is not specified, a dialog box will prompt for one.")
            .parse(args);

        wiki = WMFWiki.createInstance("en.wikipedia.org");
        boolean linksearch = parsedargs.containsKey("--linksearch");
        boolean removeblacklisted = parsedargs.containsKey("--removeblacklisted");
        String datestring = parsedargs.get("--fetchafter");
        String filename = parsedargs.get("default");
        OffsetDateTime date = datestring == null ? null : OffsetDateTime.parse(datestring);
        
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
        List<String> lines = Files.readAllLines(fp, Charset.forName("UTF-8"));
        List<Wiki.Revision>[] revisions = wiki.contribs(lines.toArray(new String[0]), "", null, date, null, Wiki.MAIN_NAMESPACE);
        Arrays.stream(revisions)
            .flatMap(List::stream)
            .filter(revision -> !revision.isContentDeleted())
            .forEach(revision -> 
        {
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
        
        // check whether the links are still there
        Map<String, List<String>> resultsbypage = new HashMap<>();
        results.forEach((revision, listoflinks) ->
        {
            String page = revision.getPage();
            List<String> list = resultsbypage.get(page);
            if (list == null)
            {
                list = new ArrayList<>();
                resultsbypage.put(page, list);
            }
            list.addAll(listoflinks);
        });
        Map<String, Map<String, Boolean>> stillthere = Pages.of(wiki).containExternalLinks(resultsbypage);
        
        // transform to wikitable
        System.out.println("{| class=\"wikitable\"\n");
        results.forEach((revision, links) ->
        {
            Map<String, Boolean> revlinkexists = stillthere.get(revision.getPage());
            StringBuilder temp = new StringBuilder("|-\n|| [[Special:Diff/");
            temp.append(revision.getID());
            temp.append("]]\n||\n");
            for (int i = 0; i < links.size(); i++)
            {
                String link = links.get(i);
                temp.append("* ");
                temp.append(link);
                boolean remaining = revlinkexists.get(link);
                temp.append(remaining ? " ('''STILL THERE''')" : " (removed)");
                temp.append("\n");
            }
            System.out.println(temp.toString());
        });
        System.out.println("|}");
        
        // then transform to a map with domain -> spammers
        Map<String, Set<String>> domains = new HashMap<>();
        results.forEach((revision, links) ->
        {
            links.forEach(link ->
            {
                String domain = ExternalLinks.extractDomain(link);
                if (domain == null)
                    return;
                else if (domains.containsKey(domain))
                    domains.get(domain).add(revision.getUser());
                else
                {
                    HashSet<String> blah = new HashSet<>();
                    blah.add(revision.getUser());
                    domains.put(domain, blah);
                }
            });
        });
        // remove blacklisted domains (if applicable)
        if (removeblacklisted)
        {
            Iterator<String> iter = domains.keySet().iterator();
            while (iter.hasNext())
            {
                if (wiki.isSpamBlacklisted(iter.next()))
                    iter.remove();
            }
        }
        // perform a linksearch to remove frequently used domains
        if (linksearch)
        {
            wiki.setQueryLimit(threshold);
            Iterator<String> iter = domains.keySet().iterator();
            while (iter.hasNext())
            {
                String domain = iter.next();
                int linkcount = wiki.linksearch("*." + domain).size();
                if (linkcount <= threshold)
                    linkcount += wiki.linksearch("*." + domain, "https").size();
                if (linkcount > threshold)
                    iter.remove();
            }
            wiki.setQueryLimit(Integer.MAX_VALUE);
        }
        
        System.out.println("== Domain list ==");
        for (String domain : domains.keySet())
            System.out.println("*{{spamlink|" + domain + "}}");
        System.out.println();
        
        System.out.println("== Blacklist log ==");
        domains.forEach((key, value) ->
        {
            String domain = key.replace(".", "\\.");
            System.out.print(" \\b" + domain + "\\b");
            for (int i = domain.length(); i < 35; i++)
                System.out.print(' ');
            System.out.print(" # ");
            for (String spammer : value)
                System.out.print("{{user|" + spammer + "}} ");
            System.out.println();
        });
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
        if (diff == null || diff.isEmpty())
        {
            ret.put(revision, links);
            return ret;
        }

        // some HTML strings we are looking for
        // see https://en.wikipedia.org/w/api.php?action=compare&fromrev=77350972&torelative=prev
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
                        links.add(matcher.group().split("[\\|<\\]\\s\\}]")[0]);
                        
                    k = y3;
                }
            }
            else
            {
                Matcher matcher = pattern.matcher(addedline);
                while (matcher.find())
                    links.add(matcher.group().split("[\\|<\\]\\s\\}]")[0]);
            }
            j = y2;
        }
        
        ret.put(revision, links);
        return ret;
    }
}
