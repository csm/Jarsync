/* Checksum32 -- A simple, "rolling" checksum based on Adler32.

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
 * A simple 32-bit "rolling" checksum. This checksum algorithm is based
 * upon the algorithm outlined in the paper "The rsync algorithm" by
 * Andrew Tridgell and Paul Mackerras. The algorithm works in such a way
 * that if one knows the sum of a block
 * <em>X<sub>k</sub>...X<sub>l</sub></em>, then it is a simple matter to
 * compute the sum for <em>X<sub>k+1</sub>...X<sub>l+1</sub></em>.
 *
 * @author Casey Marshall
 * @version $Revision$
 */
public class Checksum32 implements RollingChecksum, Cloneable, java.io.Serializable
{

    // Constants and variables.
    // -----------------------------------------------------------------

    protected final int char_offset;

    /**
     * The first half of the checksum.
     *
     * @since 1.1
     */
    protected int a;

    /**
     * The second half of the checksum.
     *
     * @since 1.1
     */
    protected int b;

    /**
     * The place from whence the current checksum has been computed.
     *
     * @since 1.1
     */
    protected int k;

    /**
     * The place to where the current checksum has been computed.
     *
     * @since 1.1
     */
    protected int l;

    /**
     * The block from which the checksum is computed.
     *
     * @since 1.1
     */
    protected byte[] block;

    /**
     * The index in {@link #new_block} where the newest byte has
     * been stored.
     *
     * @since 1.1
     */
    protected int new_index;

    /**
     * The block that is recieving new input.
     *
     * @since 1.1
     */
    protected byte[] new_block;

    // Constructors.
    // -----------------------------------------------------------------

    /**
     * Creates a new rolling checksum. The <i>char_offset</i> argument
     * affects the output of this checksum; rsync uses a char offset of
     * 0, librsync 31.
     */
    public Checksum32(int char_offset)
    {
        this.char_offset = char_offset;
        a = b = 0;
        k = 0;
    }

    public Checksum32()
    {
        this(0);
    }

    private Checksum32(Checksum32 that)
    {
        this.char_offset = that.char_offset;
        this.a = that.a;
        this.b = that.b;
        this.l = that.l;
        this.k = that.k;
        this.block = (that.block != null) ? (byte[]) ((byte[]) that.block).clone() : null;
        this.new_index = that.new_index;
        this.new_block = (that.new_block != null)
                ? (byte[]) ((byte[]) that.new_block).clone() : null;
    }

    // Public instance methods.
    // -----------------------------------------------------------------

    /**
     * Return the value of the currently computed checksum.
     *
     * @return The currently computed checksum.
     * @since 1.1
     */
    public int getValue()
    {
        return (a & 0xffff) | (b << 16);
    }

    /**
     * Reset the checksum.
     *
     * @since 1.1
     */
    public void reset()
    {
        k = 0;
        a = b = 0;
        l = 0;
    }

    /**
     * "Roll" the checksum. This method takes a single byte as byte
     * <em>X<sub>l+1</sub></em>, and recomputes the checksum for
     * <em>X<sub>k+1</sub>...X<sub>l+1</sub></em>. This is the
     * preferred method for updating the checksum.
     *
     * @param bt The next byte.
     * @since 1.1
     */
    public void roll(byte bt)
    {
        a -= block[k] + char_offset;
        b -= l * (block[k] + char_offset);
        a += bt + char_offset;
        b += a;
        block[k] = bt;
        k++;
        if (k == l) k = 0;
    }

    /**
     * Update the checksum by trimming off a byte only, not adding
     * anything.
     */
    public void trim()
    {
        a -= block[k % block.length] + char_offset;
        b -= l * (block[k % block.length] + char_offset);
        k++;
        l--;
    }

    /**
     * Update the checksum with an entirely different block, and
     * potentially a different block length.
     *
     * @param buf The byte array that holds the new block.
     * @param off From whence to begin reading.
     * @param len The length of the block to read.
     * @since 1.1
     */
    public void check(byte[] buf, int off, int len)
    {
        block = new byte[len];
        System.arraycopy(buf, off, block, 0, len);
        reset();
        l = block.length;
        int i;

        for (i = 0; i < block.length - 4; i += 4)
        {
            b += 4 * (a + block[i]) + 3 * block[i + 1] +
                    2 * block[i + 2] + block[i + 3] + 10 * char_offset;
            a += block[i] + block[i + 1] + block[i + 2]
                    + block[i + 3] + 4 * char_offset;
        }
        for (; i < block.length; i++)
        {
            a += block[i] + char_offset;
            b += a;
        }
    }

    public Object clone()
    {
        try
        {
            return super.clone();
        } catch (CloneNotSupportedException cnse)
        {
            throw new Error();
        }
    }

    public boolean equals(Object o)
    {
        return ((Checksum32) o).a == a && ((Checksum32) o).b == b;
    }
}
