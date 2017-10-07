<!--
    @(#)editorintersection.jsp 0.01 05/10/2017
    Copyright (C) 2017 MER-C
  
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
    request.setAttribute("toolname", "Article/editor intersection (beta)");

    String pages = request.getParameter("pages");
    if (pages == null)
        pages = "";
    else
        pages = ServletUtils.sanitizeForHTML(pages);

    boolean noadmin = (request.getParameter("noadmin") != null);
    boolean nobot = (request.getParameter("nobot") != null);
    boolean noanon = (request.getParameter("noanon") != null);
%>

<!doctype html>
<html>
<head>
<link rel=stylesheet href="styles.css">
<script src="collapsible.js"></script>
<title><%= request.getAttribute("toolname") %></title>
</head>

<body>
<p>
This tool retrieves the common editors of a given set of articles. Limited to 
the 1500 most recent revisions in each of 25 articles for now.

<form action="./editorintersection.jsp" method=POST>
<table>
<tr>
    <td valign=top>Articles:<br>(one per line)
    <td>
        <textarea name=pages rows=10 required>
<%= pages %>
        </textarea>
<tr>
    <td>Exclude: 
    <td><input type=checkbox name=noadmin value=1<%= (pages.isEmpty() || noadmin) ? " checked" : "" %>>admins</input>
        <input type=checkbox name=nobot value=1<%= (pages.isEmpty() || nobot) ? " checked" : "" %>>bots</input>
        <input type=checkbox name=noanon value=1<%= noanon ? " checked" : "" %>>IPs</input>
</table>
<br>
<input type=submit value=Search>
</form>

<%
    if (!pages.isEmpty())
    {
        out.println("<hr>");
        String[] temp = pages.split("\r\n");
        for (int i = 0; i < temp.length; i++)
            temp[i] = temp[i].trim();
        String[] pagesarray = Arrays.copyOf(temp, Math.min(temp.length, 24));
        Wiki wiki = Wiki.createInstance("en.wikipedia.org");
        wiki.setMaxLag(-1);
        wiki.setQueryLimit(1500);
        
        Map<String, List<Wiki.Revision>> results = ArticleEditorIntersection.articleEditorIntersection(wiki, pagesarray, noadmin, nobot, noanon);
        for (Map.Entry<String, List<Wiki.Revision>> entry : results.entrySet())
        {
            out.println("<h2>" + entry.getKey() + "</h2>");
            out.println(ParserUtils.generateUserLinks(wiki, entry.getKey()));
            
            // group by article
            Map<String, List<Wiki.Revision>> grouppage = entry.getValue()
                .stream()
                .collect(Collectors.groupingBy(Wiki.Revision::getPage));
            for (Map.Entry<String, List<Wiki.Revision>> entry2 : grouppage.entrySet())
            {
                List<Wiki.Revision> revs = entry2.getValue();
                String title = entry2.getKey() + " &ndash; " + revs.size() + " edit";
                if (revs.size() > 1)
                    title += "s";
                out.println("<p>");
                out.println(ServletUtils.beginCollapsibleSection(title, false));
                out.println(ParserUtils.revisionsToHTML(wiki, revs.toArray(new Wiki.Revision[revs.size()])));
                out.println(ServletUtils.endCollapsibleSection());
            }
        }
    }
%>
<%@ include file="footer.jsp" %>