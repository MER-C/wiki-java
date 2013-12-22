package org.wikiutils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.wikipedia.Wiki;

/**
 * Useful parsing methods for MediaWiki syntax.
 * 
 * @author Fastily
 * 
 * @see org.wikiutils.CollectionUtils
 * @see org.wikiutils.DateUtils
 * @see org.wikiutils.GUIUtils
 * @see org.wikiutils.IOUtils
 * @see org.wikiutils.LoginUtils
 * @see org.wikiutils.StringUtils
 * @see org.wikiutils.WikiUtils
 */
public class ParseUtils
{
	/**
	 * Hiding constructor from JavaDoc
	 */
	private ParseUtils()
	{
	}
	
	/**
	 * Gets the target of the redirect page. </br><b>PRECONDITION</b>: <tt>redirect</tt> must be a
	 * Redirect.
	 * 
	 * @param redirect The title of the redirect to get the target for.
	 * @param wiki The wiki object to use.
	 * 
	 * @return String The title of the redirect's target.
	 * 
	 * @throws UnsupportedOperationException If the page was not a redirect page.
	 * @throws IOException If network error
	 */

	public static String getRedirectTarget(String redirect, Wiki wiki) throws IOException
	{
		String text = wiki.getPageText(redirect).trim();

		if (text.matches("(?si)^#(redirect)\\s*?\\[\\[.+?\\]\\].*?"))
			return text.substring(text.indexOf("[[") + 2, text.indexOf("]]"));

		throw new UnsupportedOperationException("Parameter passed in is not a redirect page!");
	}
	
	/**
	 * Gets redirects of a template, returns then as a String regex. Ready for
	 * replacing instances of templates. Pass in template with "Template:" prefix.
	 * 
	 * @param template The title of the main Template (CANNOT BE REDIRECT), including the "Template:" prefix.
	 * @param wiki The wiki object to use.
	 * 
	 * @return The regex, in the form (?si)\{\{(Template:)??)(XXXXX|XXXX|XXXX...).*?\}\}, where XXXX is the
	 * template and its redirects.
	 * 
	 * @throws IOException If network error.
	 */

	public static String getRedirectsAsRegex(String template, Wiki wiki) throws IOException
	{
		String r = "(?si)\\{{2}?\\s*?(Template:)??\\s*?(" + namespaceStrip(template, wiki);
		for (String str : wiki.whatLinksHere(template, true, Wiki.TEMPLATE_NAMESPACE))
			r += "|" + namespaceStrip(str, wiki);
		r += ").*?\\}{2}?";

		return r;
	}

	/**
	 * Used to check if <tt>{{bots}}</tt> or <tt>{{robots}}</tt>, case-insensitive, is present in a String.
	 * 
	 * @param text The String to check for <tt>{{bots}}</tt> or <tt>{{nobots}}</tt>
	 * @param user The account to check for, without the "User:" prefix.
	 * 
	 * @return boolean True if this particular bot should be allowed to edit this page.
	 * 
	 */

	public static boolean allowBots(String text, String user)
	{
		return !text.matches("(?i).*?\\{\\{(nobots|bots\\|(allow=none|deny=(.*?" + user + ".*?|all)|optout=all))\\}\\}.*?");
	}

	/**
	 * Replaces all transclusions of a template/page with specified text
	 * 
	 * @param template The template (including namespace prefix) to be replaced
	 * @param replacementText The text to replace the template with (can include subst:XXXX)
	 * @param reason Edit summary to use
	 * @param wiki The wiki object to use.
	 * 
	 * @throws IOException If network error.
	 * 
	 * 
	 */

