/**
*  This program is free software; you can redistribute it and/or
*  modify it under the terms of the GNU General Public License
*  as published by the Free Software Foundation; either version 3
*  of the License, or (at your option) any later version. Additionally
*  this file is subject to the "Classpath" exception.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with this program; if not, write to the Free Software Foundation,
*  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
package org.wikibase.data;

import java.math.BigInteger;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Time extends WikibaseDataType {
    private Calendar calendar;
    private int before;
    private int after;
    private int precision;
    private URL calendarModel;
    private BigInteger year;

    public Calendar getCalendar() {
        return calendar;
    }

    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
    }

    public int getBefore() {
        return before;
    }

    public void setBefore(int before) {
        this.before = before;
    }

    public int getAfter() {
        return after;
    }

    public void setAfter(int after) {
        this.after = after;
    }

    public URL getCalendarModel() {
        return calendarModel;
    }

    public void setCalendarModel(URL calendarModel) {
        this.calendarModel = calendarModel;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public BigInteger getYear() {
        return year;
    }

    public void setYear(BigInteger year) {
        this.year = year;
    }

    public String toString() {
        String[] patternsForOldYears = new String[] { "% billion years", // precision: billion years
            "$100 million years", // precision: hundred million years
            "$10 million years", // precision: ten million years
            "$1 million years", // precision: million years
            "$100,000 years", // precision: hundred thousand years
            "$10,000 years", // precision: ten thousand years
        };
        Map<Integer, DateFormat> dateformats = new HashMap<Integer, DateFormat>() {
            {
                put(10, new SimpleDateFormat("MMMM y G", Locale.ENGLISH));
                put(11, new SimpleDateFormat("d MMMM y G", Locale.ENGLISH));
                put(12, new SimpleDateFormat("d MMMM y G HH 'hours' Z", Locale.ENGLISH));
                put(13, new SimpleDateFormat("d MMMM y G HH:mm Z", Locale.ENGLISH));
                put(14, new SimpleDateFormat("d MMMM y G HH:mm:ss Z", Locale.ENGLISH));
            }
        };
        if (5 >= precision) {
            BigInteger factor = BigInteger.valueOf(10l).pow(9 - precision);
            BigInteger[] yy = year.divideAndRemainder(factor);
            BigInteger y2 = yy[0];
            if (0 < yy[1].compareTo(BigInteger.ZERO)) {
                y2 = yy[0].add(BigInteger.ONE);
            }
            return String.format(patternsForOldYears[precision], y2);
        }
        String era = "";
        if (calendar.get(Calendar.ERA) == GregorianCalendar.BC || year.compareTo(BigInteger.valueOf(1000l)) < 0) {
            era = (calendar.get(Calendar.ERA) == GregorianCalendar.BC || year.signum() < 0) ? " BC" : " AD";
        }

        StringBuilder builder = new StringBuilder();
        switch (precision) {
        case 6:
            builder.append(Math.floor((Math.abs(year.longValue()) - 1l) / 1000.0) + 1);
            builder.append(" millenium");
            builder.append(era);
            break;
        case 7:
            builder.append(Math.floor((Math.abs(year.longValue()) - 1l) / 100.0) + 1);
            builder.append(" century");
            builder.append(era);
            break;
        case 8:
            builder.append(Math.floor(Math.abs(year.doubleValue()) / 10) * 10);
            builder.append("s");
            builder.append(era);
            break;
        case 9:
            builder.append(year);
            builder.append(era);
            break;
        default:
            builder.append(dateformats.get(precision).format(calendar.getTime()));

        }
        return builder.toString();
    }
}
