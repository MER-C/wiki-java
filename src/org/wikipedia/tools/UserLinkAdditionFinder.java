/**
 *  @(#)UserLinkAdditionFinder.java 0.03 15/06/2024
 *  Copyright (C) 2015-2024 MER-C
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
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import org.wikipedia.*;

/**
 *  Finds links added by a user in the main namespace.
 *  @author MER-C
 *  @version 0.03
 */
public class UserLinkAdditionFinder
{
    private final WMFWiki wiki;
    private final Pages pages;
    private final ExternalLinks el;
    private static final WMFWikiFarm sessions = WMFWikiFarm.instance();
    private final List<Pattern> whitelist_regexes = new ArrayList<>();
    
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
            .addSingleArgumentFlag("--wiki", "example.org", "The wiki to fetch data from (default: en.wikipedia.org)")
            .addUserInputOptions("Get links for")
            .addBooleanFlag("--linksearch", "Conduct a linksearch to count links and filter commonly used domains.")
            .addSingleArgumentFlag("--threshold", "50", "If filtering commonly used domains, the threshold number of links (default: 50)")
            .addBooleanFlag("--removeblacklisted", "Remove blacklisted links")
            .addSingleArgumentFlag("--fetchafter", "date", "Fetch only edits after this date.")
            .addSingleArgumentFlag("--fetchbefore", "date", "Fetch only edits before this date")
            .addSingleArgumentFlag("--ignorebelow", "X", "Don't return domains added less than X times")
            .addSection("If a file is not specified, a dialog box will prompt for one.")
            .parse(args);

        WMFWiki thiswiki = sessions.sharedSession(parsedargs.getOrDefault("--wiki", "en.wikipedia.org"));
        boolean linksearch = parsedargs.containsKey("--linksearch");
        boolean removeblacklisted = parsedargs.containsKey("--removeblacklisted");
        List<OffsetDateTime> dates = CommandLineParser.parseDateRange(parsedargs, "--fetchafter", "--fetchbefore");
        int threshold = Integer.parseInt(parsedargs.getOrDefault("--threshold", "50"));
        List<String> users = CommandLineParser.parseUserOptions(parsedargs, thiswiki);
        int ignorebelow = Integer.parseInt(parsedargs.getOrDefault("--ignorebelow", "-1"));
        
        UserLinkAdditionFinder finder = new UserLinkAdditionFinder(thiswiki);
        ExternalLinkPopularity elp = new ExternalLinkPopularity(thiswiki);
        elp.setMaxLinks(threshold);

        // Map structure:
        // * results: revid -> links added in that revision
        // * linkdomains: link -> domain
        // * linkcounts: domain -> count of links
        // * stillthere: page name -> link -> whether it is still there
        Map<Wiki.Revision, List<String>> results = finder.getLinksAdded(users, dates.get(0), dates.get(1));
        if (results.isEmpty())
        {
            System.out.println("No links found.");
            System.exit(0);
        }
        Map<String, String> linkdomains = new HashMap<>();
        for (Map.Entry<Wiki.Revision, List<String>> entry : results.entrySet())
        {
            for (String link : entry.getValue())
            {
                String domain = ExternalLinks.extractDomain(link);
                if (domain != null && !finder.canSkipDomain(domain, removeblacklisted))
                    linkdomains.put(link, domain);
            }
        }
                
        // remove commonly used domains and insignificant domains
        Collection<String> domains = new TreeSet(linkdomains.values());
        Map<String, Integer> linkcounts = null;
        if (linksearch)
        {
            linkcounts = elp.determineLinkPopularity(domains);
            Iterator<Map.Entry<String, Integer>> iter = linkcounts.entrySet().iterator();
            while (iter.hasNext())
            {
                Map.Entry<String, Integer> entry = iter.next();
                if (entry.getValue() >= threshold || entry.getValue() <= ignorebelow)
                {
                    iter.remove();
                    domains.remove(entry.getKey());
                }
            }
        }
        Map<String, Map<String, Boolean>> stillthere = finder.checkIfLinksAreStillPresent(results);
        
