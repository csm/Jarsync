/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id $

   TestOfChecksum32: test of the rolling checksum.
   Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

   This file is a part of Jarsync

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
   or based on Jarsync.  If you modify Jarsync you may extend this
   exception to your version of it, but you are not obligated to do so.
   If you do not wish to do so, delete this exception statement from
   your version.  */

// Tags: JARSYNC

package org.metastatic.rsync.test;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.metastatic.rsync.*;

/**
 * Conformance tests for the 32-bit rolling checksum.
 *
 * @version $Revision $
 */
public class TestOfChecksum32
{

    // Instance methods.
    // -----------------------------------------------------------------------

    @Test
    public void test()
    {
        try
        {
            Checksum32 c = new Checksum32(0);
            c.check("a".getBytes(), 0, 1);
            Assert.assertEquals("TestA", 0x610061, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestA");
        }

        try
        {
            Checksum32 c = new Checksum32(0);
            c.check("abc".getBytes(), 0, 3);
            Assert.assertEquals("TestABC", 0x24a0126, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestABC");
        }

        try
        {
            Checksum32 c = new Checksum32(0);
            c.check("abcdefghijklmnopqrstuvwxyz".getBytes(), 0, 26);
            Assert.assertEquals("TestAlphabet", 0x906c0b1f, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestAlphabet");
        }

        try
        {
            Checksum32 c = new Checksum32(0);
            c.check("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".getBytes(), 0, 62);
            Assert.assertEquals("TestASCIISubset", 0xdf2c150b, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestASCIISubset");
        }

        try
        {
            Checksum32 c = new Checksum32(0);
            c.check("Adler 32".getBytes(), 0, 8);
            Assert.assertEquals("TestAdler32", 0xc05026d, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestAdler32");
        }

        try
        {
            Checksum32 c = new Checksum32(0);
            c.check("01234567890123456789012345678901234567890123456789012345678901234567890123456789".getBytes(), 0, 80);
            Assert.assertEquals("TestEightyNumerics", 0x95e01068, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestEightyNumerics");
        }

        try
        {
            Checksum32 c = new Checksum32(0);
            c.check(new byte[]{
                    (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF
            }, 0, 4);
            Assert.assertEquals("TestDEADBEEF", 0xfdeaff38, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestDEADBEEF");
        }

        try
        {
            Checksum32 c = new Checksum32(0);
            c.check(new byte[]{
                    (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE
            }, 0, 4);
            Assert.assertEquals("TestCAFEBABE", 0xfe54ff40, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestCAFEBABE");
        }

        try
        {
            Checksum32 c = new Checksum32(0);
            byte[] buf = new byte[256];
            for (int i = 0; i < 256; i++)
                buf[i] = (byte) i;
            c.check(buf, 0, 256);
            Assert.assertEquals("TestAllByteValues", 0x6a80ff80, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestAllByteValues");
        }

        // CHAR_OFFSET = 31 ---------------------------------------------------

        try
        {
            Checksum32 c = new Checksum32(31);
            c.check("a".getBytes(), 0, 1);
            Assert.assertEquals("TestA", 0x800080, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestA");
        }

        try
        {
            Checksum32 c = new Checksum32(31);
            c.check("abc".getBytes(), 0, 3);
            Assert.assertEquals("TestABC", 0x3040183, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestABC");
        }

        try
        {
            Checksum32 c = new Checksum32(31);
            c.check("abcdefghijklmnopqrstuvwxyz".getBytes(), 0, 26);
            Assert.assertEquals("TestAlphabet", 0xbaed0e45, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestAlphabet");
        }

        try
        {
            Checksum32 c = new Checksum32(31);
            c.check("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".getBytes(), 0, 62);
            Assert.assertEquals("TestASCIISubset", 0xcbab1c8d, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestASCIISubset");
        }

        try
        {
            Checksum32 c = new Checksum32(31);
            c.check("Adler 32".getBytes(), 0, 8);
            Assert.assertEquals("TestAdler32", 0x10610365, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestAdler32");
        }

        try
        {
            Checksum32 c = new Checksum32(31);
            c.check("01234567890123456789012345678901234567890123456789012345678901234567890123456789".getBytes(), 0, 80);
            Assert.assertEquals("TestEightyNumerics", 0x1e381a18, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestEightyNumerics");
        }

        try
        {
            Checksum32 c = new Checksum32(31);
            c.check(new byte[]{
                    (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF
            }, 0, 4);
            Assert.assertEquals("TestDEADBEEF", 0xff20ffb4, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestDEADBEEF");
        }

        try
        {
            Checksum32 c = new Checksum32(31);
            c.check(new byte[]{
                    (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE
            }, 0, 4);
            Assert.assertEquals("TestCAFEBABE", 0xff8affbc, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestCAFEBABE");
        }

        try
        {
            Checksum32 c = new Checksum32(31);
            byte[] buf = new byte[256];
            for (int i = 0; i < 256; i++)
                buf[i] = (byte) i;
            c.check(buf, 0, 256);
            Assert.assertEquals("TestAllByteValues", 0xfa001e80, c.getValue());
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestAllByteValues");
        }
    }
    
    
    @Test
    public void testRoll()
    {
        try
        {
            Random r = new Random();
            for (int i = 0; i < 10; i++)
            {
                Checksum32 c1 = new Checksum32(31);
                Checksum32 c2 = new Checksum32(31);
                byte[] buf1 = new byte[201 + r.nextInt(1200)];
                r.nextBytes(buf1);
                c1.check(buf1, 0, buf1.length - 1);
                c2.check(buf1, 0, buf1.length - 1);
                c1.check(buf1, 1, buf1.length - 1);
                c2.roll(buf1[buf1.length - 1]);
                Assert.assertEquals(c1.getValue(), c2.getValue());
            }
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestRoll");
        }
    }

    @Test
    public void testRoll2()
    {
        try
        {
            Random r = new Random();
            for (int i = 0; i < 10; i++)
            {
                Checksum32 c1 = new Checksum32(0);
                Checksum32 c2 = new Checksum32(0);
                byte[] buf1 = new byte[200 + r.nextInt(1200)];
                byte[] buf2 = new byte[buf1.length];
                r.nextBytes(buf1);
                r.nextBytes(buf2);
                c1.check(buf1, 0, buf1.length);
                c2.check(buf1, 0, buf1.length);
                c1.check(buf2, 0, buf2.length);
                for (int j = 0; j < buf2.length; j++)
                {
                    c2.roll(buf2[j]);
                }
                Assert.assertEquals(c1.getValue(), c2.getValue());
            }
        } catch (Exception x)
        {
            x.printStackTrace();
            Assert.fail("TestRoll");
        }
    }
}
