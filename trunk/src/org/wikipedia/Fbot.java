package org.wikipedia;

import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.security.auth.login.*;
import java.util.regex.*;
import java.text.*;

/**
 *  A Mediawiki bot framework consisting of static methods. </br>
 *  Unless otherwise specified, assume that all Wiki objects are logged in.</br>
 *  Please report bugs <a href=http://commons.wikimedia.org/wiki/User_talk:Fastily>here</a>!</br>
 *  Licensed under the <ins>GNU GPL v3 license</ins></br>
 *  Visit our Google Code Project <a href="http://code.google.com/p/wiki-java/">home</a>
 */

public class Fbot
{
   //Hiding constructor from Javadoc
   private Fbot()
   {
      //do nothing
   } 

   /**
    *  Generic login method which turns off maxlag and allows for setting of throttle.
    *
    *  @param wiki Wiki object to perform changes on
    *  @param user User to login as, without "User:" prefix
    *  @param p User's password, in the form of a char array.
    *  @param throttle Seconds to wait in between making edits
    *
    *  @throws IOException If we had a network error
    *  @throws FailedLoginException If we had bad login information
    *
    *
    */

   public static void loginAndSetPrefs(Wiki wiki, String user, char[] p, int throttle) throws IOException, FailedLoginException
   {
      wiki.setMaxLag(-1);
      wiki.login(user, p);
      wiki.setThrottle(throttle);
   }


   /**
    *  Method reads in user/password combinations from a txt file to log in user.  In file, format should be
    *  "USERNAME:PASSWORD", separated by colon, one entry per line. 
    *
    *  @param file File name to use, with ext.
    *  @param wiki Wiki object to perform changes on
    *  @param user Which account? (no "User:" prefix)
    *  @param throttle Number of seconds to wait in between edits
    *  
    *  @throws IOException If we encountered a network error
    *  @throws FailedLoginException If user credentials do not match
    *  @throws UnsupportedOperationException if a non-recognized user is
    *  specified.
    *
    *  @see #loginAndSetPrefs
    *
    */

   public static void loginAndSetPrefs(String file, String user, int throttle, Wiki wiki) throws IOException, FailedLoginException
   {
      for(String f : FbotUtil.loadFromFile(file, ""))
         if(f.startsWith(user))
         {
            loginAndSetPrefs(wiki, user, f.substring(f.indexOf(":") + 1).trim().toCharArray(), throttle); 
            return;
         }

      throw new UnsupportedOperationException("Did not find a Username in the specified file matching String value in user param");
   }

