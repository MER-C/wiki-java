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
        var el = document.getElementById('wiki');
        el.disabled = true;
        el.required = false;
        document.getElementById('set').disabled = false;
    });
    
    document.getElementById('radio_single').addEventListener('click', function()
    {
        var el = document.getElementById('wiki');
        el.disabled = false;
        el.required = true;
        el = document.getElementById('set').disabled = true;
    });
});
