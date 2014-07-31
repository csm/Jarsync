/* RebuilderEvent -- file rebuilding event.

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

/**
 * a rebuilder event. Rebuilder events are emitted by a {@link
 * RebuilderStream} each time a new {@link Delta} is applied. The stream
 * will send this event to each of its {@link RebuilderListener}s.
 *
 * @see RebuilderStream
 * @see RebuilderListener
 */
public class RebuilderEvent extends java.util.EventObject
{

    // Fields.
    // -------------------------------------------------------------------------

    /**
     * The destination offset.
     */
    protected transient long offset;

    // Constructors.
    // -------------------------------------------------------------------------

    /**
     * Create a new rebuilder event.
     *
     * @param data   The source of this event, the data block.
     * @param offset The destination offset.
     */
    public RebuilderEvent(byte[] data, long offset)
    {
        this(data, 0, data.length, offset);
    }

    public RebuilderEvent(byte[] data, int off, int len, long offset)
    {
        super(new byte[len]);
        System.arraycopy(data, off, source, 0, len);
        this.offset = offset;
    }

    // Instance methods.
    // -------------------------------------------------------------------------

    /**
     * Get the data. This method is equivalent to {@link
     * java.util.EventObject#getSource()} but the source is already cast
     * for convenience.
     *
     * @return The data array.
     */
    public byte[] getData()
    {
        return (byte[]) source;
    }

    /**
     * Get the offset at which the data should be written.
     *
     * @return The offset.
     */
    public long getOffset()
    {
        return offset;
    }
}
