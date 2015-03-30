/**
 *  @(#)LoggedInTests.java
 *  Copyright (C) 2011 - 2014 MER-C
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
        Wiki wiki = new Wiki("test.wikipedia.org");
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

        // edit
        wiki.edit("User:MER-C/BotSandbox", "Testing " + Math.random(), "test");
        wiki.edit("User:MER-C/BotSandbox", "Testing " + Math.random(), "test");
*/             
        // watch
        wiki.watch("Main Page", "Blah");
        wiki.unwatch("Main Page", "Blah");
/*        
        // watchlist
        for (Wiki.Revision item : wiki.watchlist(false))
            System.out.println(item);
        
        // upload
        wiki.upload(new File("~/Pictures/marsface.jpg"), "Wiki.java test4.jpg", "Test image. Source: [[:File:Face on Mars with Inset.jpg]]. ∑∑ƒ∂ß", "hello ∑∑ƒ∂ß");
        
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
        
//        Wiki enWiki = new Wiki("en.wikipedia.org");
//        LoginUtils.guiLogin(enWiki);
//        enWiki.setThrottle(5);
        
        // deleted contributions
        // for (Wiki.Revision rev : enWiki.deletedContribs("Namkeenvilla"))
        //    System.out.println(rev);
        
        // revdeled information
        // Calendar start = new GregorianCalendar(2014, 1, 22);
        // Calendar end = new GregorianCalendar(2014, 1, 24);
        // for (Wiki.Revision rev : enWiki.getPageHistory("Imran Khan (singer)", start, end, false))
        //     System.out.println(rev);
        
        // revdelete
        // Wiki.Revision rev = enWiki.getRevision(600296466L);
        // enWiki.revisionDelete(Boolean.TRUE, null, Boolean.TRUE, "Testing", Boolean.TRUE, new Wiki.Revision[] { rev });
        // enWiki.revisionDelete(Boolean.FALSE, null, null, "Testing", Boolean.FALSE, new Wiki.Revision[] { rev });
        // enWiki.revisionDelete(null, null, Boolean.FALSE, "Testing", Boolean.FALSE, new Wiki.Revision[] { rev });
*/        
    }
}
