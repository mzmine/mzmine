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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.ImagingFrame;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.io.import_rawdata_imzml.Coordinates;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class ImagingUtils {

  /**
   * @param scan A {@link MergedMsMsSpectrum}
   * @return A list of all MaldiSpotInfos associated with this MS2 spectrum. May be multiple if the
   * MS2 was merged from several events.
   */
  @NotNull
  public static Map<MaldiSpotInfo, MsMsInfo> getMsMsSpotInfosFromScan(Scan scan) {
    if (scan instanceof MergedMsMsSpectrum merged) {
      final List<MassSpectrum> sourceSpectra = merged.getSourceSpectra();
      return sourceSpectra.stream().filter(s -> s instanceof MobilityScan)
          .map(s -> ((MobilityScan) s)).collect(
              Collectors.toMap(s -> ((ImagingFrame) (s.getFrame())).getMaldiSpotInfo(),
                  s -> s.getMsMsInfo(), (msMsInfo, msMsInfo2) -> msMsInfo));
    }
    return Map.of();
  }

  public static double[] transformCoordinates(MaldiSpotInfo info, ImagingRawDataFile rawDataFile) {
    final String spotName = info.spotName();
    final ImagingScan matchingScan = (ImagingScan) rawDataFile.getScans().stream().filter(
            scan -> scan instanceof ImagingScan is && is.getMaldiSpotInfo().spotName().equals(spotName))
        .findFirst().orElse(null);

    if (matchingScan == null) {
      return null;
    }

    final ImagingParameters imagingParam = rawDataFile.getImagingParam();
    if (imagingParam == null) {
      return null;
    }

    final double height = imagingParam.getLateralHeight() / imagingParam.getMaxNumberOfPixelY();
    final double width = imagingParam.getLateralWidth() / imagingParam.getMaxNumberOfPixelX();

    final Coordinates scanCoord = matchingScan.getCoordinates();
    return scanCoord != null ? new double[]{scanCoord.getX() * width, scanCoord.getY() * height}
        : null;
  }
}