        // output results
        System.out.println(finder.outputWikitableResults(results, linkcounts, stillthere));
        System.out.println("== Domain list ==");
        System.out.println(Pages.toWikitextTemplateList(domains, "spamlink", false));
        System.out.println();
        System.out.println("== Blacklist log ==");
        System.out.println(finder.generateBlacklistLog(results, linkdomains));
        System.out.flush();
    }
    
    /**
     *  Creates a new instance of this tool.
     *  @param wiki the wiki to fetch data from
     */
    public UserLinkAdditionFinder(WMFWiki wiki)
    {
        this.wiki = wiki;
        this.pages = Pages.of(wiki);
        this.el = ExternalLinks.of(wiki);
        
        String[] regex = new String[] 
        {
            ".*\\.(?:gov|int|mil)$",
            "(.*\\.)?gov\\.(?:au|br|cn|ie|in|il|ph|ru|scot|sg|ua|uk|wales|za)$",
            "(.*\\.)?gob\\.(?:ar|cl|es|mx|pe)$",
            "(.*\\.)?(?:bl|judiciary|mod|nhs|parliament|police|royal)\\.uk$",
            "(.*\\.)?\\bgouv\\.fr",
            "(.*\\.)?\\bgovt\\.nz$",
            "(.*\\.)?\\beuropa\\.eu$"
        };
        for (String r : regex)
            whitelist_regexes.add(Pattern.compile(r));
    }
    
    /**
     *  Returns the wiki that this tool fetches data from.
     *  @return (see above)
     */
    public Wiki getWiki()
    {
        return wiki;
    }

    /**
     *  Fetches the list of links added by a list of users. The list of users
     *  must be a list of usernames only, no User: prefix or wikilinks allowed.
     *  @param users the list of users to get link additions for
     *  @param earliest return edits no earlier than this date
     *  @param latest return edits no later than this date
     *  @return a Map: revision &#8594; added links
     *  @throws IOException if a network error occurs
     */
    public Map<Wiki.Revision, List<String>> getLinksAdded(List<String> users, OffsetDateTime earliest, 
        OffsetDateTime latest) throws IOException
    {
        Wiki.RequestHelper rh = wiki.new RequestHelper()
            .inNamespaces(Wiki.MAIN_NAMESPACE)
            .withinDateRange(earliest, latest);
        Map<Wiki.Revision, List<String>> results = new HashMap<>();
        List<List<Wiki.Revision>> contribs = wiki.contribs(users, null, rh);
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
     *  Filters added links for inclusion in search results. 
     * 
     *  @param domain the domain to check
     *  @param removeblacklisted remove already blacklisted links
     *  @return whether this domain can be skipped for the purpose of this
     *  search
     *  @throws IOException if a network error occurs when fetching the spam
     *  blacklist (highly unlikely)
     *  @since 0.03
     */
    public boolean canSkipDomain(String domain, boolean removeblacklisted) throws IOException
    {
        // WMF domains
        for (String wmfsite : WMFWikiFarm.WMF_DOMAINS)
            if (domain.endsWith(wmfsite))
                return true;
        // government domains
        for (Pattern p : whitelist_regexes)
            if (p.matcher(domain).matches())
                return true;
        // blacklisted domains
        if (removeblacklisted && el.isSpamBlacklisted(domain))
            return true;
        return false;
    }
    
    /**
     *  For a map that contains revision data &#8594; links added in that 
     *  revision, check whether the links still exist in the current version of
     *  the article. Such a map can be obtained by calling {@link 
     *  #getLinksAdded(List, OffsetDateTime, OffsetDateTime)}.
     *  @param data a map containing revision data &#8594; links added in that 
     *  revision
     *  @return a map containing page &#8594; link &#8594; whether it is still 
     *  there
     *  @throws IOException if a network error occurs
     */
    public Map<String, Map<String, Boolean>> checkIfLinksAreStillPresent(Map<Wiki.Revision, List<String>> data) throws IOException
    {
        Map<String, List<String>> resultsbypage = new HashMap<>();
        data.forEach((revision, listoflinks) ->
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
        return pages.containExternalLinks(resultsbypage);
    }
    
    public String outputWikitableResults(Map<Wiki.Revision, List<String>> data, 
        Map<String, Integer> linkcounts, Map<String, Map<String, Boolean>> stillthere)
    {
        StringBuilder builder = new StringBuilder(100000);
        builder.append("{| class=\"wikitable\"\n");
        for (Map.Entry<Wiki.Revision, List<String>> entry : data.entrySet())
        {
            Wiki.Revision revision = entry.getKey();
            List<String> links = entry.getValue();
            if (links.isEmpty())
                continue;
            Map<String, Boolean> revlinkexists = stillthere.get(revision.getTitle());
            builder.append("|-\n|| [[Special:Diff/");
            builder.append(revision.getID());
            builder.append("]]\n||\n");
            for (int i = 0; i < links.size(); i++)
            {
                String link = links.get(i);
                builder.append("* ");
                builder.append(link);
                boolean remaining = revlinkexists.get(link);
                builder.append(remaining ? " ('''STILL THERE'''" : " (removed");
                if (linkcounts != null)
                {
                    String domain = ExternalLinks.extractDomain(link);
                    builder.append("; ");
                    builder.append(linkcounts.get(domain));
                    builder.append(" links: [[Special:Linksearch/*.");
                    builder.append(domain);
                    builder.append("|Linksearch]])");
                }
                else
                    builder.append(")");
                builder.append("\n");
            }
        }
        builder.append("|}");
        return builder.toString();
    }
    
    public String generateBlacklistLog(Map<Wiki.Revision, List<String>> data, Map<String, String> domains)
    {
        // transform to domain -> spammers
        Map<String, List<String>> spammers = new HashMap<>();
        data.forEach((revision, listoflinks) ->
        {
            String user = revision.getUser();
            for (String link : listoflinks)
            {
                String domain = domains.get(link);
                List<String> users = spammers.getOrDefault(domain, new ArrayList<>());
                users.add(user);
                spammers.putIfAbsent(domain, users);
            }
        });
        
        // generate output
        StringBuilder sb = new StringBuilder(100000);
        spammers.forEach((key, value) ->
        {
            String domain = key.replace(".", "\\.");
            sb.append(" \\b");
            sb.append(domain);
            sb.append("\\b");
            for (int i = domain.length(); i < 35; i++)
                sb.append(' ');
            sb.append(" # ");
            for (String spammer : value)
            {
                sb.append("{{user|");
                sb.append(spammer);
                sb.append("}} ");
            }
            sb.append("\n");
        });
        return sb.toString();
    }

    /**
     *  Returns a list of external links added by a particular revision.
     *  @param revision the revision to check of added external links.
     *  @return the list of added URLs
     *  @throws IOException if a network error occurs
     */
    public List<String> parseDiff(Wiki.Revision revision) throws IOException
    {
        String diff = revision.isNew() ? revision.getText() : revision.diff(Wiki.PREVIOUS_REVISION, "inline");
        if (diff == null || diff.isEmpty()) // filter dummy edits
            return Collections.emptyList();
        Pattern linkregex = Pattern.compile("https?://.+?\\..{2,}?(?:\\s|]|<|$|&lt;)");

        // See https://en.wikipedia.org/w/api.php?action=compare&fromrev=77350972&torelative=prev&difftype=inline
        // for example HTML
        List<String> dellinks = new ArrayList<>();
        Matcher matcher = Pattern.compile("<del .+?>(.+?)</del>").matcher(diff);
        while (matcher.find())
        {
            Matcher inner = linkregex.matcher(matcher.group(0));
            while (inner.find())
                dellinks.add(inner.group().split("(\\||<|\\]|\\s|\\}|&lt;)")[0]);
        }
        
        List<String> links = new ArrayList<>();
        matcher = Pattern.compile("<ins .+?>(.+?)</ins>").matcher(diff);
        while (matcher.find())
        {
            Matcher inner = linkregex.matcher(matcher.group(0));
            while (inner.find())
                links.add(inner.group().split("(\\||<|\\]|\\s|\\}|&lt;)")[0]);
        }
        
        links.removeAll(dellinks);
        return links;
    }
}
