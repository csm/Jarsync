/* TwoKeyMap -- Two-key Map implementation.
   $Id$

Copyright (C) 2003  Casey Marshall <rsdio@metastatic.org>

This file is a part of Jarsync.

Jarsync is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation; either version 2 of the License, or (at your
option) any later version.

Jarsync is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License
along with Jarsync; if not, write to the

   Free Software Foundation, Inc.,
   59 Temple Place, Suite 330,
   Boston, MA  02111-1307
   USA

Linking Jarsync statically or dynamically with other modules is making
a combined work based on Jarsync.  Thus, the terms and conditions of
the GNU General Public License cover the whole combination.

As a special exception, the copyright holders of Jarsync give you
permission to link Jarsync with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on Jarsync.  If you modify Jarsync, you may extend this
exception to your version of it, but you are not obligated to do so.
If you do not wish to do so, delete this exception statement from your
version.  */


package org.metastatic.rsync;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

import java.util.*;

/**
 * <p>This is a "double-keyed" mapping. The first key is a 16-bit integer,
 * and the second key is a variable-length byte array. With this, we can
 * compute if a given mapping is "probably" in the map using the first key,
 * and compute whether or not a mapping is definitely in the hashtable with
 * the second key. The rationale behind this is that the first key is
 * trivial to compute and that the second key is more difficult to compute
 * but more unique.</p>
 * <p/>
 * <p>Since the strong key can be a byte array of any length, then this
 * "strong" key can be shorter (and thus less unique) than the "weak"
 * key. For this class to work properly, the stronger key should be at
 * least four bytes in length, preferably longer.</p>
 * <p/>
 * <p>The weak-key/strong-key method is inspired by (and is was written
 * for) the "hashtable" in the rsync algorithm, and has three levels of
 * key search:</p>
 * <p/>
 * <ol>
 * <li>Test if the lower 2 bytes of the weak key (the positive part of a
 * 32-bit integer in Java) have been mapped to anything yet. This method
 * always takes O(1) time, and it is assumed that the weak key is
 * trivial to compute.</li>
 * <p/>
 * <li>Test if the entire weak key is mapped to anything. Since this
 * class uses a linked list to handle collisions where the lower 2 bytes
 * are the same, this method takes at most O(n) operations (also
 * assuming that the weak key is trivial to compute).</li>
 * <p/>
 * <li>Test if both the weak and strong keys map to an Object. In
 * addition to the linked-list search of the second step, this involves
 * a search of a red-black tree, meaning that the upper-bound time
 * complexity ranges from O(lg(n)) to O(n). This works under the
 * assumption that the strong key is some sort of {@link
 * java.security.MessageDigest}, and thus takes longer to compute.</li>
 * </ol>
 * <p/>
 * <p>With this method, we can determine if it is worth it to compute the
 * strong key if we have already computed the weak key.</p>
 * <p/>
 * <p><code>null</code> is not a valid key in this map.</p>
 *
 * @author Casey Marshall
 * @version $Revision$
 */
public class TwoKeyMap<T> implements java.io.Serializable, Map<ChecksumPair, T>
{
    // Constants and variables.
    // -----------------------------------------------------------------

    private final Map<Integer, Map<StrongKey, T>> map;

    // Inner classes.
    // -----------------------------------------------------------------

    /**
     * A {@link java.util.Map.Entry} that contains another {@link
     * java.util.Map} that is keyed with stronger, larger keys, and links
     * to other SubTables whose {@link #key}'s lower four bytes are
     * equivalent.
     *
     * @author Casey Marshall
     * @version 1.1
     * @since 1.1
     */
    public class SubTable implements Map.Entry, java.io.Serializable
    {

        // Constants and variables.
        // --------------------------------------------------------------

        /**
         * The sub-table, a {@link java.util.Map} that is an instance of
         * {@link java.util.TreeMap}.
         *
         * @since 1.1
         */
        protected Map data;

        /**
         * The index in the array of sub-tables in which this entry is a
         * member.
         *
         * @since 1.1
         */
        protected Integer key;

        /**
         * The next sub-table, implements a linked list.
         *
         * @since 1.1
         */
        SubTable next;

        // Constructors.
        // --------------------------------------------------------------

        /**
         * Create a new sub-table with a given index and a given Comparator.
         *
         * @since 1.1
         */
        SubTable(Integer key)
        {
            data = new TreeMap();
            this.key = key;
            next = null;
        }

