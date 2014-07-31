/* DeltaDecoder -- decodes Delta objects from external representations.

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
THE SOFTWARE.  */


package org.metastatic.rsync;

import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.util.List;

/**
 * The superclass of all classes that decode delta objects from an
 * external, binary format.
 * <p/>
 * <p>Subclasses MAY define themselves to be accessable through the
 * {@link #getInstance(java.lang.String, org.metastatic.rsync.Configuration, java.io.InputStream)} method
 * by providing a one-argument constructor that accepts an {@link
 * InputStream} and defining the system property
 * "jarsync.deltaDecoder.<i>encoding-name</i>".
 */
public abstract class DeltaDecoder
{

    // Constants and fields.
    // -------------------------------------------------------------------------

    public static final String PROPERTY = "jarsync.deltaDecoder.";

    /**
     * The configuration.
     */
    protected final Configuration config;

    /**
     * The underlying input stream.
     */
    protected final InputStream in;

    // Constructors.
    // -------------------------------------------------------------------------

    public DeltaDecoder(Configuration config, InputStream in)
    {
        this.config = (Configuration) config.clone();
        this.in = in;
    }

    // Class methods.
    // -------------------------------------------------------------------------

    /**
     * Returns a new instance of the specified decoder.
     *
     * @param encoding The name of the decoder to get.
     * @param config   The configuration to use.
     * @param in       The source of binary data.
     * @return The new decoder.
     * @throws NullPointerException     If any parameter is null.
     * @throws IllegalArgumentException If there is no appropriate
     *                                  decoder available.
     */
    public static final DeltaDecoder getInstance(String encoding,
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
            if (!DeltaDecoder.class.isAssignableFrom(clazz))
                throw new IllegalArgumentException(clazz.getName() +
                        ": not a subclass of " +
                        DeltaDecoder.class.getName());
            Constructor c = clazz.getConstructor(new Class[]{Configuration.class,
                    InputStream.class});
            return (DeltaDecoder) c.newInstance(new Object[]{in});
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
     * Read (decode) a list of deltas from the input stream.
     *
     * @param deltas The list of deltas to write.
     * @throws IOException              If an I/O error occurs.
     * @throws IllegalArgumentException If any element of the list is not
     *                                  a {@link Delta}.
     * @throws NullPointerException     If any element is null.
     */
    public int read(List<Delta> deltas) throws IOException
    {
        int count = 0;
        Delta d = null;
        while ((d = read()) != null)
        {
            deltas.add(d);
            ++count;
        }
        return count;
    }

    // Abstract methods.
    // -------------------------------------------------------------------------

    /**
     * Read (decode) a single delta from the input stream.
     * <p/>
     * <p>If this encoding provides an end-of-deltas marker, then this method
     * is required to return <code>null</code> upon receiving this marker.
     *
     * @return The delta read, or <code>null</code>
     * @throws IOException If an I/O error occurs.
     */
    public abstract Delta read() throws IOException;
}
