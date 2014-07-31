/* MatcherStream: streaming alternative to Matcher.

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

import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A streaming version of {@link Matcher}. The idea here is that the
 * hashtable search is the most expensive operation in the rsync
 * algorithm, and it is at times undesirable to wait for this to finish
 * while whoever on the other end of the wire is waiting for our data.
 * With this construction, we can send {@link Delta}s as soon as they
 * are generated.
 * <p/>
 * <p>To implement the outgoing stream of Deltas, this class makes use
 * of a callback interface, {@link MatcherListener}. Pass a concrete
 * implementation of this interface to the {@link
 * #addListener(MatcherListener)} method, and a {@link java.util.List}
 * of {@link ChecksumPair}s before calling any of the
 * <code>update</code> methods. Once the data have been passed to these
 * methods, call {@link #doFinal()} to finish the process.
 *
 * @version $Revision$
 */
public class MatcherStream
{

    // Constants and fields.
    // -------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(MatcherStream.class.getName());

    /**
     * The configuration.
     */
    protected final Configuration config;

    /**
     * The list of {@link MatcherListener}s.
     */
    protected final List<MatcherListener> listeners;

    /**
     * The current hashtable.
     */
    protected final TwoKeyMap<Long> hashtable;

    /**
     * The intermediate byte buffer.
     */
    protected final byte[] buffer;

    /**
     * The current index in {@link #buffer}.
     */
    protected int ndx;

    /**
     * The number of bytes summed thusfar.
     */
    protected long count;

    // Constructor.
    // -------------------------------------------------------------------------

    /**
     * Create a new MatcherStream.
     *
     * @param config The current configuration.
     */
    public MatcherStream(Configuration config)
    {
        this.config = config;
        this.listeners = new LinkedList<MatcherListener>();
        this.hashtable = new TwoKeyMap<Long>();
        buffer = new byte[config.chunkSize];
        reset();
    }

    // Instance methods.
    // -------------------------------------------------------------------------

    /**
     * Add a {@link MatcherListener} to the list of listeners.
     *
     * @param listener The listener to add.
     */
    public void addListener(MatcherListener listener)
    {
        listeners.add(listener);
    }

    /**
     * Remove a {@link MatcherListener} from the list of listeners.
     *
     * @param listener The listener to add.
     * @return True if a listener was really removed (i.e. that the
     * listener was in the list to begin with).
     */
    public boolean removeListener(MatcherListener listener)
    {
        return listeners.remove(listener);
    }

    /**
     * Set the list of checksums that will be searched by this matcher.
     * This method must be called at least once before calling update.
     *
     * @param sums The checksums.
     */
    public void setChecksums(List<ChecksumPair> sums)
    {
        hashtable.clear();
        if (sums != null)
        {
            for (ChecksumPair p : sums)
            {
                hashtable.put(p, p.getOffset());
            }
        }
    }

    /**
     * Reset this matcher, to be used for another data set.
     */
    public void reset()
    {
        ndx = 0;
        count = 0L;
        hashtable.clear();
    }

