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
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

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
        assertArrayEquals(expected, MathsAndStats.quartiles(values), 1e-8, "need four inputs");
                
        values = new double[] { 1.0, 2.0, 3.0, 4.0, Double.NaN };
        assertArrayEquals(expected, MathsAndStats.quartiles(values), 1e-8, "NaN input");
        values = new double[] { Double.NEGATIVE_INFINITY, 1.0, 2.0, 3.0, Double.POSITIVE_INFINITY };
        assertArrayEquals(expected, MathsAndStats.quartiles(values), 1e-8, "Minus and plus infinity input");
        values = new double[] { Double.NEGATIVE_INFINITY, 1.0, 2.0, 3.0, 4.0 };
        expected = new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
        assertArrayEquals(expected, MathsAndStats.quartiles(values), 1e-8, "Minus infinity input");
        values = new double[] { 1.0, 2.0, 3.0, 4.0, Double.POSITIVE_INFINITY };
        expected = new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
        assertArrayEquals(expected, MathsAndStats.quartiles(values), 1e-8, "Plus infinity input");
        
        // finally we get to check the functionality
        values = new double[] { 1.0, 2.0, 3.0, 4.0 };
        expected = new double[] { 1.5, 3.5 };
        assertArrayEquals(expected, MathsAndStats.quartiles(values), 1e-8, "4n inputs");        
        values = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 };
        expected = new double[] { 1.75, 4.25 };
        assertArrayEquals(expected, MathsAndStats.quartiles(values), 1e-8, "4n+1 inputs");
        values = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 };
        expected = new double[] { 2.0, 5.0 };
        assertArrayEquals(expected, MathsAndStats.quartiles(values), 1e-8, "4n+2 inputs");
        values = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0 };
        expected = new double[] { 2.25, 5.75 };
        assertArrayEquals(expected, MathsAndStats.quartiles(values), 1e-8, "4n+3 inputs");
    }

    @Test
    public void testMedian()
    {
        double[] values = new double[0];
        assertTrue(Double.isNaN(MathsAndStats.median(values)), "zero length = NaN");
        testDoubleArrayEdgeCases(MathsAndStats::median);
        
        values = new double[] { 1.0, 2.0, 3.0 };
        assertEquals(2.0, MathsAndStats.median(values), 1e-6, "odd number of inputs");
        values = new double[] { 1.0, 2.0, 3.0, 4.0 };
        assertEquals(2.5, MathsAndStats.median(values), 1e-6, "even number of inputs");
    }
    
    @Test
    public void testValidate()
    {
        testDoubleArrayEdgeCases(MathsAndStats::validate);
        double[] values = new double[] { 1.0, 2.0, 3.0 };
        assertTrue(Double.isFinite(MathsAndStats.validate(values)));
    }
    
    /**
     *  Checks whether extreme values (negative infinity, positive infinity and
     *  NaN) are handled correctly.
     *  @param func the function to test
     */
    public void testDoubleArrayEdgeCases(ToDoubleFunction<double[]> func)
    {
        double[] values = new double[] { 1.0, 2.0, Double.NaN };
        assertTrue(Double.isNaN(func.applyAsDouble(values)), "NaN input");
        values = new double[] { Double.NEGATIVE_INFINITY, 1.0, Double.POSITIVE_INFINITY };
        assertTrue(Double.isNaN(func.applyAsDouble(values)), "Minus and plus infinity input");
        values = new double[] { Double.NEGATIVE_INFINITY, 1.0, 2.0 };
        assertEquals(Double.NEGATIVE_INFINITY, func.applyAsDouble(values), 1e-8, "Minus infinity input");
        values = new double[] { 1.0, 2.0, Double.POSITIVE_INFINITY };
        assertEquals(Double.POSITIVE_INFINITY, func.applyAsDouble(values), 1e-8, "Plus infinity input");
    }   
    
    @Test
    public void max()
    {
        assertNull(MathsAndStats.max(Collections.emptyList()), "empty input");
        List<Duration> durations = List.of(Duration.ofDays(1), Duration.ofDays(5), Duration.ofSeconds(30));
        assertEquals(durations.get(1), MathsAndStats.max(durations));
    }
    
    @Test
    public void testGenericMedian()
    {
        BinaryOperator<Integer> interpolator = (n1, n2) -> ((n1 + n2)/ 2);
        assertNull(MathsAndStats.median(Collections.emptyList(), interpolator), "zero length = null");
        assertEquals(Integer.valueOf(1), MathsAndStats.median(List.of(1), interpolator), "1 item");
        assertEquals(Integer.valueOf(2), MathsAndStats.median(List.of(1, 3), interpolator), "2 items");
        assertEquals(Integer.valueOf(3), MathsAndStats.median(List.of(1, 3, 5), interpolator), "3 items");
        assertEquals(Integer.valueOf(4), MathsAndStats.median(List.of(1, 3, 5, 7), interpolator), "4 items");
    }
    
    @Test
    public void testFormatDuration()
    {
        assertEquals("0s", MathsAndStats.formatDuration(Duration.ZERO));
        assertEquals("1s", MathsAndStats.formatDuration(Duration.ofSeconds(1)));
        assertEquals("-1s", MathsAndStats.formatDuration(Duration.ofSeconds(-1)));
        
        assertEquals("2m 0s", MathsAndStats.formatDuration(Duration.ofSeconds(120)));
        assertEquals("1m 10s", MathsAndStats.formatDuration(Duration.ofSeconds(70)));
        assertEquals("-5m 22s", MathsAndStats.formatDuration(Duration.ofSeconds(-322)));
        
        assertEquals("1h 0m", MathsAndStats.formatDuration(Duration.ofSeconds(3601)));
        assertEquals("3h 10m", MathsAndStats.formatDuration(Duration.ofSeconds(3600*3 + 610)));
        assertEquals("-5h 43m", MathsAndStats.formatDuration(Duration.ofSeconds(-3600*5 - 43*60 - 59)));
        
        assertEquals("3d 0h", MathsAndStats.formatDuration(Duration.ofSeconds(86400*3 + 3000)));
        assertEquals("1d 22h", MathsAndStats.formatDuration(Duration.ofSeconds(86400 + 22*3600 + 542)));
        assertEquals("-9d 23h", MathsAndStats.formatDuration(Duration.ofSeconds(-863999)));
        
        assertEquals("10d", MathsAndStats.formatDuration(Duration.ofSeconds(864000)));
        assertEquals("-15d", MathsAndStats.formatDuration(Duration.ofSeconds(-86400*15)));
    }
}
