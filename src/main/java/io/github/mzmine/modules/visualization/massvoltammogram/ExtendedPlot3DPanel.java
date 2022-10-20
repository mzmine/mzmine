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

package io.github.mzmine.modules.visualization.massvoltammogram;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.math.plot.Plot3DPanel;

/**
 * Class used to extend the existing Plot3DPanel by functions to export the massvoltammogram and to
 * implement the new toolbar.
 */
public class ExtendedPlot3DPanel extends Plot3DPanel {

  private final ExtendedPlotToolBar extendedPlotToolBar;

  /**
   * Data for later export. Thereby every scan needed for the massvoltammogram is represented by a
   * multidimensional array. Each scans datapoints are represented as single
   * arrays of the multidimensional array. In every singe array the mz-value is stored at index 0,
   * the corresponding intensity-value at index 1 and the voltage-value at index 2.
   */
  private List<double[][]> rawScans;
  private List<double[][]> rawScansInMzRange;

  public ExtendedPlot3DPanel() {
    removePlotToolBar();
    extendedPlotToolBar = new ExtendedPlotToolBar(this);
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

  /**
   * Method to get the plots ExtendedToolBar to integrate it into the MassvoltammogramTasks
   * toolbar.
   *
   * @return The plots toolbar.
   */
  public ExtendedPlotToolBar getExtendedPlotToolBar() {
    return extendedPlotToolBar;
  }

  /**
   * Method to add the data to the plot to be able to change the mz-range later.
   */
  public void addRawScans(List<double[][]> rawScans) {
    this.rawScans = rawScans;
  }

  /**
   * Method to add the data to the plot for later export.
   */
  public void addRawScansInMzRange(List<double[][]> rawScansInMzRange) {
    this.rawScansInMzRange = rawScansInMzRange;
  }

  /**
   * Method to get the data from the plot to edit the mz-range.
   *
   * @return All raw scans as lists of multidimensional arrays needed to draw the whole
   * massvoltammogram.
   */
  public List<double[][]> getRawScans() {
    return rawScans;
  }

  /**
   * Method to get the data to export it.
   *
   * @return The raw data of the currently drawn massvoltammogram.
   */
  public List<double[][]> getRawScansInMzRange() {
    return rawScansInMzRange;
  }
}
