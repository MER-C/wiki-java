/**
 *  @(#)AdminStats.java 0.01 15/07/2019
 *  Copyright (C) 2019 MER-C
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

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.wikipedia.*;

/**
 *  Outputs admin action reason stats for the English Wikipedia and global lock
 *  reason stats for all Wikimedia wikis.
 *  @author MER-C
 *  @version 0.01
 */
public class AdminStats
{
    private final Wiki enWiki, metaWiki;
    private OffsetDateTime start, end;
    private List<Wiki.LogEntry> deletecache;

    /**
     *  Runs this program. You must supply a start and end date.
     *  @param args the command line arguments
     *  @throws Exception if a network error occurs
     */
    public static void main(String[] args) throws Exception
    {
        Map<String, String> options = new CommandLineParser()
            .addSingleArgumentFlag("--start", "2019-04-01T00:00:00Z", "Start date")
            .addSingleArgumentFlag("--end", "2019-07-01T00:00:00Z", "End date")
            .addBooleanFlag("--printfull", "Dump histograms without grouping reasons")
            .addBooleanFlag("--locks", "Fetch statistics for global locks")
            .addBooleanFlag("--blocks", "Fetch statistics for indefinite blocks of accounts")
            .addBooleanFlag("--allblocks", "Fetch statistics for all blocks")
            .addBooleanFlag("--deletions", "Fetch statistics for deletions")
            .parse(args);
        OffsetDateTime start = OffsetDateTime.parse(options.get("--start"));
        OffsetDateTime end = OffsetDateTime.parse(options.get("--end"));
        boolean printfull = options.containsKey("--printfull");

        AdminStats stats = new AdminStats();
        stats.setDateRange(start, end);

        if (options.containsKey("--locks"))
        {
            Map<String, Long> lockhist = stats.lockStats();
            long total = lockhist.remove("TOTAL");
            if (!printfull)
                lockhist = stats.groupLockReasons(lockhist);
            System.out.println("==Lock stats==");
            System.out.println("" + total + " locks between " + start + " and " + end);
            printHistogram(lockhist, printfull);
        }

        boolean allblocks = options.containsKey("--allblocks");
        if (options.containsKey("--blocks") || allblocks)
        {
            Map<String, Long> blockhist = stats.blockStats(allblocks, allblocks);
            long total = blockhist.remove("TOTAL");
            if (!printfull)
                blockhist = stats.groupBlockReasons(blockhist);
            System.out.println("==Block stats==");
            System.out.println("" + total + " blocks between " + start + " and " + end);
            printHistogram(blockhist, printfull);
        }

        if (options.containsKey("--deletions"))
        {
            Map<String, Long> deletehist = stats.deleteStats();
            long total = deletehist.remove("TOTAL");
            if (!printfull)
                deletehist = stats.groupDeleteReasons(deletehist);
            System.out.println("==All namespaces==");
            System.out.println("" + total + " deletions between " + start + " and " + end);
            printHistogram(deletehist, printfull);

            deletehist = stats.deleteStats(Wiki.MAIN_NAMESPACE);
            total = deletehist.remove("TOTAL");
            if (!printfull)
                deletehist = stats.groupDeleteReasons(deletehist);
            System.out.println("==Main namespace==");
            System.out.println("" + total + " deletions between " + start + " and " + end);
            printHistogram(deletehist, printfull);

            deletehist = stats.deleteStats(Wiki.USER_NAMESPACE);
            total = deletehist.remove("TOTAL");
            if (!printfull)
                deletehist = stats.groupDeleteReasons(deletehist);
            System.out.println("==User namespace==");
            System.out.println("" + total + " deletions between " + start + " and " + end);
            printHistogram(deletehist, printfull);

            deletehist = stats.deleteStats(118); // draft namespace
            total = deletehist.remove("TOTAL");
            if (!printfull)
                deletehist = stats.groupDeleteReasons(deletehist);
            System.out.println("==Draft namespace==");
            System.out.println("" + total + " deletions between " + start + " and " + end);
            printHistogram(deletehist, printfull);
        }
    }

    public AdminStats()
    {
        enWiki = Wiki.newSession("en.wikipedia.org");
        metaWiki = Wiki.newSession("meta.wikimedia.org");
    }

    /**
     *  Restricts statistics to this date range. Soft required, because you
     *  will be fetching millions of entries otherwise.
     *  @param start date to start statistics
     *  @param end date to end statistics
     */
    public void setDateRange(OffsetDateTime start, OffsetDateTime end)
    {
        this.start = start;
        this.end = end;
    }

