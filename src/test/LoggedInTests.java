/**
 *  @(#)LoggedInTests.java
 *  Copyright (C) 2011 MER-C
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
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

package test;

import org.wikipedia.Wiki;

/**
 *  Tests for Wiki.java which should only be run when logged in.
 *  @author MER-C
 */
public class LoggedInTests
{
    private static Wiki wiki = new Wiki("en.wikipedia.org");

    public static void main(String[] args) throws Exception
    {
        // login
        new LoginDialog(wiki);

        // watchlist
        for (String page : wiki.getRawWatchlist())
            System.out.println(page);

        // email
        wiki.emailUser(wiki.getCurrentUser(), "Testing", "Blah", false);

        // BOT TESTS
        // org.wikipedia.bots.CPBot.main(new String[0]);

        // edit (non-existent page)
        wiki.edit("User:MER-C/dfhsdlfj", "Testing", "test", false);
    }
}
