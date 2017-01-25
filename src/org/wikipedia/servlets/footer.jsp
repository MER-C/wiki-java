<%--
    @(#)footer.jsp 0.01 20/01/2017
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
 
<%@ page import="java.util.*" %>

Copyright &copy; MER-C 2007-<%= new GregorianCalendar().get(Calendar.YEAR) %>.
This tool is free software: you can redistribute it and/or modify it under the 
terms of the <a href="//gnu.org/licenses/agpl.html">Affero GNU General Public 
License</a> as published by the Free Software Foundation, either version 3 of 
the License, or (at your option) any later version.

<p>
Source code is available <a href="//github.com/MER-C/wiki-java">here</a>. Report 
bugs at <a href="//en.wikipedia.org/wiki/User_talk:MER-C">my talk page</a> or 
the <a href="//github.com/MER-C/wiki-java/issues">Github issue tracker</a>.
        
<p><b>Tools:</b>
    <a href="./linksearch.jsp">Cross-wiki linksearch</a> |
    <a href="./masslinksearch.jsp">Mass linksearch</a> |
    <a href="./imagecci.jsp">Image contribution surveyor</a> |
    <a href="./spamarchivesearch.jsp">Spam blacklist archive search</a> |
    <a href="./prefixcontribs.jsp">Prefix contributions</a>
</body>
</html>