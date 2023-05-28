/**
 *  @(#)CommandLineParser.java 0.02 19/03/2023
 *  Copyright (C) 2018-2023 MER-C
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

import java.io.IOException;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;
import javax.swing.JFileChooser;
import org.wikipedia.Wiki;

/**
 *  Helper class that parses command line arguments.
 *  @author MER-C
 *  @version 0.02
 */
public class CommandLineParser
{
    /**
     *  Standard GPLv3 version string. 
     *  @see <a href="https://www.gnu.org/licenses/gpl.html">Text of the GNU
     *  General Public License, v3.</a>
     */
    public static final String GPL_VERSION_STRING = """
        Copyright (C) 2007 - %d MER-C and contributors.
        This is free software: you are free to change and redistribute it under the GNU
        GPL version 3 or later, see <https://www.gnu.org/licenses/gpl.html> for details.
        There is NO WARRANTY, to the extent permitted by law.
        """.formatted(OffsetDateTime.now().getYear());
        
    private final HashSet<String> params;
    private final HashSet<String> boolean_params;
    private final List<String> descriptions;
    private String synopsis, description, version;

    /**
     *  Initiates this command line parser.
     */
    public CommandLineParser()
    {
        params = new HashSet<>();
        boolean_params = new HashSet<>();
        descriptions = new ArrayList<>();
        synopsis = "";
        description = "";
        version = "";
    }
    
    /**
     *  Sets the one line synopsis of the program. The synopsis line is the very 
     *  first line output when <kbd>--help</kbd> is specified as an argument.
     *  @param classname the class name that command line arguments are parsed
     *  for
     *  @param argumentString a short string describing the command line 
     *  arguments
     *  @return this CommandLineParser
     */
    public CommandLineParser synopsis(String classname, String argumentString)
    {
        synopsis = "SYNOPSIS:\n\tjava " + classname + " " + argumentString;
        return this;
    }
    
    /**
     *  Sets the short description of the program's functionality to the given
     *  value. This is the second line output when <kbd>--help</kbd> is 
     *  specified as an argument.
     *  @param description a short description of this program
     *  @return this CommandLineParser
     */
    public CommandLineParser description(String description)
    {
        this.description = "DESCRIPTION:\n\t" + description;
        return this;
    }
    
    /**
     *  Adds a command line option to print help text and exit using the flag
     *  <kbd>--help</kbd>. The description of this flag on the help screen is
     *  <samp>Prints this screen and exits.</samp>.
     *  @return this CommandLineParser
     */
    public CommandLineParser addHelp()
    {
        descriptions.add("\t--help");
        descriptions.add("\t\tPrints this screen and exits.");
        return this;
    }
    
    /**
     *  Adds a command line option to print version text and exit using the flag
     *  <kbd>--version</kbd>. The description of this flag on the help screen is
     *  <samp>Outputs version information and exits.</samp>
     *  @param version the version text to print
     *  @return this CommandLineParser
     */
    public CommandLineParser addVersion(String version)
    {
        descriptions.add("\t--version");
        descriptions.add("\t\tOutputs version information and exits.");
        this.version = version;
        return this;
    }
    
    /**
     *  Adds a flag that allows the user to specify a single string argument.
     * 
     *  @param flag a flag, example <kbd>--flag <var>argument</var></kbd>
     *  @param argument a short (1-2 words) description of the argument
     *  @param description the description of the flag for the help screen
     *  @return this CommandLineParser
     */
    public CommandLineParser addSingleArgumentFlag(String flag, String argument, String description)
    {
        params.add(flag);
        descriptions.add("\t" + flag + " " + argument);
        descriptions.add("\t\t" + description);
        return this;
    }
    
    /**
     *  Adds a flag for a boolean option. If this option is specified, the
     *  corresponding boolean value is set to {@code true}.
     *  @param flag a flag, example <kbd>--flag</kbd>
     *  @param description the description of this boolean flag
     *  @return this CommandLineParser
     */
    public CommandLineParser addBooleanFlag(String flag, String description)
    {
        boolean_params.add(flag);
        descriptions.add("\t" + flag);
        descriptions.add("\t\t" + description);
        return this;
    }
    
    /**
     *  Adds a section to the help screen.
     *  @param text the section header
     *  @return this CommandLineParser
     */
    public CommandLineParser addSection(String text)
    {
        descriptions.add("");
        descriptions.add(text);
        return this;
    }
    
    /**
     *  Constructs and returns the string that is printed to standard output 
     *  when the user specifies <kbd>--help</kbd> on the command line.
     *  @return (see above)
     */
    public String buildHelpString()
    {
        StringBuilder sb = new StringBuilder();
        if (!synopsis.isEmpty())
        {
            sb.append(synopsis);
            sb.append("\n\n");
        }
        if (!description.isEmpty())
        {
            sb.append(description);
            sb.append("\n\n");
        }
        descriptions.forEach(line ->
        {
            sb.append(line);
            sb.append('\n');
        });
        return sb.toString();
    }
    
