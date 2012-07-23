package org.wikipedia;

import java.awt.GridLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.security.auth.login.FailedLoginException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Contains static MediaWiki bot/API methods built off MER-C's Wiki.java.
 * Please report bugs <a href=http://commons.wikimedia.org/w/index.php?title=User_talk:Fastily&action=edit&section=new>here</a>!
 * Visit our Google Code Project home <a href="http://code.google.com/p/wiki-java/">here</a>!
 * This code and project are licensed under the terms of the <a href="http://www.gnu.org/copyleft/gpl.html">GNU GPL v3 license</a>
 * 
 * @see org.wikipedia.FbotUtil
 * @see org.wikipedia.MBot
 * @see org.wikipedia.Wiki
 * @see org.wikipedia.FbotParse
 * 
 * @author Fastily
 */

public class Fbot
{
	// Hiding constructor from Javadoc
	private Fbot()
	{
		// do nothing
	}

	/**
	 * Generic login method which turns off maxlag and allows for setting of throttle.
	 * 
	 * @param wiki Wiki object to perform changes on
	 * @param user User to login as, without "User:" prefix
	 * @param p User's password, in the form of a char array.
	 * @param throttle Seconds to wait in between making edits
	 * 
	 * @throws IOException If we had a network error
	 * @throws FailedLoginException If we had bad login information
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
	 * Method reads in user/password combinations from a text file titled "px" (no extension) to log in user. In file, format should be
	 * "USERNAME:PASSWORD", separated by colon, one entry per line.
	 * 
	 * @param wiki Wiki object to perform changes on
	 * @param user Which account? (no "User:" prefix)
	 * 
	 * @throws IOException If we encountered a network error
	 * @throws FailedLoginException If user credentials do not match
	 * @throws FileNotFoundException If "px" (not "px.txt") does not exist.
	 * @throws UnsupportedOperationException if a non-recognized user is
	 * specified.
	 * 
	 * @see #loginAndSetPrefs
	 * 
	 */

	public static void loginPX(Wiki wiki, String user) throws IOException, FailedLoginException, FileNotFoundException
	{
		HashMap<String, String> c = FbotUtil.buildReasonCollection("px");
		String px = c.get(user);

		if (px == null)
			throw new UnsupportedOperationException("Did not find a Username in the specified file matching String value in user param");

		loginAndSetPrefs(wiki, user, px.toCharArray(), 1);
	}

