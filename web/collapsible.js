/**
 *  @(#)collapsible.js 0.01 06/10/2017
 *  Copyright (C) 2017 MER-C
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.

 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 *  Makes collapsible sections with headers collapsible. See 
 *  org.wikipedia.servlets.ServletUtils#beginCollapsibleSection.
 */
document.addEventListener('DOMContentLoaded', function() 
{
    var collapsibles = document.getElementsByClassName("collapsecontainer");
    for (i = 0; i < collapsibles.length; i++)
    {
        var link = collapsibles[i].getElementsByClassName("showhidelink")[0];
        link.addEventListener('click', function(event)
        {
            var collapsible = event.target.parentElement.parentElement.
                parentElement.getElementsByTagName("div")[0];
            if (collapsible.className === "collapsed")
            {
                event.target.innerHTML = "hide";
                collapsible.className = "notcollapsed";
            }
            else if (collapsible.className === "notcollapsed")
            {
                event.target.innerHTML = "show";
                collapsible.className = "collapsed";
            }
        });
    }
});