/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.modules.tools.fraggraphdashboard.FragDashboardController;
import java.util.logging.Logger;
import javafx.scene.layout.Region;

public class FragWindow {

  private static final Logger logger = Logger.getLogger(FragWindow.class.getName());

  private static final double[] caffeineMzs = new double[]{42.03426, 69.04623, 83.06062, 108.05574,
      110.06946, 122.07037, 123.04293, 138.06653, 194.49059, 195.08994};
  private static final double[] caffeineIntensities = new double[]{26.287885, 7.195664, 3.313599,
      1.74958, 24.935937, 1.416748, 7.110247, 93.499455, 1.125151, 100};

  private static final MassList caffeineSpectrum = new SimpleMassList(null, caffeineMzs,
      caffeineIntensities);

  private static FragDashboardController controller;

  public static Region testWindow() {

    controller = new FragDashboardController();
    final Region region = controller.buildView();

    controller.setInput(195.08994, caffeineSpectrum,
        new SimpleMassSpectrum(new double[0], new double[0]));

    return region;
  }
}
