/**
 *  @(#)AdminStats.java 0.02 01/01/2021
 *  Copyright (C) 2019-2021 MER-C
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
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.wikipedia.*;

/**
 *  Outputs admin action reason stats for the English Wikipedia and global lock
 *  reason stats for all Wikimedia wikis.
 *  @author MER-C
 *  @version 0.02
 */
public class AdminStats
{
    private static final Wiki metaWiki;
    private final Wiki wiki;
    private OffsetDateTime start, end;
    private List<Wiki.LogEntry> deletions, blocks, locks, protections, gblocks;

    static
    {
        metaWiki = Wiki.newSession("meta.wikimedia.org");
    }
    
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
            .addBooleanFlag("--blocks", "Fetch statistics for blocks")
            .addBooleanFlag("--indefs", "Fetch statistics for indefinite blocks only (requires --blocks)")
            .addBooleanFlag("--accounts", "Fetch statistics for blocked accounts only (requires --blocks)")
            .addBooleanFlag("--deletions", "Fetch statistics for deletions")
            .addBooleanFlag("--protections", "Fetch statistics for protections")
            .addBooleanFlag("--globalblocks", "Fetch statistics for global blocks")
            .addBooleanFlag("--login", "Adds a login prompt to access high limits")
            .parse(args);
        if (!options.containsKey("--start") || !options.containsKey("--end"))
        {
            System.err.println("ERROR: You must specify a start and end date.");
            System.exit(1);
        }
        OffsetDateTime start = OffsetDateTime.parse(options.get("--start"));
        OffsetDateTime end = OffsetDateTime.parse(options.get("--end"));
        boolean printfull = options.containsKey("--printfull");

        Wiki enWiki = Wiki.newSession("en.wikipedia.org");
        if (options.containsKey("--login"))
            Users.of(enWiki).cliLogin();
        
        AdminStats stats = new AdminStats(enWiki);
        stats.setDateRange(start, end);

        if (options.containsKey("--locks"))
        {
            Map<String, Long> lockhist = stats.lockStats();
            long total = lockhist.get("TOTAL");
            if (!printfull)
                lockhist = stats.groupLockReasons(lockhist);
            System.out.println("==Lock stats==");
            System.out.println("" + total + " locks between " + start + " and " + end);
            export(lockhist, "locks.csv");
        }

        if (options.containsKey("--blocks"))
        {
            Boolean accounts = null, indefs = null;
            if (options.containsKey("--accounts"))
                accounts = Boolean.TRUE;
            if (options.containsKey("--indefs"))
                indefs = Boolean.TRUE;
            Map<String, Long> blockhist = stats.blockStats(accounts, indefs);
            long total = blockhist.get("TOTAL");
            if (!printfull)
                blockhist = stats.groupBlockReasons(blockhist);
            System.out.println("==Block stats==");
            System.out.println("" + total + " blocks between " + start + " and " + end);
            export(blockhist, "blocks.csv");
        }

        if (options.containsKey("--deletions"))
        {
            Map<String, Long> deletehist = stats.deleteStats();
            long total = deletehist.get("TOTAL");
            if (!printfull)
                deletehist = stats.groupDeleteReasons(deletehist);
            System.out.println("==Deletion stats==");
            System.out.println("" + total + " deletions between " + start + " and " + end);
            export(deletehist, "deletions-all.csv");

            deletehist = stats.deleteStats(Wiki.MAIN_NAMESPACE);
            total = deletehist.get("TOTAL");
            if (!printfull)
                deletehist = stats.groupDeleteReasons(deletehist);
            System.out.println("===Main namespace===");
            System.out.println("" + total + " deletions between " + start + " and " + end);
            export(deletehist, "deletions-main.csv");

            deletehist = stats.deleteStats(Wiki.USER_NAMESPACE);
            total = deletehist.get("TOTAL");
            if (!printfull)
                deletehist = stats.groupDeleteReasons(deletehist);
            System.out.println("===User namespace===");
            System.out.println("" + total + " deletions between " + start + " and " + end);
            export(deletehist, "deletions-user.csv");

            deletehist = stats.deleteStats(118); // draft namespace
            total = deletehist.get("TOTAL");
            if (!printfull)
                deletehist = stats.groupDeleteReasons(deletehist);
            System.out.println("===Draft namespace===");
            System.out.println("" + total + " deletions between " + start + " and " + end);
            export(deletehist, "deletions-draft.csv");
        }
        
