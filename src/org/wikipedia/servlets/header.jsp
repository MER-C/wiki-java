<%--
    @(#)header.jsp 0.01 20/01/2017
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

<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.util.stream.*" %>
<%@ page import="java.time.*" %>

<%@ page import="org.wikipedia.*" %>
<%@ page import="org.wikipedia.servlets.*" %>
<%@ page import="org.wikipedia.tools.*" %>

<%
// Set security headers

    // Enable HSTS (force HTTPS)
    response.setHeader("Strict-Transport-Security", "max-age=31536000");
    response.setHeader("Content-Security-Policy", 
        "frame-ancestors 'none'; " + // disable framing
        "default-src 'none'; " +     // disable everything by default
        "script-src 'self'; " +      // allow only scripts from this domain
        "style-src 'self'");         // allow only stylesheets from this domain
    // disable the Referer header
    response.setHeader("Referrer-Policy", "no-referrer");

// Disallow bad robots

    String useragent = request.getHeader("User-Agent");
    if (useragent.contains("Tweetmeme") || useragent.contains("bing.com"))
    {
        response.sendError(403, "robots.txt exists for a reason. Follow it!");
        return;
    }
%>