        SubTable(int key)
        {
            this(new Integer(key));
        }

        // Public instance methods.
        // --------------------------------------------------------------

        /**
         * Get the Object that is mapped by the strong key <tt>key</tt>.
         *
         * @param key The key to look for in this sub-table.
         * @return The object mapped to by the given key, or null if there
         * is no such mapping.
         * @since 1.1
         */
        public Object get(StrongKey key)
        {
            return data.get(key);
        }

        /**
         * Map the given key to the given value.
         *
         * @param key   The key.
         * @param value The value.
         * @since 1.1
         */
        public void put(StrongKey key, Object value)
        {
            data.put(key, value);
        }

        /**
         * Test if this sub-table contains the given key.
         *
         * @param key The key to look for.
         * @return <tt>true</tt> if there is a mapping from the given key.
         * @since 1.1
         */
        public boolean containsKey(StrongKey key)
        {
            return data.containsKey(key);
        }

        /**
         * Test if this sub-table contains the given value.
         *
         * @param value The value to look for.
         * @return <tt>true</tt> if there is a mapping to the given value.
         * @since 1.1
         */
        public boolean containsValue(Object value)
        {
            return data.containsValue(value);
        }

        // Public instance methods implementing java.util.Map.Entry

        /**
         * Test if another object equals this one.
         *
         * @param o The object to test.
         * @return <tt>true</tt> If <tt>o</tt> is an instance of this class
         * and its fields are equivalent.
         * @throws java.lang.ClassCastException   If <tt>o</tt> is not an
         *                                        instance of this class.
         * @throws java.lang.NullPointerException If <tt>o</tt> is null.
         * @since 1.1
         */
        public boolean equals(Object o)
        {
            return data.equals(((SubTable) o).data) &&
                    key.equals(((SubTable) o).key);
        }

        /**
         * Get the key that maps to this entry.
         *
         * @return The key, a {@link java.lang.Integer} that maps to this
         * entry.
         * @since 1.1
         */
        public Object getKey()
        {
            return key;
        }

        /**
         * Get the value of this entry.
         *
         * @return A {@link java.util.Map} that represents the sub-table.
         * @since 1.1
         */
        public Object getValue()
        {
            return data;
        }

        /**
         * Return the hash code for this entry.
         *
         * @return The hash code for the Map that implements this sub-table.
         * @since 1.1
         */
        public int hashCode()
        {
            return data.hashCode();
        }

        // Unsupported methods from java.util.Map.Entry ------------------

        public Object setValue(Object value)
        {
            throw new UnsupportedOperationException();
        }

        // Public instance method overriding Object. ---------------------

        /**
         * Return a string representation of this object.
         *
         * @return A string representation of this object.
         * @since 1.1
         */
        public String toString()
        {
            return Integer.toHexString(key.intValue()) + " => " + data.toString();
        }
    }

    /**
     * The stronger of the two keys in this {@link java.util.Map}. It is
     * basically a wrapper around an array of bytes, providing methods
     * important for using this class as the key in a Map, namely the
     * {@link #hashCode()} and {@link #compareTo(java.lang.Object)}
     * methods.
     *
     * @author Casey Marshall
     * @version 1.0
     * @since 1.1
     */
    public static class StrongKey implements java.io.Serializable, Comparable
    {

        // Constants and variables.
        // --------------------------------------------------------------

        /**
         * The key itself. An array of some number of bytes.
         *
         * @since 1.0
         */
        protected byte[] key;

        // Constructors.
        // --------------------------------------------------------------

        /**
         * Create a new key with the specified bytes. <code>key</code> can be
         * <code>null</code>.
         *
         * @param key The bytes that will make up this key.
         * @since 1.0
         */
        StrongKey(byte[] key)
        {
            if (key != null)
                this.key = key.clone();
            else
                this.key = key;
        }

        // Instance methods.
        // --------------------------------------------------------------

        /**
         * Return the bytes that compose this Key.
         *
         * @return {@link #key}, the bytes that compose this key.
         * @since 1.0
         */
        public byte[] getBytes()
        {
            if (key != null)
                return key.clone();
            return null;
        }

        /**
         * Set the byte array that composes this key.
         *
         * @param key The bytes that will compose this key.
         * @since 1.0
         */
        public void setBytes(byte[] key)
        {
            if (key != null)
                this.key = (byte[]) key.clone();
            else
                this.key = key;
        }

