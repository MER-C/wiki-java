package org.wikipedia;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Consists of parser methods for use with MediaWiki syntax.
 *
 * @see org.wikipedia.Fbot
 * @see org.wikipedia.FbotUtil
 * @see org.wikipedia.MBot
 */

public class FbotParse
{

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
	         return parseFromPageRegex(text, FbotParse.getRedirectsAsRegex("Template:" + template, wiki));
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