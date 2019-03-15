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

package net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch;

import java.util.OptionalDouble;
import net.sf.mzmine.datamodel.DataPoint;

public class SpectralDBEntry {

  private final String name;
  private final double mz;
  private final OptionalDouble rt;
  private final String adduct;
  private final DataPoint[] dps;
  private String formula;

  public SpectralDBEntry(String name, double mz, Double rt, String adduct, String formula,
      DataPoint[] dps) {
    this.name = name;
    this.mz = mz;
    if (rt != null)
      this.rt = OptionalDouble.of(rt);
    else
      this.rt = OptionalDouble.empty();
    this.adduct = adduct;
    this.formula = formula;
    this.dps = dps;
  }

  public String getCompoundName() {
    return name;
  }

  public double getPrecursorMZ() {
    return mz;
  }

  public String getAdduct() {
    return adduct;
  }

  public DataPoint[] getDataPoints() {
    return dps;
  }

  /**
   * 
   * @return null if no retention time was set
   */
  public OptionalDouble getRetentionTime() {
    return rt;
  }

  public boolean hasRetentionTime() {
    return rt != null && rt.isPresent();
  }

  public String getFormula() {
    return formula;
  }
}
