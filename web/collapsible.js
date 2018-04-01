/**
 *  @(#)collapsible.js 0.01 06/10/2017
 *  Copyright (C) 2017 MER-C
 *
 *  This is free software: you are free to change and redistribute it under the 
 *  GNU GPL version 3 or later, see <https://www.gnu.org/licenses/gpl.html> for 
 *  details. There is NO WARRANTY, to the extent permitted by law.
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
        // collapse all divs in Javascript to keep them displayed if NoScripted
        var temp = collapsibles[i].getElementsByClassName("tocollapse")[0];
        temp.className = "collapsed";
        
        // install collapse listener
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