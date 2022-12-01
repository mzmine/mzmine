/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.align_hierarchical;

import java.util.logging.Logger;

public class LargeArrayFloat {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private boolean VERBOSE = false;

  private final long CHUNK_SIZE = 1024 * 1024 * 1024; // 1GiB

  long size;
  float[][] data;

  public LargeArrayFloat(long size) {

    this.size = size;
    if (size == 0) {
      data = null;
    } else {
      int chunks = (int) (size / CHUNK_SIZE);
      int remainder = (int) (size - ((long) chunks) * CHUNK_SIZE);

      if (VERBOSE)
        System.out.println(
            this.getClass().getSimpleName() + " > Created with " + chunks + " chunks (size: "
                + CHUNK_SIZE + " each) + a remainder of " + remainder + " => TOTAL: " + size);

      data = new float[chunks + (remainder == 0 ? 0 : 1)][];
      for (int idx = chunks; --idx >= 0;) {
        data[idx] = new float[(int) CHUNK_SIZE];
      }
      if (remainder != 0) {
        data[chunks] = new float[remainder];
      }

      // System.out.println(this.getClass().getSimpleName()
      // + " > Created with " + chunks + " chunks (size: " + CHUNK_SIZE +
      // " each) + a remainder of " + remainder
      // + " => TOTAL: " + size);
    }
  }

  public float get(long index) {

    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("Error attempting to access data element " + index
          + ".  Array is " + size + " elements long.");
    }
    int chunk = (int) (index / CHUNK_SIZE);
    int offset = (int) (index - (((long) chunk) * CHUNK_SIZE));
    return data[chunk][offset];
  }

  public void set(long index, float f) {

    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("Error attempting to access data element " + index
          + ".  Array is " + size + " elements long.");
    }
    int chunk = (int) (index / CHUNK_SIZE);
    int offset = (int) (index - (((long) chunk) * CHUNK_SIZE));
    data[chunk][offset] = f;
  }

  public void writeToFile() { // toString won't make sense for large array!

    // String str = "";
    //
    // for (int i = 0; i < size; ++i) {
    // str +=
    // }
    //
    // return str;

    // ...
  }

}
