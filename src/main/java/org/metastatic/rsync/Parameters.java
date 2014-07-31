/*

Copyright (C) 2014 Casey Marshall

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE. */

package org.metastatic.rsync;

import java.io.IOException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.StringTokenizer;

/**
 * <p>Simple parser for Samba-style config files. The parameters are
 * passed back to the caller via a simple event listener callback
 * interface, {@link ParameterListener}.</p>
 * <p/>
 * <p>A sample file looks like:</p>
 * <p/>
 * <pre>
 * [section one]
 * parameter one = value string
 * parameter two = another value
 * [section two]
 * new parameter = some value or t'other
 * </pre>
 * <p/>
 * <p>The syntax is roughly:</p>
 * <p/>
 * <pre>
 * file      ::= parameter* section* EOF
 * section   ::= header parameter*
 * header    ::= '[' NAME ']'
 * parameter ::= NAME '=' VALUE EOL
 * </pre>
 * <p/>
 * <p>Blank lines, and lines that begin with either '#' or ';' are
 * ignored. long lines may be continued by preceding the end-of-line
 * character(s) with a backslash ('\').</p>
 *
 * @version $Revision$
 */
public final class Parameters
{

    // Constants and fields.
    // -----------------------------------------------------------------------

    /**
     * The callback object.
     */
    private final ParameterListener listener;

    /**
     * The current file being read.
     */
    private LineNumberReader in;

    // Constructor.
    // -----------------------------------------------------------------------

    /**
     * Create a new parameter file parser. The argument is a concrete
     * imlpmentation of {@link ParameterListener} which will take the
     * parsed arguments.
     *
     * @param listener The parameter listener.
     */
    public Parameters(ParameterListener listener)
    {
        this.listener = listener;
    }

    // Instance methods.
    // -----------------------------------------------------------------------

    /**
     * Begin parsing file <i>filename</i>.
     *
     * @param filename The name of the file to parse.
     * @throws IOException If an I/O error occurs.
     */
    public void begin(String filename) throws IOException
    {
        in = new LineNumberReader(new FileReader(filename));
    }

    /**
     * Parse, or continue parsing the file if a parsing error occured
     * in a previous call to this method. A call to {@link
     * #begin(java.lang.String)} must have succeeded before this method
     * is called.
     *
     * @throws ParameterException If a parsing error occurs. Parsing can
     *                            continue if this exception is thrown by calling this
     *                            method again.
     * @throws IOException        If an I/O error occurs.
     */
    public void parse() throws IOException
    {
        if (in == null)
        {
            throw new IOException("nothing to parse");
        }
        String line;

        while ((line = in.readLine()) != null)
        {
            if (isIgnorable(line)) continue;

            // Concatenate continuation lines.
            while (line.endsWith("\\"))
            {
                String line2 = in.readLine();
                if (line2 == null) break;
                if (isIgnorable(line2)) continue;
                line = line.substring(0, line.length() - 1);

                // Make sure we aren't fooled by '\' followed by a space.
                if (line2.endsWith("\\"))
                {
                    line += line2.trim();
                } else if (line2.trim().endsWith("\\"))
                {
                    line2 = line2.trim() + " ";
                    line += line2;
                } else
                {
                    line += line2.trim();
                }
            }

            line = line.trim();
            int i;
            if (line.startsWith("[") && line.endsWith("]"))
            {
                listener.beginSection(crush(line.substring(1, line.length() - 1)));
            } else if ((i = line.indexOf('=')) > 1)
            {
                listener.setParameter(crush(line.substring(0, i).trim()),
                        crush(line.substring(i + 1).trim()));
            } else
            {
                throw new ParameterException("malformed line at "
                        + in.getLineNumber());
            }
        }

        in.close();
        in = null;
    }

    // Own methods.
    // -----------------------------------------------------------------------

    /**
     * Test if a line is a comment or empty.
     *
     * @param s The string to test.
     * @return true if this string is a comment or empty.
     */
    private static boolean isIgnorable(String s)
    {
        s = s.trim();
        if (s.length() == 0) return true;
        if (s.charAt(0) == '#' || s.charAt(0) == ';') return true;
        return false;
    }

    /**
     * Crush multiple spaces (or other whitespace) into a single space.
     *
     * @param s The string to crush.
     * @return The crushed string.
     */
    private static String crush(String s)
    {
        StringTokenizer tok = new StringTokenizer(s);
        if (tok.countTokens() <= 1) return s;
        String result = tok.nextToken();
        while (tok.hasMoreTokens())
            result += " " + tok.nextToken();
        return result;
    }
}
