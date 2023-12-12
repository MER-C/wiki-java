/**
 *  @(#)ThrowingFunction.java 0.01 10/12/2023
 *  Copyright (C) 2023 - 20xx MER-C
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
package org.wikipedia;

import java.io.IOException;
import java.util.function.Function;

/**
 *  Functional interface for methods that can throw IOExceptions.
 *  @param <P> the type of the function parameter
 *  @param <R> the type of the return variable
 *  @author MER-C
 *  @version 0.01
 */
@FunctionalInterface
public interface ThrowingFunction<P, R> extends Function<P, R>
{
    /**
     *  Applies this function to the given input. If an IOException happens,
     *  return null.
     *  @param input the function argument
     *  @return the function result or null if an IOException is thrown
     */
    @Override
    public default R apply(P input)
    {
        try
        {
            return applyThrows(input);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }
    
    /**
     *  Applies this function to the given input, which may throw an IOException.
     *  @param input the function argument
     *  @return the function result
     *  @throws IOException if a filesystem or network error occurs 
     */
    public R applyThrows(P input) throws IOException;
}
