/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$

   Configuration -- Wrapper around configuration data.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

   This file is a part of Jarsync.

   Jarsync is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the
   Free Software Foundation; either version 2 of the License, or (at
   your option) any later version.

   Jarsync is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Jarsync; if not, write to the

      Free Software Foundation, Inc.,
      59 Temple Place, Suite 330,
      Boston, MA  02111-1307
      USA

   Linking Jarsync statically or dynamically with other modules is
   making a combined work based on Jarsync.  Thus, the terms and
   conditions of the GNU General Public License cover the whole
   combination.

   As a special exception, the copyright holders of Jarsync give you
   permission to link Jarsync with independent modules to produce an
   executable, regardless of the license terms of these independent
   modules, and to copy and distribute the resulting executable under
   terms of your choice, provided that you also meet, for each linked
   independent module, the terms and conditions of the license of that
   module.  An independent module is a module which is not derived from
   or based on Jarsync.  If you modify Jarsync, you may extend this
   exception to your version of it, but you are not obligated to do so.
   If you do not wish to do so, delete this exception statement from
   your version.  */

package org.metastatic.rsync;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

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