    /**
     *  Computes a histogram of page deletions by reason.
     *  @param namespaces limit results to these namespaces (empty array = all
     *  namespaces)
     *  @return a (raw) histogram of deletion reasons
     *  @throws IOException if a network error occurs
     */
    public Map<String, Long> deleteStats(int... namespaces) throws IOException
    {
        if (deletecache == null)
        {
            Wiki.RequestHelper rh = enWiki.new RequestHelper()
                .withinDateRange(start, end);
            deletecache = enWiki.getLogEntries("delete", "delete", rh);
            deletecache.removeIf(log -> log.getTitle() == null || log.getComment() == null);
        }
        Map<String, Long> ret = new HashMap<>();
        var stream = deletecache.stream();
        if (namespaces.length > 0)
        {
            stream = stream.filter(log ->
            {
                int ns = enWiki.namespace(log.getTitle());
                for (int ns2 : namespaces)
                    if (ns2 == ns)
                        return true;
                return false;
            });
        }
        ret.putAll(stream.collect(Collectors.groupingBy(
                log -> log.getComment().replace("_", " ").toLowerCase(), Collectors.counting())));
        ret.put("TOTAL", Long.valueOf(deletecache.size()));
        return ret;
    }

    /**
     *  Bundles similar deletion reasons together. Deletion reasons include
     *  most CSDs, PRODs and deletion debates.
     *
     *  @see <a href="https://en.wikipedia.org/wiki/WP:CSD">Criteria for speedy
     *  deletion</a>
     *  @see <a href="https://en.wikipedia.org/wiki/WP:PROD">Proposed deletion</a>
     *  @see <a href="https://en.wikipedia.org/wiki/WP:XFD">Deletion debates</a>
     *  @param raw the list of raw deletion reasons
     *  @return the grouped deletion reasons
     */
    public Map<String, Long> groupDeleteReasons(Map<String, Long> raw)
    {
        Map<String, Long> cleanhist = new HashMap<>();
        for (var entry : raw.entrySet())
        {
            String reason = entry.getKey();
            long count = entry.getValue();

            boolean unclassified = true;

            // general CSDs
            unclassified &= classifyReason(cleanhist, entry, "Patent nonsense", "|g1]]");
            unclassified &= classifyReason(cleanhist, entry, "Test page", "|g2]]");
            unclassified &= classifyReason(cleanhist, entry, "Vandalism", "|g3]]");
            unclassified &= classifyReason(cleanhist, entry, "Created by block/ban evading sockpuppet", "|g5]]", "csd g5");
            unclassified &= classifyReason(cleanhist, entry, "Maintenance", "|g6]]", "history merge");
            unclassified &= classifyReason(cleanhist, entry, "Author/user request", "g7]]", "|u1]]", "user request", "csd u1", "author request");
            unclassified &= classifyReason(cleanhist, entry, "Dependent on deleted page", "g8]]", "delete redirect: ", "#g8");
            unclassified &= classifyReason(cleanhist, entry, "Attack page", "g10");
            unclassified &= classifyReason(cleanhist, entry, "Spam", "g11", "spam", "advert");
            unclassified &= classifyReason(cleanhist, entry, "Copyright problems", "g12", "|f9]]", ":copyright problems", ":copyright violations");
            unclassified &= classifyReason(cleanhist, entry, "Abandoned draft", "g13");
            unclassified &= classifyReason(cleanhist, entry, "Unnecessary disambiguation", "|g14]]");

            // article CSDs
            unclassified &= classifyReason(cleanhist, entry, "No content or context", "|a1]]", "|a3]]");
            unclassified &= classifyReason(cleanhist, entry, "Foreign language", "|a2]]");
            unclassified &= classifyReason(cleanhist, entry, "Fails to give reason for inclusion", "|a7]]", "|a9]]");
            unclassified &= classifyReason(cleanhist, entry, "Redundant", "|a10]]", "t3]]", "|f1]]");
            unclassified &= classifyReason(cleanhist, entry, "Made up one day", "|a11]]");

            // user CSDs
            unclassified &= classifyReason(cleanhist, entry, "User page where user does not exist", "u2]]");
            unclassified &= classifyReason(cleanhist, entry, "Misuse of Wikipedia as a webhost", "u5]]");

            // redirect CSDs
            unclassified &= classifyReason(cleanhist, entry, "Cross-namespace redirect", "|r2]]");
            unclassified &= classifyReason(cleanhist, entry, "Implausible redirect", "|r3]]");
            unclassified &= classifyReason(cleanhist, entry, "File redirect to Commons", "|r4]]");

            // category CSDs
            unclassified &= classifyReason(cleanhist, entry, "Empty category", "|c1]]");
            unclassified &= classifyReason(cleanhist, entry, "Category renaming or merger", "|c2]]", "[[wp:cfds");

            // file CSDs
            unclassified &= classifyReason(cleanhist, entry, "Corrupt file", "|f2]]");
            unclassified &= classifyReason(cleanhist, entry, "Lack of copyright information (files)", "|f3]]", "|f4]]", "|f11]]");
            unclassified &= classifyReason(cleanhist, entry, "Problems with non-free files", "|f5]]", "|f6]]", "|f7]]");
            unclassified &= classifyReason(cleanhist, entry, "File moved to Commons", "|f8]]", "nowcommons", "now on commons");

            // PROD
            unclassified &= classifyReason(cleanhist, entry, "Expired PROD", "[[wp:prod|", "proposed deletion");
            unclassified &= classifyReason(cleanhist, entry, "Expired BLP PROD", "[[wp:blpprod");

            // reposts need special treatment
            if (reason.contains("|g4]]"))
            {
                cleanhist.merge("Repost of deleted content", count, Long::sum);
                unclassified = false;
            }
            // XFD
            else
            {
                unclassified &= classifyReason(cleanhist, entry, "Deletion debate (MFD)", ":miscellany for deletion/");
                unclassified &= classifyReason(cleanhist, entry, "Deletion debate (AFD)", ":articles for deletion/");
                unclassified &= classifyReason(cleanhist, entry, "Deletion debate (TFD)", ":templates for discussion/");
                unclassified &= classifyReason(cleanhist, entry, "Deletion debate (RFD)", ":redirects for discussion/");
                unclassified &= classifyReason(cleanhist, entry, "Deletion debate (FFD)", ":files for discussion/");
                unclassified &= classifyReason(cleanhist, entry, "Deletion debate (CFD)", ":categories for discussion/");
            }

            unclassified &= classifyReason(cleanhist, entry, "Unclassified nukes", "mass deletion of pages added by");
            if (reason.isEmpty())
            {
                cleanhist.merge("''No reason given''", count, Long::sum);
                unclassified = false;
            }
            if (unclassified)
                cleanhist.merge("Unclassified", count, Long::sum);
        }
        return cleanhist;
    }

