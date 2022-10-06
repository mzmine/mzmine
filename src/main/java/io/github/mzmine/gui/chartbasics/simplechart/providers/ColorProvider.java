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

package io.github.mzmine.gui.chartbasics.simplechart.providers;

import io.github.mzmine.datamodel.RawDataFile;
import org.jetbrains.annotations.NotNull;

/**
 * The methods in this interface are used to set the <b>initial</b> dataset color. Note that the
 * dataset color is not bound to the original value. Therefore the color of the dataset can be
 * changed in the plot for better visualisation, without altering the color of e.g. the raw data
 * file (see {@link RawDataFile#getColorAWT()}
 *
 * @author https://github.com/SteffenHeu
 * @see ExampleXYProvider
 */
public interface ColorProvider {

  @NotNull
  public java.awt.Color getAWTColor();

  @NotNull
  public javafx.scene.paint.Color getFXColor();

}
