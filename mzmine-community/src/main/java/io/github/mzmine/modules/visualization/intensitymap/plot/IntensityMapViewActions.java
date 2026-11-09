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

package io.github.mzmine.modules.visualization.intensitymap.plot;

import java.util.function.Consumer;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;

/**
 * Actions of the toolbar buttons.
 *
 * @param resetView      fit the default view
 * @param topView        camera preset from the top
 * @param frontView      camera preset from the front
 * @param sideView       camera preset from the side
 * @param saveImage      save the view as image
 * @param plotBackground set the plot background, null for the one of the theme
 */
record IntensityMapViewActions(@NotNull Runnable resetView, @NotNull Runnable topView,
                               @NotNull Runnable frontView, @NotNull Runnable sideView,
                               @NotNull Runnable saveImage,
                               @NotNull Consumer<Color> plotBackground) {

}