        /**
         * The length, in bytes, of this key.
         *
         * @return The length of this key in bytes.
         * @since 1.0
         */
        public int length()
        {
            if (key != null)
                return key.length;
            return 0;
        }

        // Public instance methods overriding java.lang.Object -----------

        /**
         * Return a zero-padded hexadecimal string representing this key.
         *
         * @return A hexadecimal string of the bytes in {@link #key}.
         * @since 1.0
         */
        public String toString()
        {
            if (key == null || key.length == 0)
                return "nil";
            return Util.toHexString(key);
        }

        /**
         * The hash code for this key. This is defined as the XOR of all
         * 32-bit blocks of the {@link #key} array.
         *
         * @return The hash code for this key.
         * @since 1.0
         */
        public int hashCode()
        {
            if (key == null)
                return 0;
            int code = 0;
            for (int i = key.length - 1; i >= 0; i--)
                code ^= ((int) key[i] & 0xff) << (((key.length - i - 1) * 8) % 32);
            return code;
        }

        /**
         * Test if this key equals another. Two keys are equal if the method
         * {@link java.util.Arrays#equals(byte[], byte[])} returns true for
         * thier key arrays.
         *
         * @param o The object to compare to.
         * @return <tt>true</tt> If this key is equivalent to the argument.
         * @throws java.lang.ClassCastException If o is not a StrongKey.
         * @since 1.0
         */
        public boolean equals(Object o)
        {
            return Arrays.equals(key, ((StrongKey) o).key);
        }

        // java.lang.Comparable interface implementation -----------------

        /**
         * Compare this object to another. This method returns an integer
         * value less than, equal to, or greater than zero if this key
         * is less than, equal to, or greater than the given key. This
         * method will return
         * <ul>
         * <li>0 if the {@link #key} fields are references to the same
         * array.
         * <li>1 if {@link #key} in this class is null.
         * <li>-1 if {@link #key} in <tt>o</tt> is null (null is always less
         * than everything).
         * <li>0 if the lengths of the {@link #key} arrays are the same and
         * their contents are equivalent.
         * <li>The difference between the lengths of the keys if different.
         * <li>The difference between the first two different members of the
         * arrays.
         * </ul>
         *
         * @param o The key to compare to.
         * @return An integer derived from the differences of the two keys.
         * @throws java.lang.ClassCastException If o is not a StrongKey.
         * @since 1.0
         */
        public int compareTo(Object o)
        {
            if (!(o instanceof StrongKey))
                throw new ClassCastException(o.getClass().getName());
            StrongKey sk = (StrongKey) o;
            if (key == sk.key) return 0;
            if (key == null) return 1;
            if (sk.key == null) return -1;
            if (java.util.Arrays.equals(key, sk.getBytes())) return 0;
            if (key.length != sk.length()) return key.length - sk.length();
            byte[] arr = sk.getBytes();
            for (int i = 0; i < key.length; i++)
                if (key[i] != arr[i]) return (key[i] - arr[i]);
            return 0; // unreachable
        }
    }


    // Constructors.
    // -----------------------------------------------------------------

    /**
     * Creates a new Map with 2^16 sub-tables.
     *
     * @since 1.1
     */
    public TwoKeyMap()
    {
        map = new HashMap<Integer, Map<StrongKey, T>>();
    }

    /**
     * Create a new Map with all the mappings of the given map.
     *
     * @param m The initial mappings this Map should contain.
     * @throws java.lang.ClassCastException   If one of the keys in
     *                                        <code>m</code> is not a ChecksumPair.
     * @throws java.lang.NullPointerException If one of the keys in
     *                                        <code>m</code> is null.
     * @since 1.1
     */
    public TwoKeyMap(Map m)
    {
        this();
        putAll(m);
    }

    // Instance methods.
    // -----------------------------------------------------------------

    /**
     * Test if the map contains the lower two bytes of the weak key. This
     * is the fastest, and least accurate <code>containsKey</code>
     * method.
     *
     * @param key The key to check.
     * @return true If there is at least one entry with that weak key.
     * @since 1.1
     */
    public boolean containsKey(int key)
    {
        return map.containsKey(key);
    }

    // Public instance methods implementing java.util.Map. -------------

