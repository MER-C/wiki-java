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

import java.io.*;
import java.util.logging.LogManager;
import org.fbot.Fbot;
import org.wikipedia.Wiki;

/**
 *  Tests for Wiki.java which should only be run when logged in.
 *  @author MER-C
 */
public class LoggedInTests
{
    private static Wiki wiki = new Wiki("test.wikipedia.org");

    public static void main(String[] args) throws Exception
    {
        // login and override defaults
        System.setProperty("wiki.level", "100");
        LogManager.getLogManager().readConfiguration();
        Fbot.guiLogin(wiki);
        wiki.setThrottle(5);

        // raw watchlist
        //for (String page : wiki.getRawWatchlist())
        //    System.out.println(page);

        // email
        // wiki.emailUser(wiki.getCurrentUser(), "Testing", "Blah", false);

        // edit
        // wiki.edit("User:MER-C/BotSandbox", "Testing " + Math.random(), "test", false, false);
        // wiki.edit("User:MER-C/BotSandbox", "Testing " + Math.random(), "test", false, false);
        
        // watch
        // wiki.watch("Main Page");
        
        // watchlist
        for (Wiki.Revision item : wiki.watchlist(false))
            System.out.println(item);
        
        // upload
        wiki.upload(new File("~/Pictures/marsface.jpg"), "Wiki.java test4.jpg", "Test image. Source: [[:File:Face on Mars with Inset.jpg]]. ∑∑ƒ∂ß", "hello ∑∑ƒ∂ß");
    }
}
