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

import java.io.*;

import java.util.LinkedList;
import java.util.List;

/**
 * <p>Methods for performing the checksum search. The result of a search
 * is a {@link java.util.List} of {@link Delta} objects that, when
 * applied to a method in {@link Rebuilder}, will reconstruct the new
 * version of the data.</p>
 *
 * @version $Revision$
 */
public final class Matcher {

   // Constants and variables.
   // -----------------------------------------------------------------

   /** The list of deltas being built. */
   protected final List<Delta> deltas;

   /** The underlying matcher stream. */
   protected final MatcherStream matcher;

   /** The size of allocated byte arrays. */
   protected final int chunkSize;

   // Constructors.
   // -----------------------------------------------------------------

   /**
    * Create a matcher with the specified configuration.
    *
    * @param config The {@link Configuration} for this Matcher.
    */
   public Matcher(Configuration config) {
      deltas = new LinkedList<Delta>();
      matcher = new MatcherStream(config);
      matcher.addListener(new Callback(deltas));
      chunkSize = config.chunkSize;
   }

 // Instance methods.
   // -----------------------------------------------------------------

   /**
    * Search the given byte buffer.
    *
    * @param sums The checksums to search for.
    * @param buf  The data buffer to search.
    * @return A collection of {@link Delta}s derived from this search.
    */
   public List<Delta> hashSearch(List<ChecksumLocation> sums, byte[] buf) {
      return hashSearch(sums, buf, 0, buf.length);
   }

   /**
    * Search a portion of a byte buffer.
    *
    * @param sums The checksums to search for.
    * @param buf  The data buffer to search.
    * @param off  The offset in <code>buf</code> to begin.
    * @param len  The number of bytes to search from <code>buf</code>.
    * @return A collection of {@link Delta}s derived from this search.
    */
   public List<Delta> hashSearch(List<ChecksumLocation> sums, byte[] buf, int off, int len)
   {
      deltas.clear();
      matcher.reset();
      matcher.setChecksums(sums);
      try {
         matcher.update(buf, off, len);
         matcher.doFinal();
      } catch (ListenerException shouldNotHappen) {
      }
      return new LinkedList<Delta>(deltas);
   }

   /**
    * Search a file by name.
    *
    * @param sums The checksums to search for.
    * @param filename The name of the file to search.
    * @return A list of deltas derived from this search.
    * @throws IOException If <i>filename</i> cannot be read.
    */
   public List<Delta> hashSearch(List<ChecksumLocation> sums, String filename) throws IOException {
      return hashSearch(sums, new FileInputStream(filename));
   }

   /**
    * Search a file.
    *
    * @param sums The checksums to search for.
    * @param f    The file to search.
    * @return A list of {@link Delta}s derived from this search.
    * @throws IOException If <i>f</i> cannot be read.
    */
   public List<Delta> hashSearch(List<ChecksumLocation> sums, File f) throws IOException {
      return hashSearch(sums, new FileInputStream(f));
   }

   /**
    * Search an input stream.
    *
    * @param sums  The checksums to search.
    * @param in The input stream to search.
    * @return A collection of {@link Delta}s derived from this search.
    * @throws IOException If an exception occurs while reading.
    */
   public List<Delta> hashSearch(List<ChecksumLocation> sums, InputStream in) throws IOException {
      deltas.clear();
      matcher.reset();
      matcher.setChecksums(sums);
      byte[] buffer = new byte[chunkSize];
      int len = 0;
      try {
         while ((len = in.read(buffer)) != -1)
            matcher.update(buffer, 0, len);
         matcher.doFinal();
      } catch (ListenerException shouldNeverHappen) {
      }
      return new LinkedList<Delta>(deltas);
   }

   // Inner classes.
   // -----------------------------------------------------------------------

   /**
    * Trivial implementation of a MatcherListener that simply adds
    * incoming deltas to a List.
    */
   private class Callback implements MatcherListener {

      // Fields.
      // --------------------------------------------------------------------

      private final List<Delta> deltas;

      // Constructors.
      // --------------------------------------------------------------------

      Callback(List<Delta> deltas) {
         this.deltas = deltas;
      }

      // Instance methods.
      // --------------------------------------------------------------------

      public void update(MatcherEvent event) {
         deltas.add(event.getDelta());
      }
   }
}