    /**
     * Put the given object at the location specified by a key pair.
     *
     * @param key   The {@link ChecksumPair} to use as the key.
     * @param value The value to map to.
     * @return The old value mapped, if any.
     * @since 1.1
     */
    public T put(ChecksumPair key, T value)
    {
        Map<StrongKey, T> m = map.get(key.getWeak());
        if (m == null)
        {
            m = new TreeMap<StrongKey, T>();
            map.put(key.getWeak(), m);
        }
        StrongKey strongKey = new StrongKey(key.getStrong());
        return m.put(strongKey, value);
    }

    /**
     * Test if this map contains either the supplied weak key (if the
     * argument is an Integer) or both the weak and strong keys (if the
     * argument is a {@link ChecksumPair}).
     *
     * @param key The key to check.
     * @return true If the map contains the given weak key (if
     * <code>key</code> is an Integer) or the given pair of keys (if
     * <code>key</code> is a {@link ChecksumPair}).
     * @since 1.1
     */
    public boolean containsKey(Object key)
    {
        SubTable t;
        if (key instanceof Integer)
        {
            return containsKey(((Integer) key).intValue());
        }
        else if (key instanceof ChecksumPair)
        {
            Map<StrongKey, T> m = map.get(((ChecksumPair) key).getWeak());
            if (m != null)
            {
                return m.containsKey(new StrongKey(((ChecksumPair) key).getStrong()));
            }
        }
        return false;
    }

    /**
     * Get the object mapped to by the given key pair. The argument
     * SHOULD be a {@link ChecksumPair}.
     *
     * @param key The key of the object to get.
     * @return The object keyed by <code>key</code>, or <code>null</code>
     * if there is no such object.
     * @since 1.1
     */
    public T get(Object key)
    {
        ChecksumPair pair = (ChecksumPair) key;
        Map<StrongKey, T> m = map.get(pair.getWeak());
        if (m != null)
            return m.get(new StrongKey(pair.getStrong()));
        return null;
    }

    /**
     * Clear this Map.
     *
     * @since 1.1
     */
    public void clear()
    {
        map.clear();
    }

    /**
     * Test if the given value is in one of the sub-tables.
     *
     * @param value The value to search for.
     * @return <tt>true</tt> if <tt>value</tt> is in this Map.
     * @since 1.1
     */
    public boolean containsValue(Object value)
    {
        for (Map<StrongKey, T> m : map.values())
        {
            if (m.containsValue(value))
                return true;
        }
        return false;
    }

    /**
     * Return an unmodifiable set of the SubTable objects in this class.
     *
     * @return A set of all sub-tables from this class.
     * @since 1.1
     */
    public Set<Entry<ChecksumPair, T>> entrySet()
    {
        return new AbstractSet<Entry<ChecksumPair, T>>()
        {
            @Override
            public Iterator<Entry<ChecksumPair, T>> iterator()
            {
                Iterator<Iterator<Entry<ChecksumPair, T>>> it =
                        Iterators.transform(map.entrySet().iterator(), new Function<Entry<Integer, Map<StrongKey, T>>, Iterator<Entry<ChecksumPair, T>>>()
                        {
                            @Override
                            public Iterator<Entry<ChecksumPair, T>> apply(final Entry<Integer, Map<StrongKey, T>> input)
                            {
                                final Iterator<Entry<StrongKey, T>> it = input.getValue().entrySet().iterator();
                                return new Iterator<Entry<ChecksumPair, T>>()
                                {
                                    @Override
                                    public boolean hasNext()
                                    {
                                        return it.hasNext();
                                    }

                                    @Override
                                    public Entry<ChecksumPair, T> next()
                                    {
                                        Entry<StrongKey, T> next = it.next();
                                        return new AbstractMap.SimpleEntry<ChecksumPair, T>(new ChecksumPair(input.getKey(), next.getKey().getBytes()), next.getValue());
                                    }

                                    @Override
                                    public void remove()
                                    {
                                        it.remove();
                                    }
                                };
                            }
                        });
                return Iterators.concat(it);
            }

            @Override
            public int size()
            {
                return TwoKeyMap.this.size();
            }
        };
    }

    /**
     * Test if this object equals another.
     *
     * @return <tt>true</tt> if <tt>o</tt> is an instance of this class
     * and if it contains the same sub-tables.
     * @throws java.lang.ClassCastException   if <tt>o</tt> is not an
     *                                        instance of this class.
     * @throws java.lang.NullPointerException if <tt>o</tt> is null.
     * @since 1.1
     */
    public boolean equals(Object o)
    {
        try
        {
            Map<ChecksumPair, T> m = (Map<ChecksumPair, T>) o;
            return entrySet().equals(m.entrySet());
        }
        catch (ClassCastException e)
        {
        }
        catch (NullPointerException e)
        {
        }
        return false;
    }

