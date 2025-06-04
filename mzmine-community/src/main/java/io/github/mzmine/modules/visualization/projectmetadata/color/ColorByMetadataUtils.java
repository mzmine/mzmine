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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.modules.visualization.projectmetadata.MetadataColumnDoesNotExistException;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;

public class ColorByMetadataUtils {

  /**
   * Groups the files by the value in the metadata column. If raw data files have no mapping (null)
   * they will be put into a group at first index. Sorted by doubleValue ascending.
   *
   * @return If the column is null, a list with the input files is returned. If the column is not in
   * the table, an error is thrown. If values are null - the first entry will be a list for all null
   * value entries. Each list has at least 1 element.
   * @throws MetadataColumnDoesNotExistException If the column does not exist. Does not throw if the
   *                                             column is null.
   */
  public static @NotNull ColorByMetadataResults colorByColumn(
      final @Nullable MetadataColumn<?> column, final @NotNull List<RawDataFile> raws) {
    return colorByColumn(column, raws, ColorByMetadataConfig.createDefault());
  }

  /**
   * Groups the files by the value in the metadata column. If raw data files have no mapping (null)
   * they will be put into a group at first index. Sorted by doubleValue ascending.
   *
   * @return If the column is null, a list with the input files is returned. If the column is not in
   * the table, an error is thrown. If values are null - the first entry will be a list for all null
   * value entries. Each list has at least 1 element.
   * @throws MetadataColumnDoesNotExistException If the column does not exist. Does not throw if the
   *                                             column is null.
   */
  public static @NotNull ColorByMetadataResults colorByColumn(
      final @Nullable MetadataColumn<?> column, final @NotNull List<RawDataFile> raws,
      final @NotNull ColorByMetadataConfig config) {
    var groups = ProjectService.getMetadata().groupFilesByColumnIncludeNull(raws, column);

    if (config.isUseGradient(groups.size()) && column != null && column.hasNaturalOrder()) {
      // create paint scale gradient
      final double min = groups.getFirst().doubleValue();
      final double max = Math.max(groups.getLast().doubleValue(), Math.nextUp(min));

      final PaintScale paintScale = config.cloneResetTransformPalette()
          .toPaintScale(config.transform(), Range.closed(min, max));
      final List<ColorByMetadataGroup> coloredGroups = groups.stream().map(
          group -> new ColorByMetadataGroup(group,
              FxColorUtil.awtColorToFX(paintScale.getPaint(group.doubleValue())), column)).toList();

      return new ColorByMetadataResults(coloredGroups, paintScale, true, config);
    } else {
      // color as categories
      final SimpleColorPalette colors = config.cloneResetCategoryPalette();
      final List<ColorByMetadataGroup> coloredGroups = groups.stream()
          .map(group -> new ColorByMetadataGroup(group, colors.getNextColor(), column)).toList();
      final PaintScale paintScale = createPaintScale(coloredGroups);
      return new ColorByMetadataResults(coloredGroups, paintScale, false, config);
    }
  }

  /**
   * Creates paint scale from groups that are already sorted by doubleValue
   *
   * @param groups sorted groups
   * @return paint scale with all groups
   */
  public static PaintScale createPaintScale(List<ColorByMetadataGroup> groups) {
    // already sorted by doubleValue
    final double min = groups.getFirst().doubleValue();
    final double max = groups.getLast().doubleValue();

    // should never be visible as all colors are used
    Color defaultColor = Color.RED;

    // upper end is excluded so need to add a tiny fraction to the upper bound
    var paintScale = new LookupPaintScale(min, Math.nextUp(max), defaultColor);
    for (ColorByMetadataGroup group : groups) {
      paintScale.add(group.doubleValue(), group.colorAWT());
    }
    return paintScale;
  }
}
