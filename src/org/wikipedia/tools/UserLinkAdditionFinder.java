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
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import org.wikipedia.*;

/**
 *  Finds links added by a user in the main namespace.
 *  @author MER-C
 *  @version 0.02
 */
public class UserLinkAdditionFinder
{
    private static int threshold = 50;
    private static WMFWiki wiki = WMFWiki.createInstance("en.wikipedia.org");

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
            .addSingleArgumentFlag("--user", "user", "Get links for this user only.")
            .addBooleanFlag("--linksearch", "Conduct a linksearch to count links and filter commonly used domains.")
            .addBooleanFlag("--removeblacklisted", "Remove blacklisted links")
            .addSingleArgumentFlag("--fetchafter", "date", "Fetch only edits after this date.")
            .addSection("If a file is not specified, a dialog box will prompt for one.")
            .parse(args);

        boolean linksearch = parsedargs.containsKey("--linksearch");
        boolean removeblacklisted = parsedargs.containsKey("--removeblacklisted");
        String user = parsedargs.get("--user");
        String datestring = parsedargs.get("--fetchafter");
        String filename = parsedargs.get("default");
        OffsetDateTime date = datestring == null ? null : OffsetDateTime.parse(datestring);

        // read in from file
        List<String> users;
        if (user == null)
        {
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
            users = Files.readAllLines(fp, Charset.forName("UTF-8"));
        }
        else
            users = Arrays.asList(user);

        // fetch and parse edits
        Map<Wiki.Revision, List<String>> results = getLinksAdded(users, date);
        if (results.isEmpty())
        {
            System.out.println("No links found.");
            System.exit(0);
        }

        // then transform to a map with domain -> spammers and domain -> link count
        Map<String, Set<String>> domains = new HashMap<>();
        Map<String, Integer> linkcounts = new HashMap<>();
        wiki.setQueryLimit(threshold);
        Iterator<Map.Entry<Wiki.Revision, List<String>>> iter = results.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<Wiki.Revision, List<String>> entry = iter.next();
            Wiki.Revision revision = entry.getKey();
            Iterator<String> links = entry.getValue().iterator();
            while (links.hasNext())
            {
                String link = links.next();
                String domain = ExternalLinks.extractDomain(link);
                // remove any dodgy links and any blacklisted links if asked for
                if (domain == null
                    || (removeblacklisted && wiki.isSpamBlacklisted(domain)))
                {
                    links.remove();
                    continue;
                }
                if (domains.containsKey(domain))
                {
                    domains.get(domain).add(revision.getUser());
                    continue;
                }
                // remove any frequently used domains if asked for
                if (linksearch)
                {
                    int linkcount = wiki.linksearch("*." + domain).size();
                    if (linkcount < threshold)
                        linkcount += wiki.linksearch("*." + domain, "https").size();
                    if (linkcount >= threshold)
                    {
                        links.remove();
                        continue;
                    }
                    linkcounts.put(domain, linkcount);
                }

                HashSet<String> blah = new HashSet<>();
                blah.add(revision.getUser());
                domains.put(domain, blah);
            }
        }
        wiki.setQueryLimit(Integer.MAX_VALUE);

        // check whether the links are still there
        Map<String, List<String>> resultsbypage = new HashMap<>();
        results.forEach((revision, listoflinks) ->
        {
            String page = revision.getTitle();
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
            if (links.isEmpty())
                return;
            Map<String, Boolean> revlinkexists = stillthere.get(revision.getTitle());
            StringBuilder temp = new StringBuilder("|-\n|| [[Special:Diff/");
            temp.append(revision.getID());
            temp.append("]]\n||\n");
            for (int i = 0; i < links.size(); i++)
            {
                String link = links.get(i);
                temp.append("* ");
                temp.append(link);
                boolean remaining = revlinkexists.get(link);
                temp.append(remaining ? " ('''STILL THERE'''" : " (removed");
                if (linksearch)
                {
                    String domain = ExternalLinks.extractDomain(link);
                    temp.append("; ");
                    temp.append(linkcounts.get(domain));
                    temp.append(" links: [[Special:Linksearch/*.");
                    temp.append(domain);
                    temp.append("|http]], [[Special:Linksearch/https://*.");
                    temp.append(domain);
                    temp.append("|https]])");
                }
                else
                    temp.append(")");
                temp.append("\n");
            }
            System.out.println(temp.toString());
        });
        System.out.println("|}");

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
     *  Fetches the list of links added by a list of users. The list of users
     *  must be a list of usernames only, no User: prefix or wikilinks allowed.
     *  @param users the list of users to get link additions for
     *  @param earliest return edits no earlier than this date
     *  @return a Map: revision &#8594; added links
     *  @throws IOException if a network error occurs
     */
    public static Map<Wiki.Revision, List<String>> getLinksAdded(List<String> users, OffsetDateTime earliest) throws IOException
    {
        Wiki.RequestHelper rh = wiki.new RequestHelper()
            .inNamespaces(Wiki.MAIN_NAMESPACE)
            .withinDateRange(earliest, null);
        Map<Wiki.Revision, List<String>> results = new HashMap<>();
        List<List<Wiki.Revision>> contribs = wiki.contribs(users, null, rh, null);
        List<Wiki.Revision> revisions = contribs.stream()
            .flatMap(List::stream)
            .filter(revision -> !revision.isContentDeleted())
            .collect(Collectors.toList());
        for (Wiki.Revision revision : revisions)
        {
            // remove all sets { revision, links... } where no links are added
            List<String> temp = parseDiff(revision);
            if (!temp.isEmpty())
                results.put(revision, temp);
        }
        return results;
    }

    /**
     *  Returns a list of external links added by a particular revision.
     *  @param revision the revision to check of added external links.
     *  @return the list of added URLs
     *  @throws IOException if a network error occurs
     */
    public static List<String> parseDiff(Wiki.Revision revision) throws IOException
    {
        // fetch the diff
        String diff = revision.isNew() ? revision.getText() : revision.diff(Wiki.PREVIOUS_REVISION);
        // filter dummy edits
        if (diff == null || diff.isEmpty())
            return Collections.emptyList();
        List<String> links = new ArrayList<>();

        // some HTML strings we are looking for
        // see https://en.wikipedia.org/w/api.php?action=compare&fromrev=77350972&torelative=prev
        String diffaddedbegin = "<td class=\"diff-addedline\">";
        String diffaddedend = "</td>";
        String deltabegin = "<ins class=\"diffchange diffchange-inline\">";
        String deltaend = "</ins>";
        // link regex
        Pattern pattern = Pattern.compile("https?://.+?\\..{2,}?(?:\\s|]|<|$)");

        // Condense deltas to avoid problems like https://en.wikipedia.org/w/index.php?title=&diff=prev&oldid=486611734
        diff = diff.toLowerCase(wiki.locale());
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
        return links;
    }
}
