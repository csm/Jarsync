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
 * A general interface for 32-bit checksums that have the "rolling"
 * property.
 *
 * @author Casey Marshall
 * @version $Revision$
 */
public interface RollingChecksum extends Cloneable, java.io.Serializable
{

    // Methods.
    // -----------------------------------------------------------------------

    /**
     * Returns the currently-computed 32-bit checksum.
     *
     * @return The checksum.
     */
    int getValue();

    /**
     * Resets the internal state of the checksum, so it may be re-used
     * later.
     */
    void reset();

    /**
     * Update the checksum with a single byte. This is where the
     * "rolling" method is used.
     *
     * @param bt The next byte.
     */
    void roll(byte bt);

    /**
     * Update the checksum by simply "trimming" the
     * least-recently-updated byte from the internal state. Most, but not
     * all, checksums can support this.
     */
    void trim();

    /**
     * Replaces the current internal state with entirely new data.
     *
     * @param buf    The bytes to checksum.
     * @param offset The offset into <code>buf</code> to start reading.
     * @param length The number of bytes to update.
     */
    void check(byte[] buf, int offset, int length);

    /**
     * Copies this checksum instance into a new instance. This method
     * should be optional, and only implemented if the class implements
     * the {@link java.lang.Cloneable} interface.
     *
     * @return A clone of this instance.
     */
    Object clone();

    /**
     * Tests if a particular checksum is equal to this checksum. This
     * means that the other object is an instance of this class, and its
     * internal state equals this checksum's internal state.
     *
     * @param o The object to test.
     * @return <code>true</code> if this checksum equals the other
     * checksum.
     */
    boolean equals(Object o);
}
