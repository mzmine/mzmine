/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.projectmetadata.color;

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.renderer.PaintScale;

public record ColorByMetadataResults(List<ColorByMetadataGroup> groups, PaintScale paintScale,
                                     boolean isGradient, ColorByMetadataConfig config) {

  public int numFiles() {
    return groups.stream().mapToInt(ColorByMetadataGroup::size).sum();
  }

  public int size() {
    return groups.size();
  }

  public ColorByMetadataGroup get(int groupIndex) {
    return groups.get(groupIndex);
  }

  /**
   * @return the min double value to enable comparison. Date and numbers are converted and strings
   * usually just follow the group order as integers.
   */
  public double getMinValueDouble() {
    return groups.getFirst().doubleValue();
  }

  /**
   * @return the max double value to enable comparison. Date and numbers are converted and strings
   * usually just follow the group order as integers.
   */
  public double getMaxValueDouble() {
    return groups.getLast().doubleValue();
  }

  /**
   * Creates a color palette with the default palette but all colors defined by groups
   */
  @NotNull
  public SimpleColorPalette createColorPalette() {
    final SimpleColorPalette colors = ConfigService.getDefaultColorPalette().clone(true);
    colors.clear();
    for (ColorByMetadataGroup group : groups) {
      colors.add(group.color());
    }
    return colors;
  }
}
