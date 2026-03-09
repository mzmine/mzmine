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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DoubleMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetadataColumnNormalizationTypeParametersTest {

  @BeforeEach
  void setUpProject() {
    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());
  }

  @Test
  void checkParameterValuesPassesForNumericMetadataColumn() {
    final DoubleMetadataColumn concentration = new DoubleMetadataColumn("concentration");
    ProjectService.getMetadata().addColumn(concentration);
    final MetadataColumnNormalizationTypeParameters parameters = MetadataColumnNormalizationTypeParameters.create(
        concentration.getTitle());

    final List<String> errors = new ArrayList<>();
    final boolean valid = parameters.checkParameterValues(errors, false);

    assertTrue(valid);
    assertTrue(errors.isEmpty());
  }

  @Test
  void checkParameterValuesFailsForMissingMetadataColumn() {
    final MetadataColumnNormalizationTypeParameters parameters = MetadataColumnNormalizationTypeParameters.create(
        "missing_column");

    final List<String> errors = new ArrayList<>();
    final boolean valid = parameters.checkParameterValues(errors, false);

    assertFalse(valid);
    assertTrue(errors.stream()
        .anyMatch(m -> m.contains("missing_column") && m.contains("does not exist")));
  }

  @Test
  void checkParameterValuesFailsForNonNumericMetadataColumn() {
    final StringMetadataColumn groupColumn = new StringMetadataColumn("sample_group");
    ProjectService.getMetadata().addColumn(groupColumn);
    final MetadataColumnNormalizationTypeParameters parameters = MetadataColumnNormalizationTypeParameters.create(
        groupColumn.getTitle());

    final List<String> errors = new ArrayList<>();
    final boolean valid = parameters.checkParameterValues(errors, false);

    assertFalse(valid);
    assertTrue(errors.stream().anyMatch(m -> m.contains("must be numeric")));
  }

  @Test
  void checkParameterValuesSkipsMetadataChecksWhenRequested() {
    final MetadataColumnNormalizationTypeParameters parameters = MetadataColumnNormalizationTypeParameters.create(
        "missing_column");

    final List<String> errors = new ArrayList<>();
    final boolean valid = parameters.checkParameterValues(errors, true);

    assertTrue(valid);
    assertTrue(errors.isEmpty());
  }
}