    /**
     * Return the hash code of this object.
     *
     * @return The hash code of this object.
     * @since 1.1
     */
    public int hashCode()
    {
        return map.hashCode();
    }

    /**
     * Test if there are no mappings in this map.
     *
     * @return <tt>true</tt> if this map is empty.
     * @since 1.1
     */
    public boolean isEmpty()
    {
        return (size() == 0);
    }

    /**
     * Return an unmodifiable set of all the pairs of keys in this
     * mapping.
     *
     * @return A set of all the {@link ChecksumPair}s in this mapping.
     * @since 1.1
     */
    public Set<ChecksumPair> keySet()
    {
        return new AbstractSet<ChecksumPair>()
        {
            @Override
            public Iterator<ChecksumPair> iterator()
            {
                Iterator<Entry<ChecksumPair, T>> it = entrySet().iterator();
                return Iterators.transform(entrySet().iterator(), new Function<Entry<ChecksumPair, T>, ChecksumPair>()
                {
                    @Override
                    public ChecksumPair apply(Entry<ChecksumPair, T> input)
                    {
                        return input.getKey();
                    }
                });
            }

            @Override
            public int size()
            {
                return TwoKeyMap.this.size();
            }
        };
    }

    /**
     * Put every entry in <code>m</code> in this map. This method will
     * only work if the keys of <code>m</code> are of type {@link
     * ChecksumPair}.
     *
     * @param m The mappings to put.
     * @throws java.lang.ClassCastException   If every key in <code>m</code>
     *                                        is not a ChecksumPair.
     * @throws java.lang.NullPointerException If a key in <code>m</code>
     *                                        is null.
     * @see #put(org.metastatic.rsync.ChecksumPair, T)
     * @since 1.1
     */
    public void putAll(Map<? extends ChecksumPair, ? extends T> m)
    {
        for (Entry<? extends ChecksumPair, ? extends T> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }

    /**
     * Removes a single mapping if the argument is a {@link ChecksumPair}, or
     * an entire {@link SubTable} if the argument is a {@link
     * java.lang.Integer}.
     *
     * @param k The key of the object to be removed.
     * @return The removed object, if such a mapping existed.
     * <code>null</code> otherwise.
     * @since 1.1
     */
    public T remove(Object k)
    {
        ChecksumPair key = (ChecksumPair) k;
        if (key != null)
        {
            Map<StrongKey, T> m = map.get(key.getWeak());
            if (m != null)
            {
                T ret = m.remove(new StrongKey(key.getStrong()));
                if (m.isEmpty())
                    map.remove(key.getWeak());
                return ret;
            }
            return null;
        }
        return null;
    }

    /**
     * Return the number of mappings.
     *
     * @return The number of mappings.
     * @since 1.1
     */
    public int size()
    {
        int size = 0;
        for (Map<StrongKey, T> m : map.values())
            size += m.size();
        return size;
    }

    /**
     * Return a Collection of all the values mapped by this object.
     *
     * @return A Collection of all values mapped by this object.
     * @since 1.1
     */
    public Collection<T> values()
    {
        return new AbstractCollection<T>()
        {
            @Override
            public Iterator<T> iterator()
            {
                return Iterators.transform(entrySet().iterator(), new Function<Entry<ChecksumPair, T>, T>()
                {
                    @Override
                    public T apply(Entry<ChecksumPair, T> input)
                    {
                        return input.getValue();
                    }
                });
            }

            @Override
            public int size()
            {
                return TwoKeyMap.this.size();
            }
        };
    }

    // Public instance methods overriding java.lang.Object. ------------

    /**
     * Create a printable version of this Map.
     *
     * @return A {@link java.lang.String} representing this object.
     * @since 1.1
     */
    public String toString()
    {
        StringBuilder str = new StringBuilder("{");
        boolean first = true;
        for (Entry<ChecksumPair, T> e : entrySet())
        {
            if (!first)
                str.append(", ");
            str.append('(');
            str.append(Integer.toHexString(e.getKey().getWeak()));
            str.append(", ");
            str.append(Util.toHexString(e.getKey().getStrong()));
            str.append(") => ");
            str.append(e.getValue());
            first = false;
        }
        str.append("}");
        return str.toString();
    }
}
