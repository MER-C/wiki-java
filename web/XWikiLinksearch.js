/**
 *  @(#)XWikiLinksearch.js 0.01 20/09/2016
 *  Copyright (C) 2016 MER-C
 *
 *  This is free software: you are free to change and redistribute it under the 
 *  Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
 *  for details. There is NO WARRANTY, to the extent permitted by law.
 */

/**
 *  Toggles between the "single wiki" and "multiple wiki" modes of the 
 *  XWikiLinksearch tool.
 */
document.addEventListener('DOMContentLoaded', function() 
{
    document.getElementById('radio_multi').addEventListener('click', function()
    {
        disableElement(document.getElementById('wiki'));
        enableRequiredElement(document.getElementById('set'));
    });
    
    document.getElementById('radio_single').addEventListener('click', function()
    {
        enableRequiredElement(document.getElementById('wiki'));
        disableElement(document.getElementById('set'));
    });
});
