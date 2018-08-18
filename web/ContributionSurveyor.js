/**
 *  @(#)XWikiLinksearch.js 0.01 18/08/2018
 *  Copyright (C) 2016 MER-C
 *
 *  This is free software: you are free to change and redistribute it under the 
 *  Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
 *  for details. There is NO WARRANTY, to the extent permitted by law.
 */

/**
 *  Toggles between the "user" and "category" modes of the ContributionSurveyor
 *  tool.
 */
document.addEventListener('DOMContentLoaded', function() 
{
    document.getElementById('radio_user').addEventListener('click', function()
    {
        disableElement(document.getElementById('category'));
        enableRequiredElement(document.getElementById('user'));
    });
    
    document.getElementById('radio_category').addEventListener('click', function()
    {
        enableRequiredElement(document.getElementById('category'));
        disableElement(document.getElementById('user'));
    });
});
