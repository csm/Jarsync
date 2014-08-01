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

import com.google.common.base.Preconditions;

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
    final int weak;

    /**
     * The strong checksum.
     *
     * @since 1.1
     */
    final byte[] strong;

    // Constructors.
    // -------------------------------------------------------------------------

    /**
     * Create a new checksum pair with no associated offset.
     *
     * @param weak   The weak checksum.
     * @param strong The strong checksum.
     */
    public ChecksumPair(int weak, byte[] strong)
    {
        this.weak = weak;
        this.strong = Preconditions.checkNotNull(strong).clone();
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
        if (obj instanceof ChecksumPair)
            return weak == ((ChecksumPair) obj).weak &&
                    Arrays.equals(strong, ((ChecksumPair) obj).strong);
        return false;
    }

    /**
     * Returns a String representation of this pair.
     *
     * @return The String representation of this pair.
     * @since 1.2
     */
    public String toString()
    {
        return String.format("ChecksumPair(weak=%08x, strong=%s)", weak, Util.toHexString(strong));
    }
}
