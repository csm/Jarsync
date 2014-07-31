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

/**
 * This class represents an update to a file or array of bytes wherein
 * the bytes themselves have not changed, but have moved to another
 * location. This is represented by three fields: the offset in the
 * original data, the offset in the new data, and the length, in bytes,
 * of this block.
 *
 * @version $Revision$
 */
public class Offsets implements Delta, java.io.Serializable
{

    // Constants and variables
    // ------------------------------------------------------------------------

    private static final long serialVersionUID = 2787420454508237262L;

    /**
     * The original offset.
     *
     * @since 1.1
     */
    protected long oldOffset;

    /**
     * The new offset.
     *
     * @since 1.1
     */
    protected long newOffset;

    /**
     * The size of the moved block, in bytes.
     *
     * @since 1.1
     */
    protected int blockLength;

    // Constructors
    // -----------------------------------------------------------------

    /**
     * Create a new pair of offsets. The idea behind this object is
     * that this sort of {@link Delta} represents original data
     * that has simply moved in the new data.
     *
     * @param oldOffset   The offset in the original data.
     * @param newOffset   The offset in the new data.
     * @param blockLength The size, in bytes, of the block that has moved.
     * @since 1.1
     */
    public Offsets(long oldOffset, long newOffset, int blockLength)
    {
        this.oldOffset = oldOffset;
        this.newOffset = newOffset;
        this.blockLength = blockLength;
    }

    // Instance methods.
    // -----------------------------------------------------------------------

    // Delta interface implementation.

    public long getWriteOffset()
    {
        return newOffset;
    }

    public int getBlockLength()
    {
        return blockLength;
    }

    // Property accessor methods

    /**
     * Get the original offset.
     *
     * @return The original offset.
     */
    public long getOldOffset()
    {
        return oldOffset;
    }

    /**
     * Set the original offset.
     *
     * @param off The new value for the original offset.
     */
    public void setOldOffset(long off)
    {
        oldOffset = off;
    }

    /**
     * Get the updated offset.
     *
     * @return The updated offset.
     */
    public long getNewOffset()
    {
        return newOffset;
    }

    /**
     * Set the updated offset.
     *
     * @param off The new value for the updated offset.
     */
    public void setNewOffset(long off)
    {
        newOffset = off;
    }

    /**
     * Set the block size.
     *
     * @param len The new value for the block size.
     */
    public void setBlockLength(int len)
    {
        blockLength = len;
    }

    // Public instance methods overriding java.lang.Object -------------

    /**
     * Return a {@link java.lang.String} representation of this object.
     *
     * @return A string representing this object.
     */
    public String toString()
    {
        return "[ old=" + oldOffset + " new=" + newOffset
                + " len=" + blockLength + " ]";
    }

    /**
     * Test if one object is equal to this one.
     *
     * @return <tt>true</tt> If <tt>o</tt> is an Offsets instance and the
     * {@link #oldOffset}, {@link #newOffset}, and {@link
     * #blockLength} fields are all equal.
     * @throws java.lang.ClassCastException   If <tt>o</tt> is not an
     *                                        instance of this class.
     * @throws java.lang.NullPointerException If <tt>o</tt> is null.
     */
    public boolean equals(Object o)
    {
        return oldOffset == ((Offsets) o).oldOffset
                && newOffset == ((Offsets) o).newOffset
                && blockLength == ((Offsets) o).blockLength;
    }

    /**
     * Returns the hash code of this object, defined as:
     * <blockquote>
     * <tt>{@link #oldOffset} + {@link #newOffset} + {@link
     * #blockLength}
     * % 2^32</tt>
     * </blockquote>
     *
     * @return The hash code of this object.
     */
    public int hashCode()
    {
        return (int) (oldOffset + newOffset + blockLength);
    }
}
