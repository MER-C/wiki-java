/**
 *  @(#)Unpatroller.java 0.01 09/08/2020
 *  Copyright (C) 2020 - 20xx MER-C and contributors
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

import java.util.*;
import org.wikipedia.*;

/**
 *  Mass unpatrols pages patrolled by a given user.
 *  @author MER-C
 *  @version 0.01
 */
public class Unpatroller
{
    /**
     *  Runs this program.
     *  @param args the command line arguments: [0] user to unpatrol [1] reason
     *  for unpatrolling
     */
    public static void main(String[] args) throws Exception
    {
        WMFWiki enWiki = WMFWiki.newSession("en.wikipedia.org");
        org.wikiutils.LoginUtils.guiLogin(enWiki);
        Wiki.RequestHelper rh = enWiki.new RequestHelper()
            .byUser(args[0]);
        List<Wiki.LogEntry> logs = enWiki.getLogEntries("pagetriage-curation", "reviewed", rh);
        rh = enWiki.new RequestHelper();
        List<String> titles = new ArrayList<>();
        for (Wiki.LogEntry log : logs)
            titles.add(log.getTitle());
        var pageinfo = enWiki.getPageInfo(titles);
        for (int i = 0; i < logs.size(); i++)
        {
            Wiki.LogEntry log = logs.get(i);
            String title = log.getTitle();
            if (!(Boolean)pageinfo.get(i).get("exists"))
            {
                System.out.println("Skipping " + title + " - article may have been deleted.");
                continue;
            }
            List<Wiki.LogEntry> pagelogs = enWiki.getLogEntries("pagetriage-curation", null, rh.byTitle(title));
            if (pagelogs.get(0).getUser().equals(log.getUser()))
            {
                System.out.println("Unpatrolling " + title);
                enWiki.triageNewPage((Long)pageinfo.get(i).get("pageid"), args[1], false, true);
            }
            // else: page has been subsequently un/repatrolled by someone
            System.out.println("Skipping " + title + " - article has been unpatrolled already.");
        }
    }
}
