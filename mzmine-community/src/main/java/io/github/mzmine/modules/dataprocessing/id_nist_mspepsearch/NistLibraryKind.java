/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch;

/**
 * How a library has to be handed to MSPepSearch. The NIST main and replicate libraries use their
 * own file format and are rejected with "MS Library initiation error -10" if passed as
 * {@code /LIB}; every other shipped library uses the user format and only works as {@code /LIB}.
 * <p>
 * The declaration order is also the sort order of the library selection list.
 */
public enum NistLibraryKind {

  /**
   * The NIST main library ({@code .in6} name index), passed as {@code /MAIN}. At most one.
   */
  MAIN("/MAIN"),
  /**
   * The NIST replicate library ({@code .inr} name index), passed as {@code /REPL}. At most one.
   */
  REPLICATE("/REPL"),
  /**
   * Any user-format library ({@code .INU} name index) - MS/MS, RI, APCI and custom libraries -
   * passed as {@code /LIB}.
   */
  USER("/LIB");

  private final String argument;

  NistLibraryKind(final String argument) {
    this.argument = argument;
  }

  /**
   * @return the MSPepSearch command line switch for this kind of library.
   */
  public String getArgument() {
    return argument;
  }
}
