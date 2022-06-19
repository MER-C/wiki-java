/**
 *  @(#)package-info.java 0.01 15/02/2018
 *  Copyright (C) 2018-20xx MER-C
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
 *  A collection of MediaWiki/Wikimedia related utilities, including a rather
 *  sketchy bot framework that consists of only one file.
 *
 *  <p>
 *  This package does not, and will not, have any dependencies outside of the
 *  core JDK. Only the java.base, java.net.http and java.logging modules are 
 *  required.
 *
 *  <p>
 *  All methods should work on a vanilla installation of MediaWiki with the 
 *  exception of {@link WMFWiki} and {@link WMFWikiFarm}. Required extension(s) 
 *  for any given method are denoted in the documentation.
 */
package org.wikipedia;
