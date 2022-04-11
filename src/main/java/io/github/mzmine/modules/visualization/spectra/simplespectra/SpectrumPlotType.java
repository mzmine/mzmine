/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra;

import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.Scan;

/**
 * Used in the {@link SpectraPlot} to determine if a continuous or centroid renderer is used.
 *
 * @author SteffenHeu https://github.com/SteffenHeu / steffen.heuckeroth@uni-muenster.de
 */
public enum SpectrumPlotType {
  AUTO, CENTROID, PROFILE;

  public static SpectrumPlotType fromScan(Scan scan) {
    if (scan.getSpectrumType() == MassSpectrumType.CENTROIDED) {
      return SpectrumPlotType.CENTROID;
    } else {
      return SpectrumPlotType.PROFILE;
    }
  }

  public static SpectrumPlotType fromMassSpectrumType(MassSpectrumType type) {
    if (type == MassSpectrumType.CENTROIDED) {
      return SpectrumPlotType.CENTROID;
    } else {
      return SpectrumPlotType.PROFILE;
    }
  }
}
