/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFUtils;
import java.util.List;

public class UnloadedTdfMobilityScanMassList extends UnloadedTdfMobilityScan implements MassList {

  protected final double noiseLevel;

  public UnloadedTdfMobilityScanMassList(Frame frame, int mobilityScanIndex, double noiseLevel,
      TDFUtils utils) {
    super(frame, mobilityScanIndex, utils);
    this.noiseLevel = noiseLevel;
  }

  public UnloadedTdfMobilityScanMassList(Frame frame, int mobilityScanIndex, double[] mzs,
      double[] intensities, double noiseLevel) {
    super(frame, mobilityScanIndex, mzs, intensities);
    this.noiseLevel = noiseLevel;
  }

  @Override
  protected void loadRawData() {
    final TDFUtils tdfUtils =
        utils != null ? utils : ((TdfImsRawDataFileImpl) frame.getDataFile()).getTdfUtils();

    final List<double[][]> doubles = tdfUtils.loadDataPointsForFrame(frame.getFrameId(),
        mobilityScanIndex, (long) mobilityScanIndex + 1);
    double[][] mzIntensities = MZmineCore.getModuleInstance(CentroidMassDetector.class)
        .getMassValues(doubles.get(0)[0], doubles.get(0)[1], noiseLevel);
    mzs = mzIntensities[0];
    intensities = mzIntensities[1];
    numberOfDataPoints = mzs.length;
  }
}
