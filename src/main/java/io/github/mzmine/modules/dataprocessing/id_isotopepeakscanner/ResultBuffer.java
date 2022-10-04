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

package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import java.util.ArrayList;

/**
 * This class serves as a buffer for possible result peaks. It stores the number of possibly
 * matching peaks for every expected isotope peak. The row index (addRow), row ID (addID) are also
 * stored so you can either manage via id or row index.
 *
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class ResultBuffer {
  private int found;
  private ArrayList<Integer> row;
  private ArrayList<Integer> ID;

  public int getFoundCount() {
    return found;
  }

  public void addFound() {
    this.found++;
  }

  public int getSize() {
    return row.size();
  }

  public void addRow(int r) {
    row.add((Integer) r);
  }

  public void addID(int id) {
    ID.add((Integer) id);
  }

  public int getRow(int i) {
    return row.get(i).intValue();
  }

  public int getID(int i) {
    return ID.get(i).intValue();
  }

  public ResultBuffer() {
    found = 0;
    row = new ArrayList<Integer>();
    ID = new ArrayList<Integer>();
  }
}
