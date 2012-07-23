package org.wikipedia;

import java.io.File;
import java.io.IOException;
import javax.security.auth.login.FailedLoginException;

/**
 * Class consisting of stand-alone, multi-threaded bot methods. These methods
 * are designed to get the specified task done, as damn <i>fast</i> as possible.
 * Currently consists of an Upload and Deletion bot. You may wish to adjust
 * <a href="http://stackoverflow.com/questions/1565388/increase-heap-size-in-java">adjust
 * heap space</a> according to the method you're running (Uploads may require
 * more space depending on the size of files you're uploading and how many
 * threads you've chosen to instantiate). Make feature requests
 * <a href="http://commons.wikimedia.org/w/index.php?title=User_talk:Fastily&action=edit&section=new">here</a> (fast)
 * or on the project's Google Code <a href="http://code.google.com/p/wiki-java/issues/list">issue tracker</a> (slow).
 * 
 * @see org.wikipedia.Fbot
 * @see org.wikipedia.FbotUtil
 * @see org.wikipedia.FbotParse
 * 
 * @author Fastily
 */

public class MBot
{
	/**
	 * Represents the option for uploading in Mbot.
	 */
	private static final short UPLOAD = 101;

	/**
	 * Represents the option for deleting in Mbot.
	 */
	private static final short DELETE = 102;

	/**
	 * The list of Wiki objects we'll be acting on.
	 */

	private Wiki[] wikis;

	/**
	 * Sublists for each wiki object to act upon. Determined by <tt>instances</tt> param in constructor.
	 */
	private String[][] lists;

	/**
	 * The reason(s) to use when performing a set action on <tt>list</tt>.
	 */
	private String[] reason;

	/**
	 * Constructor that creates an MBot object.
	 * 
	 * @param user The username to use
	 * @param px The password to use (This is not saved anywhere once the constructor exits)
	 * @param domain The domain to use (e.g. "commons.wikimedia.org", "en.wikipedia.org")
	 * @param instances The maximum number of threads to create when performing the requested action. Remember, you need to
	 * adjust your heap space accordingly for the task at hand and for the number of threads lest you should get out of memory
	 * exceptions!
	 * @param list The list we'll be acting upon
	 * @param reason The reason(s) to use when performing the specified actions.
	 * 
	 * @throws FailedLoginException If the login credentials you used were invalid.
	 * @throws IOException If we encountered a network error.
	 */

	public MBot(String user, char[] px, String domain, int instances, String[] list, String... reason) throws FailedLoginException, IOException
	{
		new Wiki(domain).login(user, px); // Testing for bad login credentials & network errors

		lists = FbotUtil.arraySplitter(list, instances); // determine number of splits

		// Create the actual Wiki objects
		wikis = new Wiki[lists.length];
		for (int i = 0; i < lists.length; i++)
			wikis[i] = Fbot.wikiFactory(user, px, domain);

		this.reason = reason;
	}

	/**
	 * Uploads files specified in the constructor. Interprets the <tt>list</tt> param
	 * in the constructor as a list of file paths and attempts to upload them to the
	 * specified Wiki with the specified credentials. Only the <i>first</i> reason param shall be interpreted
	 * as the text of the file description page; if you include more than one reason, the rest will be ignored
	 * Files will be uploaded using their default, system names. If an upload of a particular file fails
	 * for whatever reason, a stack trace shall be printed and uploading of the file shall be skipped.
	 * 
	 * @throws UnsupportedOperationException If you did not specify at least one reason in the constructor.
	 */
	public void upload()
	{
		if (reason.length < 1)
			throw new UnsupportedOperationException("You must provide at LEAST one arg in 'reason'.");
		this.generateThreadsAndRun(UPLOAD);
	}

	/**
	 * Deletes files specified in the constructor. Interprets the <tt>list</tt> param
	 * in the constructor as a list of wiki pages and attempts to delete them from the
	 * specified Wiki with the specified credentials. Only the <i>first</i> reason param shall be interpreted
	 * as the rationale to use when deleting the file; if you include more than one reason, the rest will be ignored.
	 * </br></br><b>CAVEAT:</b> If a deletion of a particular file fails for whatever reason, a stack trace,
	 * the method will continue to try and delete the page until the page is deleted.
	 * This means that if you're blocked or do not have the proper userrights associated with the account you're
	 * using to perform said action, the program will loop endlessly.
	 * 
	 * @throws UnsupportedOperationException If you did not specify at least one reason in the constructor.
	 */
	public void delete()
	{
		if (reason.length < 1)
			throw new UnsupportedOperationException("You must provide at LEAST one arg in 'reason'.");
		this.generateThreadsAndRun(DELETE);
	}

	/**
	 * Generic method to generate threads and set run mode. Splits arrays into smaller parts based on the
	 * wikis.length parameter. Will only generate as many threads as splits (i.e. # of threads = [0, wikis.length]).
	 * 
	 * @param mode The run mode to use. Must be one of the private static final fields specified above.
	 */
	private void generateThreadsAndRun(short mode)
	{
		for (int i = 0; i < lists.length; i++)
			new Thread(new MBotT(mode, wikis[i], lists[i])).start();
	}

	/**
	 * Grunt class that does all the actual work for MBot. Can be configured to run in different modes
	 * (currently limited to delete and upload, but more on the way hopefully!)
	 * 
	 */
	private class MBotT implements Runnable
	{
		/**
		 * Represents the mode (e.g. upload/delete) we'll be using
		 */
		private short option;

		/**
		 * The list we'll be acting on. This should be a split of a whole passed in to constructor.
		 */
		private String[] l;

		/**
		 * The list we'll be acting on. This should be a split of a whole passed in to constructor.
		 */
		private Wiki wiki;

		/**
		 * Constructor for MBotT.
		 * 
		 * @param option The mode we'll be using
		 * @param wiki The wiki object to use
		 * @param l The list of Strings to act upon.
		 */
		protected MBotT(short option, Wiki wiki, String[] l)
		{
			this.option = option;
			this.l = l;
			this.wiki = wiki;
		}

		/**
		 * Run class required by Runnable super interface. Basically consists of a fat switch
		 * statement which determines which method to run (e.g. upload, delete).
		 * 
		 * @throws UnsupportedOperationException If the mode specified in the constructor is
		 * not a supported mode.
		 */
		public void run()
		{
			switch (option)
			{
				case UPLOAD:
					this.upload();
					break;
				case DELETE:
					this.delete();
					break;
				default:
					throw new UnsupportedOperationException("Invalid option used!");
			}
		}

		/**
		 * Performs upload. If any issues (exceptions) are encountered it prints out a stacktrace
		 * and skips that file.
		 */
		private void upload()
		{
			for (String f : l)
			{
				try
				{
					File x = new File(f);
					wiki.upload(x, x.getName(), reason[0], "");
				} catch (Throwable e)
				{
					e.printStackTrace();
				}
			}
		}

		/**
		 * Performs deletion. Will continue to try and delete the files if exceptions are encountered
		 * so please be sure that this is running on an account with delete permissions, otherwise, you'll
		 * find yourself in an infinite loop :3
		 */
		private void delete()
		{
			for (String s : l)
			{
				try
				{
					boolean success = false;
					do
					{
						wiki.delete(s, reason[0]);
						success = true;
					} while (!success);
				} catch (Throwable e)
				{
					e.printStackTrace();
					System.err.println("Trying again.");
				}
			}
		}

	}
}
