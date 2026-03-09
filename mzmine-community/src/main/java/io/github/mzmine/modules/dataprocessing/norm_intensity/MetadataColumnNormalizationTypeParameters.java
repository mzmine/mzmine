/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataColumnParameters.AvailableTypes;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingParameter;
import io.github.mzmine.project.ProjectService;
import java.util.Collection;
import java.util.stream.Collectors;

public class MetadataColumnNormalizationTypeParameters extends SimpleParameterSet {

  public static final MetadataGroupingParameter metadataColumn = new MetadataGroupingParameter(
      "Metadata column",
      "Select numeric metadata values used to normalize each raw file. Each data file must have a value in that column. Use 0 to disable normalization for that file.",
      AvailableTypes.NUMBER);

  public MetadataColumnNormalizationTypeParameters() {
    super(metadataColumn);
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {
    final boolean superCheck = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);

    if (skipRawDataAndFeatureListParameters) {
      return superCheck;
    }

    final MetadataColumn<?> column = ProjectService.getMetadata()
        .getColumnByName(getValue(metadataColumn));
    if (column == null) {
      errorMessages.add(
          "Metadata column %s does not exist. (columns = %s)".formatted(getValue(metadataColumn),
              ProjectService.getMetadata().getColumns().stream().map(MetadataColumn::getTitle)
                  .collect(Collectors.joining(", "))));
    }
    if (column != null && column.getType() != AvailableTypes.NUMBER) {
      errorMessages.add("Metadata column does  must be numeric.");
    }

    return superCheck && errorMessages.isEmpty();
  }
}
