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

package net.sf.mzmine.util.spectraldb.entry;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.util.maths.similarity.spectra.SpectraSimilarity;

public class SpectralDBPeakIdentity extends SimplePeakIdentity {
  private static final DecimalFormat COS_FORM = new DecimalFormat("0.000");

  private final SpectralDBEntry entry;
  private final SpectraSimilarity similarity;

  private Scan queryScan;
  private String massListName;

  public SpectralDBPeakIdentity(Scan queryScan, String massListName, SpectralDBEntry entry,
      SpectraSimilarity similarity, String method) {
    super(
        MessageFormat.format("{0} as {3} ({1}) {2} cos={4}",
            entry.getField(DBEntryField.NAME).orElse("NONAME"), // Name
            entry.getField(DBEntryField.MZ).orElse(""), // precursor m/z
            entry.getField(DBEntryField.FORMULA).orElse(""), // molevular formula
            entry.getField(DBEntryField.IONTYPE).orElse(""), // Ion type
            COS_FORM.format(similarity.getCosine())), // cosine similarity
        entry.getField(DBEntryField.FORMULA).orElse("").toString(), method, "", "");
    this.entry = entry;
    this.similarity = similarity;
    this.queryScan = queryScan;
    this.massListName = massListName;
  }

  public SpectralDBEntry getEntry() {
    return entry;
  }

  public SpectraSimilarity getSimilarity() {
    return similarity;
  }

  public Scan getQueryScan() {
    return queryScan;
  }

  public String getMassListName() {
    return massListName;
  }

  public DataPoint[] getQueryDataPoints() {
    if (massListName == null || queryScan == null || queryScan.getMassList(massListName) == null)
      return null;
    return queryScan.getMassList(massListName).getDataPoints();
  }

  public DataPoint[] getLibraryDataPoints(DataPointsTag tag) {
    switch (tag) {
      case ORIGINAL:
        return entry.getDataPoints();
      case FILTERED:
        return similarity.getLibrary();
      case ALIGNED:
        return similarity.getAligned()[0];
      case MERGED:
        return new DataPoint[0];
    }
    return new DataPoint[0];
  }

  public DataPoint[] getQueryDataPoints(DataPointsTag tag) {
    switch (tag) {
      case ORIGINAL:
        DataPoint[] dp = getQueryDataPoints();
        return dp == null ? new DataPoint[0] : getQueryDataPoints();
      case FILTERED:
        return similarity.getQuery();
      case ALIGNED:
        return similarity.getAligned()[1];
      case MERGED:
        return new DataPoint[0];
    }
    return new DataPoint[0];
  }

}
