<%--
    @(#)imagecci.jsp 0.02 26/01/2017
    Copyright (C) 2011 - 2017 MER-C

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
--%>

<%@ include file="header.jsp" %>
<%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ page import="java.net.URLEncoder" %>

<%
    String user = request.getParameter("user");
    if (user != null)
    {
        // create download prompt
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(user, "UTF-8") + ".txt");

        // get results
        Wiki wiki = new Wiki("en.wikipedia.org");
        Wiki.User wpuser = wiki.getUser(user);
        String[][] survey = ContributionSurveyor.imageContributionSurvey(wiki, wpuser);

        // output results
        out.println("=== Uploads to " + wiki.getDomain() + " ===");
        int i = 0;
        for (String entry : survey[0])
        {
            i++;
            if (i % 20 == 1)
            {
%>
==== Local files <%= i %> to <%= Math.min(i + 19, survey[0].length) %> ====
<%
            }
%>*[[:<%= entry %>]]
<%
        }
%>
=== Uploads to commons.wikimedia.org ===
<%
        i = 0;
        for (String entry : survey[1])
        {
            i++;
            if (i % 20 == 1)
            {
%>
==== Commons files <%= i %> to <%= Math.min(i + 19, survey[1].length) %> ====
<%
            }
%>*[[:<%= entry %>]]
<%
        }
%>
=== Transferred files on commons.wikimedia.org ===
WARNING: may be inaccurate, depending on username.

<%
        i = 0;
        for (String entry : survey[2])
        {
            i++;
            if (i % 20 == 1)
            {
%>
==== Transferred files <%= i %> to <%= Math.min(i + 19, survey[2].length) %> ====
<%
            }
%>*[[:<%= entry %>]]
<%
        }
    }
    else
    {
%>

<!doctype html>
<html>
<head>
<link rel=stylesheet href="styles.css">
<title>Image contribution surveyor</title>
</head>

<body>
<p>
This tool generates a listing of a user's image uploads (regardless of whether 
they are deleted) for use at <a href="//en.wikipedia.org/wiki/WP:CCI">Contributor 
copyright investigations.</a>

<p>
<form action="./imagecci.jsp" method=GET>
    
<p>User to survey: 
<input type=text name=user required>
<input type=submit value="Survey user">
</form>

<br>
<br>
<hr>
<p>Image contribution surveyor: <%@ include file="footer.jsp" %>
    
<%
    }
%>