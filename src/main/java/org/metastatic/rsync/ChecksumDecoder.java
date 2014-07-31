/* ChecksumDecoder -- decodes checksums from external representations.

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
import java.io.InputStream;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.util.List;

/**
 * The base class of objects that decode (internalize) checksum pairs
 * from byte streams.
 *
 * @version $Revision$
 */
public abstract class ChecksumDecoder
{

    // Constants and fields.
    // -------------------------------------------------------------------------

    /**
     * Property prefix for checksum encoders.
     */
    public static final String PROPERTY = "jarsync.checksumDecoder.";

    /**
     * The configuration object.
     */
    protected Configuration config;

    /**
     * The input stream being read from.
     */
    protected InputStream in;

    // Constructor.
    // -------------------------------------------------------------------------

    public ChecksumDecoder(Configuration config, InputStream in)
    {
        this.config = (Configuration) config.clone();
        this.in = in;
    }

    // Class method.
    // -------------------------------------------------------------------------

    /**
     * Gets an instance of a checksum decoder for the specified
     * encoding.
     *
     * @param encoding The encoding name.
     * @param config   The configuration object.
     * @param in       The input stream.
     * @throws NullPointerException     If any parameter is null.
     * @throws IllegalArgumentException If the specified encoding cannot
     *                                  be found, or if any of the arguments are inappropriate.
     */
    public static ChecksumEncoder getInstance(String encoding,
                                              Configuration config,
                                              InputStream in)
    {
        if (encoding == null || config == null || in == null)
            throw new NullPointerException();
        if (encoding.length() == 0)
            throw new IllegalArgumentException();
        try
        {
            Class clazz = Class.forName(System.getProperty(PROPERTY + encoding));
            if (!ChecksumEncoder.class.isAssignableFrom(clazz))
                throw new IllegalArgumentException(clazz.getName() +
                        ": not a subclass of " +
                        ChecksumEncoder.class.getName());
            Constructor c = clazz.getConstructor(new Class[]{Configuration.class,
                    InputStream.class});
            return (ChecksumEncoder) c.newInstance(new Object[]{config, in});
        } catch (ClassNotFoundException cnfe)
        {
            throw new IllegalArgumentException("class not found: " +
                    cnfe.getMessage());
        } catch (NoSuchMethodException nsme)
        {
            throw new IllegalArgumentException("subclass has no constructor");
        } catch (InvocationTargetException ite)
        {
            throw new IllegalArgumentException(ite.getMessage());
        } catch (InstantiationException ie)
        {
            throw new IllegalArgumentException(ie.getMessage());
        } catch (IllegalAccessException iae)
        {
            throw new IllegalArgumentException(iae.getMessage());
        }
    }

    // Instance methods.
    // -------------------------------------------------------------------------

    /**
     * Decodes checksums from the stream, storing them into the
     * specified list, until the end of checksums is encountered.
     *
     * @param sums The list to store the sums into.
     * @return The number of checksums read.
     * @throws IOException          If an I/O error occurs.
     * @throws NullPointerException If any element of the list is null.
     */
    public int read(List<ChecksumPair> sums) throws IOException
    {
        if (sums == null)
            throw new NullPointerException();
        int count = 0;
        ChecksumPair pair;
        while ((pair = read()) != null)
        {
            sums.add(pair);
            ++count;
        }
        return count;
    }

    // Abstract methods.
    // -------------------------------------------------------------------------

    /**
     * Decodes a checksum pair from the input stream.
     *
     * @return The pair read, or null if the end of stream is encountered.
     * @throws IOException If an I/O error occurs.
     */
    public abstract ChecksumPair read() throws IOException;
}
