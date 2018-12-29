package org.wikiutils;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit tests for ParseUtils.java.
 *  @author MER-C
 */
public class ParseUtilsTest
{
    
    public ParseUtilsTest()
    {
    }

    @Test
    public void getTemplateParam()
    {
        assertEquals("{{name2|param=blah}}", ParseUtils.getTemplateParam("{{name|parm1={{name2|param=blah\n}}}}", "parm1", false), "nested template");
        assertEquals("[[Main Page|blah]]", ParseUtils.getTemplateParam("{{name|parm1=[[Main Page|blah]]}}", "parm1", false), "wikilink");
        assertEquals("{{{1|Hello}}}", ParseUtils.getTemplateParam("{{name|parm1={{{1|Hello}}}}}", "parm1", false), "{{{1}}}");
    }
    
}
