package org.wikiutils;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

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
        assertEquals("getTemplateParam: nested template", "{{name2|param=blah}}", ParseUtils.getTemplateParam("{{name|parm1={{name2|param=blah\n}}}}", "parm1", false));
        assertEquals("getTemplateParam: wikilink", "[[Main Page|blah]]", ParseUtils.getTemplateParam("{{name|parm1=[[Main Page|blah]]}}", "parm1", false));
        assertEquals("getTemplateParam: {{{1}}}", "{{{1|Hello}}}", ParseUtils.getTemplateParam("{{name|parm1={{{1|Hello}}}}}", "parm1", false));
    }
    
}
