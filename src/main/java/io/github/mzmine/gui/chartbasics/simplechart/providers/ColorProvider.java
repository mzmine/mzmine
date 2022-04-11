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
