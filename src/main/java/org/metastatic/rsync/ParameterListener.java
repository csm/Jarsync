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

/**
 * A listener for parsing Samba-style config files. There are two events
 * that occur during parsing: starting named sections and setting
 * parameters.
 *
 * @version $Revision$
 */
public interface ParameterListener extends java.util.EventListener
{

    /**
     * Begin a named section. This method will be called on every new
     * section definition.
     *
     * @param sectionName The new section's name.
     */
    void beginSection(String sectionName);

    /**
     * Set a parameter. This method may be called prior to the first call
     * to {@link #beginSection}, if there are global options. If this
     * method is called after a call to {@link
     * #beginSection(java.lang.String)}, this parameter belongs to that
     * section.
     *
     * @param name  The parameter's name.
     * @param value The parameter's value.
     */
    void setParameter(String name, String value);
}
