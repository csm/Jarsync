package org.metastatic.rsync.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.metastatic.rsync.*;

/**
 * Copyright (C) 2013 Memeo, Inc.
 * All Rights Reserved
 */
public class SyncTest
{
    @Test
    public void test() throws IOException, NoSuchAlgorithmException
    {
        Configuration.Builder builder = Configuration.Builder.create();
        Configuration conf = builder.strongSum(MessageDigest.getInstance("MD5")).build();
        runtest(conf);
    }

    @Test
    public void testWithXXHash() throws IOException, NoSuchAlgorithmException
    {
        byte[] seed = new byte[] { (byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe,
                (byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef };
        Configuration.Builder builder = Configuration.Builder.create();
        Configuration conf = builder.strongSum(MessageDigest.getInstance("XXHash64", new JarsyncProvider())).checksumSeed(seed).isSeedPrefix(true).build();
        runtest(conf);
    }

    private void runtest(Configuration conf) throws IOException
    {
        byte[] a = new byte[700];
        Arrays.fill(a, (byte) 'a');
        byte[] b = new byte[700];
        Arrays.fill(b, (byte) 'b');
        byte[] c = new byte[700];
        Arrays.fill(c, (byte) 'c');

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(a);
        out.write(b);
        out.write(c);
        byte[] text1 = out.toByteArray();

        out = new ByteArrayOutputStream();
        out.write(new byte[10]);
        out.write(b);
        out.write(a);
        out.write(a);
        out.write(new byte[123]);
        out.write(c);
        out.write(new byte[12]);
        out.write(a);
        out.write(c);
        out.write(new byte[1]);
        out.write(b);
        byte[] text2 = out.toByteArray();

        List<ChecksumPair> checksums = new Generator(conf).generateSums(text1);
        System.out.println("checksums: " + checksums);
        List<Delta> deltas = new Matcher(conf).hashSearch(checksums, text2);
        System.out.println("deltas: " + deltas);
        byte[] text3 = Rebuilder.rebuild(text1, deltas);
        Assert.assertArrayEquals(text2, text3);
    }
}
