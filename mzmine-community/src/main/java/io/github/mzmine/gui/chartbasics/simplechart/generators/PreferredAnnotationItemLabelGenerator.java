/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.gui.chartbasics.simplechart.generators;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * Generic item label generator for chart datasets that expose {@link FeatureListRow} as item
 * objects via {@link XYItemObjectProvider}.
 */
public class PreferredAnnotationItemLabelGenerator implements XYItemLabelGenerator {

  @Override
  public @Nullable String generateLabel(final @NotNull XYDataset dataset, final int series,
      final int item) {
    if (!(dataset instanceof XYItemObjectProvider<?> itemObjectProvider)) {
      return null;
    }

    final Object itemObject = itemObjectProvider.getItemObject(item);
    if (!(itemObject instanceof FeatureListRow row)) {
      return null;
    }

    final FeatureAnnotation annotation = row.getPreferredAnnotation();
    return annotation == null ? null : annotation.getCompoundName();
  }
}
