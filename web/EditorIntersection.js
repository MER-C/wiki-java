/**
 *  @(#)EditorIntersection.js 0.01 11/10/2017
 *  Copyright (C) 2017 MER-C
 *
 *  This is free software: you are free to change and redistribute it under the 
 *  Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
 *  for details. There is NO WARRANTY, to the extent permitted by law.
 */

/**
 *  Toggles between the different modes of the article-editor intersection tool.
 */
document.addEventListener('DOMContentLoaded', function() 
{
    document.getElementById('radio_cat').addEventListener('click', function()
    {
        var el = document.getElementById('pages');
        el.disabled = true;
        el.required = false;
        el = document.getElementById('user');
        el.disabled = true;
        el.required = false;
        el = document.getElementById('category');
        el.disabled = false;
        el.required = true;
    });
    
    document.getElementById('radio_user').addEventListener('click', function()
    {
        var el = document.getElementById('pages');
        el.disabled = true;
        el.required = false;
        el = document.getElementById('user');
        el.disabled = false;
        el.required = true;
        el = document.getElementById('category');
        el.disabled = true;
        el.required = false;
    });
    
    document.getElementById('radio_pages').addEventListener('click', function()
    {
        var el = document.getElementById('pages');
        el.disabled = false;
        el.required = true;
        el = document.getElementById('user');
        el.disabled = true;
        el.required = false;
        el = document.getElementById('category');
        el.disabled = true;
        el.required = false;
    });
});
