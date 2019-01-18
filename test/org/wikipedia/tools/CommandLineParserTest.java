/**
 *  @(#)CommandLineParserUnitTest.java 0.01 19/02/2018
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
package org.wikipedia.tools;

import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit tests for {@link CommandLineParser}.
 *  @author MER-C
 */
public class CommandLineParserTest
{
    
    @Test
    public void synopsis()
    {
        CommandLineParser clp = new CommandLineParser().synopsis("TestProgram", "[test arguments]");
        assertEquals("SYNOPSIS:\n\tjava TestProgram [test arguments]\n\n", clp.buildHelpString());
    }
    
    @Test
    public void description()
    {
        CommandLineParser clp = new CommandLineParser().description("Test description");
        assertEquals("DESCRIPTION:\n\tTest description\n\n", clp.buildHelpString());
    }
    
    @Test
    public void addHelp()
    {
        CommandLineParser clp = new CommandLineParser().addHelp();
        assertEquals("\t--help\n\t\tPrints this screen and exits.\n", clp.buildHelpString());
    }
    
    @Test
    public void version()
    {
        String version = "Test Program v0.01: Copyright (C) MER-C 2018.\n";
        CommandLineParser clp = new CommandLineParser().addVersion(version);
        assertEquals("\t--version\n\t\tOutputs version information and exits.\n", clp.buildHelpString());
        assertEquals(version, clp.buildVersionString());
    }
    
    @Test
    public void addSingleArgumentFlag()
    {
        String description = "Test flag";
        CommandLineParser clp = new CommandLineParser()
            .addSingleArgumentFlag("--test", "[something]", description);
        assertEquals("\t--test [something]\n\t\t" + description + "\n", clp.buildHelpString());
    }
    
    @Test
    public void addBooleanFlag()
    {
        String description = "Test Boolean flag";
        CommandLineParser clp = new CommandLineParser().addBooleanFlag("--test", description);
        assertEquals("\t--test\n\t\t" + description + "\n", clp.buildHelpString());
    }
    
    @Test
    public void addSection()
    {
        String title = "Test section:";
        CommandLineParser clp = new CommandLineParser().addSection(title);
        assertEquals("\n" + title + "\n", clp.buildHelpString());
    }
    
    @Test
    public void buildHelpString()
    {
        // Integration test
        String actual = new CommandLineParser()
            .synopsis("TestProgram", "[test arguments]")
            .description("A description of the program")
            .addHelp()
            .addVersion("Test Program v0.01: Copyright (C) MER-C 2018.")
            .addSection("Options:")
            .addBooleanFlag("--boolean", "A boolean flag.")
            .addSingleArgumentFlag("--flag", "[string]", "Set some value to string.")
            .buildHelpString();
        String expected = "SYNOPSIS:\n" +
            "\tjava TestProgram [test arguments]\n\n" +
            "DESCRIPTION:\n" +
            "\tA description of the program\n\n" +
            "\t--help\n" +
            "\t\tPrints this screen and exits.\n" +
            "\t--version\n" +
            "\t\tOutputs version information and exits.\n\n" +
            "Options:\n" + 
            "\t--boolean\n" +
            "\t\tA boolean flag.\n" +
            "\t--flag [string]\n" +
            "\t\tSet some value to string.\n";
        System.out.println(actual);
        assertEquals(expected, actual);
    }
    
    @Test
    public void buildVersionString()
    {
        String version = "Test Program v0.01: Copyright (C) MER-C 2018.\n";
        String actual = new CommandLineParser().addVersion(version).buildVersionString();
        assertEquals(version, actual);
    }
    
    @Test
    public void parse()
    {
        CommandLineParser clp = new CommandLineParser()
            .addBooleanFlag("--true", "A true boolean variable.")
            .addBooleanFlag("--false", "A false boolean variable.")
            .addSingleArgumentFlag("--string", "SomeString", "A String variable.");
        
        // no arguments therefore empty
        String[] args = new String[0];
        assertTrue(clp.parse(args).isEmpty(), "no arguments");
        
        // parse some arguments
        args = new String[] { "--true", "--string", "SpecifiedString", "default1", "default2" };
        Map<String, String> map = clp.parse(args);
        Iterator<Map.Entry<String, String>> iter = map.entrySet().iterator();
        Map.Entry<String, String> entry = iter.next();
        assertEquals("--true", entry.getKey(), "boolean argument");
        assertEquals("true", entry.getValue(), "boolean argument");
        entry = iter.next();
        assertEquals("--string", entry.getKey(), "string argument");
        assertEquals("SpecifiedString", entry.getValue(), "string argument");
        // default argument
        entry = iter.next();
        assertEquals("default", entry.getKey(), "default argument");
        assertEquals("default1 default2", entry.getValue(), "default argument");
        
        // cannot test --help and --version because of VM exit
    }
}
