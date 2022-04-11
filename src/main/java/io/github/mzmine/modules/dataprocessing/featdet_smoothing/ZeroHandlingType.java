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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing;

import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import java.util.List;

/**
 * Defines how values that were previously zero shall be handled when returning the smoothed values.
 * Not currently used due to questions regarding the implementation. see {@link
 * SmoothingTask#createNewSeries(IntensitySeries, ModularFeature, double[], List)}
 */
public enum ZeroHandlingType {
  /**
   * Values that were previously zero will be zero after smoothing.
   */
  KEEP, //
  /**
   * Values that were previously zero might get an intensity if determined by the smoothing
   * algorithm.
   */
  OVERRIDE
}
