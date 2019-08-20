<!--
    @(#)prefixcontribs.jsp 0.01 24/01/2017
    Copyright (C) 2013 - 2017 MER-C
  
    This is free software: you are free to change and redistribute it under the 
    Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
    for details. There is NO WARRANTY, to the extent permitted by law.
-->

<%
    request.setAttribute("toolname", "Prefix contributions");
    request.setAttribute("earliest_default", LocalDate.now(ZoneOffset.UTC).minusDays(7));

    String prefix = ServletUtils.sanitizeForAttribute(request.getParameter("prefix"));    
%>
<%@ include file="datevalidate.jspf" %>
<%@ include file="header.jspf" %>

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
    <td>Show changes from:
    <td><input type=date name=earliest value="<%= earliest %>"> to 
        <input type=date name=latest value="<%= latest %>"> (inclusive)
</table>
<input type=submit value="Search">
</form>

<%
    if (!prefix.isEmpty())
    {
        if (prefix.length() < 4)
            request.setAttribute("error", "ERROR: search key of insufficient length.");
        else
        {
            Wiki enWiki = Wiki.newSession("en.wikipedia.org");
            enWiki.setMaxLag(-1);
            enWiki.setQueryLimit(1000);
            Wiki.RequestHelper rh = enWiki.new RequestHelper()
                .withinDateRange(earliest_odt, latest_odt);
            List<Wiki.Revision> revisions = enWiki.prefixContribs(prefix, rh);
            out.println("<hr>");
            if (revisions.isEmpty())
                out.println("<p>\nNo contributions found.");
            else
                out.println(Revisions.of(enWiki).toHTML(revisions));
            if (revisions.size() == 1000)
                out.println("<p>\nAt least 1000 contributions found.");
            else
                out.println("<p>\n" + revisions.size() + " contributions found.");
        }
    }
%>
<%@ include file="footer.jspf" %>