/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.import_rawdata_mzml.util;

import io.github.mzmine.datamodel.MZmineRuntimeException;
import java.util.ArrayDeque;
import javolution.text.CharArray;

/**
 * This class is not thread safe. If more than one thread attempts to use {@link #enter(CharArray)}
 * or {@link #exit(CharArray)} methods, the behavior is undefined.
 */
public class TagTracker {

  private ArrayDeque<CharArray> stack;
  private ArrayDeque<CharArray> pool;
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
    stack = new ArrayDeque<>();
    pool = new ArrayDeque<>();
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
   * @param tag a {@link CharArray} object.
   */
  public void enter(CharArray tag) {
    final CharArray arr = borrowArr(tag.length());
    copyContent(tag, arr);
    stack.push(arr);
    if (stack.size() > maxDepth)
      throw new IllegalStateException("Max stack depth [" + maxDepth + "] exceeded");
  }

  /**
   * <p>exit.</p>
   *
   * @param tag a {@link CharArray} object.
   */
  public void exit(CharArray tag) {
    final CharArray top = stack.peek();
    if (top == null) {
      throw new MZmineRuntimeException("Stack exit called when the stack was empty.");
    }
    if (!top.equals(tag)) {
      throw (new MZmineRuntimeException(
          "Cannot exit tag '" + tag + "'. Last tag entered was '" + top + "'"));
    }
    returnArr(stack.pop());
  }

  /**
   * <p>inside.</p>
   *
   * @param tag a {@link CharSequence} object.
   * @return a boolean.
   */
  public boolean inside(CharSequence tag) {
    if (tag == null)
      return false;
    for (CharArray aStack : stack) {
      if (aStack.contentEquals(tag)) {
        return true;
      }
    }
    return false;
  }

  /**
   * <p>current.</p>
   *
   * @return a {@link CharArray} object.
   */
  public CharArray current() {
    final CharArray top = stack.peek();
    if (top == null)
      return new CharArray("");
    return top;
  }

  private CharArray createArr(int capacity) {
    return new CharArray(capacity);
  }

  private CharArray borrowArr(int capacity) {
    if (pool.isEmpty())
      return createArr(capacity);
    final CharArray c = pool.pop();
    if (c.array().length < capacity) {
      c.setArray(new char[capacity], 0, 0);
    }
    return c;
  }

  /**
   * Copy content from c1 to c2. If c2's buffer is not big enough, it will be reassigned.
   * 
   * @param c1 Copy from.
   * @param c2 Copy to. Potentially increasing the buffer size and erasing all previous content.
   */
  private void copyContent(final CharArray c1, CharArray c2) {
    final char[] a1 = c1.array();
    final char[] a2 = c2.array().length >= c1.length() ? c2.array() : new char[c1.length()];
    for (int i = 0; i < c1.length(); i++) {
      a2[i] = a1[c1.offset() + i];
    }
    c2.setArray(a2, 0, c1.length());
  }

  private void returnArr(CharArray arr) {
    pool.push(arr);
  }

}
