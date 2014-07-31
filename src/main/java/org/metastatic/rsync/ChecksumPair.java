/* ChecksumPair: A pair of weak, strong checksums.

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

import java.util.Arrays;

/**
 * A pair of weak and strong checksums for use with the Rsync algorithm.
 * The weak "rolling" checksum is typically a 32-bit sum derived from
 * the Adler32 algorithm; the strong checksum is usually a 128-bit MD4
 * checksum.
 *
 * @author Casey Marshall
 * @version $Revision$
 */
public class ChecksumPair implements java.io.Serializable
{

    // Constants and variables.
    // -------------------------------------------------------------------------

    /**
     * The weak, rolling checksum.
     *
     * @since 1.1
     */
    int weak;

    /**
     * The strong checksum.
     *
     * @since 1.1
     */
    byte[] strong;

    /**
     * The offset in the original data where this pair was
     * generated.
     */
    long offset;

    /**
     * The number of bytes these sums are over.
     */
    int length;

    /**
     * The sequence number of these sums.
     */
    int seq;

    // Constructors.
    // -------------------------------------------------------------------------

    /**
     * Create a new checksum pair.
     *
     * @param weak   The weak, rolling checksum.
     * @param strong The strong checksum.
     * @param offset The offset at which this checksum was computed.
     * @param length The length of the data over which this sum was
     *               computed.
     * @param seq    The sequence number of this checksum pair.
     */
    public ChecksumPair(int weak, byte[] strong, long offset,
                        int length, int seq)
    {
        this.weak = weak;
        this.strong = strong;
        this.offset = offset;
        this.length = length;
        this.seq = seq;
    }

    /**
     * Create a new checksum pair with no length or sequence fields.
     *
     * @param weak   The weak checksum.
     * @param strong The strong checksum.
     * @param offset The offset at which this checksum was computed.
     */
    public ChecksumPair(int weak, byte[] strong, long offset)
    {
        this(weak, strong, offset, 0, 0);
    }

    /**
     * Create a new checksum pair with no associated offset.
     *
     * @param weak   The weak checksum.
     * @param strong The strong checksum.
     */
    public ChecksumPair(int weak, byte[] strong)
    {
        this(weak, strong, -1L, 0, 0);
    }

    /**
     * Default 0-arguments constructor for package access.
     */
    ChecksumPair()
    {
    }

    // Instance methods.
    // -------------------------------------------------------------------------

    /**
     * Get the weak checksum.
     *
     * @return The weak checksum.
     * @since 1.1
     */
    public int getWeak()
    {
        return weak;
    }

    /**
     * Get the strong checksum.
     *
     * @return The strong checksum.
     * @since 1.1
     */
    public byte[] getStrong()
    {
        return strong;
    }

    /**
     * Return the offset from where this checksum pair was generated.
     *
     * @return The offset.
     */
    public long getOffset()
    {
        return offset;
    }

    /**
     * Return the length of the data for which this checksum pair was
     * generated.
     *
     * @return The length.
     */
    public int getLength()
    {
        return length;
    }

    /**
     * Return the sequence number of this checksum pair, if any.
     *
     * @return The sequence number.
     */
    public int getSequence()
    {
        return seq;
    }

    // Public instance methods overriding java.lang.Object.
    // -------------------------------------------------------------------------

    public int hashCode()
    {
        return weak;
    }

    /**
     * We define equality for this object as equality between two weak
     * sums and equality between two strong sums.
     *
     * @param obj The Object to test.
     * @return True if both checksum pairs are equal.
     */
    public boolean equals(Object obj)
    {
        return weak == ((ChecksumPair) obj).weak &&
                Arrays.equals(strong, ((ChecksumPair) obj).strong);
    }

    /**
     * Returns a String representation of this pair.
     *
     * @return The String representation of this pair.
     * @since 1.2
     */
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        String s;
        s = Integer.toHexString(getWeak());
        for (int i = 0; i < 8 - s.length(); i++)
        {
            buf.append('0');
        }
        String weak = buf.toString() + s;
        return "ChecksumPair(len=" + length + " offset=" + offset + " weak=" + weak
                + " strong=" + Util.toHexString(strong) + ")";
    }
}
