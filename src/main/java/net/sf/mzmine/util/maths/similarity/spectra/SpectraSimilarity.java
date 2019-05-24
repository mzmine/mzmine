/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.util.maths.similarity.spectra;

import java.util.ArrayList;
import java.util.List;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.util.scans.ScanAlignment;

public class SpectraSimilarity {
  private double cosine;
  private int overlap;
  private String name;

  private DataPoint[] library;
  private DataPoint[] query;
  private DataPoint[][] aligned;

  public SpectraSimilarity(String name, double cosine, int overlap) {
    this.name = name;
    this.cosine = cosine;
    this.overlap = overlap;
  }

  public SpectraSimilarity(String name, double cosine, int overlap, DataPoint[] library,
      DataPoint[] query, List<DataPoint[]> alignedDP) {
    this.name = name;
    this.cosine = cosine;
    this.overlap = overlap;
    this.library = library;
    this.query = query;
    if (alignedDP != null) {
      // filter unaligned
      List<DataPoint[]> filtered = new ArrayList<>(alignedDP);
      ScanAlignment.removeUnaligned(filtered);
      aligned = new DataPoint[2][];
      aligned[0] = filtered.get(0);
      aligned[1] = filtered.get(1);
    }
  }


  /**
   * Number of overlapping signals
   * 
   * @return
   */
  public int getOverlap() {
    return overlap;
  }

  /**
   * Cosine similarity
   * 
   * @return
   */
  public double getCosine() {
    return cosine;
  }

  public String getFunctionName() {
    return name;
  }

  public DataPoint[] getLibrary() {
    return library;
  }

  public DataPoint[] getQuery() {
    return query;
  }

  public DataPoint[][] getAligned() {
    return aligned;
  }
}
