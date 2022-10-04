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

package io.github.mzmine.modules.dataprocessing.featdet_gridmass;

class DatumExpand implements Comparable<DatumExpand> {
  Datum dato;
  boolean left;
  boolean right;
  boolean up;
  boolean down;
  double expanded = Double.MAX_VALUE; // expanded to a certain threshold
  int index;
  double minIntensity = 0;

  DatumExpand(Datum dato, boolean l, boolean r, boolean up, boolean dw, int pos) {
    this.dato = dato;
    this.left = l;
    this.right = r;
    this.up = up;
    this.down = dw;
    this.index = pos;
    minIntensity = dato.intensity;
  }

  public int compareTo(DatumExpand other) {
    if (dato.scan < other.dato.scan)
      return -1;
    if (dato.scan > other.dato.scan)
      return 1;

    // equal scan, then sort by lower mz
    if (dato.mz < other.dato.mz)
      return -1;
    if (dato.mz > other.dato.mz)
      return 1;

    return 0;
  }

}
