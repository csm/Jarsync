/* ChecksumLocation.java -- a checksum pair + offset and length.

Copyright (C) 2014  Casey Marshall

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

import com.google.common.base.Preconditions;

/**
 * A {@link org.metastatic.rsync.ChecksumPair} and information about the location
 * that pair was generated over.
 */
public class ChecksumLocation
{
    private final ChecksumPair checksumPair;

    private final long offset;
    private final int length;
    private final int seq;

    /**
     * Create a checksum location.
     *
     * @param checksumPair The checksum pair.
     * @param offset The offset of the data the checksums were computed over.
     * @param length The length of the data.
     * @param seq A sequence number for this location.
     */
    public ChecksumLocation(ChecksumPair checksumPair, long offset, int length, int seq)
    {
        this.checksumPair = Preconditions.checkNotNull(checksumPair);
        this.offset = offset;
        this.length = length;
        this.seq = seq;
    }

    /**
     * Create a checksum location. The sequence will default to zero.
     *
     * @param checksumPair The checksum pair.
     * @param offset The offset of the data the checksums were computed over.
     * @param length The length of the data.
     */
    public ChecksumLocation(ChecksumPair checksumPair, long offset, int length)
    {
        this(checksumPair, offset, length, 0);
    }

    /**
     * Create a checksum location. The sequence and length will default to zero.
     *
     * @param checksumPair The checksum pair.
     * @param offset The offset of the data the checksums were computed over.
     */
    public ChecksumLocation(ChecksumPair checksumPair, long offset)
    {
        this(checksumPair, offset, 0, 0);
    }

    /**
     * Get the checksum pair.
     *
     * @return The checksum pair.
     */
    public ChecksumPair getChecksumPair()
    {
        return checksumPair;
    }

    /**
     * Get the data offset.
     *
     * @return The offset.
     */
    public long getOffset()
    {
        return offset;
    }

    /**
     * Get the data length.
     *
     * @return The length.
     */
    public int getLength()
    {
        return length;
    }

    /**
     * Get the sequence number.
     *
     * @return The sequence number.
     */
    public int getSeq()
    {
        return seq;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChecksumLocation that = (ChecksumLocation) o;

        if (length != that.length) return false;
        if (offset != that.offset) return false;
        if (seq != that.seq) return false;
        if (checksumPair != null ? !checksumPair.equals(that.checksumPair) : that.checksumPair != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = checksumPair != null ? checksumPair.hashCode() : 0;
        result = 31 * result + (int) (offset ^ (offset >>> 32));
        result = 31 * result + length;
        result = 31 * result + seq;
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("ChecksumLocation(");
        sb.append("checksumPair=").append(checksumPair);
        sb.append(", offset=").append(offset);
        sb.append(", length=").append(length);
        sb.append(", seq=").append(seq);
        sb.append(')');
        return sb.toString();
    }
}
