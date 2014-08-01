/* 
   Copyright (C) 2014  Casey Marshall

This file is a part of Jessie.

Jessie is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

Jessie is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jessie; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
USA

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version.  */

package org.metastatic.rsync;

import com.google.common.base.Preconditions;

public class ChecksumLocation
{
    private final ChecksumPair checksumPair;

    private final long offset;
    private final int length;
    private final int seq;

    public ChecksumLocation(ChecksumPair checksumPair, long offset, int length, int seq)
    {
        this.checksumPair = Preconditions.checkNotNull(checksumPair);
        this.offset = offset;
        this.length = length;
        this.seq = seq;
    }

    public ChecksumLocation(ChecksumPair checksumPair, long offset, int length)
    {
        this(checksumPair, offset, length, 0);
    }

    public ChecksumLocation(ChecksumPair checksumPair, long offset)
    {
        this(checksumPair, offset, 0, 0);
    }

    public ChecksumPair getChecksumPair()
    {
        return checksumPair;
    }

    public long getOffset()
    {
        return offset;
    }

    public int getLength()
    {
        return length;
    }

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