	/**
	 * Login with loginAndSetPrefs(), but using a GUI. Throttle auto-set to 6 edits/min.
	 * 
	 * @param wiki Wiki object to perform changes on
	 * 
	 * @see #loginAndSetPrefs
	 * 
	 */
	public static void guiLogin(Wiki wiki)
	{

		JPanel pl = new JPanel(new GridLayout(2, 2));
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
			System.err.println("Network Error, program will now exit.");
		}
	}

	/**
	 * Gets the target of the redirect page. </br><b>PRECONDITION</b>: <tt>redirect</tt> must be a Redirect.
	 * 
	 * @param redirect The title of the redirect to get the target for.
	 * @param wiki The wiki object to use.
	 * 
	 * @throws Throwable If there was a network issue, non-existent page issue, or if we tried to access a Special: page.
	 * @throws UnsupportedOperationException If the page was not a redirect page.
	 * 
	 * @return String The title of the redirect's target.
	 */

	public static String getRedirectTarget(String redirect, Wiki wiki) throws Throwable
	{
		String text = wiki.getPageText(redirect).trim();

		if (text.matches("(?si)^#(redirect)\\s*?\\[\\[.+?\\]\\].*?"))
			return text.substring(text.indexOf("[[") + 2, text.indexOf("]]"));

		throw new UnsupportedOperationException("Parameter passed in is not a redirect page!");
	}

	/**
	 * Checks to see if a given page exists on Wiki (page is not a 'red-link')
	 * 
	 * @param page The page to check for.
	 * @param wiki The wiki object to use.
	 * 
	 * @throws Throwable
	 * 
	 * @return True if the page exists.
	 */

	public static boolean exists(String page, Wiki wiki) throws Throwable
	{
		return ((Boolean) wiki.getPageInfo(page).get("exists")).booleanValue();
	}

	/**
	 * A template that could be used when listing pages. Follows *[[:<tt>TITLE</tt>]]\n.
	 * 
	 * @param page Page to dump report into.
	 * @param list The list of pages that need to be dumped
	 * @param headerText Leading description text. Specify "" for no lead.
	 * @param footerText Ending description text. Specify "" for no end.
	 * @param wiki The wiki object to use.
	 * 
	 * @throws Throwable
	 * 
	 */

	public static void dbrDump(String page, String[] list, String headerText, String footerText, Wiki wiki) throws Throwable
	{
		String dump = headerText + "  This report last updated as of ~~~~~\n";
		for (String s : list)
			dump += "*[[:" + s + "]]\n";
		dump += "\n" + footerText;
		wiki.edit(page, dump, "Updating list");
	}

	/**
	 * Returns all the items in an array that are within the specified namespace.
	 * 
	 * @param list The list of items to use
	 * @param namespace The namespace of items to return
	 * @param wiki The wiki object to use.
	 * 
	 * @return The list of items in the list that were in the specified namespace.
	 * 
	 * @throws IOException If we had an issue populating namespace cache
	 * 
	 */

	public static String[] listNamespaceSort(String[] list, int namespace, Wiki wiki) throws IOException
	{
		ArrayList<String> l = new ArrayList<String>();
		for (String s : list)
		{
			if (wiki.namespace(s) == namespace)
				l.add(s);
		}

		return l.toArray(new String[0]);
	}

	/**
	 * Deletes all the elements in an array and their associated talk pages
	 * 
	 * @param list The array to use
	 * @param reason The reason to use while deleting
	 * @param talkReason The reason to use when deleting talk pages of the pages we're deleting. Specify "null" if talk pages are not to be deleted
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
	 * Attempts to add specified text to the end of each page in a list.
	 * 
	 * @param pages The list of pages to use
	 * @param text The text to append
	 * @param summary Edit summary to use.
	 * @param wiki The wiki object to use.
	 * 
	 */

	public static void addTextList(String[] pages, String text, String summary, Wiki wiki)
	{
		for (String page : pages)
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
	 * Replaces a file with another for a given list of pages.
	 * 
	 * @param list The list of pages to perform replacement on
	 * @param file The file to be replaced, without the "File:" prefix
	 * @param replacement The file to replace the first file with, without the "File:" prefix
	 * @param summary The edit summary to use
	 * @param wiki The wiki object to use.
	 * 
	 */

	public static void fileReplace(String[] list, String file, String replacement, String summary, Wiki wiki)
	{
		file = file.replace("_", " "); // need to guarantee that we aren't using underscores
		String regex = "(?i)(" + file + "|" + file.replace(" ", "_") + ")";

		for (String page : list)
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
	 * Creates Wiki objects with the specified username, password, and domain. </br></br>
	 * <b>PRECONDITION:</b> Username, password, and domain <span style="color:Red;font-weight:bold">MUST</span> be valid.
	 * Method will continue to loop until credentials are accepted so you might just find yourself in an infinite loop if
	 * they're not!
	 * 
	 * @param u The username to use
	 * @param p The password to use
	 * @param domain The domain to use (e.g. "commons.wikimedia.org")
	 * 
	 * @return The resulting Wiki object
	 * 
	 * @throws FailedLoginException If User/Password combination do not match
	 * @throws IOException If we had a network error.
	 * 
	 */

	public static Wiki wikiFactory(String u, char[] p, String domain) throws FailedLoginException, IOException
	{
		Wiki wiki = new Wiki(domain);
		try
		{
			boolean success = false;
			do
			{
				loginAndSetPrefs(wiki, u, p, 1);
				success = true;
			} while (!success);
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
		return wiki;
	}
}
