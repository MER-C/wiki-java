/**
 *  @(#)LogEntries.java 0.01 02/06/2024
 *  Copyright (C) 2024-20XX MER-C and contributors
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

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 *  Utility class for {@link Wiki.LogEntry}. For utility methods and data specific 
 *  to {@link Wiki.Revision}s, see {@link Revisions}.
 *  @author MER-C
 *  @version 0.01
 */
public class LogEntries
{
    /**
     *  Turns a list of revisions into human-readable wikitext. Be careful, as
     *  slowness may result when copying large amounts of wikitext produced by
     *  this method, or by the wiki trying to parse it. Takes the form of:
     *
     *  <p>*2009-01-01 00:00 User (talk | contribs) [action] [target] (comment)
     *  @param logs a bunch of log entries
     *  @return those log entries formatted as wikitext
     */
    public static String toWikitext(Iterable<Wiki.LogEntry> logs)
    {
        StringBuilder buffer = new StringBuilder(100000);
        buffer.append("<div style=\"font-family: monospace; font-size: 120%\">\n");
        for (Wiki.LogEntry log : logs)
        {
            // timestamp
            buffer.append("*");
            buffer.append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(log.getTimestamp()));
            buffer.append(" ");
            
            // user
            String user2 = log.getUser();
            if (user2 == null || user2.equals(Wiki.Event.USER_DELETED))
                buffer.append(Events.DELETED_EVENT_HTML);
            else
            {
                buffer.append(Users.generateWikitextSummaryLinksShort(user2));
                buffer.append(" ");
            }
            
            // action
            buffer.append(log.getAction());
            buffer.append(" ");
            
            // target
            String target = log.getTitle();
            if (target != null)
            {
                buffer.append("[[:");
                buffer.append(target);
                buffer.append("]] ");
            }
            
            // comment
            String summary = log.getComment();
            if (summary == null || summary.equals(Wiki.Event.COMMENT_DELETED))
                buffer.append(Events.DELETED_EVENT_HTML);
            else
            {
                // kill wikimarkup
                buffer.append("(<nowiki>");
                buffer.append(summary);
                buffer.append("</nowiki>)");
            }
            
            // details
            Map details = log.getDetails();
            if (details != null && !details.isEmpty())
            {
                buffer.append(" ");
                buffer.append(details.toString());
            }
            buffer.append("\n");
        }
        buffer.append("</div>");
        return buffer.toString();
    }
}