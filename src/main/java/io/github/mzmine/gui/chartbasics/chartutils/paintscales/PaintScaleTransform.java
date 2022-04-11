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

package io.github.mzmine.gui.chartbasics.chartutils.paintscales;

import java.util.function.DoubleFunction;

public enum PaintScaleTransform {
  LINEAR(d -> d, d -> d),
  LOG10(Math::log10, d -> Math.pow(10, d)),
  LOG2(val -> Math.log(val) / Math.log(2), d -> Math.pow(2, d)),
  SQRT(Math::sqrt, d -> Math.pow(d, 2));

  private final DoubleFunction<Double> transform;
  private final DoubleFunction<Double> revertTransform;

  PaintScaleTransform(DoubleFunction<Double> transform, DoubleFunction<Double> revertTransform) {
    this.transform = transform;
    this.revertTransform = revertTransform;
  }

  public double transform(double value) {
    return transform.apply(value);
  }

  public double revertTransform(double transformedValue) {
    return revertTransform.apply(transformedValue);
  }
}
