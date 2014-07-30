/**
 * This is an implementation of the rsync algorithm in Java.
 *
 * <p>From a high level, the rsync algorithm uses three phases:</p>
 *
 * <ol>
 *     <li>Checksum generation. On the destination, the target bytes are scanned and turned into
 *     a list of checksum pairs. This list is sent to the sender. See {@link org.metastatic.rsync.Generator}
 *     and {@link org.metastatic.rsync.GeneratorStream}.</li>
 *
 *     <li>Block matching. On the source, the list of checksums and the source bytes are used to
 *     generate a sequence of deltas. The source sends this secquence of deltas to the destination. See
 *     {@link org.metastatic.rsync.Matcher} and {@link org.metastatic.rsync.MatcherStream}.</li>
 *
 *     <li>Patching. The list of deltas, along with the old target bytes, are used to reconstruct
 *     the bytes on the source side. See {@link org.metastatic.rsync.Rebuilder} and
 *     {@link org.metastatic.rsync.RebuilderStream}</li>
 * </ol>
 *
 * <p>Jarsync's API for these three phases all come in two flavors: streaming, or all-at-once.</p>
 *
 * <p>The streaming API takes in incremental updates, and as things progress, invokes callbacks with the
 * intermediate results, as they are computed. E.g., you can update a {@link org.metastatic.rsync.GeneratorStream} with parts
 * of a file, and send generated checksum pairs over the wire as they are generated; or, update a {@link org.metastatic.rsync.MatcherStream}
 * with checksums as they are read off the wire, and send out deltas as they are generated.</p>
 *
 * <p>The all-at-once API takes whole files, byte arrays, or collections and performs the entire task in a single
 * method call. This is a convenience API, and it (mostly) builds on the streaming API.</p>
 */
package org.metastatic.rsync;