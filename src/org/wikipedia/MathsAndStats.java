/**
 *  @(#)MathsAndStats.java 0.01 03/09/2018
 *  Copyright (C) 2018 MER-C
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

import java.util.*;
import java.util.function.*;

/**
 *  Maths and stats stuff.
 *  @author MER-C
 */
public class MathsAndStats
{
    /**
     *  Computes the upper and lower quartiles of an array of numbers. Assumes 
     *  that the input array is sorted in ascending order. See <a 
     *  href="https://en.wikipedia.org/wiki/Quartile">here</a> (method 3).
     *  @param values the values to compute quartiles for (must not be null)
     *  @return an array: { Q1, Q3 } or { NaN, NaN } if less than four numbers
     *  are supplied
     */
    public static double[] quartiles(double[] values)
    {
        int size = values.length;
        if (size < 4)
            return new double[] { Double.NaN, Double.NaN };
        double validationresult = validate(values);
        if (!Double.isFinite(validationresult))
            return new double[] { validationresult, validationresult };
                    
        int middle = values.length / 2;
        int n = values.length / 4;
        double[] ret = new double[2];
        
        switch (values.length % 4)
        {
            case 0:
            case 2:
                double[] temp = Arrays.copyOfRange(values, 0, middle);
                ret[0] = median(temp);
                temp = Arrays.copyOfRange(values, middle, values.length);
                ret[1] = median(temp);
                return ret;
            case 1:
                ret[0] = values[n - 1]/4 + values[n] * 3/4;
                ret[1] = values[3*n] * 3/4 + values[3*n + 1]/4;
                return ret;
            case 3:
                ret[0] = values[n] * 3/4 + values[n+1]/4;
                ret[1] = values[3*n + 1]/4 + values[3*n+2] * 3/4;
                return ret;
        }
        throw new AssertionError("Unreachable.");
    }
    
    /**
     *  Computes the median of an array of numbers. Assumes that the input 
     *  array is sorted in ascending order. 
     *  @param values the values to compute the median for (must not be null)
     *  @return the median, or NaN if the <var>values.length</var> is zero
     */
    public static double median(double[] values)
    {
        int size = values.length;
        if (size < 1)
            return Double.NaN;
        double validationresult = validate(values);
        if (!Double.isFinite(validationresult))
            return validationresult;
        
        int middle = size / 2;
        if (size % 2 == 1)
            return values[middle];
        else
            return (values[middle - 1] + values[middle])/2;
    }
    
    /**
     *  Checks for edge cases in the input list of values. Assumes that the 
     *  input array is sorted in ascending order. Returns NaN if any number is
     *  NaN or if positive and negative infinity are both present, and the 
     *  respective infinity value if either is present. Use as follows:
     * 
     *  {@code <pre>
     *  double result = validate(input);
     *  if (!Double.isFinite(result))
     *      return result;
     *  </pre>}
     * 
     *  @param values a double array (must not be null)
     *  @return (see above)
     */
    public static double validate(double[] values)
    {
        double last = values[values.length - 1];
        // quick reminder:
        // Double.NEGATIVE_INFINITY < everything < Double.POSITIVE_INFINITY < Double.NaN
        if (Double.isNaN(last))
            return Double.NaN;
        if (values[0] == Double.NEGATIVE_INFINITY)
        {
            if (last == Double.POSITIVE_INFINITY)
                return Double.NaN;
            return Double.NEGATIVE_INFINITY;
        }
        if (last == Double.POSITIVE_INFINITY)
            return Double.POSITIVE_INFINITY;
        return 0;
    }
    
    /**
     *  Computes the maximum of a bunch of {@code Comparable} objects. 
     *  @param <T> an Object implementing {@code Comparable}
     *  @param values a list of comparable objects
     *  @return the maximum of the bunch, or null if the bunch is empty
     */
    public static <T extends Comparable> T max(Iterable<T> values)
    {
        T max = null;
        for (T value : values)
            if (max == null || value.compareTo(max) > 0)
                max = value;
        return max;
    }
    
    /**
     *  Computes the median of a list of {@code Comparable} objects. Assumes 
     *  that the input list is sorted in ascending order.
     *  @param <T> an Object implementing {@code Comparable}
     *  @param values a list of comparable objects
     *  @param interpolator a binary operator that performs a linear interpolation 
     *  between two objects (must not be null)
     *  @return the median of the list, or null if the list is empty
     */
    public static <T extends Comparable> T median(List<T> values, BinaryOperator<T> interpolator)
    {
        Objects.requireNonNull(interpolator);
        int size = values.size();
        if (size == 0)
            return null;
        
        int desired = size / 2;
        if ((size % 2) == 0)
            return interpolator.apply(values.get(desired - 1), values.get(desired));
        else
            return values.get(desired);
    }
}
