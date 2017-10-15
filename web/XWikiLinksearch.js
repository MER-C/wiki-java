/**
 *  @(#)XWikiLinksearch.js 0.01 20/09/2016
 *  Copyright (C) 2016 MER-C
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
