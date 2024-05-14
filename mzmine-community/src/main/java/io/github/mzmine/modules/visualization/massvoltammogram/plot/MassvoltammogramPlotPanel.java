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

package io.github.mzmine.modules.visualization.massvoltammogram.plot;

import io.github.mzmine.modules.visualization.massvoltammogram.utils.Massvoltammogram;
import java.awt.Color;
import java.awt.Font;
import org.math.plot.Plot3DPanel;

/**
 * Class used to extend the existing Plot3DPanel by functions to export the massvoltammograms data
 * and to implement the new toolbar.
 */
public class MassvoltammogramPlotPanel extends Plot3DPanel {

  //The toolbar.
  private final MassvoltammogramToolBar massvoltammogramToolBar;

  //Exchanging the plots toolbar fot the new toolbar on initialization.
  public MassvoltammogramPlotPanel(Massvoltammogram massvoltammogram) {

    //Removing the original plot toolbar and exchanging it for the MassvoltammogramToolBar.
    removePlotToolBar();
    massvoltammogramToolBar = new MassvoltammogramToolBar(this, massvoltammogram);

    //Setting the font of the axis labels.
    Font axisLabelFont = new Font("Arial", Font.PLAIN, 14);
    getAxis(0).setLabelFont(axisLabelFont);
    getAxis(1).setLabelFont(axisLabelFont);
    getAxis(2).setLabelFont(axisLabelFont);

    //Setting the font of the axis tick marks.
    Font tickmarkFont = new Font("Arial", Font.PLAIN, 12);
    getAxis(0).setLightLabelFont(tickmarkFont);
    getAxis(1).setLightLabelFont(tickmarkFont);
    getAxis(2).setLightLabelFont(tickmarkFont);
  }

  /**
   * @return Returns the plots MassvoltammogramToolBar.
   */
  public MassvoltammogramToolBar getMassvoltammogramToolBar() {
    return massvoltammogramToolBar;
  }

  /**
   * Sets the plots background transparent.
   *
   * @param bgTransparent True sets the background transparent, false sets it back to white.
   */
  public void setBackgroundTransparent(boolean bgTransparent) {
    if (bgTransparent) {
      plotCanvas.setBackground(new Color(0f, 0f, 0f, 0f));

    } else {
      plotCanvas.setBackground(Color.white);
    }
  }

  /**
   * Sets the intensity axis bounds to fixed values
   *
   * @param minValue The intensity axis min value.
   * @param maxValue The intensity axis max value.
   */
  public void scaleIntensityAxis(double minValue, double maxValue) {

    setFixedBounds(2, minValue, maxValue);
  }
}
