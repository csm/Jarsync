/* RebuilderStream: streaming file reconstructor.

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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;

/**
 * A "streaming" alternative to {@link Rebuilder}. To use this class,
 * create an intsance with a file argument representing the file being
 * rebuilt. Then register one or more implementations of the {@link
 * RebuilderListener} interface, which will write the data to the new
 * file. Then call the {@link #update(Delta)} method for each {@link
 * Delta} to be applied.
 * <p/>
 * <p>Note that unlike the {@link GeneratorStream} and {@link
 * MatcherStream} classes this class does not need a {@link
 * Configuration}, nor does it have any "doFinal" method -- it is
 * completely stateless (except for the file) and the operations are
 * finished when the last delta has been applied.
 * <p/>
 * <p>This class is optimal for situations where the deltas are coming
 * in a stream over a communications link, and when it would be
 * inefficient to wait until all deltas are received.
 */
public class RebuilderStream
{

    // Fields.
    // -----------------------------------------------------------------------

    /**
     * The basis file.
     */
    protected RandomAccessFile basisFile;

    /**
     * The list of {@link RebuilderListener}s.
     */
    protected final LinkedList<RebuilderListener> listeners;

    // Constructors.
    // -----------------------------------------------------------------------

    /**
     * Create a new rebuilder.
     */
    public RebuilderStream()
    {
        listeners = new LinkedList<RebuilderListener>();
    }

    // Instance methods.
    // -----------------------------------------------------------------------

    /**
     * Add a RebuilderListener listener to this rebuilder.
     *
     * @param listener The listener to add.
     * @throws IllegalArgumentException If <i>listener</i> is null.
     */
    public void addListener(RebuilderListener listener)
    {
        if (listener == null)
            throw new IllegalArgumentException();
        listeners.add(listener);
    }

    /**
     * Remove a listener from this rebuilder.
     *
     * @param listener The listener to remove.
     */
    public void removeListener(RebuilderListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * Set the basis file.
     *
     * @param file The basis file.
     * @throws IOException If the file is not readable.
     */
    public void setBasisFile(File file) throws IOException
    {
        if (basisFile != null)
        {
            basisFile.close();
            basisFile = null;
        }
        if (file != null)
            basisFile = new RandomAccessFile(file, "r");
    }

    /**
     * Set the basis file.
     *
     * @param file The basis file name.
     * @throws IOException If the file name is not the name of a readable file.
     */
    public void setBasisFile(String file) throws IOException
    {
        if (basisFile != null)
        {
            basisFile.close();
            basisFile = null;
        }
        if (file != null)
            basisFile = new RandomAccessFile(file, "r");
    }

    /**
     *
     */
    public void doFinal() throws IOException
    {
        if (basisFile != null)
            basisFile.close();
    }

    /**
     * Update this rebuilder with a delta.
     *
     * @param delta The delta to apply.
     * @throws IOException If there is an error reading from the basis
     *                     file, or if no basis file has been specified.
     */
    public void update(Delta delta) throws IOException, ListenerException
    {
        ListenerException exception = null, current = null;
        RebuilderEvent e = null;
        if (delta instanceof DataBlock)
        {
            e = new RebuilderEvent(((DataBlock) delta).getData(),
                    delta.getWriteOffset());
        } else
        {
            if (basisFile == null)
                throw new IOException("offsets found but no basis file specified");
            int len = Math.min(delta.getBlockLength(),
                    (int) (basisFile.length() - ((Offsets) delta).getOldOffset()));
            if (len < 0)
                return;
            byte[] buf = new byte[len];
            basisFile.seek(((Offsets) delta).getOldOffset());
            len = basisFile.read(buf);
            e = new RebuilderEvent(buf, 0, len, delta.getWriteOffset());
        }
        for (RebuilderListener listener : listeners)
        {
            try
            {
                (listener).update(e);
            } catch (ListenerException le)
            {
                if (exception != null)
                {
                    current.setNext(le);
                    current = le;
                } else
                {
                    exception = le;
                    current = le;
                }
            }
        }
        if (exception != null)
            throw exception;
    }
}
