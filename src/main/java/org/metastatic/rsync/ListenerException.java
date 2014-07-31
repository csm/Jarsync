/* ListenerException.java -- Exception thrown by listeners.

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

/**
 * Signals an exception raised by an {@link org.metastatic.rsync.GeneratorListener}, {@link
 * org.metastatic.rsync.MatcherListener}, or {@link org.metastatic.rsync.RebuilderListener}.
 *
 * <p>Listener exceptions may contain other exceptions (the "cause") and
 * may be chained together if there are multiple failures accross
 * multiple listeners.
 */
public class ListenerException extends Exception
{

    // Fields.
    // -----------------------------------------------------------------------

    protected ListenerException next;

    protected Throwable cause;

    // Constructors.
    // -----------------------------------------------------------------------

    public ListenerException(Throwable cause)
    {
        super();
        this.cause = cause;
    }

    public ListenerException(Throwable cause, String msg)
    {
        super(msg);
        this.cause = cause;
    }

    public ListenerException(String msg)
    {
        super(msg);
    }

    public ListenerException()
    {
        super();
    }

    // Instance methods.
    // -----------------------------------------------------------------------

    /**
     * Returns the next exception in this chain, or <code>null</code> if
     * there are no other exceptions.
     *
     * @return The next exception.
     */
    public ListenerException getNext()
    {
        return next;
    }

    /**
     * Sets the next exception in this chain.
     *
     * @param next The next exception.
     */
    public void setNext(ListenerException next)
    {
        this.next = next;
    }

    /**
     * Gets the cause of this exception, or <code>null</code> if the
     * cause is unknown.
     *
     * @return The cause.
     */
    public Throwable getCause()
    {
        return cause;
    }

    /**
     * Sets the cause of this exception.
     *
     * @param cause The cause of this exception.
     */
    public synchronized Throwable initCause(Throwable cause)
    {
        Throwable old = this.cause;
        this.cause = cause;
        return old;
    }
}
