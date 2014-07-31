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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * A Configuration is a mere collection of objects and values that
 * compose a particular configuration for the algorithm, for example the
 * message digest that computes the strong checksum.
 * <p/>
 * <p>Usage of a Configuration involves setting the member fields of
 * this object to thier appropriate values; thus, it is up to the
 * programmer to specify the {@link #strongSum}, {@link #weakSum},
 * {@link #blockLength} and {@link #strongSumLength} to be used. The
 * other fields are optional.</p>
 *
 * @author Casey Marshall
 * @version $Revision$
 */
public class Configuration implements Cloneable, java.io.Serializable
{

    // Constants and variables.
    // ------------------------------------------------------------------------

    /**
     * The default block size.
     */
    public static final int BLOCK_LENGTH = 700;

    /**
     * The default chunk size.
     */
    public static final int CHUNK_SIZE = 32768;

    /**
     * The message digest that computes the stronger checksum.
     */
    public transient final MessageDigest strongSum;

    /**
     * The rolling checksum.
     */
    public transient final RollingChecksum weakSum;

    /**
     * The length of blocks to checksum.
     */
    public final int blockLength;

    /**
     * The effective length of the strong sum.
     */
    public final int strongSumLength;

    /**
     * Whether or not to do run-length encoding when making Deltas.
     */
    public final boolean doRunLength;

    /**
     * The seed for the checksum, to perturb the strong checksum and help
     * avoid collisions in plain rsync (or in similar applicaitons).
     */
    public final byte[] checksumSeed;

    /**
     * The maximum size of byte arrays to create, when they are needed.
     * This vale defaults to 32 kilobytes.
     */
    public final int chunkSize;

    /**
     * A builder object for a configuration.
     *
     * <p>All settable values of this builder are optional except for the strong sum. You <em>must</em>
     * initialize the strong sum property with an appropriate {@link java.security.MessageDigest}
     * instance.</p>
     */
    public static class Builder
    {
        private MessageDigest strongSum;
        private RollingChecksum weakSum = new Checksum32();
        private int blockLength = BLOCK_LENGTH;
        private int chunkSize = CHUNK_SIZE;
        private Optional<Integer> strongSumLength = Optional.absent();
        private boolean doRunLength = false;
        private byte[] checksumSeed = null;

        private Builder()
        {
        }

        /**
         * Create a new builder object.
         *
         * @return The new configuration builder.
         */
        public static Builder create()
        {
            return new Builder();
        }

        /**
         * Set the strong sum.
         *
         * @param strongSum
         * @return This builder.
         */
        public Builder strongSum(MessageDigest strongSum)
        {
            Preconditions.checkNotNull(strongSum);
            this.strongSum = strongSum;
            return this;
        }

        /**
         * Set the weak sum. The default used, if not set, is {@link org.metastatic.rsync.Checksum32},
         * created with the default constructor.
         *
         * @param weakSum
         * @return This builder.
         */
        public Builder weakSum(RollingChecksum weakSum)
        {
            Preconditions.checkNotNull(weakSum);
            this.weakSum = weakSum;
            return this;
        }

        /**
         * Sets the block length. The default is {@link #BLOCK_LENGTH}.
         *
         * @param blockLength
         * @return This builder.
         */
        public Builder blockLength(int blockLength)
        {
            Preconditions.checkArgument(blockLength > 0);
            this.blockLength = blockLength;
            return this;
        }

        /**
         * Set the strong sum length. If not set, the digest size of the strong sum is used.
         *
         * @param strongSumLength
         * @return This builder.
         */
        public Builder strongSumLength(int strongSumLength)
        {
            Preconditions.checkArgument(strongSumLength > 0);
            if (strongSum != null)
                Preconditions.checkArgument(strongSumLength <= strongSum.getDigestLength());
            this.strongSumLength = Optional.of(strongSumLength);
            return this;
        }

        /**
         * Set the chunk length. The default is {@link #CHUNK_SIZE}.
         * @param chunkLength
         * @return This builder.
         */
        public Builder chunkLength(int chunkLength)
        {
            Preconditions.checkArgument(chunkLength > 0);
            this.chunkSize = chunkLength;
            return this;
        }

        /**
         * Set whether or not to do run-length encoding. The default is false.
         * @param doRunLength
         * @return This builder.
         */
        public Builder doRunLength(boolean doRunLength)
        {
            this.doRunLength = doRunLength;
            return this;
        }

        /**
         * Set the checksum seed. The default is null.
         * @param checksumSeed
         * @return This builder.
         */
        public Builder checksumSeed(byte[] checksumSeed)
        {
            if (checksumSeed != null)
                this.checksumSeed = checksumSeed.clone();
            else
                this.checksumSeed = null;
            return this;
        }

        /**
         * Build a configuration object.
         * @return The new configuration.
         */
        public Configuration build()
        {
            if (strongSum == null)
                throw new IllegalStateException("must be configured with a strong sum");
            return new Configuration(strongSum, weakSum, blockLength, strongSumLength.or(strongSum.getDigestLength()),
                                     doRunLength, checksumSeed, chunkSize);
        }
    }

    // Constructors.
    // ------------------------------------------------------------------------

    private Configuration(MessageDigest strongSum, RollingChecksum weakSum, int blockLength, int strongSumLength, boolean doRunLength, byte[] checksumSeed, int chunkSize)
    {
        this.strongSum = strongSum;
        this.weakSum = weakSum;
        this.blockLength = blockLength;
        this.strongSumLength = strongSumLength;
        this.doRunLength = doRunLength;
        this.checksumSeed = checksumSeed;
        this.chunkSize = chunkSize;
    }

    /**
     * Private copying constructor.
     */
    private Configuration(Configuration that)
    {
        MessageDigest strong;
        try
        {
            strong = (MessageDigest) (that.strongSum != null
                    ? that.strongSum.clone()
                    : null);
        } catch (CloneNotSupportedException cnse)
        {
            try
            {
                strong = MessageDigest.getInstance(
                        that.strongSum.getAlgorithm());
            } catch (NoSuchAlgorithmException nsae)
            {
                // Fucked up situation. We die now.
                throw new Error(nsae);
            }
        }
        this.strongSum = strong;
        this.weakSum = (RollingChecksum) (that.weakSum != null
                ? that.weakSum.clone()
                : null);
        this.blockLength = that.blockLength;
        this.doRunLength = that.doRunLength;
        this.strongSumLength = that.strongSumLength;
        this.checksumSeed = (byte[]) (that.checksumSeed != null
                ? that.checksumSeed.clone()
                : null);
        this.chunkSize = that.chunkSize;
    }

    // Instance methods.
    // -----------------------------------------------------------------------

    public Object clone()
    {
        return new Configuration(this);
    }

    // Serialization methods.
    // -----------------------------------------------------------------------

    /*private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
        out.writeUTF(strongSum != null ? strongSum.getAlgorithm() : "NONE");
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        String s = in.readUTF();
        if (!s.equals("NONE"))
        {
            try
            {
                strongSum = MessageDigest.getInstance(s);
            } catch (NoSuchAlgorithmException nsae)
            {
                throw new java.io.InvalidObjectException(nsae.getMessage());
            }
        }
    }*/
}
