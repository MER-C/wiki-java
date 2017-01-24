<!--
    @(#)prefixcontribs.jsp 0.01 24/01/2017
    Copyright (C) 2013 - 2017 MER-C
  
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.
  
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
-->

<%@ include file="header.jsp" %>
<%@ page contentType="text/html" pageEncoding="UTF-8" 
    trimDirectiveWhitespaces="true" %>

<%
    String prefix = request.getParameter("prefix");
    if (prefix == null)
        prefix = "";
    else
        prefix = ServletUtils.sanitizeForAttribute(prefix);

    String temp = request.getParameter("time");
    int time = (temp == null) ? 7 : Integer.parseInt(temp);
    time = Math.max(time, 0);
%>

<!doctype html>
<html>
<head>
<link rel=stylesheet href="styles.css">
<title>Prefix contributions</title>
</head>

<body>
<p>
This tool retrieves contributions of an IP range or username prefix. To search 
for an IPv4 range, use a search key of (say) 111.222. for 111.222.0.0/16. /24s 
work similarly. IPv6 ranges must be specified with all bytes filled, leading 
zeros removed and letters in upper case e.g. 1234:0:0567:AABB: . No sanitization
is performed on IP addresses. Timeouts are more likely for longer time spans.

<form action="./prefixcontribs.jsp" method=GET>
<table>
<tr>
    <td>Search string:
    <td><input type=text name=prefix required value="<%= prefix %>">
<tr>
    <td>For last:
    <td><input type=text name=time required value="<%= time %>"> days
</table>
<input type=submit value="Search">
</form>

<%
    if (!prefix.isEmpty())
    {
%>
<hr>
<%
        if (prefix.length() < 4)
        {
%>
<span class="error">ERROR: search key of insufficient length.</span>
<%
        }
        else
        {
            Wiki enWiki = new Wiki("en.wikipedia.org");
            enWiki.setMaxLag(-1);
            Calendar cutoff = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            cutoff.add(Calendar.DAY_OF_MONTH, -1 * time);
            Wiki.Revision[] revisions = enWiki.contribs("", prefix, cutoff, null);
            if (revisions.length == 0)
            {
%>
<p>
No contributions found.
<%
            }
            else
            {
%>
<%= ParserUtils.revisionsToHTML(enWiki, revisions) %>
<%
            }
        }
    }
%>

<br>
<br>
<hr>
<p>Prefix contributions: <%@ include file="footer.jsp" %>