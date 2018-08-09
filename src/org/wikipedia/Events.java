/**
 *  @(#)Events.java 0.01 26/06/2018
 *  Copyright (C) 2018-20XX MER-C and contributors
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

import java.util.*;
import java.time.*;

/**
 *  Utility class for {@link Wiki.Event}s. For utility methods and data specific 
 *  to {@link Wiki.Revision}s, see {@link Revisions}.
 *  @author MER-C
 *  @version 0.01
 */
public class Events
{
    /**
     *  Standard HTML for RevisionDeleted fields. (The class name, 
     *  <code>history-deleted</code>, comes from MediaWiki.)
     */
    public static final String DELETED_EVENT_HTML = "<span class=\"history-deleted\">deleted</span>";
    
    /**
     *  Computes the time elapsed between each of a list of {@link Wiki.Event}s.
     *  The return list has length {@code events.size() - 1}, with the first
     *  element being the time elapsed between the first and second events in 
     *  the input list, the second duration being the difference between the
     *  second and third events and so forth. It is recommended you sort the 
     *  input list first by timestamp so that the latest timestamp is first.
     * 
     *  @param events a list of events
     *  @throws IllegalArgumentException if you supply less than two events
     *  @return the time between those events
     */
    public static List<Duration> timeBetweenEvents(List<? extends Wiki.Event> events)
    {
        int size = events.size();
        if (size < 2)
            throw new IllegalArgumentException("You must supply two or more events to calculate times between them.");
        List<Duration> durations = new ArrayList<>(size);
        for (int i = 1; i < size; i++)
            durations.add(Duration.between(events.get(i).getTimestamp(), events.get(i-1).getTimestamp()));
        return durations;
    }
}