    /**
     *  Classifies given reasons and adds them to the appropriate entry in the
     *  grouped reason histogram.
     *
     *  @param clean the histogram of grouped reasons
     *  @param entry the reason to classify with the frequency which it occurs
     *  @param description the reason that is the classification
     *  @param tolookfor the list of key words to look for
     *  @return whether the reason was NOT classified
     */
    public boolean classifyReason(Map<String, Long> clean, Map.Entry<String, Long> entry, String description, String... tolookfor)
    {
        String reason = entry.getKey();
        for (String x : tolookfor)
        {
            if (reason.contains(x))
            {
                clean.merge(description, entry.getValue(), Long::sum);
                return false;
            }
        }
        return true;
    }

    /**
     *  Bins locks of global accounts by reason. The total number of locks is
     *  available in a special key "TOTAL".
     *  @return a map: lock reason &#8594; count
     *  @see <a href="https://meta.wikimedia.org/wiki/SRG">Requests for global
     *  locking</a>
     *  @throws IOException if a network error occurs
     */
    public Map<String, Long> lockStats() throws IOException
    {
        Wiki.RequestHelper rh = metaWiki.new RequestHelper()
            .withinDateRange(start, end);
        List<Wiki.LogEntry> le = metaWiki.getLogEntries("globalauth", null, rh);
        le.removeIf(log -> log.getTitle() == null || log.getComment() == null);
        HashMap<String, Long> ret = new HashMap<>();
        ret.putAll(le.stream()
            .collect(Collectors.groupingBy(log -> log.getComment().toLowerCase(), Collectors.counting())));
        ret.put("TOTAL", Long.valueOf(le.size()));
        return ret;
    }

    /**
     *  Groups similar lock reasons together. Lock reasons are mutually
     *  exclusive.
     *  @param lockhist the histogram of lock reasons to group (remove the TOTAL
     *  key first)
     *  @return a map: lock reason &#8594; count
     */
    public Map<String, Long> groupLockReasons(Map<String, Long> lockhist)
    {
        Map<String, Long> cleanlockhist = new HashMap<>();
        for (var entry : lockhist.entrySet())
        {
            String reason = entry.getKey();
            long count = entry.getValue();

            String key = "Unclassified";
            if (reason.contains("spam-only") || reason.contains("spambot"))
                key = "Spamming";
            else if (reason.contains("long-term abuse") || reason.contains("banned"))
                key = "Long term abuse";
            else if (reason.contains("cross-wiki abuse") || reason.contains("crosswiki abuse"))
                key = "Cross wiki abuse";
            else if (reason.contains("user name") || reason.contains("username"))
                key = "Inappropriate username";
            else if (reason.contains("compromised"))
                key = "Compromised";
            else if (reason.contains("vandalism"))
                key = "Vandalism";

            cleanlockhist.merge(key, count, Long::sum);
        }
        return cleanlockhist;
    }

