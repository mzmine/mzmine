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
package io.github.mzmine.modules.dataprocessing.align_path.functions;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.ParameterSet;

public interface ScoreCalculator {

  /**
   *
   * @param path
   * @param peak
   * @param params
   * @return
   */
  double calculateScore(AlignmentPath path, FeatureListRow peak, ParameterSet params);

  double getWorstScore();

  /**
   * Is score calculated by calculate Score in any way meaningful? If path and peak don't match in
   * ScoreCalculator's mind, value returned from calculateScore may still be finite, but matches
   * returns false.
   * 
   * @param path
   * @param peak
   * @param params
   * @return
   */
  boolean matches(AlignmentPath path, FeatureListRow peak, ParameterSet params);

  boolean isValid(FeatureListRow peak);

  String name();
}