   /**
    *  Login with loginAndSetPrefs(), but using a GUI.  Throttle auto-set to 6 edits/min.
    *
    *  @param wiki Wiki object to perform changes on
    *
    *  @see #loginAndSetPrefs
    *
    */
   public static void guiLogin(Wiki wiki)
   {

      JPanel pl = new JPanel(new GridLayout(2,2));
      pl.add(new JLabel("Username:"));
      JTextField u = new JTextField(12);
      pl.add(u);
      pl.add(new JLabel("Password:"));
      JPasswordField px = new JPasswordField(12);
      pl.add(px);

      int ok = JOptionPane.showConfirmDialog(null, pl, "Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
      if (ok != JOptionPane.OK_OPTION)
         System.exit(1);
      try
      {
         loginAndSetPrefs(wiki, u.getText().trim(), px.getPassword(), 10);
      }
      catch (FailedLoginException e)
      {
         e.printStackTrace();
         JOptionPane.showMessageDialog(null, "Username and password do not match on Database, program will now exit");
         System.exit(1);
      }
      catch (IOException e)
      {
         e.printStackTrace();
         System.err.println("Fatal Network Error, program will now exit.");
      }
   }

   /**
    *  Used to check if <tt>{{bots}}</tt> or <tt>{{robots}}</tt>, case-insensitive, is present in a String.
    *  
    *  @param text The String to check for <tt>{{bots}}</tt> or <tt>{{nobots}}</tt>
    *  @param user The account to check for, without the "User:" prefix.
    *
    *  @return boolean True if this particular bot should be allowed to edit this page.
    *  
    */

   public static boolean allowBots(String text, String user)
   {
      return !text.matches("(?i).*?\\{\\{(nobots|bots\\|(allow=none|deny=(.*?" + user + ".*?|all)|optout=all))\\}\\}.*?");
   }

   /**
    *  Strips the namespace prefix of a page, if applicable. If there is no
    *  namespace attached to the passed in string, then the original string is
    *  returned.
    *
    *  @param title The String to remove a namespace identifier from.
    *
    *  @return The String without a namespace identifier.
    *
    */
   public static String namespaceStrip(String title)
   {
      int i = title.indexOf(":");
      if(i > 0)
         return title.substring(i + 1);
      return title;
   }

   /**
    *  Gets the target of the redirect page. </br>PRECONDITION: <tt>redirect</tt> must be a Redirect.
    *
    *  @param redirect The title of the redirect to get the target for.
    *  @param wiki The wiki object to use.
    *
    *  @throws Throwable If there was a network issue, non-existent page issue, or if we tried to access a Special: page.
    *  @throws UnsupportedOperationException If the page was not a redirect page.
    *  
    *  @return String The title of the redirect's target.
    */

   public static String getRedirectTarget(String redirect, Wiki wiki) throws Throwable 
   {
      String text = wiki.getPageText(redirect).trim();
      if(text.startsWith("#"))
         return text.substring(text.indexOf("[[") + 2, text.indexOf("]]"));

      throw new UnsupportedOperationException("Parameter passed in is not a redirect page!");
   }

   /**
    *  Checks to see if a given page exists on Wiki (i.e. page is not a 'red-link')
    *
    *  @param page The page to check for.
    *  @param wiki The wiki object to use.
    *
    *  @throws Throwable
    *  
    *  @return True if the page exists.
    */

   public static boolean exists(String page, Wiki wiki) throws Throwable
   {
      return ((Boolean) wiki.getPageInfo(page).get("exists")).booleanValue();
   }

   /**
    *  Gets redirects of a template, returns then as a String regex.  Ready for 
    *  replacing instances of templates.  Pass in template with "Template:" prefix.
    *
    *  @param template The title of the main Template (CANNOT BE REDIRECT), including the "Template:" prefix.
    *  @param wiki The wiki object to use.
    *
    *  @throws Throwable
    *  
    *  @return The regex, in the form (?si)\{\{(Template:)??)(XXXXX|XXXX|XXXX...).*?\}\}, where XXXX is the
    *  template and its redirects.
    */

   public static String getRedirectsAsRegex(String template, Wiki wiki) throws Throwable
   {
      String r = "(?si)\\{\\{(Template:)??(" + namespaceStrip(template);
      for(String str : wiki.whatLinksHere(template, true, Wiki.TEMPLATE_NAMESPACE))
         r += "|" + namespaceStrip(str);
      r += ").*?\\}\\}";

      return r;
   }

   /**
    *  Checks to see if a file has at least one <b>file link</b> to the mainspace. 
    *
    *  @param file The file to check. Be sure to include "File:" prefix.
    *  @param wiki The wiki object to use.
    *
    *  @throws Throwable
    *  
    *  @return True if the file has at least one mainspace file link.
    */

   public static boolean hasMainspaceFileLink(String file, Wiki wiki) throws Throwable
   {
      return wiki.imageUsage(namespaceStrip(file), Wiki.MAIN_NAMESPACE).length > 0;
   }

   /**
    *  Deletes <ins>all</ins> members of a category and then the category itself.
    *
    *  @param cat The category to fetch items from, INCLUDING "Category:" prefix.
    *  @param reason The reason to use when deleting the category members.
    *  @param namespace Restricted to deleting members of a particular namespace.
    *  @param talkReason The reason to use when deleting the talk pages of the category members.
    *  @param catReason The reason to use when deleting the category.
    *  @param wiki The wiki object to use.
    *
    *  @throws IOException If we had a network error
    *
    *  @return An array containing the elements we were unable to delete.
    *  
    */

   public static String[] categoryNuke(String cat, String reason, int namespace, String talkReason, String catReason, Wiki wiki) throws IOException
   {
      String[] f = arrayNuke(listNamespaceSort(wiki.getCategoryMembers(namespaceStrip(cat)), Wiki.FILE_NAMESPACE, wiki), reason, talkReason, wiki);
      try
      {
         superDelete(cat, catReason, talkReason, wiki);
      }
      catch (Throwable e)
      {
	//don't care
      }

      return f;
   }


   /**
    *  Standard Fbot Database dump template.  Follows *[[:_TITLE_]]\n.
    *
    *  @param page Page to dump report into.
    *  @param list The list of pages that need to be dumped
    *  @param headerText Leading description text.  Specify "" for no lead.
    *  @param footerText Ending description text.  Specify "" for no end.
    *  @param wiki The wiki object to use.
    *
    *  @throws Throwable
    *  
    */

   public static void dbrDump(String page, String[] list, String headerText, String footerText, Wiki wiki) throws Throwable
   {
      String dump = headerText + "  This report last updated as of ~~~~~\n";
      for(String s : list)
         dump += "*[[:" + s + "]]\n";
      dump += "\n" + footerText;
      wiki.edit(page, dump, "Updating list");
   }

   /**
    *  Standard Fbot Database dump template.  Follows *[[:_TITLE_]]\n.
    *
    *  @param page Page to dump report into.
    *  @param list The list of pages that need to be dumped
    *  @param headerText Leading description text.  Specify "" for no lead.
    *  @param wiki The wiki object to use.
    *
    *  @throws Throwable
    *  
    */

   public static void dbrDump(String page, String[] list, String headerText, Wiki wiki) throws Throwable
   {
      dbrDump(page, list, headerText, "", wiki);
   }

   /**
    *  Replaces all transclusions of a template/page with specified text
    *
    *  @param template The template (including namespace prefix) to be replaced
    *  @param replacementText The text to replace the template with (can include subst:XXXX)
    *  @param reason Edit summary to use
    *  @param wiki The wiki object to use.
    *
    *  @throws Throwable 
    *  
    */

   public static void templateReplace(String template, String replacementText, String reason, Wiki wiki) throws Throwable
   {
      String[] list = wiki.whatTranscludesHere(template);
      if(template.startsWith("Template:"))
         template = namespaceStrip(template);

      for(String page : list)
      {
         try
         {
            wiki.edit(page, wiki.getPageText(page).replaceAll("(?i)(" + template + ")", replacementText), reason);
         }
         catch (Throwable e)
         {
            e.printStackTrace();
         }
      }
   }

   /**
    *  Returns all the items in an array that are within the specified namespace. 
    *
    *  @param list The list of items to use
    *  @param namespace The namespace of items to return
    *  @param wiki The wiki object to use.
    *
    *  @return The list of items in the list that were in the specified namespace.
    *
    *  @throws IOException If we had an issue populating namespace cache
    *  
    */

   public static String[] listNamespaceSort(String[] list, int namespace, Wiki wiki) throws IOException
   {
      ArrayList<String> l = new ArrayList<String>();
      for(String s : list)
      {
         if(wiki.namespace(s) == namespace)
            l.add(s);
      }

      return l.toArray(new String[0]);
   }


   /**
    *  Determines if two arrays share at least one element.
    *
    *  @param a1 The first array
    *  @param a2 The second array
    *
    *  @return True if the arrays share at least one element
    *  
    */

   public static boolean arraysShareElement(String[] a1, String[] a2)
   {
      return Wiki.intersection(a1, a2).length > 0;
   }


   /**
    *  Attempts to delete all the elements in an array
    *
    *  @param list The array to use
    *  @param reason The reason to use while deleting
    *  @param wiki The wiki object to use.
    *
    *  @return An array containing the elements we were unable to delete
    */

   public static String[] arrayNuke(String[] list, String reason, Wiki wiki)
   {
      ArrayList<String> f = new ArrayList<String>();
      for(String s : list)
      {
         try
         {
            wiki.delete(s, reason);
         }
         catch (Throwable e)
         {
            f.add(s);
         }
      }
      return f.toArray(new String[0]);
   }

   /**
    *  Deletes all the elements in an array and their associated talk pages
    *
    *  @param list The array to use
    *  @param reason The reason to use while deleting
    *  @param talkReason The reason to use when deleting talk pages of the pages we're deleting
    *  @param wiki The wiki object to use.
    *
    *  @return An array containing the elements we were unable to delete.
    */

   public static String[] arrayNuke(String[] list, String reason, String talkReason, Wiki wiki)
   {
      String[] f = arrayNuke(list, reason, wiki);
      for(String s : list)
      {
         try
         {
            wiki.delete(wiki.getTalkPage(s), talkReason);
         }
         catch (Throwable e)
         {
            //We'll probably only be here if we tried to delete the talk page of a talk page :P
         }
      } 
      return f;
   }


   /**
    *  Adds specified text to the end of a page
    *
    *  @param pages The list of pages to use
    *  @param text The text to append
    *  @param summary Edit summary to use.
    *  @param wiki The wiki object to use.
    *  
    */

   public static void addTextList(String[] pages, String text, String summary, Wiki wiki)
   {
      for(String page : pages)
      {
         try
         {
            wiki.edit(page, wiki.getPageText(text) + text, summary);
         }
         catch (Throwable e)
         {
            e.printStackTrace();
         }
      }
   }


   /**
    *  Replaces a file with another for a given list of pages.
    *
    *  @param list The list of pages to perform replacement on
    *  @param file The file to be replaced, without the "File:" prefix
    *  @param replacement The file to replace the first file with, without the "File:" prefix
    *  @param summary The edit summary to use
    *  @param wiki The wiki object to use.
    *  
    */

   public static void fileReplace(String[] list, String file, String replacement, String summary, Wiki wiki)
   {
      file = file.replace("_", " "); //need to guarantee that we aren't using underscores
      String regex = "(?i)(" + file + "|" + file.replace(" ", "_") + ")";

      for(String page : list)
      {
         try
         {
            wiki.edit(page, wiki.getPageText(page).replaceAll(regex, replacement), summary);
         }
         catch (Throwable e)
         {
            e.printStackTrace();
         }
      }
   }   

   /**
    *  Deletes a page and it's talk page (if applicable)
    *
    *  @param page The page to delete
    *  @param reason The reason to use when deleting this page
    *  @param tReason The reason to use when deleting the talk page
    *  @param wiki The wiki object to use.
    *  
    * 
    *  @throws Throwable if we had a problem
    */

   public static void superDelete(String page, String reason, String tReason, Wiki wiki) throws Throwable
   {
      wiki.delete(page, reason);
      wiki.delete(wiki.getTalkPage(page), tReason);
   }

   /**
    *  Attempts to parse out a template parameter.
    *
    *  @param template The template to work on.  Must be entered in format {{NAME|PARM1|PARAM2|...}}
    *  @param number The parameter to retrieve: {{NAME|1|2|3|4...}}
    * 
    *  @return The param we parsed out or null if we didn't find a param matching the specified criteria
    * 
    */
   public static String getTemplateParam(String template, int number)
   {
      try
      {
         return templateParamStrip(template.split("\\|")[number]);
      }
      catch (ArrayIndexOutOfBoundsException e)
      {
         return null;
      }
   }


   /**
    *  Attempts to parse out a template parameter based on specification
    *
    *  @param template The template to work on.  Must be entered in format {{NAME|PARM1|PARAM2|...}}
    *  @param param The parameter to retrieve, without "=". 
    * 
    *  @return The param we parsed out or null if we didn't find a param matching the specified criteria
    * 
    */

   public static String getTemplateParam(String template, String param) 
   {
      ArrayList<String> f = new ArrayList<String>();
      for(String s : template.split("\\|"))
         f.add(s.trim());

      for(String p : f)
         if(p.startsWith(param))
            return templateParamStrip(p);

      return null; //if nothing matched  
   }

   /**
    *  Returns the param of a template.  e.g. If we get "|foo = baz", we return baz.  
    *
    *  @param p Must be a param in the form "|foo = baz" or "foo = baz"
    * 
    *  @return The param we parsed out
    * 
    */

   public static String templateParamStrip(String p)
   {
      int i = p.indexOf("=");
      if(i == -1)
         return p;
      else
         return p.substring(i+1).replace("}}", "").trim();
   }

   /**
    *  Parses out the first instance of a template from a body of text, based on specified template.  
    *
    *  @param text Text to search
    *  @param template The Template (in template namespace) to look for.  DO NOT add namespace prefix.
    *  @param redirects Whether to incorporate redirects to this template or not.
    *  @param wiki The wiki object to use
    * 
    *  @return The template we parsed out, in the form {{TEMPLATE|ARG1|ARG2|...}} or null, if we didn't find the specified template.
    *
    *  @throws Throwable if Big boom
    * 
    */

   public static String parseTemplateFromPage(String text, String template, boolean redirects, Wiki wiki) throws Throwable
   {
      if(redirects)
         return parseFromPageRegex(text, getRedirectsAsRegex("Template:" + template, wiki));
      else
         return parseFromPageRegex(text, "(?s)\\{\\{(Template:)??(" + template + ").*?\\}\\}");
   }

   /**
    *  Parses out the first group of matching text, based on specified regex.  Useful for parsing out templates.
    *
    *  @param text Text to search
    *  @param regex The regex to use
    * 
    *  @return The text we parsed out, or null if we didn't find anything. 
    * 
    */

   public static String parseFromPageRegex(String text, String regex)
   {
      Matcher m = Pattern.compile(regex).matcher(text);
      if(m.find())
         return text.substring(m.start(), m.end());
      else
         return null;
   }

   /**
    *  Returns a Gregorian Calendar offset by a given number of days from the current system clock.  
    *  Use positive int to offset by future days and negative numbers to offset to days before.  
    *  Automatically set to UTC.
    *
    *  @param days The number of days to offset by -/+
    * 
    *  @return The newly modified calendar.
    * 
    */

   public static GregorianCalendar offsetTime(int days)
   {
      GregorianCalendar utc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
      utc.setTimeInMillis(new GregorianCalendar().getTime().getTime() + 86400000L*days);

      return utc;
   }


   /**
    *  Outputs the date/time in UTC.  Based on the format and offset in days.
    *
    *  @param format Must be specified in accordance with java.text.DateFormat.  
    *  @param offset The offset from the current time, in days.  Accepts both positive (for future) and negative values (for past)
    * 
    *  @return The formatted date string.
    * 
    */

   public static String fetchDateUTC(String format, int offset)
   {
      SimpleDateFormat sdf = new SimpleDateFormat(format);
      sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

      return sdf.format(offsetTime(offset).getTime());
   } 


   /**
    *  Logs in using a user-defined file named "px".
    *
    *  @param wiki The wiki object to use
    *  @param user The user to be logged in
    *
    *  @throws Throwable
    *  @see #loginAndSetPrefs
    */

   public static void loginPX(Wiki wiki, String user) throws Throwable
   {
      Fbot.loginAndSetPrefs("px", user, 1, wiki);
   }

}