	public static void templateReplace(String template, String replacementText, String reason, Wiki wiki) throws IOException
	{
		String[] list = wiki.whatTranscludesHere(template);
		if (template.startsWith("Template:"))
			template = namespaceStrip(template, wiki);

		for (String page : list)
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
	 * Strips the namespace prefix of a page, if applicable. If there is no
	 * namespace attached to the passed in string, then the original string is
	 * returned.
	 * 
	 * @param title The String to remove a namespace identifier from.
	 * @param wiki the home wiki
	 * @return The String without a namespace identifier.
         * @throws IOException if a network error occurs (rare)
	 * 
	 */
	public static String namespaceStrip(String title, Wiki wiki) throws IOException
	{
		String ns = wiki.namespaceIdentifier(wiki.namespace(title));
                return ns.isEmpty() ? title : title.substring(ns.length() + 1);
	}

	/**
	 * Attempts to parse out a template parameter.
	 * 
	 * @param template The template to work on. Must be entered in format {{NAME|PARM1|PARAM2|...}}
	 * @param number The parameter to retrieve: {{NAME|1|2|3|4...}}
	 * 
	 * @return The param we parsed out or null if we didn't find a param matching the specified criteria
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
	 * Attempts to parse out a template parameter based on specification
	 * 
	 * @param template The template to work on. Must be entered in format {{NAME|PARM1|PARAM2|...}}
	 * @param param The parameter to retrieve, without "=".
	 * 
	 * @return The param we parsed out or null if we didn't find a param matching the specified criteria
	 * 
	 */
	public static String getTemplateParam(String template, String param)
        {
                ArrayList<String> f = getTemplateParameters(template);
                if(f == null) return null;
                for (String p : f)
                        if (p.startsWith(param))
                                return p.substring(p.indexOf("=")+1).trim();

                return null; // if nothing matched
        }

	/**
	 * Returns the param of a template. e.g. If we get "|foo = baz", we return baz.
	 * 
	 * @param p Must be a param in the form "|foo = baz" or "foo = baz"
	 * 
	 * @return The param we parsed out
	 * 
	 */

	public static String templateParamStrip(String p)
	{
		int i = p.indexOf("=");
		if (i == -1)
			return p.replace("}}", "").trim();
		else
			return p.substring(i + 1).replace("}}", "").trim();
	}

	/**
	 * Parses out the first instance of a template from a body of text, based on specified template.
	 * 
	 * @param text Text to search
	 * @param template The Template (in template namespace) to look for. DO NOT add namespace prefix.
	 * @param redirects Specify <tt>true</tt> to incorporate redirects in this parse job for this template.
	 * @param wiki The wiki object to use
	 * 
	 * @return The template we parsed out, in the form {{TEMPLATE|ARG1|ARG2|...}} or NULL, if we didn't find the specified template.
	 * @throws IOException If network error
	 */

	public static String parseTemplateFromPage(String text, String template, boolean redirects, Wiki wiki) throws IOException
	{
		return redirects ? parseFromPageRegex(text, getRedirectsAsRegex("Template:" + template, wiki)) : parseFromPageRegex(text, "(?si)\\{\\{\\s*?(Template:)??\\s*?(" + template + ").*?\\}\\}");
	}

	/**
	 * Parses out the first group of matching text, based on specified regex. Useful for parsing out templates.
	 * 
	 * @param text Text to search
	 * @param regex The regex to use
	 * 
	 * @return The text we parsed out, or null if we didn't find anything.
	 * 
	 */

	public static String parseFromPageRegex(String text, String regex)
	{
		Matcher m = Pattern.compile(regex).matcher(text);
		if (m.find())
			return text.substring(m.start(), m.end());
		else
			return null;
	}
        
        /**
         *  Counts the occurrences of a regular expression in the given string.
         *  @param text the string to examine
         *  @param regex the regular expression to look for
         *  @return (see above)
         *  Contributed by Hunsu
         */
        public static int countOccurrences(String text, String regex)
        {
                Pattern p = Pattern.compile(Pattern.quote(regex));
                Matcher m = p.matcher(text);
                int count = 0;
                while (m.find())
                        count++;
                return count;
        }

        /**
         *  Returns the list of parameters used in the given template.
         *  @param template the string to parse
         *  @return (see above)
         *  Contributed by Hunsu
         */
        public static ArrayList<String> getTemplateParameters(String template)
        {
                ArrayList<String> f = new ArrayList<String>();
                int i = template.indexOf('|');
                if (i == -1)
                        return null; // the template doesn't have parameters;
                template = template.substring(i + 1, template.length() - 2);
                for (String s : template.split("\\|"))
                        f.add(s.trim());

                for (i = 0; i < f.size(); i++)
                {
                        String s = f.get(i);
                        if ((countOccurrences(s, "{{") != countOccurrences(s, "}}") || countOccurrences(
                                        s, "[[") != countOccurrences(s, "]]")) && i != f.size()-1)
                        {
                                s += "|" + f.get(i+1);
                                f.remove(i);
                                f.remove(i);
                                f.add(i, s);
                                i--;
                        }
                }
                return f;
        }

        /**
         *  Removes the given template parameter from the input string.
         *  Example: removeTemplateParam("{{template|A|B|C}}", "B") == {{template|A|C}}.
         *  @param template the input string
         *  @param param the parameter to remove
         *  @return the input string with the given parameter removed
         *  Contributed by Hunsu
         */
        public static String removeTemplateParam(String template, String param)
        {
                String newTemplate = "";
                ArrayList<String> f = getTemplateParameters(template);
                if (f == null) return template;
                for (String p : f)
                        if (p.startsWith(param))
                                f.remove(p);
                return newTemplate;
        }
}