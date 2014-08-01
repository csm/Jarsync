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

package org.metastatic.rsync.test;

import org.metastatic.rsync.JarsyncProvider;
import org.metastatic.rsync.Util;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HashBench
{
    public static void main(String... argv) throws Exception
    {
        JarsyncProvider provider = new JarsyncProvider();
        MessageDigest md4 = MessageDigest.getInstance("MD4", provider);
        MessageDigest murmur = MessageDigest.getInstance("Murmur3", provider);

        List<Double> md4times = new ArrayList<Double>(4096);
        List<Double> murmurTimes = new ArrayList<Double>(4096);

        Random r = new Random(31337);
        for (int i = 0; i < 4096; i++)
        {
            byte[] bytes = new byte[1000];
            r.nextBytes(bytes);

            long begin = System.nanoTime();
            md4.reset();
            for (int j = 0; j < 1000; j++)
                md4.update(bytes);
            md4.digest();
            long end = System.nanoTime();
            md4times.add(Util.toMillis(end - begin));

            begin = System.nanoTime();
            murmur.reset();
            for (int j = 0; j < 1000; j++)
                murmur.update(bytes);
            murmur.digest();
            end = System.nanoTime();
            murmurTimes.add(Util.toMillis(end - begin));
        }

        System.out.printf("MD4     max: % 12f, min: % 12f, mean: % 12f%n", max(md4times), min(md4times), mean(md4times));
        System.out.printf("Murmur3 max: % 12f, min: % 12f, mean: % 12f%n", max(murmurTimes), min(murmurTimes), mean(murmurTimes));
    }

    static double max(List<Double> l)
    {
        double max = -Double.MAX_VALUE;
        for (double d : l)
            if (d > max)
                max = d;
        return max;
    }

    static double min(List<Double> l)
    {
        double min = Double.MAX_VALUE;
        for (double d : l)
            if (d < min)
                min = d;
        return min;
    }

    static double mean(List<Double> l)
    {
        double sum = 0;
        for (double d : l)
            sum += d;
        return sum / l.size();
    }
}
