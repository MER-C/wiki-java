<!--
    @(#)masslinksearch.jsp 0.01 20/01/2017
    Copyright (C) 2016 - 2017 MER-C
  
    This is free software: you are free to change and redistribute it under the 
    Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
    for details. There is NO WARRANTY, to the extent permitted by law.
-->

<%@ include file="header.jsp" %>
<%@ page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>

<%
    request.setAttribute("toolname", "Mass linksearch");

    boolean https = (request.getParameter("https") != null);

    String wiki = request.getParameter("wiki");
    wiki = (wiki == null) ? "" : ServletUtils.sanitizeForAttribute(wiki);

    // parse inputdomains to pure list of domains
    String inputdomains = request.getParameter("domains");
    if (inputdomains != null)
    {
        inputdomains = ServletUtils.sanitizeForHTML(inputdomains).trim().toLowerCase()
        // \\bexample\\.com\\b to example.com
            .replace("\\b", "").replace("\\.", ".")
        // *{{LinkSummary|example.com}} to example.com
            .replaceAll("\\*\\s*?\\{\\{(link\\s?summary(live)?|spamlink)\\|", "")
            .replace("}}", "");
    }
    else
        inputdomains = "";
%>

<!doctype html>
<html>
<head>
<link rel=stylesheet href="styles.css">
<title><%= request.getAttribute("toolname") %></title>
</head>

<body>
<p>
This tool searches a single project for a large collection of links. Enter 
domain names (example.com, *{{LinkSummary|example.com}} and \\bexample\\.com\\b
are all acceptable) below, one per line. A timeout is more likely when searching
for more domains.

<p>
<form action="./masslinksearch.jsp" method=POST>
<table>
<tr>
    <td>Wiki:
    <td><input type=text name=wiki required value="<%= wiki %>">
<tr>
    <td valign=top>Domains:
    <td>
        <textarea name=domains rows=10 required>
<%= inputdomains %>
        </textarea>
<tr>
    <td>Additional protocols:
    <td><input type=checkbox name=https value=1<%= (inputdomains.isEmpty() ||
         https) ? " checked" : "" %>>HTTPS
</table>
<br>
<input type=submit value=Search>
</form>

<%
    if (!inputdomains.isEmpty() && !wiki.isEmpty())
    {
        out.println("<hr>");
        String[] domains = inputdomains.split("\r\n");
        Wiki w = Wiki.createInstance(wiki);
        w.setMaxLag(-1);
        w.setQueryLimit(500);

        StringBuilder regex = new StringBuilder();
        StringBuilder linksummary = new StringBuilder();
        ExternalLinks el = ExternalLinks.of(w);

        for (String domain : domains)
        {
            domain = domain.trim();
            
            // compute results
            List<String[]> temp = w.linksearch("*." + domain, "http");
            if (https)
            {
                List<String[]> temp2 = w.linksearch("*." + domain, "https");
                temp.addAll(temp2);
            }

            // reformat domain list to regex and linksummary
            regex.append("\\b");
            regex.append(domain.replace(".", "\\."));
            regex.append("\\b\n");
            linksummary.append("*{{LinkSummary|");
            linksummary.append(domain);
            linksummary.append("}}\n");

            out.println("<h3>Results for " + domain + "</h3>");
            out.println(el.linksearchResultsToHTML(temp, domain));
        }
%>
<hr>
<h3>Reformatted domain lists</h3>
<textarea readonly rows=10>
<%= regex %>
</textarea>
<textarea readonly rows=10>
<%= linksummary %>
</textarea>
<%
    }
%>
<%@ include file="footer.jsp" %>