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


package io.github.mzmine.modules.dataprocessing.featdet_masscalibration.errormodeling;

import com.google.common.math.Stats;

import java.util.List;

/**
 * Class containing methods estimating distribution bias
 * <p>
 * used with distributions of measurement errors to estimate systematic error of the measurements
 * <p>
 * for instance: after extracting a certain substantial subset of all mass measurement PPM errors,
 * the measured mass spectrum bias can be estimated by taking the arithmetic mean of considered errors
 */
public class BiasEstimator {
  /**
   * Returns arithmetic mean of a distribution of items
   * when the list of items is empty, zero is returned
   * (conveniently, as this method is used to estimate bias from a distribution of errors)
   *
   * @param items
   * @return
   */
  public static double arithmeticMean(List<Double> items) {
    if (items.size() == 0) {
      return 0;
    }
    return Stats.meanOf(items);
  }
}
