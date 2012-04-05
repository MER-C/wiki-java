import java.io.*;
import java.util.*;
import org.wikipedia.*;
import java.awt.*;
import javax.swing.*;
import javax.security.auth.login.*;
import java.util.regex.*;

/**
 *  Bot framework for Fbot family of bots consisting of static methods.  Supplements MER-C's Wiki.java.  
 *  Unless otherwise specified, assume that all Wiki objects have been 'logged in'.
 *  As far as I know, everything is fairly stable.  
 *Report bugs to [http://commons.wikimedia.org/wiki/User_talk:Fastily].  
 *  For best results, use with Wiki.java r48: [http://code.google.com/p/wiki-java/source/browse/trunk/src/org/wikipedia/Wiki.java?spec=svn48&r=48]
 *  Licensed under GNU GPL v3.
 */

public class Fbot
{
   //Hiding Fbot() from appearing with a constructor in javadocs
   private Fbot()
   {
     //do nothing
   } 

   /**
    *  Splits an array into multiple parts by requesting user input. Useful for 
    *  running the same bot task in multiple terminal windows.  Input requested
    *  includes how many splits and which split this window should be running.
    *  Very hackish, temporary fix.
    *
    *  @param z the array to split
    *
    *  @return the split array
    */
   public static String[] arraySplitter(String[] z)
   {
      Scanner m = new Scanner(System.in);
      int splits = arraySplitterAssist("How many to split: ", z.length);
      int part = arraySplitterAssist("Part of split (1-" + splits + "): ", splits);

      String[] xf;
      if(splits == 1)
         xf = z;
      else if(part == 1)
         xf = Arrays.copyOfRange(z, 0, z.length/splits);
      else if(part == splits)
         xf = Arrays.copyOfRange(z, z.length/splits*(splits-1), z.length);
      else
         xf = Arrays.copyOfRange(z, z.length/splits*(part-1), z.length/splits*part);

      return xf;
   }

   /**
    *  Assist method to arraySplitter().  Idiot proofs user input.  User must
    *  enter a integer value and the value must be a positive value less than the
    *  values previously specified.
    *
    *  @param prompt Message user will see.
    *  @param upperLimit User cannot enter a value that exceeds this value.
    *
    *  @return the number specified by the user
    *
    *  @see #arraySplitter
    *
    *
    */

   private static int arraySplitterAssist(String prompt, int upperLimit)
   {
      Scanner m = new Scanner(System.in);
      while (true)
      {
         System.out.print(prompt);
         while(!m.hasNextInt())
         {
            m.nextLine();
            System.out.println("Not an integer; try again.");
            System.out.print(prompt);
         }
         int i = m.nextInt();
         if(i >= 1 && i <= upperLimit)
            return i;
         System.out.println("Invalid number.  Entry must satisfy [1, " + upperLimit + "].  Please try again.");

      }
   }

   /**
    *  Generic login method which also sets maxlag and throttle.  Max lag set to
    *  1000000.
    *
    *  @param wiki Wiki object to perform changes on
    *  @param user User to login as, without "User:" prefix
    *  @param p User's password, in the form of a char array.
    *  @param throttle Seconds to wait in between making edits
    *
    *  @throws IOException If we had a network error
    *  @throws FailedLoginException If login credentials were wrong
    *
    *
    */

