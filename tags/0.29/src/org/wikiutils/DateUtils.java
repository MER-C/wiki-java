package org.wikiutils;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.wikipedia.Wiki;

/**
 * Contains a few Calendar/Date related methods.
 * 
 * @author Fastily
 * 
 * @see org.wikiutils.CollectionUtils
 * @see org.wikiutils.GUIUtils
 * @see org.wikiutils.IOUtils
 * @see org.wikiutils.LoginUtils
 * @see org.wikiutils.ParseUtils
 * @see org.wikiutils.StringUtils
 * @see org.wikiutils.WikiUtils
 */
public class DateUtils
{
	/**
	 * Hiding constructor from JavaDoc
	 */
	private DateUtils()
	{
	}
	/**
	 * Returns a Gregorian Calendar offset by a given number of days from the current system clock.
	 * Use positive int to offset by future days and negative numbers to offset to days before.
	 * Automatically set to UTC.
	 * 
	 * @param days The number of days to offset by -/+. Use 0 for no offset.
	 * 
	 * @return The newly modified calendar.
	 * 
	 */

	public static GregorianCalendar offsetTime(int days)
	{
		GregorianCalendar utc = (GregorianCalendar) new Wiki().makeCalendar();
		utc.setTimeInMillis(utc.getTime().getTime() + 86400000L * days);

		return utc;
	}
	
	
	/**
	 * Outputs the date/time in UTC. Based on the format and offset in days.
	 * 
	 * @param format Must be specified in accordance with java.text.DateFormat.
	 * @param offset The offset from the current time, in days. Accepts positive values (for future),
	 *           negative values (for past), and 0 for no offset.
	 * 
	 * @return The formatted date string.
	 * 
	 */

	public static String fetchDateUTC(String format, int offset)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		return sdf.format(offsetTime(offset).getTime());
	}
}