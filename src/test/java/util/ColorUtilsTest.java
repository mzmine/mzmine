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

package util;

import io.github.mzmine.util.color.ColorUtils;
import io.github.mzmine.util.color.ColorsFX;
import io.github.mzmine.util.color.Vision;
import java.util.logging.Logger;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ColorUtilsTest {

  private static final Logger logger = Logger.getLogger(ColorUtilsTest.class.getName());

  @Test
  void testDistance() {
    ColorsFX.getSevenColorPalette(Vision.DEUTERANOPIA, true);
    final var orange = new Color(0.902f, 0.624f, 0f, 1f); // orange
    final var brightBlue = new Color(0.337f, 0.706f, 0.914f, 1f); // sky blue
    final var green = new Color(0.f, 0.620f, 0.451f, 1f); // bluish green
    final var yellow = new Color(0.941f, 0.894f, 0.259f, 1f); // yellow
    final var darkBlue = new Color(0.f, 0.447f, 0.698f, 1f); // blue
    final var red = new Color(0.835f, 0.369f, 0.f, 1f); // vermillion (darker orange)
    final var pink = new Color(0.800f, 0.475f, 0.655f, 1f);

    Assertions.assertEquals(133.22658710091355d, ColorUtils.getColorDifference(orange, red));
    Assertions.assertEquals(205.02990544415744d,
        ColorUtils.getColorDifference(brightBlue, darkBlue));
    Assertions.assertEquals(264.08099643825886d,
        ColorUtils.getColorDifference(darkBlue, Color.BLUE));
    Assertions.assertEquals(409.5452849092953d, ColorUtils.getColorDifference(green, yellow));
    Assertions.assertEquals(764.8339663572415d,
        ColorUtils.getColorDifference(Color.BLACK, Color.WHITE));
  }

}