    /**
     * Update this matcher with a single byte.
     *
     * @param b The next byte
     */
    public void update(byte b) throws ListenerException
    {
        ListenerException exception = null, current = null;
        buffer[ndx++] = b;
        count++;
        if (ndx < config.blockLength)
        {
            // We have not seen a full block since the last match.
            return;
        } else if (ndx == config.blockLength)
        {
            config.weakSum.check(buffer, 0, config.blockLength);
        } else
        {
            config.weakSum.roll(b);
        }
        Long oldOffset = hashSearch(buffer, ndx - config.blockLength,
                config.blockLength);
        if (oldOffset != null)
        {
            if (ndx > config.blockLength)
            {
                DataBlock d = new DataBlock(count - ndx, buffer, 0,
                        ndx - config.blockLength);
                Offsets o = new Offsets(oldOffset,
                        count - config.blockLength, config.blockLength);
                for (Iterator it = listeners.listIterator(); it.hasNext(); )
                {
                    try
                    {
                        MatcherListener l = (MatcherListener) it.next();
                        l.update(new MatcherEvent(d));
                        l.update(new MatcherEvent(o));
                    } catch (ListenerException le)
                    {
                        if (exception != null)
                        {
                            current.setNext(le);
                            current = le;
                        } else
                        {
                            exception = le;
                            current = le;
                        }
                    }
                }
                if (exception != null)
                    throw exception;
            } else
            {
                Offsets o = new Offsets(oldOffset,
                        count - config.blockLength, config.blockLength);
                for (Iterator it = listeners.listIterator(); it.hasNext(); )
                {
                    try
                    {
                        MatcherListener l = (MatcherListener) it.next();
                        l.update(new MatcherEvent(o));
                    } catch (ListenerException le)
                    {
                        if (exception != null)
                        {
                            current.setNext(le);
                            current = le;
                        } else
                        {
                            exception = le;
                            current = le;
                        }
                    }
                }
                if (exception != null)
                    throw exception;
            }
            ndx = 0;
        } else if (ndx == buffer.length)
        {
            DataBlock d = new DataBlock(count - ndx, buffer, 0,
                    buffer.length - (config.blockLength - 1));
            for (MatcherListener listener : listeners)
            {
                try
                {
                    MatcherListener l = listener;
                    l.update(new MatcherEvent(d));
                } catch (ListenerException le)
                {
                    if (exception != null)
                    {
                        current.setNext(le);
                        current = le;
                    } else
                    {
                        exception = le;
                        current = le;
                    }
                }
            }
            if (exception != null)
                throw exception;
            System.arraycopy(buffer, buffer.length - (config.blockLength - 1),
                    buffer, 0, config.blockLength - 1);
            ndx = config.blockLength - 1;
        }
    }

    /**
     * Update this matcher with a portion of a byte array.
     *
     * @param buf The next bytes.
     * @param off The offset to begin at.
     * @param len The number of bytes to update.
     */
    public void update(byte[] buf, int off, int len) throws ListenerException
    {
        ListenerException exception = null, current = null;
        int n = Math.min(len, config.blockLength);
        Long oldOffset;
        int i = off;

        while (i < len + off)
        {
            byte bt = buffer[ndx++] = buf[i++];
            count++;
            if (ndx < config.blockLength)
            {
                continue;
            } else if (ndx == config.blockLength)
            {
                config.weakSum.check(buffer, 0, config.blockLength);
            } else
            {
                config.weakSum.roll(bt);
            }
            oldOffset = hashSearch(buffer, ndx - config.blockLength,
                    config.blockLength);
            if (oldOffset != null)
            {
                if (ndx > config.blockLength)
                {
                    DataBlock d = new DataBlock(count - ndx, buffer, 0,
                            ndx - config.blockLength);
                    Offsets o = new Offsets(oldOffset,
                            count - config.blockLength, config.blockLength);
                    for (Iterator it = listeners.listIterator(); it.hasNext(); )
                    {
                        try
                        {
                            MatcherListener l = (MatcherListener) it.next();
                            l.update(new MatcherEvent(d));
                            l.update(new MatcherEvent(o));
                        } catch (ListenerException le)
                        {
                            if (exception != null)
                            {
                                current.setNext(le);
                                current = le;
                            } else
                            {
                                exception = le;
                                current = le;
                            }
                        }
                    }
                    if (exception != null)
                        throw exception;
                } else
                {
                    Offsets o = new Offsets(oldOffset.longValue(),
                            count - config.blockLength, config.blockLength);
                    for (Iterator it = listeners.listIterator(); it.hasNext(); )
                    {
                        try
                        {
                            MatcherListener l = (MatcherListener) it.next();
                            l.update(new MatcherEvent(o));
                        } catch (ListenerException le)
                        {
                            if (exception != null)
                            {
                                current.setNext(le);
                                current = le;
                            } else
                            {
                                exception = le;
                                current = le;
                            }
                        }
                    }
                }
                if (exception != null)
                    throw exception;
                ndx = 0;
            } else if (ndx == buffer.length)
            {
                DataBlock d = new DataBlock(count - ndx, buffer, 0,
                        buffer.length - (config.blockLength - 1));
                for (MatcherListener listener : listeners)
                {
                    try
                    {
                        MatcherListener l = listener;
                        l.update(new MatcherEvent(d));
                    } catch (ListenerException le)
                    {
                        if (exception != null)
                        {
                            current.setNext(le);
                            current = le;
                        } else
                        {
                            exception = le;
                            current = le;
                        }
                    }
                }
                if (exception != null)
                    throw exception;
                System.arraycopy(buffer, buffer.length - (config.blockLength - 1),
                        buffer, 0, config.blockLength - 1);
                ndx = config.blockLength - 1;
            }
        }
    }

