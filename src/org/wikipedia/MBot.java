package org.wikipedia;

import java.io.*;
import java.util.*;

/**
 * Class consisting of stand-alone, multi-threaded bot methods. These methods 
 * are designed to get the specified task done, as <i>fast</i> as possible.  
 * Currently consists of an Upload and Deletion bot. There should only be once 
 * instance of this bot running at a time.  Please note that you 
 * <span style="font-weight:bold;color:#FF0000">MUST</span> call setup() before
 * running any of these bot methods.  Be sure to 
 * <a href="http://stackoverflow.com/questions/1565388/increase-heap-size-in-java">adjust 
 * heap space</a> according to the method you're running (Uploads will require 
 * more space depending on the size of files you're uploading and how many 
 * threads you've chosen to instantiate).  Now accepting feature requests 
 * <a href="http://commons.wikimedia.org/w/index.php?title=User_talk:Fastily&action=edit&section=new">here</a>.
 *
 * @see org.wikipedia.Fbot
 * @see org.wikipedia.FbotUtil
 */

public class MBot
{
   /**
    * The username to be logging in under.
    */
   private String username;

   /**
    * The password to be using.
    */
   private char[] px;

   /**
    * The domain we'll be using: e.g. commons.wikimedia.org, en.wikipedia.org, ect.
    */ 
   private String domain;
   
   private static MBot instance = null;

   /**
    * Constructor, hiding from Javadoc and forcing singleton.
    */
   private MBot()
   {
      //do nothing
   }
   
   /**
    *  Returns the single instance.
    *  @return (see above)
    */
   public static MBot getInstance()
   {
       if (instance == null)
           instance = new MBot();
       return instance;
   }

   /**
    * Initializes the username and password fields; this method MUST be called before running any of the actual bot methods. </br>PRECONDITION: Must be a valid username and password combination.
    *
    * @param u The username to login under
    * @param p The password to use
    * @param d The domain to use (e.g. "commons.wikimedia.org", "en.wikipedia.org")
    */
   public void setup(String u, char[] p, String d)
   {
      username = u;
      px = p;
      domain = d;
   }


   /**
    * Uploads all WMF valid file-types in a particular directory. 
    *
    * @param dir The directory to use
    * @param desc The text to go on the file description page
    * @param num The max number of threads to run simultaneously.
    *
    * @throws Throwable If we had a network or login issue.
    */

   public void upload(String dir, String desc, int num) throws Throwable
   {
      ArrayList<String> l = new ArrayList<String>();
      for(String s : new File(dir).list())
         if(FbotUtil.isUploadable(s))
            l.add(dir + "/" + s);

      upload(l.toArray(new String[0]), desc, num);
   }

   /**
    * Attempts to upload all the files in an array.
    *
    * @param filelist A list of Strings representing the files to upload
    * @param desc The text to go on the file description page
    * @param num The max number of threads to run simultaneously.
    *
    * @throws Throwable If we had a network of login issue.
    */
   public synchronized void upload(String[] filelist, String desc, int num) throws Throwable
   {
      for(String[] l : FbotUtil.arraySplitter(filelist, num))
         new Thread(new UploadT(l, desc)).start();
   }

   /**
    * Attempts to delete all the pages in an array
    *
    * @param list The list of pages to delete.
    * @param reason The reason to use when deleting.
    * @param num The max number of threads to run simultaneously.
    *
    * @throws Throwable If we had a network of login issue.
    */
   public synchronized void delete(String[] list, String reason, int num) throws Throwable
   {
      for(String[] l : FbotUtil.arraySplitter(list, num))
         new Thread(new DeleteT(l, reason)).start();
   }

   /**
    * Factory method to generate Wiki objects.  Will attempt to login 4 times; if we fail all four times, it'll drop an error.
    *
    * @return A wiki object, logged in.
    * @throws UnsupportedOperationException If we had a network of login issue.
    */

   private Wiki generateWiki()
   {
      short cnt = 0;
      Wiki wiki = new Wiki(domain);

      try
      {
         cnt++;
         Fbot.loginAndSetPrefs(wiki, username, px, 1); 
      }
      catch(Throwable e)
      {
         if(cnt > 4)
            throw new UnsupportedOperationException("Bad Login Information or Broken");
      }
      return wiki;
   }


   /**
    * Can be used as a single instance of an upload bot thread.  
    */

   private class UploadT implements Runnable
   {

      /**
       * The wiki object we'll auto-generate
       */
      private Wiki wiki = generateWiki();

      /**
       * The files this thread will be uploading.
       */
      private String[] files;

      /**
       * The description to be used when uploading
       */
      private String desc; 

      /**
       * The constructor to create a new UploadT instance.
       *
       * @param files The list of files this instance will upload
       * @param desc The description to use when uploading.
       */

      protected UploadT(String[] files, String desc)
      {
         this.files = files;
         this.desc = desc;
      }

      /**
       * The run method mandated by Runnable.  Basically does all the uploading.
       */

      public void run()
      {
         for(String file : files)
         {
            try
            {
               File f = new File(file);
               wiki.upload(f, f.getName(), desc, "");
            }
            catch(Throwable e)
            {
               e.printStackTrace();
            }
         }
      }

   } //end UploadT


   private class DeleteT implements Runnable
   {
      /**
       * The list of pages to delete
       */
      private String[] list;

      /**
       * The wiki object we'll auto-generate
       */
      private Wiki wiki = generateWiki();

      /**
       * The reason to use when deleting pages
       */
      private String reason;

      /**
       * The constructor to create a new DeleteT object
       *
       * @param list The list of pages to use
       * @param reason The reason to use when deleting pages.
       */

      protected DeleteT(String list[], String reason)
      {
         this.list = list;
         this.reason = reason;
      }

      /**
       * The run method mandated by Runnable.  Basically does all the deleting.
       */

      public void run()
      {
         boolean success = false;
         for(String s : list)
         {
            try
            {
	       while(!success)
	       {
                  wiki.delete(s, reason);
		  success = true;
	       }
            }
            catch(Throwable e)
            {
               e.printStackTrace();
            }
         }
      }
   }//end DeleteT

} //end MBot
