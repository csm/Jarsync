/* vim:set softtabstop=3 shiftwidth=3 tabstop=3 expandtab tw=72:
   $Id$

   Rebuilder: File reconstruction from deltas.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import java.util.*;

/**
 * Methods for file reconstruction from deltas.
 *
 * @version $Revision$
 */
public class Rebuilder
{

    // Constants and variables.
    // -----------------------------------------------------------------------

    /**
     * The prefix for temporary files.
     */
    private static final String TMP_PREFIX = ".jarsync-";

    /**
     * The suffix for temporary files.
     */
    private static final String TMP_SUFFIX = ".temp";

    // Class methods.
    // -----------------------------------------------------------------------

    /**
     * Reconstruct data into a new byte array.
     *
     * @param buf    The original data.
     * @param deltas The deltas to apply.
     * @return The reconstructed data.
     */
    public static byte[] rebuild(byte[] buf, List deltas)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            rebuild(baos, buf, deltas);
        } catch (IOException shouldNotHappen)
        {
        }
        return baos.toByteArray();
    }

    /**
     * Reconstruct data into an output stream.
     *
     * @param out    The sink for reconstructed data.
     * @param buf    The original data.
     * @param deltas The deltas to apply.
     */
    public static void rebuild(OutputStream out, byte[] buf, List<Delta> deltas)
            throws IOException
    {
        Delta[] darray = deltas.toArray(new Delta[0]);
        Arrays.sort(darray, new OffsetComparator());

        for (Delta aDarray : darray)
        {
            if (aDarray instanceof DataBlock)
            {
                out.write(((DataBlock) aDarray).getData());
            } else
            {
                out.write(buf, (int) ((Offsets) aDarray).getOldOffset(),
                        aDarray.getBlockLength());
            }
        }
    }

    /**
     * Reconstruct a file into an output stream.
     *
     * @param out     The sink for reconstructed data.
     * @param oldFile The original file.
     * @param deltas  The deltas to apply.
     */
    public static void
    rebuild(OutputStream out, File oldFile, List<Delta> deltas)
            throws IOException
    {
        RandomAccessFile f = new RandomAccessFile(oldFile, "r");
        Delta[] darray = deltas.toArray(new Delta[0]);
        Arrays.sort(darray, new OffsetComparator());
        byte[] buf = new byte[1024];

        for (int i = 0; i < darray.length; i++)
        {
            if (darray[i] instanceof DataBlock)
            {
                out.write(((DataBlock) darray[i]).getData());
            } else
            {
                f.seek(((Offsets) darray[i]).getOldOffset());
                int len = 0, total = 0;
                do
                {
                    len = f.read(buf);
                    total += len;
                    out.write(buf, 0, len);
                } while (total < darray[i].getBlockLength());
            }
        }
    }

    /**
     * Reconstruct a file into a new file created with {@link
     * java.io.File#createTempFile(java.lang.String, java.lang.String, java.io.File)}.
     * This file can then be renamed to the destination.
     *
     * @param oldFile The original file.
     * @param deltas  A collection of {@link Delta}s to apply to the original
     *                file.
     * @return A unique {@link java.io.File} containing the reconstruction.
     */
    public static File rebuildFile(File oldFile, List deltas)
            throws IOException
    {
        File newFile = File.createTempFile(TMP_PREFIX, TMP_SUFFIX,
                oldFile.getParentFile());
        rebuildFile(oldFile, newFile, deltas);
        return newFile;
    }

    /**
     * Reconstruct a file into <code>newFile</code>.
     *
     * @param oldFile The original file.
     * @param newFile The file to write the reconstruction to. This must be a
     *                different file than <code>oldFile</code>
     * @param deltas  The {@link Delta}s to apply.
     */
    public static void rebuildFile(File oldFile, File newFile, List<Delta> deltas)
            throws IOException
    {
        if (oldFile.equals(newFile))
        {
            throw new IOException("cannot read and write to the same file");
        }
        RandomAccessFile out = new RandomAccessFile(newFile, "rw");
        RandomAccessFile in = null;
        try
        {
            in = new RandomAccessFile(oldFile, "r");
        } catch (IOException ignore)
        {
        }

        for (Delta o : deltas)
        {
            if (o instanceof DataBlock)
            {
                long off = ((DataBlock) o).getOffset();
                out.seek(off);
                out.write(((DataBlock) o).getData());
            } else if (o instanceof Offsets)
            {
                if (in == null)
                {
                    throw new IOException("original file does not exist or not readable");
                }
                int len = ((Offsets) o).getBlockLength();
                long off1 = ((Offsets) o).getOldOffset();
                long off2 = ((Offsets) o).getNewOffset();
                byte[] buf = new byte[len];
                in.seek(off1);
                in.read(buf);
                out.seek(off2);
                out.write(buf);
            }
        }

        if (in != null)
            in.close();
        out.close();
    }

    /**
     * Reconstruct a file in-place. The contents of <code>file</code> will be
     * overwritten with the contents of the reconstructed file.
     *
     * @param file   The file to reconstruct.
     * @param deltas The {@link Delta}s to apply.
     */
    public static void rebuildFileInPlace(File file, List<Delta> deltas)
            throws IOException
    {
        final boolean copyOnly = !file.exists();
        RandomAccessFile f = new RandomAccessFile(file, "rw");
        List<Offsets> offsets = new LinkedList<Offsets>();
        List<DataBlock> dataBlocks = new LinkedList<DataBlock>();
        TreeMap<Offsets, Set<Offsets>> digraph = new TreeMap<Offsets, Set<Offsets>>(new OffsetComparator());
        long newFileLength = 0;

        for (Delta o : deltas)
        {
            if (o instanceof Offsets)
            {
                if (copyOnly)
                    throw new IOException("original file does not exist.");
                offsets.add((Offsets) o);
                digraph.put((Offsets) o, new HashSet());
                newFileLength = Math.max(newFileLength,
                        ((Offsets) o).getNewOffset() + ((Offsets) o).getBlockLength());
            } else if (o instanceof DataBlock)
            {
                dataBlocks.add((DataBlock) o);
                newFileLength = Math.max(newFileLength,
                        ((DataBlock) o).getOffset() + ((DataBlock) o).getBlockLength());
            }
        }

        if (copyOnly)
        {
            for (Iterator i = dataBlocks.iterator(); i.hasNext(); )
            {
                DataBlock d = (DataBlock) i.next();
                f.seek(d.getWriteOffset());
                f.write(d.getData());
            }
            if (f.length() < newFileLength)
            {
                f.setLength(newFileLength);
            }
            f.close();
            return;
        }

        // build the digraph
        for (Iterator i = offsets.iterator(); i.hasNext(); )
        {
            Offsets o1 = (Offsets) i.next();
            Set<Offsets> adj = digraph.get(o1);
            for (Iterator j = offsets.iterator(); j.hasNext(); )
            {
                Offsets o2 = (Offsets) j.next();
                if (o1 == o2) continue;
                if (conflict(o1, o2))
                {
                    adj.add(o2);
                }
            }
        }

        // Sort the digraph topologically, removing nodes that cause cycles.
        TopologicalSorter ts = new TopologicalSorter(digraph);
        ts.sort();

        for (Object o1 : ts.getCycleNodes())
        {
            Offsets o = (Offsets) o1;
            byte[] buf = new byte[o.getBlockLength()];
            f.seek(o.getOldOffset());
            f.read(buf);
            dataBlocks.add(new DataBlock(o.getNewOffset(), buf));
        }

        for (Object o1 : ts.getFinished())
        {
            Offsets o = (Offsets) o1;
            byte[] buf = new byte[o.getBlockLength()];
            f.seek(o.getOldOffset());
            f.read(buf);
            f.seek(o.getNewOffset());
            f.write(buf);
        }

        for (DataBlock db : dataBlocks)
        {
            f.seek(db.getOffset());
            f.write(db.getData());
        }
        if (f.length() > newFileLength)
        {
            // Doesn't exist everywhere.
            f.setLength(newFileLength);
        }

        f.close();
    }

    // Own methods. ----------------------------------------------------------

    /**
     * Test if the first offset will write to the reading area of the second
     * offset.
     */
    private static boolean conflict(Offsets o1, Offsets o2)
    {
        return (o1.getNewOffset() >= o2.getOldOffset()
                && o1.getNewOffset() <= o2.getOldOffset() + o2.getBlockLength())
                || (o1.getNewOffset() + o1.getBlockLength() >= o2.getOldOffset()
                && o1.getNewOffset() + o1.getBlockLength() <= o2.getOldOffset()
                + o2.getBlockLength());
    }

    // Private inner classes.
    // -----------------------------------------------------------------------

    /**
     * <p>This class topologically sorts a directed graph encoded as a mapping
     * from vertices to sets of adjacent vertices. It is used when
     * reconstructing a file in-place, where it is possible that Offsets
     * objects overlap.</p>
     * <p/>
     * <p>Since a topological ordering of a directed graph is only possible in
     * acyclic graphs, this sorter will remove any nodes that cause cycles,
     * leaving the rest of the graph acyclic. After the graph is sorted the
     * ordering can be obtained with {@link #getFinished()}, which is a {@link
     * java.util.List} of nodes in their sorted order. Nodes that were part of
     * cycles but have been removed can be retrieved with {@link
     * #getCycleNodes()}, which returns a {@link java.util.Set} of nodes that
     * were removed.</p>
     * <p/>
     * <p>Despite the terminology of "removed" nodes, the graph passed to the
     * constructor is not modified.</p>
     *
     * @version 1.1
     */
    private static class TopologicalSorter
    {
        private static enum Color
        {
            White,
            Gray,
            Black
        }

        // Constants and variables.
        // --------------------------------------------------------------------

        /**
         * The color for unvisited nodes.
         */
        private static final String WHITE = "white";

        /**
         * The color for visited nodes.
         */
        private static final String GRAY = "gray";

        /**
         * The color for finished nodes.
         */
        private static final String BLACK = "black";

        /**
         * The graph, encoded as a mapping between nodes and {@link
         * java.util.Set}s of adjacent nodes.
         */
        private Map<Offsets, Set<Offsets>> graph;

        /**
         * A mapping between nodes and their color.
         */
        private Map<Offsets, Color> colors;

        /**
         * The ordered list of finished nodes.
         */
        private List<Offsets> finished;

        /**
         * The set of nodes that had to be removed from cycles.
         */
        private Set<Offsets> cycleNodes;

        // Constructor.
        // --------------------------------------------------------------------

        /**
         * Create a new topological sorter for the given graph.
         *
         * @param graph The graph to sort.
         */
        TopologicalSorter(Map<Offsets, Set<Offsets>> graph)
        {
            this.graph = graph;
            colors = new HashMap<Offsets, Color>();
            finished = new LinkedList<Offsets>();
            cycleNodes = new HashSet<Offsets>();
        }

        // Instance methods.
        // --------------------------------------------------------------------

        /**
         * Sort the graph. After this method returns the finished and removed
         * nodes are available.
         */
        void sort()
        {
            DFS();
        }

        /**
         * Return the ordered list of nodes.
         *
         * @return The ordered nodes.
         */
        List<Offsets> getFinished()
        {
            return finished;
        }

        /**
         * Get the nodes that caused cycles.
         *
         * @return The removed nodes.
         */
        Set<Offsets> getCycleNodes()
        {
            return cycleNodes;
        }

        // Own methods.

        /**
         * <p>Depth-first search with removing nodes with back edges. As this
         * method runs it places nodes into a list, which is a topological
         * sorting of this graph. Any node encountered with a back-edge is
         * put into the cycle-nodes list.</p>
         * <p/>
         * <p>References:</p>
         * <ol>
         * <li>T. Cormen, C. Leiserson, and R. Rivest: <i>Introduction to
         * Algorithms</i>, pp. 477-487 (1990 The Massachusetts Institute of
         * Technology).</li>
         * </ol>
         */
        private void DFS()
        {
            for (Offsets offsets : graph.keySet())
            {
                colors.put(offsets, Color.White);
            }
            for (Offsets u : graph.keySet())
            {
                if (colors.get(u) == Color.White)
                {
                    DFSVisit(u);
                }
            }
        }

        /**
         * DFS_VISIT. Visit a single node in a depth-first search.
         *
         * @param u The node to visit.
         */
        private void DFSVisit(Offsets u)
        {
            colors.put(u, Color.Gray);
            for (Offsets v : graph.get(u))
            {
                if (colors.get(v) == Color.White)
                {
                    DFSVisit(v);
                } else if (colors.get(v) == Color.Gray)
                {
                    cycleNodes.add(u);
                }
            }
            colors.put(u, Color.Black);
            if (!cycleNodes.contains(u))
            {
                finished.add(u);
            }
        }
    }

    /**
     * Sort Offsets and DataBlocks objects by increasing write offset.
     */
    private static class OffsetComparator implements Comparator
    {
        public OffsetComparator()
        {
        }

        public int compare(Object o1, Object o2)
        {
            long offset1 = 0;
            long offset2 = 0;
            if (o1 instanceof Delta)
            {
                offset1 = ((Delta) o1).getWriteOffset();
            }
            if (o2 instanceof Delta)
            {
                offset2 = ((Delta) o2).getWriteOffset();
            }
            return (int) (offset1 - offset2);
        }

        public boolean equals(Object o)
        {
            return (o instanceof OffsetComparator);
        }
    }
}