    public static void loginAndSetPrefs(Wiki wiki, String user, char[] p, int throttle) throws IOException, FailedLoginException
    {
      wiki.setMaxLag(1000000);
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
     for(String f : loadFromFile(file, ""))
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
    *  @throws IOException
    *  @throws FailedLoginException
    *
    *  @see #loginAndSetPrefs
    *
    */
   public static void guiLogin(Wiki wiki) throws IOException, FailedLoginException
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
    *  Loads list of pages contained in a local file into an array.  One newline per item.
    *  
    *  @param file directory of the file to load from. 
    *  @param prefix append prefix/namespace to items?  If not, specify "" as
    *  arg.
    *
    *  @return The resulting array
    *
    *  @throws FileNotFoundException if the specified file cannot be found.
    *
    *
    */

   public static String[] loadFromFile(String file, String prefix) throws FileNotFoundException
   {
      Scanner m = new Scanner(new File(file));
      ArrayList<String> l = new ArrayList<String>();
      while (m.hasNextLine())
         l.add(prefix + m.nextLine().trim());
      return l.toArray(new String[0]);
   }

   /**
    *  Loads list from a page on a wiki.  Note that each item to load into a list
    *  must be on a new line, or else the parser will not work.  Will ignore any
    *  lines beginning with the "<" character (useful for inline commenting in wiki-text).
    *
    *  
    *  @param page Wiki page to load from
    *  @param wiki The wiki object to use for this action
    *  @param prefix Can be used to prepend a prefix to every item in the list.
    *  Use empty string "" if no prefix required.
    *
    *  @return The resulting list
    *
    *  @throws IOException in case of network failure
    *
    */

   public static String[] getList(String page, Wiki wiki, String prefix) throws IOException
   {
      Scanner m = new Scanner(wiki.getPageText(page));
      ArrayList<String> list = new ArrayList<String>();

      String x;
      while(m.hasNextLine())
         if(! (x = m.nextLine().trim()).startsWith("<") && x.length() > 1)
            list.add(prefix + x);
      return list.toArray(new String[0]);
   }

   /**
    *  Removes character sequence(s) from each element of an ArrayList<String>.
    * 
    *  
    *  @param l The ArrayList<String> to be actioned upon
    *  @param crap List of String(s) to strip from each element of l
    *
    */

   public static void stripCrap(ArrayList<String> l, String[] crap)
   {
      for (String c : crap)
         for(int i = 0; i < l.size(); i++)
            l.set(i, l.get(i).replace(c, ""));
   }

   /**
    *  Used to check if {{bots}} or {{nobots}}, case ignored, is present on a
    *  given page. 
    *  
    *  @param text The text to check for aformentioned template occurances.
    *  @param user The current bot's username.  Used to check if the bot is being
    *  explicitly allowed or denied.
    *
    *  @return boolean indicating whether the bot should edit this page.
    *  
    */

   public static boolean allowBots(String text, String user)
   {
      return !text.matches("(?si).*\\{\\{(nobots|bots\\|(allow=none|deny=(.*?" + user + ".*?|all)|optout=all))\\}\\}.*");
   }

   /**
    *  Strips the namespace prefix of a page, if applicable. If there is no
    *  namespace attached to the passed in string, then the original string is
    *  returned.
    *
    *  @param title The title to remove a namespace from
    *
    *  @return The title without a namespace.
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
    *  Performs same function as String contains() from java.lang.String, but is
    *  not case-sensitive.
    *
    *  @param text main body of text to check.
    *  @param s2 we will try to check if s2 is contained in text.
    *
    *  @return boolean indicating whether s2 is contained in text,
    *  case-insensitive.
    *
    */

   public static boolean containsIgnoreCase(String text, String s2)
   {
      return text.toUpperCase().contains(s2.toUpperCase());
   }

   /**
    *  Converts an int (0-11) to a month (String).  I have this because I'm too
    *  lazy to figure out how to work java.util.Calendar. 
    *
    *  @param d int value (0-11).  0 = Janurary, 1 = Februaray, ect.
    *
    *  @return Month representation of the passed in int or null if value was not within 0-11
    *  
    */

   public static String getMonthText(int d)
   {
      switch(d)
      {
         case 0: return "January";
         case 1: return "February";
         case 2: return "March";
         case 3: return "April";
         case 4: return "May";
         case 5: return "June";
         case 6: return "July";
         case 7: return "August";
         case 8: return "September";
         case 9: return "October";
         case 10: return "November";
         case 11: return "December";
         default : return null;
      }
   } 

   /**
    *  Determines if a substring in an array of strings is present in at least 
    *  one string of that array.  String substring is the substring to search for. 
    *
    *  @param list The list of elements to use
    *  @param substring The substring to search for in the list
    *  @param caseinsensitive If true, ignore upper/lowercase distinctions.
    *
    *  @return boolean True if we found a matching substring in the specified list.
    *  
    */

  public static boolean listElementContains(String[] list, String substring, boolean caseinsensitive)
  {
    if(caseinsensitive)
    { 
      substring = substring.toLowerCase();
      for(String s : list)
        if(s.toLowerCase().contains(substring))
	   return true;
      return false;
    }
    else
    {
      for(String s : list)
	if(s.contains(substring))
	  return true;
      return false;
    } 
  }
  

   /**
    *  Outputs a text file (.txt) representing the elements of an array.  Broken at the
    *  moment, seems to work only half of the time.
    *
    *  @param list The list of elements to use
    *  @param outputTitle Title to output txt file to, w/o file extension. 
    *
    *  @throws IOException If we encounter a read-write exception.
    *  
    */

  public static void dumpAsFile(String[] list, String outputTitle) throws IOException
  {
    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputTitle + ".txt"), "UTF-8"));
    for (String s : list)
      out.write(s + "\n");
    out.flush();
    out.close();
  }

   /**
    *  Gets the target of the title, which is presumed to be a redirect.
    *
    *  @param redirect The title of the redirect to get the target for.
    *  @param wiki The wiki object to use.
    *
    *  @throws Throwable If Kaboom
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
    *  Checks to see if a given page exists locally (i.e. page is not a 'red-link')
    *
    *  @param page The title to check for.
    *  @param wiki The wiki object to use.
    *
    *  @throws Throwable If Kaboom
    *  
    *  @return boolean True if the page exists.
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
    *  @throws Throwable If Kaboom
    *  
    *  @return String The regex, in the form (?si)\{\{(Template:)??)(XXXXX|XXXX|XXXX...).*?\}\}, where XXXX is the
    *  template and its redirects.
    */

  public static String getRedirectsAsRegex(String template, Wiki wiki) throws Throwable
  {
    String r = "(?si)\\{\\{(Template:)??(" + namespaceStrip(template);
    for(String str : wiki.whatLinksHere(template, Wiki.TEMPLATE_NAMESPACE, true))
      r += "|" + namespaceStrip(str);
    r += ").*?\\}\\}";

    return r;
   }
 
   /**
    *  checks to see if a file has at least one FILE LINK to the mainspace.  Be sure to pass in file with "File:" prefix.
    *
    *  @param file The file to check. Be sure to include "File:" prefix.
    *  @param wiki The wiki object to use.
    *
    *  @throws Throwable If Kaboom
    *  
    *  @return boolean true if the file has at least one mainspace file link.
    */

   public static boolean hasMainspaceFileLink(String file, Wiki wiki) throws Throwable
   {
     for(String page : wiki.imageUsage(namespaceStrip(file)))
       if(wiki.namespace(page) == Wiki.MAIN_NAMESPACE)
	   return true;
     return false;
   }

   /**
    *  Deletes ALL members of a category and then the category itself.
    *
    *  @param cat The category to fetch items from, INCLUDING "Category:" prefix.
    *  @param reason The reason to use when deleting the category members.
    *  @param namespace Restricted to deleting members of a particular namespace.
    *  @param talkReason The reason to use when deleting the talk pages of the category members.
    *  @param catReason The reason to use when deleting the category.
    *  @param wiki The wiki object to use.
    *
    *  @throws IOException if we had a network error
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
	//well, we did try…
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
    *  @throws Throwable If Kaboom
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
    *  @throws Throwable If Kaboom
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
    *  @throws Throwable if Kaboom
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
    *  Returns all the items in an array that are in the specified namespace. 
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
    *  Checks if two arrays share at least one element.
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
    *  Deletes a page and it's talk page (if possible)
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
    *  @return The param we parsed out or Null if we didn't find a param matching the specified criteria
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
    *  @return The param we parsed out or Null if we didn't find a param matching the specified criteria
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
   
}
