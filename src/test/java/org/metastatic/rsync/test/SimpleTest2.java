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

// Tags: JARSYNC

package org.metastatic.rsync.test;

import java.security.*;
import java.util.*;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.metastatic.rsync.*;

/**
 * Exercise the rsync algorithm 50 times, each time with randomly
 * generated data sets and differences, and with a random message digest
 * algorithm.
 *
 * @version $Revision $
 */
public class SimpleTest2
{

    // Fields.
    // -----------------------------------------------------------------------

    Random rand;

    static Logger logger = Logger.getLogger(SimpleTest2.class.getName());

    // Constructor.
    // -----------------------------------------------------------------------

    public SimpleTest2()
    {
        rand = new Random();
    }

    // Instance methods.
    // -----------------------------------------------------------------------

    @Test
    public void test()
    {
        String[] mds = getMessageDigests();
        logger.info("rsyncTest");
        Security.addProvider(new JarsyncProvider());
        MessageDigest strongSum;
        // Make sure we use our MD4 at least once!
        try
        {
            strongSum = MessageDigest.getInstance("MD4", "JARSYNC");
        } catch (Exception x)
        {
            throw new Error(x);
        }

        for (int i = 0; i < 50; i++)
        {
            Configuration.Builder builder = Configuration.Builder.create();
            Configuration conf = builder.strongSum(strongSum).blockLength(rand.nextInt(1400) + 250).weakSum(new Checksum32()).build();
            byte[] n3w = new byte[rand.nextInt(1000000) + 500];
            rand.nextBytes(n3w);
            byte[] old = null;
            try
            {
                old = mutate(n3w);
            } catch (Exception x)
            {
                throw new Error(x);
            }
            logger.info("TEST #" + (i + 1) + ": old data=" + old.length
                    + " bytes, target data=" + n3w.length + " bytes, blocks="
                    + conf.blockLength + " bytes, digest="
                    + conf.strongSum.getAlgorithm());
            Generator gen = new Generator(conf);
            List sums = gen.generateSums(old);
            logger.info("\tGenerated " + sums.size() + " checksums.");
            Matcher mat = new Matcher(conf);
            List deltas = mat.hashSearch(sums, n3w);
            int copies = 0, inserts = 0;
            for (Iterator it = deltas.iterator(); it.hasNext(); )
            {
                if (it.next() instanceof DataBlock)
                    inserts++;
                else
                    copies++;
            }
            logger.info("\tDeltas: " + copies + " copy commands, "
                    + inserts + " insert commands.");
            byte[] reconst = Rebuilder.rebuild(old, deltas);
            Assert.assertArrayEquals(n3w, reconst);
            try
            {
                strongSum = MessageDigest.getInstance(
                        mds[rand.nextInt(mds.length)]);
            } catch (Exception x)
            {
                throw new Error(x);
            }
        }
    }

    // Own methods.
    // -----------------------------------------------------------------------

    /**
     * Derived from `mutate.pl' from librsync, (C) 1999, 2000 by Martin
     * Pool and (C) 1999 by Andrew Tridgell.
     */
    private byte[] mutate(byte[] b)
            throws Exception
    {
        StringBuffer corpus = new StringBuffer(new String(b, "ISO-8859-1"));
        int nmuts = 1 + rand.nextInt(30);
        logger.fine("\t" + nmuts + " mutations");
        while (--nmuts > 0)
        {
            int from_off = rand.nextInt(corpus.length());
            int from_len = (int) (rand.nextDouble()
                    * rand.nextInt(corpus.length() - from_off));
            int to_off = rand.nextInt(corpus.length());
            int to_len = (int) (rand.nextDouble()
                    * rand.nextInt(corpus.length() - to_off));
            switch (rand.nextInt(3))
            {
                case 0:
                    logger.fine("\tcopy and overwrite (" + from_off + ", "
                            + from_len + ") -> (" + to_off + ", " + to_len + ")");
                    corpus.replace(to_off, to_off + to_len,
                            corpus.substring(from_off, from_off + from_len));
                    break;
                case 1:
                    logger.fine("\tcopy and insert (" + from_off + ", "
                            + from_len + ") -> (" + to_off + ", " + to_len + ")");
                    corpus.insert(to_off,
                            corpus.substring(from_off, from_off + from_len));
                    break;
                case 2:
                    logger.fine("\tdelete (" + from_off + ", " + from_len + ")");
                    corpus.delete(from_off, from_off + from_len);
                    break;
            }
        }
        return corpus.toString().getBytes("ISO-8859-1");
    }

    private String[] getMessageDigests()
    {
        HashSet algs = new HashSet();
        String[] tries = {
                "md2", "md4", "md5", "sha-1", "ripemd128", "ripemd160",
                "tiger", "whirlpool", "brokenmd4"
        };
        for (int i = 0; i < tries.length; i++)
        {
            try
            {
                MessageDigest.getInstance(tries[i]);
                algs.add(tries[i]);
            } catch (Exception x)
            {
            }
        }
        return (String[]) algs.toArray(new String[algs.size()]);
    }
}
