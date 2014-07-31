/* ChecksumEncoder -- encodes checksums to external representations.

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
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * The base class of objects that encode (externalize) checksum pairs
 * to byte streams.
 *
 * @version $Revision$
 */
public abstract class ChecksumEncoder
{

    // Constants and fields.
    // -------------------------------------------------------------------------

    /**
     * Property prefix for checksum encoders.
     */
    public static final String PROPERTY = "jarsync.checksumEncoder.";

    /**
     * The configuration object.
     */
    protected Configuration config;

    /**
     * The output stream being written to.
     */
    protected OutputStream out;

    // Constructor.
    // -------------------------------------------------------------------------

    public ChecksumEncoder(Configuration config, OutputStream out)
    {
        this.config = (Configuration) config.clone();
        this.out = out;
    }

    // Class method.
    // -------------------------------------------------------------------------

    /**
     * Gets an instance of a checksum encoder for the specified
     * encoding.
     *
     * @param encoding The encoding name.
     * @param config   The configuration object.
     * @param out      The output stream.
     * @throws NullPointerException     If any parameter is null.
     * @throws IllegalArgumentException If the specified encoding cannot
     *                                  be found, or if any of the arguments are inappropriate.
     */
    public static ChecksumEncoder getInstance(String encoding,
                                              Configuration config,
                                              OutputStream out)
    {
        if (encoding == null || config == null || out == null)
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
                    OutputStream.class});
            return (ChecksumEncoder) c.newInstance(new Object[]{config, out});
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
     * Encodes a list of checksums to the output stream.
     *
     * @param sums The sums to write.
     * @throws IOException              If an I/O error occurs.
     * @throws NullPointerException     If any element of the list is null.
     * @throws IllegalArgumentException If any element of the list is
     *                                  not a {@link ChecksumPair}.
     */
    public void write(List<ChecksumPair> sums) throws IOException
    {
        for (ChecksumPair o : sums)
        {
            if (o == null)
                throw new NullPointerException();
        }
        for (ChecksumPair sum : sums)
        {
            write(sum);
        }
    }

    // Abstract methods.
    // -------------------------------------------------------------------------

    /**
     * Encodes a checksum pair to the output stream.
     *
     * @param pair The pair to write.
     * @throws IOException If an I/O error occurs.
     */
    public abstract void write(ChecksumPair pair) throws IOException;

    /**
     * Finishes encoding by emitting any end-of-checksums markers.
     *
     * @throws IOException If an I/O error occurs.
     */
    public abstract void doFinal() throws IOException;

    /**
     * This method returns <code>true</code> if the checksums must be
     * presented in order of ascending offset.
     *
     * @return true if this encoder requires order.
     */
    public abstract boolean requiresOrder();
}
