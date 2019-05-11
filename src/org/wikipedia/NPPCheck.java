/**
 *  @(#)NPPCheck.java 0.01 11/05/2019
 *  Copyright (C) 2019 - 20xx MER-C
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
package org.wikipedia;

import java.time.*;
import java.util.*;

/**
 *  Provides a listing of NPP article patrols and AFC acceptances for a given
 *  user.
 *  @author MER-C
 *  @version 0.01
 */
public class NPPCheck
{
    /**
     *  Runs this program.
     *  @param args args[0] is the username
     *  @throws Exception if a network error occurs
     */
    public static void main(String[] args) throws Exception
    {
        Wiki enWiki = Wiki.newSession("en.wikipedia.org");
        
        // patrol log
        Wiki.RequestHelper rh = enWiki.new RequestHelper().byUser(args[0]).inNamespaces(Wiki.MAIN_NAMESPACE);
        List<Wiki.LogEntry> le = enWiki.getLogEntries("patrol", "patrol", rh);
        System.out.println("==NPP patrols ==");
        System.out.println("{| class=\"wikitable sortable\"");
        System.out.println("! Title !! Create timestamp !! Patrol timestamp !! Article age (s) !! "
            + "Time since last patrol (s) !! Creator");
        OffsetDateTime last = null;
        for (Wiki.LogEntry log : le)
        {
            System.out.println("|-");
            String title = log.getTitle();
            
            OffsetDateTime patroldate = log.getTimestamp();
            OffsetDateTime createdate = null;
            String user = null;
            if (enWiki.namespace(title) == Wiki.MAIN_NAMESPACE)
            {
                Wiki.Revision first = enWiki.getFirstRevision(title);
                if (first != null)
                {
                    user = first.getUser();
                    createdate = first.getTimestamp();
                }
            }
            
            // log in reverse chronological order
            Duration dt_patrol = last == null ? null : Duration.between(patroldate, last);
            if (createdate == null)
            {
                System.out.printf("| [[:%s]] || NA || %s || NA || %d || {{user|%s}}\n", 
                    title, patroldate, dt_patrol == null ? -1 : dt_patrol.getSeconds(), user);
            }
            else
            {
                Duration dt_article = Duration.between(createdate, patroldate);            
                System.out.printf("| [[:%s]] || %s || %s || %d || %d || {{user|%s}}\n", 
                    title, createdate, patroldate, dt_article.getSeconds(),  
                    dt_patrol == null ? -1 : dt_patrol.getSeconds(), user);
            }
            last = patroldate;
        }
        System.out.println("|}\n");
        
        // AFC acceptances
        rh = enWiki.new RequestHelper().byUser(args[0]).inNamespaces(118);
        le = enWiki.getLogEntries(Wiki.MOVE_LOG, "move", rh);
        System.out.println("==AFC acceptances ==");
        System.out.println("{| class=wikitable");
        System.out.println("! Draft !! Title !! Create timestamp !! Accept timestamp !! Draft age (s) !! "
            + "Time since last accept (s) !! Creator");
        last = null;
        for (Wiki.LogEntry log : le)
        {
            System.out.println("|-");
            
            String title = (String)log.getDetails();
            OffsetDateTime patroldate = log.getTimestamp();
            OffsetDateTime createdate = null;
            String user = null;
            if (enWiki.namespace(title) == Wiki.MAIN_NAMESPACE)
            {
                Wiki.Revision first = enWiki.getFirstRevision(title);
                if (first != null)
                {
                    user = first.getUser();
                    createdate = first.getTimestamp();
                }
            }
            
            Duration dt_patrol = last == null ? null : Duration.between(patroldate, last);
            if (createdate == null)
            {
                System.out.printf("| [[:%s]] || [[:%s]] || NA || %s || NA || %d || {{user|%s}}\n", 
                    log.getTitle(), title, patroldate, dt_patrol == null ? -1 : dt_patrol.getSeconds(), user);
            }
            else
            {
                Duration dt_article = Duration.between(createdate, patroldate); 
                System.out.printf("| [[:%s]] || [[:%s]] || %s || %s || %d || %d || {{user|%s}}\n", 
                    log.getTitle(), title, createdate, patroldate, dt_article.getSeconds(), 
                    dt_patrol == null ? -1 : dt_patrol.getSeconds(), user);
            }
            last = patroldate;
        }
        System.out.println("|}");
    }
}
