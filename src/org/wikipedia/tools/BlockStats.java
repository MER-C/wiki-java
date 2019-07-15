/**
 *  @(#)BlockStats.java 0.01 15/07/2019
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

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.wikipedia.*;

/**
 *  Outputs block reason stats for the English Wikipedia and global lock reason
 *  stats for all Wikimedia wikis.
 *  @author MER-C
 *  @version 0.01
 */
public class BlockStats
{
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
            .parse(args);
        OffsetDateTime start = OffsetDateTime.parse(options.get("--start"));
        OffsetDateTime end = OffsetDateTime.parse(options.get("--end"));
            
        Wiki wiki = Wiki.newSession("meta.wikimedia.org");
        Wiki.RequestHelper rh = wiki.new RequestHelper()
            .withinDateRange(start, end);
        List<Wiki.LogEntry> le = wiki.getLogEntries("globalauth", null, rh);
        
        // raw lock reasons
        Map<String, Long> lockhist = le.stream()            
            .collect(Collectors.groupingBy(log -> log.getComment().toLowerCase(), Collectors.counting()));
        
        // group common lock reasons
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

        wiki = Wiki.newSession("en.wikipedia.org");
        rh = wiki.new RequestHelper()
//            .filterBy(Map.of("temp", Boolean.FALSE, "account", Boolean.TRUE))
            .withinDateRange(start, end);
        List<Wiki.LogEntry> lelocal = wiki.getBlockList(null, rh);
         lelocal.removeIf(log -> log.getUser().equals("ProcseeBot"));
        
        // raw block reasons
        Map<String, Long> blockhist = lelocal.stream()
            .collect(Collectors.groupingBy(log -> log.getComment().toLowerCase(), Collectors.counting()));
        
        // group common lock reasons
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
                key = "Rangeblocks";
            else if (reason.contains("{{anonblock"))
                key = "Anonymous blocks";
                
            cleanblockhist.merge(key, count, Long::sum);
        }
        
        System.out.println("==Lock stats==");
        System.out.println("" + le.size() + " locks between " + le.get(le.size() - 1).getTimestamp()
            + " and " + le.get(0).getTimestamp());
        printHistogram(cleanlockhist, false);
        
        System.out.println("==Block stats==");        
        System.out.println("" + lelocal.size() + " indefinite blocks between " 
            + lelocal.get(lelocal.size() - 1).getTimestamp() + " and " 
            + lelocal.get(0).getTimestamp());
        printHistogram(cleanblockhist, false);
        
        System.out.println("==Full reasons==");
        printHistogram(lockhist, true);
        printHistogram(blockhist, true);
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
