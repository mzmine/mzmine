/*
 * Copyright 2006-2022 The MZmine Development Team
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
 */

package io.github.mzmine.modules.visualization.massvoltammogram.plot;

import io.github.mzmine.modules.visualization.massvoltammogram.utils.PlotData;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.math.plot.Plot3DPanel;

/**
 * Class used to extend the existing Plot3DPanel by functions to export the massvoltammograms data
 * and to implement the new toolbar.
 */
public class ExtendedPlot3DPanel extends Plot3DPanel {

  private PlotData plotData;
  private final ExtendedPlotToolBar extendedPlotToolBar;

  //Exchanging the plots toolbar fot the new toolbar on initialization.
  public ExtendedPlot3DPanel() {
    removePlotToolBar();
    extendedPlotToolBar = new ExtendedPlotToolBar(this);
  }

  public ExtendedPlotToolBar getExtendedPlotToolBar() {
    return extendedPlotToolBar;
  }

  public void addPlotData(PlotData plotData) {
    this.plotData = plotData;
  }

  public PlotData getPlotData() {
    return plotData;
  }

  //Extending the png export function to work with the extended plot toolbar.
  @Override
  public void toGraphicFile(File file) throws IOException {
    super.toGraphicFile(file);

    //Extracting the buffered frame as an image.
    Image image = createImage(getWidth(), getHeight());
    paint(image.getGraphics());
    image = new ImageIcon(image).getImage();

    BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
        BufferedImage.TYPE_INT_RGB);
    Graphics g = bufferedImage.createGraphics();
    g.drawImage(image, 0, 0, Color.WHITE, null);
    g.dispose();

    //saving the buffered image to a png file.
    try {
      ImageIO.write(bufferedImage, "PNG", file);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
    }
  }
}