    /**
     * Update this matcher with a byte array.
     *
     * @param buf The next bytes.
     */
    public void update(byte[] buf) throws ListenerException
    {
        update(buf, 0, buf.length);
    }

    /**
     * Flush any buffered data and reset this instance.
     */
    public void doFinal() throws ListenerException
    {
        ListenerException exception = null, current = null;
        if (ndx > 0)
        {
            int off = Math.max(0, ndx - config.blockLength);
            int len = Math.min(ndx, config.blockLength);
            config.weakSum.check(buffer, off, len);
            Long oldOff = hashSearch(buffer, off, len);
            if (oldOff != null)
            {
                if (off > 0)
                {
                    DataBlock d = new DataBlock(count - ndx, buffer, 0, off);
                    for (Iterator it = listeners.listIterator(); it.hasNext(); )
                    {
                        try
                        {
                            MatcherListener l = (MatcherListener) it.next();
                            l.update(new MatcherEvent(d));
                        } catch (ListenerException le)
                        {
                            if (exception != null)
                            {
                                current.setNext(le);
                                current = le;
                            } else
                            {
                                exception = le;
                                current = le;
                            }
                        }
                    }
                    if (exception != null)
                        throw exception;
                }
                Offsets o = new Offsets(oldOff, count - len, len);
                for (Iterator it = listeners.listIterator(); it.hasNext(); )
                {
                    try
                    {
                        MatcherListener l = (MatcherListener) it.next();
                        l.update(new MatcherEvent(o));
                    } catch (ListenerException le)
                    {
                        if (exception != null)
                        {
                            current.setNext(le);
                            current = le;
                        } else
                        {
                            exception = le;
                            current = le;
                        }
                    }
                }
                if (exception != null)
                    throw exception;
            } else
            {
                DataBlock d = new DataBlock(count - ndx, buffer, 0, ndx);
                for (Iterator it = listeners.listIterator(); it.hasNext(); )
                {
                    try
                    {
                        MatcherListener l = (MatcherListener) it.next();
                        l.update(new MatcherEvent(d));
                    } catch (ListenerException le)
                    {
                        if (exception != null)
                        {
                            current.setNext(le);
                            current = le;
                        } else
                        {
                            exception = le;
                            current = le;
                        }
                    }
                }
                if (exception != null)
                    throw exception;
            }
        }
        reset();
    }

    // Own methods.
    // -------------------------------------------------------------------------

    /**
     * Search if a portion of the given byte array is in the map,
     * returning its original offset if it is.
     *
     * @param block The block of bytes to search for.
     * @param off   The offset in the block to begin.
     * @param len   The number of bytes to read from the block.
     * @return The original offset of the given block if it was found in
     * the map. null if it was not found.
     */
    protected Long hashSearch(byte[] block, int off, int len)
    {
        Integer weakSum = config.weakSum.getValue();
        if (hashtable.containsKey(weakSum.intValue()))
        {
            logger.log(Level.FINE, "hash hit on weak key: {0}", String.format("%08x", weakSum));
            config.strongSum.reset();
            if (config.checksumSeed != null && config.isSeedPrefix)
                config.strongSum.update(config.checksumSeed);
            config.strongSum.update(block, off, len);
            if (config.checksumSeed != null && !config.isSeedPrefix)
                config.strongSum.update(config.checksumSeed);
            byte[] digest = new byte[config.strongSumLength];
            System.arraycopy(config.strongSum.digest(), 0, digest, 0, digest.length);
            logger.log(Level.FINE, "looking up strong key: {0}", Util.toHexString(digest));
            Long ret = hashtable.get(new ChecksumPair(weakSum, digest));
            logger.log(Level.FINE, "looked up {0}", ret);
            return ret;
        }
        return null;
    }
}
