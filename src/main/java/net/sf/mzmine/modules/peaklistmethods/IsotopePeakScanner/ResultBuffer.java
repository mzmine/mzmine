/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.IsotopePeakScanner;

import java.util.ArrayList;

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

  ResultBuffer() {
    found = 0;
    row = new ArrayList<Integer>();
    ID = new ArrayList<Integer>();
  }
}
