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

package net.sf.mzmine.modules.peaklistmethods.identification.localdbsearch;

import java.text.MessageFormat;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;

public class SpectralDBPeakIdentity extends SimplePeakIdentity {

  private final String name;
  private final double mz;
  private final double rt;
  private final String adduct;
  private final DataPoint[] dps;

  public SpectralDBPeakIdentity(String name, double mz, double rt, String adduct, String formula,
      String method, DataPoint[] dps) {
    super(MessageFormat.format("{0} ({1}) {2}", name, mz, formula), formula, method, "", "");
    this.name = name;
    this.mz = mz;
    this.rt = rt;
    this.adduct = adduct;
    this.dps = dps;
  }

  public String getCompoundName() {
    return name;
  }

  public double getMz() {
    return mz;
  }

  public String getAdduct() {
    return adduct;
  }

  public DataPoint[] getDataPoints() {
    return dps;
  }

  public double getRetentionTime() {
    return rt;
  }



}
