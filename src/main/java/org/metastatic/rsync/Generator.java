/* Generator: Checksum generation methods.

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;

/**
 * A checksum generator. This class can take a byte array or an input stream,
 * and will generate a list of {@link org.metastatic.rsync.ChecksumPair} objects
 * for that data.
 */
public class Generator
{

    // Constants and variables.
    // ------------------------------------------------------------------------

    /**
     * Our configuration. Contains such things as our rolling checksum
     * and message digest.
     */
    protected final Configuration config;

    // Constructors.
    // ------------------------------------------------------------------------

    public Generator(Configuration config)
    {
        this.config = config;
    }

    // Instance methods.
    // ------------------------------------------------------------------------

    /**
     * Generate checksums over an entire byte array, with a base offset
     * of 0.
     *
     * @param buf The byte buffer to checksum.
     * @return A {@link java.util.List} of {@link ChecksumPair}s
     * generated from the array.
     * @see #generateSums(byte[], int, int, long)
     */
    public List<ChecksumPair> generateSums(byte[] buf)
    {
        return generateSums(buf, 0, buf.length, 0);
    }

    /**
     * Generate checksums over a portion of a byte array, with a base
     * offset of 0.
     *
     * @param buf The byte array to checksum.
     * @param off The offset in <code>buf</code> to begin.
     * @param len The number of bytes to checksum.
     * @return A {@link java.util.List} of {@link ChecksumPair}s
     * generated from the array.
     * @see #generateSums(byte[], int, int, long)
     */
    public List<ChecksumPair> generateSums(byte[] buf, int off, int len)
    {
        return generateSums(buf, off, len, 0);
    }

    /**
     * Generate checksums over an entire byte array, with a specified
     * base offset. This <code>baseOffset</code> is added to the offset
     * stored in each {@link ChecksumPair}.
     *
     * @param buf        The byte array to checksum.
     * @param baseOffset The offset from whence this byte array came.
     * @return A {@link java.util.List} of {@link ChecksumPair}s
     * generated from the array.
     * @see #generateSums(byte[], int, int, long)
     */
    public List<ChecksumPair> generateSums(byte[] buf, long baseOffset)
    {
        return generateSums(buf, 0, buf.length, baseOffset);
    }

    /**
     * Generate checksums over a portion of abyte array, with a specified
     * base offset. This <code>baseOffset</code> is added to the offset
     * stored in each {@link ChecksumPair}.
     *
     * @param buf        The byte array to checksum.
     * @param off        From whence in <code>buf</code> to start.
     * @param len        The number of bytes to check in
     *                   <code>buf</code>.
     * @param baseOffset The offset from whence this byte array came.
     * @return A {@link java.util.List} of {@link ChecksumPair}s
     * generated from the array.
     */
    public List<ChecksumPair> generateSums(byte[] buf, int off, int len, long baseOffset)
    {
        int count = (len + (config.blockLength - 1)) / config.blockLength;
        int remainder = len % config.blockLength;
        int offset = off;
        List<ChecksumPair> sums = new ArrayList<ChecksumPair>(count);

        for (int i = 0; i < count; i++)
        {
            int n = Math.min(len, config.blockLength);
            ChecksumPair pair = generateSum(buf, offset, n, offset + baseOffset);
            pair.seq = i;

            sums.add(pair);
            len -= n;
            offset += n;
        }

        return sums;
    }

    /**
     * Generate checksums for an entire file.
     *
     * @param f The {@link java.io.File} to checksum.
     * @return A {@link java.util.List} of {@link ChecksumPair}s
     * generated from the file.
     * @throws java.io.IOException if <code>f</code> cannot be read from.
     */
    public List<ChecksumPair> generateSums(File f) throws IOException
    {
        long len = f.length();
        int count = (int) ((len + (config.blockLength + 1)) / config.blockLength);
        long offset = 0;
        FileInputStream fin = new FileInputStream(f);
        List<ChecksumPair> sums = new ArrayList<ChecksumPair>(count);
        int n = (int) Math.min(len, config.blockLength);
        byte[] buf = new byte[n];

        for (int i = 0; i < count; i++)
        {
            int l = fin.read(buf, 0, n);
            if (l == -1) break;
            ChecksumPair pair = generateSum(buf, 0, Math.min(l, n), offset);
            pair.seq = i;

            sums.add(pair);
            len -= n;
            offset += n;
            n = (int) Math.min(len, config.blockLength);
        }

        fin.close();
        return sums;
    }

    /**
     * Generate checksums for an InputStream.
     *
     * @param in The {@link java.io.InputStream} to checksum.
     * @return A {@link java.util.List} of {@link ChecksumPair}s
     * generated from the bytes read.
     * @throws java.io.IOException if reading fails.
     */
    public List<ChecksumPair> generateSums(InputStream in) throws IOException
    {
        List<ChecksumPair> sums = null;
        byte[] buf = new byte[config.blockLength * config.blockLength];
        long offset = 0;
        int len = 0;

        while ((len = in.read(buf)) != -1)
        {
            if (sums == null)
            {
                sums = generateSums(buf, 0, len, offset);
            } else
            {
                sums.addAll(generateSums(buf, 0, len, offset));
            }
            offset += len;
        }

        return sums;
    }

    /**
     * Generate a sum pair for an entire byte array.
     *
     * @param buf        The byte array to checksum.
     * @param fileOffset The offset in the original file from whence
     *                   this block came.
     * @return A {@link ChecksumPair} for this byte array.
     */
    public ChecksumPair generateSum(byte[] buf, long fileOffset)
    {
        return generateSum(buf, 0, buf.length, fileOffset);
    }

    /**
     * Generate a sum pair for a portion of a byte array.
     *
     * @param buf        The byte array to checksum.
     * @param off        Where in <code>buf</code> to start.
     * @param len        How many bytes to checksum.
     * @param fileOffset The original offset of this byte array.
     * @return A {@link ChecksumPair} for this byte array.
     */
    public ChecksumPair
    generateSum(byte[] buf, int off, int len, long fileOffset)
    {
        ChecksumPair p = new ChecksumPair();
        config.weakSum.check(buf, off, len);
        if (config.checksumSeed != null && config.isSeedPrefix)
            config.strongSum.update(config.checksumSeed);
        config.strongSum.update(buf, off, len);
        if (config.checksumSeed != null && !config.isSeedPrefix)
            config.strongSum.update(config.checksumSeed);
        p.weak = config.weakSum.getValue();
        p.strong = new byte[config.strongSumLength];
        System.arraycopy(config.strongSum.digest(), 0, p.strong, 0, p.strong.length);
        p.offset = fileOffset;
        p.length = len;
        return p;
    }
}
