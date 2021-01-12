/**
 *  @(#)DataTable.java 0.01 05/01/2021
 *  Copyright (C) 2021-20xx MER-C
 *
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
package org.wikipedia;

import java.util.*;

/**
 *  A table of data (two columns only for now).
 *  @author MER-C
 */
public class DataTable
{
    private Map<?, ?> hist;
    private List<String> headers;
    
    /**
     *  Creates a new data table.
     *  @param hist the input data
     *  @param headers the column headers used for export
     *  @return the constructed table
     *  @throws IllegalArgumentException if the number of headers does not equal
     *  the number of columns
     */
    public static DataTable create(Map<?, ?> hist, List<String> headers)
    {
        DataTable table = new DataTable(hist);
        table.setHeaders(headers);
        return table;
    }
    
    private DataTable(Map<?, ?> hist)
    {
        this.hist = hist;
    }
    
    /**
     *  Returns a read-only view of the table's headers.
     *  @return (see above)
     */
    public List<String> getHeaders()
    {
        return Collections.unmodifiableList(headers);
    }
    
    /**
     *  Sets the headers for this table.
     *  @param headers the headers for this table
     *  @throws IllegalArgumentException if the number of headers does not equal
     *  the number of columns
     */
    public void setHeaders(List<String> headers)
    {
        if (headers.size() != 2)
            throw new IllegalArgumentException("The number of headers must equal the number of columns.");
        this.headers = headers;
    }
    
    /**
     *  Exports the table to CSV. Note: returns a String instead of writing 
     *  directly to a file because that String may be returned as a file 
     *  download of a server.
     *  @return the table in CSV format
     */
    public String formatAsCSV()
    {
        StringBuilder sb = new StringBuilder("\"");
        sb.append(headers.get(0));
        for (int i = 1; i < headers.size(); i++)
        {
            sb.append("\",\"");
            sb.append(headers.get(i));
        }
        sb.append("\"\n");
        for (var entry : hist.entrySet())
        {
            sb.append("\"");
            sb.append(entry.getKey().toString().replace("\"", "\"\""));
            var value = entry.getValue();
            if (value == null)
            {
                sb.append("\",");
            }
            else if (value instanceof Number)
            {
                sb.append("\",");
                sb.append(value);
            }
            else
            {
                sb.append("\",\"");
                sb.append(value.toString().replace("\"", "\"\""));
                sb.append("\"");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    /**
     *  Exports the table to wikitext. Note: returns a String instead of writing 
     *  directly to a file because that String may be returned as a file 
     *  download of a server.
     *  @return the table in wikitext format
     */
    public String formatAsWikitext()
    {
        StringBuilder sb = new StringBuilder("{| class=\"wikitable sortable\"\n! ");
        sb.append(headers.get(0));
        for (int i = 1; i < headers.size(); i++)
        {
            sb.append(" !! ");
            sb.append(headers.get(i));
        }
        sb.append("\n");
        for (var entry : hist.entrySet())
            sb.append(WikitextUtils.addTableRow(List.of(entry.getKey().toString(), entry.getValue().toString())));
        sb.append("|}\n");
        return sb.toString();
    }
}