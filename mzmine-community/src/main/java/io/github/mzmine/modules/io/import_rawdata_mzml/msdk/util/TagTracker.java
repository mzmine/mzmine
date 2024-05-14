/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.util;

import io.github.msdk.MSDKRuntimeException;
import java.util.LinkedHashSet;

/**
 * This class is not thread safe. If more than one thread attempts to use {@link #enter(String)}
 * or {@link #exit(String)} methods, the behavior is undefined.
 */
public class TagTracker {

  // use hashmap for fast lookup
  // last element is current tag
  private LinkedHashSet<String> stack = new LinkedHashSet<>();
  /** Constant <code>DEFAULT_CHAR_ARR_SIZE=64</code> */
  protected final static int DEFAULT_CHAR_ARR_SIZE = 64;

  /**
   * If the stack grows larger than this value, something is likely not OK. Will throw an exception
   * in such a case.
   */
  private int maxDepth;
  /** Constant <code>DEFAULT_MAX_DEPTH=128</code> */
  protected static final int DEFAULT_MAX_DEPTH = 128;

  /**
   * <p>Constructor for TagTracker.</p>
   *
   * @param maxDepth a int.
   */
  public TagTracker(int maxDepth) {
    this.maxDepth = maxDepth;
  }

  /**
   * Creates a tracker with default max depth {@link #DEFAULT_MAX_DEPTH}.
   */
  public TagTracker() {
    this(DEFAULT_MAX_DEPTH);
  }

  /**
   * <p>enter.</p>
   *
   * @param tag a {@link String} object.
   */
  public void enter(String tag) {
    stack.addLast(tag);
    if (stack.size() > maxDepth)
      throw new IllegalStateException("Max stack depth [" + maxDepth + "] exceeded");
  }

  /**
   * <p>exit.</p>
   *
   * @param tag a {@link String} object.
   */
  public void exit(String tag) {
    final String top = stack.removeLast();
    if (top == null) {
      throw new MSDKRuntimeException("Stack exit called when the stack was empty.");
    }
    if (!top.equals(tag)) {
      throw (new MSDKRuntimeException(
          "Cannot exit tag '" + tag + "'. Last tag entered was '" + top + "'"));
    }
  }

  /**
   * <p>inside.</p>
   *
   * @param tag a {@link String} object.
   * @return a boolean.
   */
  public boolean inside(String tag) {
    return tag != null && stack.contains(tag);
  }

  /**
   * <p>current.</p>
   *
   * @return a {@link String} object.
   */
  public String current() {
    final String top = stack.getLast();
    if (top == null) {
      return "";
    }
    return top;
  }

}
