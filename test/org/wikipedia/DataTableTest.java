/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wikipedia;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author MER-C
 */
public class DataTableTest
{
    private final DataTable dt1, dt2;
    private final LinkedHashMap<String, Integer> map1;
    private final LinkedHashMap<String, String> map2;
    private final List<String> headers;
    
    /**
     *  Construct wiki objects for each test so that tests are independent.
     */
    public DataTableTest()
    {
        map1 = new LinkedHashMap();
        map1.put("Value1", 10);
        map1.put("Value2", 20);
        map2 = new LinkedHashMap();
        map2.put("Simple key", "Simple value");
        map2.put("Test \"string\",", "Blah, \"Blah\"");
        headers = List.of("Column1", "Column2");
        dt1 = DataTable.create(map1, headers);
        dt2 = DataTable.create(map2, headers);
    }
    
    @Test
    public void create()
    {
        List<String> newheaders = List.of("Test1", "Test2", "Test3");
        assertThrows(IllegalArgumentException.class, () -> DataTable.create(map1, newheaders));
    }
    
    @Test
    public void headers() 
    {
        assertEquals(headers, dt1.getHeaders());
        
        List<String> newheaders = List.of("Test1", "Test2", "Test3");
        assertThrows(IllegalArgumentException.class, () -> DataTable.create(map1, newheaders));
        
        List<String> newheaders2 = List.of("Test1", "Test2");
        dt1.setHeaders(newheaders2);
        assertEquals(newheaders2, dt1.getHeaders());
    }

    @Test
    public void testFormatAsCSV() 
    {
        String expected = """
            "Column1","Column2"
            "Value1",10
            "Value2",20
            """;
        assertEquals(expected, dt1.formatAsCSV());
        
        // more challenging string values
        expected = """
            "Column1","Column2"
            "Simple key","Simple value"
            "Test ""string"",","Blah, ""Blah"\""
            """.replace("\\\"", "\"");
        assertEquals(expected, dt2.formatAsCSV());
    }

    @Test
    public void testFormatAsWikitext() 
    {
        String expected = """
            {| class="wikitable sortable"
            ! Column1 !! Column2
            |-
            | Value1 || 10
            |-
            | Value2 || 20
            |}
            """;
        assertEquals(expected, dt1.formatAsWikitext());
        
        // more challenging string values
        expected = """
            {| class="wikitable sortable"
            ! Column1 !! Column2
            |-
            | Simple key || Simple value
            |-
            | Test "string", || Blah, "Blah"
            |}
            """;
        assertEquals(expected, dt2.formatAsWikitext());
    }
}
