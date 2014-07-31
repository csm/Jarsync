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

package org.metastatic.rsync.test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.metastatic.rsync.*;

public class TestGenerators
{
    /**
     * Let's see if generating a checksum list works no matter the size of the
     * update we give {@link org.metastatic.rsync.GeneratorStream}.
     */
    @Test
    public void testGenerators() throws NoSuchAlgorithmException, ListenerException
    {
        Configuration.Builder builder = Configuration.Builder.create();
        Configuration config = builder.strongSum(MessageDigest.getInstance("MD5")).build();

        byte[] corpus = new byte[4096];
        for (int i = 0; i < corpus.length; i++)
            corpus[i] = (byte) i;

        final List<ChecksumPair> fromOnes = new ArrayList<ChecksumPair>();
        final List<ChecksumPair> fromFives = new ArrayList<ChecksumPair>();

        GeneratorStream byOnes = new GeneratorStream(config);
        byOnes.addListener(new GeneratorListener()
        {
            @Override
            public void update(GeneratorEvent event) throws ListenerException
            {
                fromOnes.add(event.getChecksumPair());
            }
        });
        GeneratorStream byFives = new GeneratorStream(config);
        byFives.addListener(new GeneratorListener()
        {
            @Override
            public void update(GeneratorEvent event) throws ListenerException
            {
                fromFives.add(event.getChecksumPair());
            }
        });

        for (int i = 0; i < corpus.length; i++)
            byOnes.update(corpus[i]);
        byOnes.doFinal();
        for (int i = 0; i < corpus.length; i += 5)
            byFives.update(corpus, i, Math.min(5, corpus.length - i));
        byFives.doFinal();

        List<ChecksumPair> atOnce = new Generator(config).generateSums(corpus);

        Assert.assertEquals(fromOnes, fromFives);
        Assert.assertEquals(fromFives, atOnce);
    }
}
