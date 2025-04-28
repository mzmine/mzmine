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

package io.github.mzmine.modules.dataanalysis.rowsboxplot;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.modules.visualization.projectmetadata.color.ColorByMetadataGroup;
import io.github.mzmine.modules.visualization.projectmetadata.color.ColorByMetadataResults;
import io.github.mzmine.modules.visualization.projectmetadata.color.ColorByMetadataUtils;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

public class RowBoxPlotDataset extends DefaultBoxAndWhiskerCategoryDataset {

  public RowBoxPlotDataset(FeatureListRow row, @Nullable MetadataColumn<?> groupingColumn,
      AbundanceMeasure abundance) {

    if (groupingColumn == null) { // no grouping, so just a single group
      final List<Float> values = row.getFeatures().stream().map(abundance::get)
          .filter(Objects::nonNull).toList();
      add(values, 0, row.toString());
      return;
    }

    // use the same method for grouping like ColorByMetadataTask and PCAScoresProvider
    // TODO where is the color of this plot / dataset/ renderer defined? it should use the colors of the groups
    // for now it is correct as it uses standard colors
    final List<RawDataFile> files = row.getFeatureList().getRawDataFiles();
    final ColorByMetadataResults grouping = ColorByMetadataUtils.colorByColumn(groupingColumn,
        files);

    for (ColorByMetadataGroup group : grouping.groups()) {
      final String groupName = group.valueString();
      final List<Float> values = group.files().stream()
          .map(file -> abundance.get((ModularFeature) row.getFeature(file)))
          .filter(Objects::nonNull).toList();
      add(values, groupName, "%s %s".formatted(row.toString(), row.getPreferredAnnotationName()));
    }
  }

}
