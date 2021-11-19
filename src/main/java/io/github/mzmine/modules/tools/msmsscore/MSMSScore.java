/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.tools.msmsscore;

import java.util.Map;

/**
 * Wrapper class for a score of MS/MS evaluation, with a mapping from MS/MS data points to
 * interpreted formulas
 */
public class MSMSScore {

  private final float score;
  private final Map<Double, String> annotation;

  public MSMSScore(float score, Map<Double, String> annotation) {
    this.score = score;
    this.annotation = annotation;
  }

  public float getScore() {
    return score;
  }

  public Map<Double, String> getAnnotation() {
    return annotation;
  }

}
