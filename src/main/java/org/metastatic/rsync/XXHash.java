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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigestSpi;

public class XXHash extends MessageDigestSpi
{
    private static final long PRIME64_1 = 0x9e3779b185ebca87L;
    private static final long PRIME64_2 = 0xc2b2ae3d27d4eb4fL;
    private static final long PRIME64_3 = 0x165667b19e3779f9L;
    private static final long PRIME64_4 = 0x85ebca77c2b2ae63L;
    private static final long PRIME64_5 = 0x27d4eb2f165667c5L;

    private long totalLength;
    private boolean seeded;
    private long seed;
    private long v1, v2, v3, v4;
    private int bufpos;
    private final byte[] buffer = new byte[32];

    @Override
    protected int engineGetDigestLength()
    {
        return 8;
    }

    private void seed()
    {
        seed = ((buffer[0] & 0xffL) << 56) | ((buffer[1] & 0xffL) << 48)
                | ((buffer[2] & 0xffL) << 40) | ((buffer[3] & 0xffL) << 32)
                | ((buffer[4] & 0xffL) << 24) | ((buffer[5] & 0xffL) << 16)
                | ((buffer[6] & 0xffL) << 8)  |  (buffer[7] & 0xffL);
        v1 = seed + PRIME64_1 + PRIME64_2;
        v2 = seed + PRIME64_2;
        v3 = seed;
        v4 = seed - PRIME64_1;
        bufpos = 0;
        seeded = true;
    }

    private static long swap(long input)
    {
        return ((input << 56) & 0xff00000000000000L)
             | ((input << 40) & 0x00ff000000000000L)
             | ((input << 24) & 0x0000ff0000000000L)
             | ((input <<  8) & 0x000000ff00000000L)
             | ((input >>  8) & 0x00000000ff000000L)
             | ((input >> 24) & 0x0000000000ff0000L)
             | ((input >> 40) & 0x000000000000ff00L)
             | ((input >> 56) & 0x00000000000000ffL);
    }

    private static long rotl(long input, int shift)
    {
        return (input >>> shift) | (input << (64 - shift));
    }

    @Override
    protected void engineUpdate(byte input)
    {
        if (!seeded)
        {
            buffer[bufpos++] = input;
            if (bufpos == 8)
                seed();
        }
        else
        {
            engineUpdate(new byte[] { input }, 0, 1);
        }
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len)
    {
        if (!seeded)
        {
            int count = Math.min(len, 8 - bufpos);
            System.arraycopy(input, offset, buffer, bufpos, count);
            offset += count;
            len -= count;
            bufpos += count;
            if (bufpos >= 8)
                seed();
        }

        int i = offset;
        if (bufpos > 0)
        {
            int count = Math.min(buffer.length - bufpos, len);
            System.arraycopy(input, offset, buffer, bufpos, count);
            i += count;
            bufpos += count;
            if (bufpos == buffer.length)
            {
                ByteBuffer buf = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
                v1 += buf.getLong(0) * PRIME64_2;
                v1 = rotl(v1, 31);
                v1 *= PRIME64_1;
                v2 += buf.getLong(8) * PRIME64_2;
                v2 += rotl(v2, 31);
                v2 *= PRIME64_1;
                v3 += buf.getLong(16) * PRIME64_2;
                v3 = rotl(v3, 31);
                v3 *= PRIME64_1;
                v4 += buf.getLong(24) * PRIME64_2;
                v4 = rotl(v4, 31);
                v4 *= PRIME64_1;
                bufpos = 0;
            }
        }
        if (i + 32 < offset + len)
        {
            long t1 = v1;
            long t2 = v2;
            long t3 = v3;
            long t4 = v4;

            for (; i + 32 < offset + len; i += 32)
            {
                ByteBuffer buf;
                try
                {
                    buf = ByteBuffer.wrap(input, i, 32).order(ByteOrder.LITTLE_ENDIAN);
                }
                catch (IndexOutOfBoundsException e)
                {
                    throw e;
                }
                t1 += buf.getLong(0) * PRIME64_2;
                t1 = rotl(t1, 31);
                t1 *= PRIME64_1;
                t2 += buf.getLong(8) * PRIME64_2;
                t2 += rotl(t2, 31);
                t2 *= PRIME64_1;
                t3 += buf.getLong(16) * PRIME64_2;
                t3 = rotl(t3, 31);
                t3 *= PRIME64_1;
                t4 += buf.getLong(24) * PRIME64_2;
                t4 = rotl(t4, 31);
                t4 *= PRIME64_1;
            }

            v1 = t1;
            v2 = t2;
            v3 = t3;
            v4 = t4;
        }

        if (i < offset + len)
        {
            int count = (offset + len) - i;
            System.arraycopy(input, i, buffer, 0, count);
            bufpos = count;
        }

        totalLength += len;
    }

    @Override
    protected byte[] engineDigest()
    {
        if (!seeded)
            seed();
        long h;
        if (totalLength >= 32)
        {
            long t1 = v1;
            long t2 = v2;
            long t3 = v3;
            long t4 = v4;
            h = rotl(t1, 1) + rotl(t2, 7) + rotl(t3, 12) + rotl(t4, 18);

            t1 *= PRIME64_2;
            t1 = rotl(t1, 31);
            t1 *= PRIME64_1;
            h ^= t1;
            h = h * PRIME64_1 + PRIME64_4;

            t2 *= PRIME64_2;
            t2 = rotl(t2, 31);
            t2 *= PRIME64_1;
            h ^= t2;
            h = h * PRIME64_1 + PRIME64_4;

            t3 *= PRIME64_2;
            t3 = rotl(t3, 31);
            t3 *= PRIME64_1;
            h ^= t3;
            h = h * PRIME64_1 + PRIME64_4;

            t4 *= PRIME64_2;
            t4 = rotl(t4, 31);
            t4 *= PRIME64_1;
            h ^= t4;
            h = h * PRIME64_1 + PRIME64_4;
        }
        else
            h = seed + PRIME64_5;

        h += totalLength;

        int i = 0;
        while (i <= bufpos - 8)
        {
            long k = ByteBuffer.wrap(buffer, i, bufpos - i).order(ByteOrder.LITTLE_ENDIAN).getLong(0);
            k *= PRIME64_2;
            k = rotl(k, 31);
            k *= PRIME64_1;
            h ^= k;
            h = rotl(h, 27) * PRIME64_1 + PRIME64_4;
            i += 8;
        }
        while (i <= bufpos - 4)
        {
            long k = ByteBuffer.wrap(buffer, i, bufpos - i).order(ByteOrder.LITTLE_ENDIAN).getInt(0);
            h ^= k * PRIME64_1;
            h = rotl(h, 23) * PRIME64_2 + PRIME64_3;
            i += 4;
        }
        while (i < bufpos)
        {
            h ^= buffer[i] * PRIME64_5;
            h = rotl(h, 11) * PRIME64_1;
            i++;
        }

        h ^= h >> 33;
        h *= PRIME64_2;
        h ^= h >> 29;
        h *= PRIME64_3;
        h ^= h >> 32;

        engineReset();
        byte[] ret = new byte[8];
        ByteBuffer result = ByteBuffer.wrap(ret).order(ByteOrder.BIG_ENDIAN);
        result.putLong(h);
        return ret;
    }

    @Override
    protected void engineReset()
    {
        totalLength = 0;
        seeded = false;
        seed = 0;
        v1 = v2 = v3 = v4 = 0;
        bufpos = 0;
    }
}
