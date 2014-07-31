/* DeltaEncoder -- encodes Delta objects to external representations.

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
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * The superclass of objects that encode sets of deltas to external
 * representations, such as the over-the-wire format of rsync or the
 * rdiff file format.
 * <p/>
 * <p>Subclasses MAY define themselves to be accessable through the
 * {@link #getInstance(java.lang.String, org.metastatic.rsync.Configuration, java.io.OutputStream)} method
 * by providing a one-argument constructor that accepts an {@link
 * OutputStream} and defining the system property
 * "jarsync.deltaEncoder.<i>encoding-name</i>".
 */
public abstract class DeltaEncoder
{

    // Constants and fields.
    // -------------------------------------------------------------------------

    public static final String PROPERTY = "jarsync.deltaEncoder.";

    /**
     * The configuration.
     */
    protected Configuration config;

    /**
     * The output stream.
     */
    protected OutputStream out;

    // Constructors.
    // -------------------------------------------------------------------------

    /**
     * Creates a new delta encoder.
     *
     * @param config The configuration.
     * @param out    The output stream to write the data to.
     */
    public DeltaEncoder(Configuration config, OutputStream out)
    {
        this.config = (Configuration) config.clone();
        this.out = out;
    }

    // Class methods.
    // -------------------------------------------------------------------------

    /**
     * Returns a new instance of the specified encoder.
     *
     * @throws IllegalArgumentException If there is no appropriate
     *                                  encoder available.
     */
    public static final DeltaEncoder getInstance(String encoding,
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
            if (!DeltaEncoder.class.isAssignableFrom(clazz))
                throw new IllegalArgumentException(clazz.getName() +
                        ": not a subclass of " +
                        DeltaEncoder.class.getName());
            Constructor c = clazz.getConstructor(new Class[]{Configuration.class,
                    OutputStream.class});
            return (DeltaEncoder) c.newInstance(new Object[]{config, out});
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
     * Write (encode) a list of deltas to the output stream. This method does
     * <b>not</b> call {@link #doFinal()}.
     * <p/>
     * <p>This method checks every element of the supplied list to ensure that
     * all are either non-null or implement the {@link Delta} interface, before
     * writing any data.
     *
     * @param deltas The list of deltas to write.
     * @throws IOException              If an I/O error occurs.
     * @throws IllegalArgumentException If any element of the list is not
     *                                  a {@link Delta}.
     * @throws NullPointerException     If any element is null.
     */
    public void write(List<Delta> deltas) throws IOException
    {
        for (Delta o : deltas)
        {
            if (o == null)
                throw new NullPointerException();
        }
        for (Delta delta : deltas) write(delta);
    }

    // Abstract methods.
    // -----------------------------------------------------------------------

    /**
     * Write (encode) a single delta to the output stream.
     *
     * @param d The delta to write.
     * @throws IOException If an I/O error occurs.
     */
    public abstract void write(Delta d) throws IOException;

    /**
     * Finish encoding the deltas (at least, this set of deltas) and write any
     * encoding-specific end-of-deltas entity.
     *
     * @throws IOException If an I/O error occurs.
     */
    public abstract void doFinal() throws IOException;

    /**
     * Returns whether or not this encoder requires the deltas it is
     * presented to be in <i>write offset</i> order, that is, the deltas
     * passed to the <code>write</code> methods <b>must</b> be presented
     * in increasing order of their {@link Delta#getWriteOffset()}
     * values.
     *
     * @return True if this encoder requires write order.
     */
    public abstract boolean requiresOrder();
}