        if (options.containsKey("--protections"))
        {
            Map<String, Long> prothist = stats.protectStats();
            long total = prothist.get("TOTAL");
            if (!printfull)
                prothist = stats.groupProtectionReasons(prothist);
            System.out.println("==Protection stats==");
            System.out.println("" + total + " protections between " + start + " and " + end);
            export(prothist, "protections-all.csv");
            
            prothist.clear();
            prothist = stats.protectStats(Wiki.MAIN_NAMESPACE);
            total = prothist.get("TOTAL");
            if (!printfull)
                prothist = stats.groupProtectionReasons(prothist);
            System.out.println("===Main namespace===");
            System.out.println("" + total + " protections between " + start + " and " + end);
            export(prothist, "protections-main.csv");
        }
        
        if (options.containsKey("--globalblocks"))
        {
            Map<String, Long> blockhist = stats.globalBlockStats();
            long total = blockhist.get("TOTAL");
            if (!printfull)
                blockhist = stats.groupGlobalBlockReasons(blockhist);
            System.out.println("==Global block stats==");
            System.out.println("" + total + " global blocks between " + start + " and " + end);
            export(blockhist, "gblocks.csv");
        }
    }

    public AdminStats(Wiki wiki)
    {
        this.wiki = wiki;
        deletions = new ArrayList<>();
        blocks = new ArrayList<>();
        locks = new ArrayList<>();
        protections = new ArrayList<>();
        gblocks = new ArrayList<>();
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
        deletions.clear();
        blocks.clear();
        locks.clear();
        protections.clear();
    }

    /**
     *  Computes a histogram of page deletions by reason. The total number of
     *  deletions is available under a special TOTAL key.
     *  @param namespaces limit results to these namespaces (empty array = all
     *  namespaces)
     *  @return a (raw) histogram of deletion reasons
     *  @throws IOException if a network error occurs
     */
    public Map<String, Long> deleteStats(int... namespaces) throws IOException
    {
        if (deletions.isEmpty())
        {
            Wiki.RequestHelper rh = wiki.new RequestHelper()
                .withinDateRange(start, end);
            List<Wiki.LogEntry> temp = wiki.getLogEntries(Wiki.DELETION_LOG, "delete", rh);
            for (Wiki.LogEntry log : temp)
                if (log.getTitle() != null && log.getComment() != null)
                    deletions.add(log);
        }
        
        List<Wiki.LogEntry> lelocal = new ArrayList<>(deletions);
        if (namespaces.length > 0)
        {
            lelocal.removeIf(log ->
            {
                int ns = wiki.namespace(log.getTitle());
                for (int ns2 : namespaces)
                    if (ns2 == ns)
                        return false;
                return true;
            });
        }
        Map<String, Long> ret = new TreeMap<>();
        ret.putAll(lelocal.stream().collect(Collectors.groupingBy(
                log -> log.getComment().replace("_", " ").toLowerCase(), Collectors.counting())));
        ret.put("TOTAL", Long.valueOf(lelocal.size()));
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
        Map<String, Long> cleanhist = new TreeMap<>();
        for (var entry : raw.entrySet())
        {
            String reason = entry.getKey();
            long count = entry.getValue();
            if (reason.equals("TOTAL"))
                continue;
            boolean unclassified = true;
            
            // consume copyright problems
            if (reason.contains(":copyright problems"))
            {
                cleanhist.merge("Copyright problems", count, Long::sum);
                unclassified = false;
            }

            // general CSDs
            unclassified &= classifyReason(cleanhist, entry, "Patent nonsense", "|g1]]");
            unclassified &= classifyReason(cleanhist, entry, "Test page", "|g2]]");
            unclassified &= classifyReason(cleanhist, entry, "Vandalism", "|g3]]");
            unclassified &= classifyReason(cleanhist, entry, "Created by block/ban evading sockpuppet", "|g5]]", "csd g5");
            unclassified &= classifyReason(cleanhist, entry, "Maintenance", "|g6]]", "history merge", "history-merge");
            unclassified &= classifyReason(cleanhist, entry, "Author/user request", "g7]]", "|u1]]", "user request", "csd u1", "author request");
            unclassified &= classifyReason(cleanhist, entry, "Dependent on deleted page", "g8]]", "delete redirect: ", "#g8");
            unclassified &= classifyReason(cleanhist, entry, "Attack page", "g10");
            unclassified &= classifyReason(cleanhist, entry, "Spam", "g11", "spam", "advert");
            unclassified &= classifyReason(cleanhist, entry, "Copyright violations", "g12", "|f9]]", ":copyright violations");
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
            unclassified &= classifyReason(cleanhist, entry, "File moved to Commons", "|f8]]", "nowcommons", 
                "now on commons", "now on wikimedia commons");

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
            {
//                System.out.println(reason + ": " + count);
                cleanhist.merge("Unclassified", count, Long::sum);
            }
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
     *  available in a special key "TOTAL". Caveat: it is not currently possible
     *  to determine whether a log entry represents a lock or unlock because they
     *  are the same action. This is buried in the log details. (FIXME for WMFWiki).
     * 
     *  @return a map: lock reason &#8594; count
     *  @see <a href="https://meta.wikimedia.org/wiki/SRG">Requests for global
     *  locking</a>
     *  @throws IOException if a network error occurs
     */
    public Map<String, Long> lockStats() throws IOException
    {
        if (locks.isEmpty())
        {
            Wiki.RequestHelper rh = metaWiki.new RequestHelper()
                .withinDateRange(start, end);
            locks = metaWiki.getLogEntries(WMFWiki.GLOBAL_AUTH_LOG, null, rh);
            locks.removeIf(log -> log.getTitle() == null || log.getComment() == null);
        }
        Map<String, Long> ret = new TreeMap<>();
        ret.putAll(locks.stream()
            .collect(Collectors.groupingBy(log -> log.getComment().toLowerCase(), Collectors.counting())));
        ret.put("TOTAL", Long.valueOf(locks.size()));
        return ret;
    }

    /**
     *  Groups similar lock reasons together. Lock reasons are mutually
     *  exclusive.
     *  @param lockhist the histogram of lock reasons to group 
     *  @return a map: lock reason &#8594; count
     */
    public Map<String, Long> groupLockReasons(Map<String, Long> lockhist)
    {
        Map<String, Long> cleanlockhist = new TreeMap<>();
        for (var entry : lockhist.entrySet())
        {
            String reason = entry.getKey();
            long count = entry.getValue();
            if (reason.equals("TOTAL"))
                continue;

            String key = "Unclassified";
            if (reason.equals(""))
                key = "(no reason given)";
            else if (reason.contains("spam"))
                key = "Spamming";
            else if (reason.contains("long-term abuse") || reason.contains("banned") || reason.contains("lock evasion"))
                key = "Long term abuse";
            else if (reason.contains("cross-wiki abuse") || reason.contains("crosswiki abuse"))
                key = "Cross wiki abuse";
            else if (reason.contains("user name") || reason.contains("username") || reason.contains("impersonation"))
                key = "Inappropriate username";
            else if (reason.contains("compromised"))
                key = "Compromised";
            else if (reason.contains("vandalism"))
                key = "Vandalism";
//            else
//                System.out.println(reason + ": " + count);
            
            cleanlockhist.merge(key, count, Long::sum);
        }
        return cleanlockhist;
    }

    /**
     *  Fetches block stats for the English Wikipedia and bins them by reason.
     *  The total number of blocks is available in a special key "TOTAL".
     *  @param accounts true = accounts only, false = IP addresses only, 
     *  null = both
     *  @param indefs true = indefinite blocks only, false = temporary blocks
     *  only, null = both
     *  @return a map: block reason &#8594; count
     *  @throws IOException if a network error occurs
     */
    public Map<String, Long> blockStats(Boolean accounts, Boolean indefs) throws IOException
    {
        if (blocks.isEmpty())
        {
            Wiki.RequestHelper rh = wiki.new RequestHelper()
                .withinDateRange(start, end);
            // Special:Blocklist contains current blocks only
            List<Wiki.LogEntry> lelocal = wiki.getLogEntries(Wiki.BLOCK_LOG, "block", rh);
            for (Wiki.LogEntry log : lelocal)
                if (log.getTitle() != null && log.getComment() != null)
                    blocks.add(log);
        }
        
        // filter expiry
        List<Wiki.LogEntry> lelocal = new ArrayList<>(blocks);
        if (indefs != null)
        {
            lelocal.removeIf(log ->
            {
                String expiry = log.getDetails().get("expiry");
                boolean indefinite = expiry.equals("infinity");
                if (Boolean.TRUE.equals(indefs) && indefinite)
                    return false;
                if (Boolean.FALSE.equals(indefs) && !indefinite)
                    return false;
                return true;
            });
        }
        
        // filter accounts
        if (accounts != null)
        {
            lelocal.removeIf(log ->
            {
                String user = log.getTitle();
                // quick and dirty
                boolean ip = user.matches("User:\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")
                    || user.matches("User:([0-9a-f]{0,4}:){1,}[0-9a-f]{0,4}")
                    || user.contains("/"); // rangeblocks, forbidden character in usernames
                if (Boolean.TRUE.equals(accounts) && !ip)
                    return false;
                if (Boolean.FALSE.equals(accounts) && ip)
                    return false;
                return true;
            });
        }
        
        Map<String, Long> ret = new TreeMap<>();
        ret.putAll(lelocal.stream()
            .collect(Collectors.groupingBy(log -> log.getComment().toLowerCase(), Collectors.counting())));
        ret.put("TOTAL", Long.valueOf(lelocal.size()));
        return ret;
    }

    /**
     *  Groups similar block reasons together. Block reasons are mutually
     *  exclusive.
     *  @param blockhist the block reason histogram to group
     *  @return a histogram with grouped block reasons
     */
    public Map<String, Long> groupBlockReasons(Map<String, Long> blockhist)
    {
        Map<String, Long> cleanblockhist = new TreeMap<>();
        for (var entry : blockhist.entrySet())
        {
            String reason = entry.getKey();
            long count = entry.getValue();
            if (reason.equals("TOTAL"))
                continue;

            String key = "Unclassified";
            // spamming
            if (reason.contains("spam") || reason.contains("advertising") ||
                reason.contains("promotion") || reason.contains("[[wp:paid"))
                key = "Spamming";
            // possible spamming
            else if (reason.contains("{{uw-softerblock}}") || reason.contains("{{uw-causeblock}}"))
                key = "Promotional username soft blocks";
            // copyright problems
            else if (reason.contains("copyright"))
                key = "Copyright violations";
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
            else if (reason.contains("[[wp:edit warring"))
                key = "Edit warring";
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
            // sockpuppetry
            // deliberately low down to capture as many underlying block reasons as possible
            // must be after proxies because some proxies use the SOCKS protocol and this is cited
            // in block summaries
            else if (reason.contains("{{checkuserblock-account}}") || reason.contains("sock") ||
                reason.contains("block evasion") || reason.contains("term abuse") || reason.contains("banned"))
                key = "Sockpuppetry and long term abuse";
            // anon block
            else if (reason.contains("{{rangeblock") || reason.contains("{{checkuser"))
                key = "Range blocks";
            else if (reason.contains("{{anonblock"))
                key = "Anonymous blocks";
//            else
//                System.out.println(reason + ": " + count);


            cleanblockhist.merge(key, count, Long::sum);
        }
        return cleanblockhist;
    }
    
    /**
     *  Fetches page protection stats for the English Wikipedia and bins them 
     *  by reason. The total number of protections is available in a special key
     *  "TOTAL".
     *  @param namespaces limit results to these namespaces (empty array = all
     *  namespaces)
     *  @return a map: block reason &#8594; count
     *  @throws IOException if a network error occurs
     */
    public Map<String, Long> protectStats(int... namespaces) throws IOException
    {
        if (protections.isEmpty())
        {
            Wiki.RequestHelper rh = wiki.new RequestHelper()
                .withinDateRange(start, end);
            List<Wiki.LogEntry> lelocal = wiki.getLogEntries(Wiki.PROTECTION_LOG, "protect", rh);
            for (Wiki.LogEntry log : lelocal)
                if (log.getTitle() != null && log.getComment() != null)
                    protections.add(log);
        }
        
        // namespace filter
        List<Wiki.LogEntry> lelocal = new ArrayList<>(protections);
        if (namespaces.length > 0)
        {
            lelocal.removeIf(log ->
            {
                int ns = wiki.namespace(log.getTitle());
                for (int ns2 : namespaces)
                    if (ns2 == ns)
                        return false;
                return true;
            });
        }
        
        Map<String, Long> ret = new TreeMap<>();
        ret.putAll(lelocal.stream()
            .collect(Collectors.groupingBy(log -> log.getComment().toLowerCase(), Collectors.counting())));
        ret.put("TOTAL", Long.valueOf(lelocal.size()));
        return ret;
    }
    
    /**
     *  Groups similar protection reasons together. Protection reasons are mutually
     *  exclusive.
     *  @param prothist the protection reason histogram to group
     *  @return a histogram with grouped protection reasons
     */
    public Map<String, Long> groupProtectionReasons(Map<String, Long> prothist)
    {
        // TODO: add protection level, type and expiry
        Map<String, Long> cleanprothist = new TreeMap<>();
        for (var entry : prothist.entrySet())
        {
            String reason = entry.getKey();
            long count = entry.getValue();
            if (reason.equals("TOTAL"))
                continue;

            String key = "Unclassified";
            if (reason.equals(""))
                key = "(no reason given)";
            // deliberately high up
            else if (reason.contains("arbitration enforcement") || reason.contains("wp:a/i/pia"))
                key = "Arbitration enforcement";
            else if (reason.contains("[[wp:gs/"))
                key = "General sanctions enforcement";
            // remaining content based reasons
            else if (reason.contains("[[wp:blp"))
                key = "BLP violations";
            else if (reason.contains("spam"))
                key = "Spamming";
            else if (reason.contains("copyright") || reason.contains("copyvio"))
                key = "Copyright violations";
            else if (reason.contains("[[wp:pp#content dispute") || reason.contains("edit war") || reason.contains("move war"))
                key = "Edit warring/content dispute";
            else if (reason.contains("verifiability") || reason.contains("[[wp:intref"))
                key = "Addition of unsourced material";
            else if (reason.contains("vandal"))
                key = "Vandalism";
            else if (reason.contains("wp:pp#user pages"))
                key = "User request";
            else if (reason.contains("high-risk") || reason.contains("highly visible") || reason.contains("upcoming tfa"))
                key = "High risk page";
            
            // deliberately low down to capture as many underlying reasons as possible
            else if (reason.contains("sock") || reason.contains("block evasion") || reason.contains("lta"))
                key = "Sock puppetry";
            else if (reason.contains("[[wp:disruptive editing"))
                key = "Unclassified disruptive editing";
            else if (reason.contains("[[wp:salt"))
                key = "Unclassified salting";
//            else
//                System.out.println(reason + ": " + count);

            cleanprothist.merge(key, count, Long::sum);
        }
        return cleanprothist;
    }
    
    /**
     *  Bins locks of global accounts by reason. The total number of blocks is
     *  available in a special key "TOTAL". 
     * 
     *  @return a map: block reason &#8594; count
     *  @see <a href="https://meta.wikimedia.org/wiki/SRG">Requests for global
     *  blocks</a>
     *  @throws IOException if a network error occurs
     *  @since 0.02
     */
    public Map<String, Long> globalBlockStats() throws IOException
    {
        if (gblocks.isEmpty())
        {
            Wiki.RequestHelper rh = metaWiki.new RequestHelper()
                .withinDateRange(start, end);
            gblocks = metaWiki.getLogEntries(WMFWiki.GLOBAL_BLOCK_LOG, "gblock2", rh);
            gblocks.removeIf(log -> log.getTitle() == null || log.getComment() == null);
        }
        Map<String, Long> ret = new TreeMap<>();
        ret.putAll(gblocks.stream()
            .collect(Collectors.groupingBy(log -> log.getComment().toLowerCase(), Collectors.counting())));
        ret.put("TOTAL", Long.valueOf(gblocks.size()));
        return ret;
    }
    
    /**
     *  Groups similar global block reasons together. Block reasons are mutually
     *  exclusive.
     *  @param blockhist the block reason histogram to group
     *  @return a histogram with grouped block reasons
     *  @since 0.02
     */
    public Map<String, Long> groupGlobalBlockReasons(Map<String, Long> blockhist)
    {
        Map<String, Long> cleanblockhist = new TreeMap<>();
        for (var entry : blockhist.entrySet())
        {
            String reason = entry.getKey();
            long count = entry.getValue();
            if (reason.equals("TOTAL"))
                continue;

            String key = "Unclassified";
            // spamming
            if (reason.equals(""))
                key = "(no reason given)";
            else if (reason.contains("spam"))
                key = "Spamming";
            else if (reason.contains("long-term abuse") || reason.contains("banned") || reason.contains("lock evasion"))
                key = "Long term abuse";
            else if (reason.contains("cross-wiki abuse") || reason.contains("crosswiki abuse") || reason.contains("cross wiki abuse"))
                key = "Cross wiki abuse";
            else if (reason.contains("open prox"))
                key = "Open proxy/webhost";
            else if (reason.contains("vandalism"))
                key = "Vandalism";
//            else
//                System.out.println(reason + ": " + count);

            cleanblockhist.merge(key, count, Long::sum);
        }
        return cleanblockhist;
    }

    /**
     *  Exports results to wikitext and prints them out to standard output, and
     *  exports results to CSV writing them to the given filename.
     *  @param hist the histogram to export
     *  @param filename the CSV filename to export to
     *  @throws IOException if a filesystem error occurs
     */
    public static void export(Map<String, Long> hist, String filename) throws IOException
    {
        DataTable dt = DataTable.create(hist, List.of("Reason", "Count"));
        System.out.println(dt.formatAsWikitext());
        Files.writeString(Paths.get(filename), dt.formatAsCSV());
    }
       
    /**
     *  Fetches the list of block log entries used to compute statistics.
     *  @return (see above)
     */
    public List<Wiki.LogEntry> getBlockLogEntries()
    {
        return new ArrayList<>(blocks);
    }
    
    /**
     *  Fetches the list of deletion log entries used to compute statistics.
     *  @return (see above)
     */
    public List<Wiki.LogEntry> getDeleteLogEntries()
    {
        return new ArrayList<>(deletions);
    }
    
    /**
     *  Fetches the list of global account log entries used to compute statistics.
     *  @return (see above)
     */
    public List<Wiki.LogEntry> getLockLogEntries()
    {
        return new ArrayList<>(locks);
    }
    
    /**
     *  Fetches the list of protection log entries used to compute statistics.
     *  @return (see above)
     */
    public List<Wiki.LogEntry> getProtectLogEntries()
    {
        return new ArrayList<>(protections);
    }
    
    /**
     *  Fetches the list of global block log entries used to compute statistics.
     *  @return (see above)
     *  @since 0.02
     */
    public List<Wiki.LogEntry> getGlobalBlockLogEntries()
    {
        return new ArrayList<>(protections);
    }
}