    /**
     *  Returns the string that is printed to standard output when the user 
     *  specifies <kbd>--version</kbd> on the command line.
     *  @return (see above)
     */
    public String buildVersionString()
    {
        return version;
    }
    
    /**
     *  Parses command line arguments into a {@code LinkedHashMap}. If a boolean
     *  option is not specified, it is not present in the return map. If a 
     *  boolean option is specified, the corresponding value is "true". If the
     *  user specifies <kbd>--help</kbd> or <kbd>--version</kbd>, print the
     *  corresponding text and exit. Any non-matching arguments are concatenated
     *  (with spaces in between) with key "default" (if there are none, there is 
     *  no key). Keys and default arguments are in the order specified by the 
     *  user.
     * 
     *  @param args the command line arguments to parse
     *  @return a LinkedHashMap containing the parsed arguments
     */
    public Map<String, String> parse(String[] args)
    {
        StringJoiner defaultargs = new StringJoiner(" ");
        Map<String, String> ret = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals("--help"))
            {
                System.out.println(buildHelpString());
                System.exit(0);
            }
            else if (args[i].equals("--version"))
            {
                System.out.println(buildVersionString());
                System.exit(0);
            }
            else if (params.contains(args[i]))
                ret.put(args[i], args[++i]);
            else if (boolean_params.contains(args[i]))
                ret.put(args[i], "true");
            else
                defaultargs.add(args[i]);
        }
        if (defaultargs.length() != 0)
            ret.put("default", defaultargs.toString());
        return ret;
    }
    
    /**
     *  Fetches a list of users specified on the command line with the options
     *  <kbd>--user</kbd> (singular) or <kbd>--category</kbd> (an entire category).
     *  @param parsedargs parsed arguments from {@link #parse}
     *  @param wiki the wiki to fetch category members from
     *  @return a list of users
     *  @throws IOException if a network error occurs
     */
    public static List<String> parseUserOptions(Map<String, String> parsedargs, Wiki wiki) throws IOException
    {
        List<String> users = new ArrayList<>();
        String category = parsedargs.get("--category");
        String user = parsedargs.get("--user");
        
        if (category != null)
            for (String member : wiki.getCategoryMembers(category, true, Wiki.USER_NAMESPACE))
                users.add(wiki.removeNamespace(member));
        if (user != null)
            users.add(user);
        return users;
    }
    
    /**
     *  Parses and validates an interval between two dates specified on the 
     *  command line. Exits if the start of the interval is after the end of 
     *  the interval.
     *  @param parsedargs parsed arguments from {@link #parse}
     *  @param startflag the flag for the start date
     *  @param endflag the flag for the end date
     *  @return a list: position 0 is the start date, position 1 is the end date
     *  (either or both may be null)
     *  @since 0.02
     */
    public static List<OffsetDateTime> parseDateRange(Map<String, String> parsedargs, String startflag, String endflag)
    {
        String startstring = parsedargs.get(startflag);
        String endstring = parsedargs.get(endflag);
        OffsetDateTime startdate = (startstring == null) ? null : OffsetDateTime.parse(startstring);
        OffsetDateTime enddate = (endstring == null) ? null : OffsetDateTime.parse(endstring);
        if (enddate != null && startdate != null && enddate.isBefore(startdate))
        {
            System.err.println("End date " + endstring + " specified in " + endflag + " is before the start date " +
                startstring + " specified in " + startflag + ".");
            System.exit(2);
        }
        List<OffsetDateTime> ret = new ArrayList<>();
        ret.add(startdate);
        ret.add(enddate);
        return ret;
    }
    
    /**
     *  Parses a single argument into a path. If that argument is missing, show
     *  a single select filechooser. If the filechooser is cancelled, exit.
     *  @param parsedargs parsed command line arguments
     *  @param fileoption the argument to look for
     *  @param prompt the title of the filechooser
     *  @param error the error to print if nothing was selected in the 
     *  filechooser
     *  @param save controls the type of filechooser, true = save, false = open
     *  @return the path selected by the user
     *  @throws IOException if a I/O error occurs
     *  @since 0.02
     */
    public static Path parseFileOption(Map<String, String> parsedargs, String fileoption, String prompt, 
        String error, boolean save) throws IOException
    {
        String fpath = parsedargs.get(fileoption);
        if (fpath == null)
        {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle(prompt);
            int fcresult = save ? fc.showSaveDialog(null) : fc.showOpenDialog(null);
            if (fcresult == JFileChooser.APPROVE_OPTION)
                return fc.getSelectedFile().toPath();
            System.err.println(error);
            System.exit(3);
        }
        return Paths.get(fpath);
    }
}
