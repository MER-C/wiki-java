package org.wikiutils;

import java.io.IOException;
import java.util.ArrayList;
import org.wikipedia.Wiki;

/**
 * Contains several editing or log action related methods.
 * 
 * @author Fastily
 * 
 * @see org.wikiutils.CollectionUtils
 * @see org.wikiutils.DateUtils
 * @see org.wikiutils.GUIUtils
 * @see org.wikiutils.IOUtils
 * @see org.wikiutils.LoginUtils
 * @see org.wikiutils.ParseUtils
 * @see org.wikiutils.StringUtils
 */
public class WikiUtils
{

	/**
	 * Hiding constructor from JavaDoc
	 */
	private WikiUtils()
	{
	}

	/**
	 * Returns all the items in an array that are within the specified namespace. e.g.
	 * <tt>listNamespaceSort(list, WIKI.FILE_NAMESPACE, wiki);</tt>
	 * 
	 * @param wiki The wiki object to use.
	 * @param ns The namespace filter.  Items in this namespace shall be returned.
	 * @param pages The titles (including namespace) to check.
	 * 
	 * @return The list of items in the list that were in the specified namespace.
	 * 
	 * @throws IOException Network error
	 * 
	 */

	public static String[] listNamespaceSort(Wiki wiki, int ns, String...pages) throws IOException
	{
		ArrayList<String> l = new ArrayList<String>();
		for (String s : pages)
			if (wiki.namespace(s) == ns)
				l.add(s);

		return l.toArray(new String[0]);
	}

	/**
	 * Deletes all the elements in an array and their associated talk pages
	 * 
	 * @param list The array to use
	 * @param reason The reason to use while deleting
	 * @param talkReason The reason to use when deleting talk pages of the pages we're deleting.
	 *           Specify "null" if talk pages are not to be deleted
	 * @param wiki The wiki object to use.
	 * 
	 * @return An array containing the elements we were unable to delete.
	 */

	public static String[] arrayNuke(String[] list, String reason, String talkReason, Wiki wiki)
	{
		ArrayList<String> f = new ArrayList<String>();
		for (String s : list)
		{
			try
			{
				wiki.delete(s, reason);
			}
			catch (Throwable e)
			{
				f.add(s);
				continue;
			}

			if (talkReason != null)
			{
				try
				{
					wiki.delete(wiki.getTalkPage(s), talkReason);
				}
				catch (Throwable e)
				{
					// We'll probably only be here if we tried to delete a talk page of a talk page :P
				}
			}

		}
		return f.toArray(new String[0]);
	}

	/**
	 * Generic find and replace method.
	 * 
	 * @param find The text to be found. You can use a regex for this
	 * @param replacement The text to replace any text matching the <tt>find</tt> field
	 * @param summary The edit summary to use
	 * @param wiki The wiki object to use
	 * @param pages The wiki pages to act upon.
	 */
	public static void findAndReplace(String find, String replacement, String summary, Wiki wiki, String... pages)
	{
		for (String s : pages)
		{
			try
			{
				wiki.edit(s, wiki.getPageText(s).replaceAll(find, replacement), summary);
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Performs null edit on a list of pages by wiki. PRECONDITION: wiki object must be logged in
	 * 
	 * @param wiki The wiki object to use
	 * @param l The list of pages to null edit.
	 */
	public static void nullEdit(Wiki wiki, String... l)
	{
		for (String s : l)
		{
			try
			{
				System.err.println("Attempting null edit on '" + s + "'");
				wiki.edit(s, wiki.getPageText(s), "");
				
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Determines if a category is empty.
	 * 
	 * @param wiki The wiki object to use
	 * @param cat Category name, with or without "Category:" prefix, in any language, is fine.
	 * 
	 * @return True if the category is empty.
	 */
	public static boolean categoryIsEmpty(Wiki wiki, String cat)
	{
		try
		{
			return wiki.getCategoryMembers(ParseUtils.namespaceStrip(cat, wiki)).length == 0;
		}
		catch (Throwable e)
		{
			return false;
		}
	}
	
	/**
	 * Removes all titles of a certain namespace. 
	 * 
	 * @param wiki The wiki object to use.
	 * @param titles The list of titles to use.
	 * @param ns The namespaces to filter out.
	 * @return A new list of titles with this change.
	 * @throws IOException Eh?
	 */
	public static String[] namespaceFilter(Wiki wiki, String[] titles, int... ns) throws IOException
	{
		ArrayList<String> l = new ArrayList<String>();
		for(String s : titles)
		{
			boolean flag = false;
			for(int i : ns)
				if(wiki.namespace(s) == i)
				{
					flag = true;
					break;
				}
			
			if(!flag)
				l.add(s);
		}
		
		return l.toArray(new String[0]);		
		
	}
	
}