/**
 *  @(#)BlockLockStuff.java 0.01 14/08/2021
 *  Copyright (C) 2021-20XX MER-C and contributors
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
 *  Utility for dealing with sockpuppets.
 *  @author MER-C
 *  @version 0.01
 */
public class BlockLockStuff
{
    public static void main(String[] args) throws Exception
    {
        WMFWikiFarm sessions = new WMFWikiFarm();
        WMFWiki meta = WMFWiki.sharedMetaWikiSession();
        Wiki enWiki = sessions.sharedSession("en.wikipedia.org");
        List<String> socks = enWiki.getCategoryMembers("Category:Wikipedia sockpuppets of Bodiadub", true, Wiki.USER_NAMESPACE);
        System.out.println("Not locked:");
        System.out.println("*{{MultiLock");
        for (String sock : socks)
        {
            // TODO: this is an inefficient way of determining whether an account
            // is locked - there is an additional API call that is still one user = one call
            // but less data transfer. Also, as usual, the W?F can't be arsed doing this
            // properly: https://phabricator.wikimedia.org/T261752
            Map<String, Object> ginfo = sessions.getGlobalUserInfo(sock);
            if (!(Boolean)ginfo.get("locked"))
                System.out.print("|" + meta.removeNamespace(sock));
        }
        System.out.println("}}");
        
        // TODO: accept arbitrary input
        // TODO: determine unblocked accounts
        // TODO: determine G5 date - first lock or block as per block list
    }
}
