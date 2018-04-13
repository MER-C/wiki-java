/**
 *  @(#)LoggedInTests.java
 *  Copyright (C) 2011 - 2018 MER-C
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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.LogManager;
import org.wikiutils.LoginUtils;
import org.wikipedia.Wiki;

/**
 *  Tests for Wiki.java which should only be run when logged in.
 *  @author MER-C
 */
public class LoggedInTests
{
    public static void main(String[] args) throws Exception
    {
        // login
        Wiki wiki = Wiki.createInstance("test.wikipedia.org");
        LoginUtils.guiLogin(wiki);
        wiki.setThrottle(5);
  /*   
        // raw watchlist
        for (String page : wiki.getRawWatchlist())
            System.out.println(page);
     
        // file move
        wiki.move("File:Test12345.png", "File:Test123456.png", "test12345", true, false, false);
        wiki.move("File:Test123456.png", "File:Test12345.png", "test123456", true, false, false);
        
        // email
        //wiki.emailUser(wiki.getCurrentUser(), "Testing", "Blah", false);

        // watch
        wiki.watch("Main Page", "Blah");
        wiki.unwatch("Main Page", "Blah");
  
        // watchlist
        for (Wiki.Revision item : wiki.watchlist(false))
            System.out.println(item);
     
        ///////////////////////
        // ADMIN STUFF
        ///////////////////////

        // deleted revisions
        for (Wiki.Revision rev : wiki.getDeletedHistory("User:MER-C/UnitTests/Delete"))
            System.out.println(rev);

        // deleted prefix index
        for (String page : wiki.deletedPrefixIndex("B", Wiki.MAIN_NAMESPACE))
            System.out.println(page);
        
        // logout
        wiki.logout();

        // TODO: move the following to testwiki
        
//        Wiki enWiki = Wiki.createInstance("en.wikipedia.org");
//        LoginUtils.guiLogin(enWiki);
//        enWiki.setThrottle(5);
        
        // deleted contributions
        // for (Wiki.Revision rev : enWiki.deletedContribs("Namkeenvilla"))
        //    System.out.println(rev);
*/        
    }
}
