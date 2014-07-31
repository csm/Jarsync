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
THE SOFTWARE.  */

package org.metastatic.rsync;

/**
 * A Delta is, in the Rsync algorithm, one of two things: (1) a block
 * of bytes and an offset, or (2) a pair of offsets, one old and one
 * new.
 *
 * @version $Revision$
 * @see DataBlock
 * @see Offsets
 */
public interface Delta
{
    /**
     * The size of the block of data this class represents.
     *
     * @return The size of the block of data this class represents.
     * @since 1.1
     */
    int getBlockLength();

    /**
     * Get the offset at which this Delta should be written.
     *
     * @return The write offset.
     * @since 1.2
     */
    long getWriteOffset();
}
