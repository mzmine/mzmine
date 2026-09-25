/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.surface3d.plot;

import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DAxisKind;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DBounds;
import org.jetbrains.annotations.NotNull;

/**
 * Everything an axes instance needs to rebuild itself, so that zoom-dependent tick counts can be
 * regenerated and several tiles can share one description.
 */
record Surface3DAxesSpec(@NotNull Surface3DBounds bounds, @NotNull String xLabel,
                         @NotNull String yLabel, @NotNull Surface3DAxisKind xKind,
                         @NotNull Surface3DAxisKind yKind, @NotNull String intensityLabel,
                         double intensityMaximum, double intensityBaseline,
                         @NotNull PaintScaleTransform transform,
                         @NotNull Surface3DFormat format) {

}