    /**
     *  Fetches block stats for the English Wikipedia and bins them by reason.
     *  The total number of blocks is available in a special key "TOTAL".
     *  @param accountsonly look at registered accounts only
     *  @param indefonly look at indefinite blocks only
     *  @return a map: block reason &#8594; count
     *  @see <a href="https://en.wikipedia.org/wiki/Special:Blocklist">list of
     *  current blocks</a>
     *  @throws IOException if a network error occurs
     */
    public Map<String, Long> blockStats(boolean accountsonly, boolean indefonly) throws IOException
    {
        Wiki.RequestHelper rh = enWiki.new RequestHelper()
            .withinDateRange(start, end);
        if (accountsonly)
            rh = rh.filterBy(Map.of("account", Boolean.TRUE));
        if (accountsonly)
            rh = rh.filterBy(Map.of("temp", Boolean.FALSE));

        List<Wiki.LogEntry> lelocal = enWiki.getBlockList(null, rh);
        lelocal.removeIf(log -> log.getTitle() == null || log.getComment() == null);
        lelocal.removeIf(log -> log.getUser().equals("ProcseeBot"));
        Map<String, Long> ret = new HashMap<>();
        ret.putAll(lelocal.stream()
            .collect(Collectors.groupingBy(log -> log.getComment().toLowerCase(), Collectors.counting())));
        ret.put("TOTAL", Long.valueOf(lelocal.size()));
        return ret;
    }

    /**
     *  Groups similar block reasons together. Block reasons are mutually
     *  exclusive.
     *  @param blockhist the block reason histogram to group (remove the TOTAL
     *  key first)
     *  @return a histogram with grouped block reasons
     */
    public Map<String, Long> groupBlockReasons(Map<String, Long> blockhist)
    {
        Map<String, Long> cleanblockhist = new HashMap<>();
        for (var entry : blockhist.entrySet())
        {
            String reason = entry.getKey();
            long count = entry.getValue();

            String key = "Unclassified";
            // sockpuppetry
            if (reason.contains("{{checkuserblock-account}}") || reason.contains("sock") ||
                reason.contains("block evasion") || reason.contains("term abuse") || reason.contains("banned"))
                key = "Sockpuppetry and long term abuse";
            // spamming
            else if (reason.contains("spam") || reason.contains("advertising") ||
                reason.contains("promotion") || reason.contains("[[wp:paid"))
                key = "Spamming";
            // possible spamming
            else if (reason.contains("{{uw-softerblock}}") || reason.contains("{{uw-causeblock}}"))
                key = "Promotional username soft blocks";
            // vandals
            else if (reason.contains("vandalism") || reason.contains("{{uw-vaublock}}")
                || reason.contains("{{school block}}"))
                key = "Vandalism";
            else if (reason.contains("edit filter"))
                key = "Triggering the edit filter";
            // unauthorized or other bot problems
            else if (reason.matches("\\sbot\\s") || reason.contains("{{uw-botublock"))
                key = "Unauthorized, malfunctioning bot or bot username";
            // other bad usernames
            else if (reason.contains("<!-- username ") || reason.contains("{{uw-ublock") || reason.contains("{{uw-uhblock"))
                key = "Other inappropriate username";
            // NOTHERE
            else if (reason.contains("nothere") || reason.contains("not here"))
                key = "Not here to build the encyclopedia";
            // BLP
            else if (reason.contains("[[wp:biographies") || reason.contains("blp"))
                key = "BLP violations";
            // tendentious editing
            else if (reason.contains("[[wp:disruptive"))
                key = "Disruptive editing";
            // harassment
            else if (reason.contains("harass") || reason.contains("[[wp:no personal")
                || reason.contains("{{oversight") || reason.contains("trolling")
                || reason.contains("attack page"))
                key = "Harassment";
            // addition of unsourced material
            else if (reason.contains("unsourced content") || reason.contains("citing sources"))
                key = "Addition of unsourced material";
            // proxies
            else if (reason.contains("{{colocation") || reason.contains("{{webhost") || reason.contains(" proxy}}"))
                key = "Open proxy/webhost";
            // anon block
            else if (reason.contains("{{rangeblock") || reason.contains("{{checkuser"))
                key = "Range blocks";
            else if (reason.contains("{{anonblock"))
                key = "Anonymous blocks";

            cleanblockhist.merge(key, count, Long::sum);
        }
        return cleanblockhist;
    }

    public static void printHistogram(Map<String, Long> hist, boolean collapsible)
    {
        if (collapsible)
            System.out.println("{| class=\"wikitable sortable collapsible collapsed\"");
        else
            System.out.println("{| class=\"wikitable sortable\"");

        System.out.println("! Reason !! Count");
        for (var entry : hist.entrySet())
        {
            String reason = collapsible ? "<nowiki>" + entry.getKey() + "</nowiki>" : entry.getKey();
            System.out.println(WikitextUtils.addTableRow(List.of(reason, entry.getValue().toString())));
        }

        System.out.println("|}");
    }
}
