/**
 *  @(#)common.js 0.01 18/08/2018
 *  Copyright (C) 2018 MER-C
 *
 *  This is free software: you are free to change and redistribute it under the 
 *  Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
 *  for details. There is NO WARRANTY, to the extent permitted by law.
 */

// Toggle elements
disableElement = function(el)
{
    el.disabled = true;
    el.required = false;
}

enableRequiredElement = function(el)
{
    el.disabled = false;
    el.required = true;
}