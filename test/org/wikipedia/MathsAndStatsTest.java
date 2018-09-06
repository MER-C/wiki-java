/**
 *  @(#)MathsAndStatsTest.java 0.01 06/09/2018
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

import java.time.*;
import java.util.*;
import java.util.function.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *  Unit tests for {@link MathsAndStats}.
 *  @author MER-C
 */
public class MathsAndStatsTest
{
    @Test
    public void testQuartiles()
    {
        double[] values = new double[] { 1.0, 2.0, 3.0 };
        double[] expected = new double[] { Double.NaN, Double.NaN };
        assertArrayEquals("quartiles: need four numbers", expected, MathsAndStats.quartiles(values), 1e-8);
                
        values = new double[] { 1.0, 2.0, 3.0, 4.0, Double.NaN };
        assertArrayEquals("quartiles: NaN input", expected, MathsAndStats.quartiles(values), 1e-8);
        values = new double[] { Double.NEGATIVE_INFINITY, 1.0, 2.0, 3.0, Double.POSITIVE_INFINITY };
        assertArrayEquals("quartiles: Minus and plus infinity input", expected, MathsAndStats.quartiles(values), 1e-8);
        values = new double[] { Double.NEGATIVE_INFINITY, 1.0, 2.0, 3.0, 4.0 };
        expected = new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
        assertArrayEquals("quartiles: Minus infinity input", expected, MathsAndStats.quartiles(values), 1e-8);
        values = new double[] { 1.0, 2.0, 3.0, 4.0, Double.POSITIVE_INFINITY };
        expected = new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
        assertArrayEquals("quartiles: Plus infinity input", expected, MathsAndStats.quartiles(values), 1e-8);
        
        // finally we get to check the functionality
        values = new double[] { 1.0, 2.0, 3.0, 4.0 };
        expected = new double[] { 1.5, 3.5 };
        assertArrayEquals("quartiles: 4n inputs", expected, MathsAndStats.quartiles(values), 1e-8);        
        values = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 };
        expected = new double[] { 1.75, 4.25 };
        assertArrayEquals("quartiles: 4n+1 inputs", expected, MathsAndStats.quartiles(values), 1e-8);
        values = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 };
        expected = new double[] { 2.0, 5.0 };
        assertArrayEquals("quartiles: 4n+2 inputs", expected, MathsAndStats.quartiles(values), 1e-8);
        values = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0 };
        expected = new double[] { 2.25, 5.75 };
        assertArrayEquals("quartiles: 4n+3 inputs", expected, MathsAndStats.quartiles(values), 1e-8);
    }

    @Test
    public void testMedian()
    {
        double[] values = new double[0];
        assertTrue("median: zero length = NaN", Double.isNaN(MathsAndStats.median(values)));
        testDoubleArrayEdgeCases(MathsAndStats::median);
        
        values = new double[] { 1.0, 2.0, 3.0 };
        assertEquals("median, odd number of inputs", 2.0, MathsAndStats.median(values), 1e-6);
        values = new double[] { 1.0, 2.0, 3.0, 4.0 };
        assertEquals("median, even number of inputs", 2.5, MathsAndStats.median(values), 1e-6);
    }
    
    @Test
    public void testValidate()
    {
        testDoubleArrayEdgeCases(MathsAndStats::validate);
        double[] values = new double[] { 1.0, 2.0, 3.0 };
        assertTrue("Normal input", Double.isFinite(MathsAndStats.validate(values)));
    }
    
    /**
     *  Checks whether extreme values (negative infinity, positive infinity and
     *  NaN) are handled correctly.
     *  @param func the function to test
     */
    public void testDoubleArrayEdgeCases(ToDoubleFunction<double[]> func)
    {
        double[] values = new double[] { 1.0, 2.0, Double.NaN };
        assertTrue("NaN input", Double.isNaN(func.applyAsDouble(values)));
        values = new double[] { Double.NEGATIVE_INFINITY, 1.0, Double.POSITIVE_INFINITY };
        assertTrue("Minus and plus infinity input", Double.isNaN(func.applyAsDouble(values)));
        values = new double[] { Double.NEGATIVE_INFINITY, 1.0, 2.0 };
        assertEquals("Minus infinity input", Double.NEGATIVE_INFINITY, func.applyAsDouble(values), 1e-8);
        values = new double[] { 1.0, 2.0, Double.POSITIVE_INFINITY };
        assertEquals("Plus infinity input", Double.POSITIVE_INFINITY, func.applyAsDouble(values), 1e-8);
    }   
    
    @Test
    public void max()
    {
        assertNull("max: empty input", MathsAndStats.max(Collections.emptyList()));
        List<Duration> durations = Arrays.asList(Duration.ofDays(1), Duration.ofDays(5), Duration.ofSeconds(30));
        assertEquals("max", durations.get(1), MathsAndStats.max(durations));
    }
    
    @Test
    public void testGenericMedian()
    {
        BinaryOperator<Integer> interpolator = (n1, n2) -> ((n1 + n2)/ 2);
        assertNull("generic median: zero length = null", MathsAndStats.median(Collections.emptyList(), interpolator));
        assertEquals("generic median: 1 item", Integer.valueOf(1), MathsAndStats.median(Arrays.asList(1), interpolator));
        assertEquals("generic median: 2 item", Integer.valueOf(2), MathsAndStats.median(Arrays.asList(1, 3), interpolator));
        assertEquals("generic median: 3 item", Integer.valueOf(3), MathsAndStats.median(Arrays.asList(1, 3, 5), interpolator));
        assertEquals("generic median: 3 item", Integer.valueOf(4), MathsAndStats.median(Arrays.asList(1, 3, 5, 7), interpolator));
    }
}
