package org.wikipedia;

import java.io.*;
import java.util.*;

/**
 * Class consisting of functional, multi-threaded bots.  Currently contains Upload Bot and a Deletion bot.  There should only be once instance of this bot running at a time.  As of 2012-06-02, this class is UNTESTED. Use at your own risk :P 
 *
 */

public class MBot
{
   /**
    * The username to be logging in under.
    */
   private static String username;

   /**
    * The password to be using.
    */
   private static char[] px;

   /**
    * The domain we'll be using: e.g. commons.wikimedia.org, en.wikipedia.org, ect.
    */ 
    private static String domain;
   
   /**
    * Constructor, hiding from Javadoc
    */
    private MBot()
    {
	//do nothing
    }

   /**
    * Initializes the username and password fields; this method MUST be called before running any of the actual bot methods. </br>PRECONDITION: Must be a valid username and password combination.
    *
    * @param u The username to login under
    * @param p The password to use
    * @param wiki
    */
   public static void setup(String u, char[] p, String d)
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

   public static void upload(String dir, String desc, int num) throws Throwable
   {
     ArrayList<String> l = new ArrayList<String>();
     for(String s : new File(dir).list())
	if(FbotUtil.isUploadable(s))
	  l.add(s);

     upload(l.toArray(new String[0]), desc, num);
   }

   /**
    * Uploads all WMF valid file-types in a String array. 
    *
    * @param filelist A list of Strings representing the files to upload
    * @param desc The text to go on the file description page
    * @param num The max number of threads to run simultaneously.
    *
    * @param Throwable If 
    */
   public static void upload(String[] filelist, String desc, int num) throws Throwable
   {
     for(String[] l : FbotUtil.arraySplitter(filelist, num))
        new Thread(new UploadT(l, desc)).start();
   }

   /**
    * Factory method to generate Wiki objects.  Will attempt to login 4 times; if we fail all four times, it'll drop an error.
    *
    * @return A wiki object, logged in.
    * @throws UnsupportedOperationException if bad login or network issue.
    */

    private static Wiki generateWiki()
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

    private static class UploadT implements Runnable
    {

   /**
    * The wiki object we'll auto-generate
    */
      private Wiki wiki;

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

      public UploadT(String[] files, String desc)
      {
	wiki = generateWiki();
	this.files = files;
	this.desc = desc;
      }

   /**
    * The run method mandated by Runnable.  Basically that does all the uploading.
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
} //end MBot
