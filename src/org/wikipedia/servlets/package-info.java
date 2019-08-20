/**
 *  @(#)package-info.java 0.01 15/02/2018
 *  Copyright (C) 2018 MER-C
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

/**
 *  This package contains JSP files for online tools (which are invisible to 
 *  JavaDoc) as well as a helper class to generate HTML for said tools. The
 *  backends for these tools reside in {@link org.wikipedia.tools}. The tools
 *  are hosted on <a href="https://wikipediatools.appspot.com">
 *  wikipediatools.appspot.com</a> (there is no dependence on any Google API).
 *
 *  <h3>Deploying these tools on your infrastructure</h3>
 *  <p>
 *  As the name suggests, this package requires a compliant implementation of
 *  the <a href="https://jcp.org/en/jsr/detail?id=369">Java servlet API.</a>
 *  There are no other dependencies other than the core JDK. You can generate
 *  a WAR file to deploy to your servlet container by running:
 *
 *  <pre>
 *  mvn clean package jetty-jspc:jspc javadoc:javadoc war:war -Dmaven.test.skip
 *  </pre>
 * 
 *  @see <a href="https://wikipediatools.appspot.com">wikipediatools.appspot.com</a>
 *  @see org.wikipedia.tools
 */
package org.wikipedia.servlets;
