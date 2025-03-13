/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.projectmetadata;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class SampleTypeFilter {

  private final EnumSet<SampleType> types;

  public SampleTypeFilter(@NotNull final Collection<SampleType> types) {
    this.types = types.isEmpty() ? EnumSet.noneOf(SampleType.class)
        : EnumSet.copyOf(types); // copy, list might change content
  }

  public static SampleTypeFilter all() {
    return new SampleTypeFilter(List.of(SampleType.values()));
  }

  public static SampleTypeFilter of(@NotNull final SampleType... types) {
    return new SampleTypeFilter(List.of(types));
  }

  public static SampleTypeFilter of(@NotNull final List<SampleType> types) {
    return new SampleTypeFilter(types);
  }

  public static SampleTypeFilter of(@NotNull final SampleType type) {
    return new SampleTypeFilter(List.of(type));
  }

  public static SampleTypeFilter qc() {
    return of(SampleType.QC);
  }

  public static SampleTypeFilter blank() {
    return of(SampleType.BLANK);
  }

  public static SampleTypeFilter sample() {
    return of(SampleType.SAMPLE);
  }

  public static SampleTypeFilter calibration() {
    return of(SampleType.CALIBRATION);
  }

  public boolean matches(final SampleType type) {
    return types.contains(type);
  }

  /**
   * Checks if the sample type metadata column of this file matches the filter. if the column does
   * not exist, tries to parse the file name.
   *
   * @param file
   * @return
   */
  public boolean matches(RawDataFile file) {
    final MetadataTable metadata = ProjectService.getProjectManager().getCurrentProject()
        .getProjectMetadata();
    final MetadataColumn<String> metadataColumn = (MetadataColumn<String>) metadata.getColumnByName(
        MetadataColumn.SAMPLE_TYPE_HEADER);
    final SampleType type;
    if (metadataColumn != null) {
      final String sampleType = metadata.getValue(metadataColumn, file);
      type = SampleType.ofString(sampleType);
    } else {
      type = SampleType.ofString(file.getName());
    }

    return matches(type);
  }

  /**
   * Filters a list of rows to contain only rows that contain at least one feature of the sample
   * types described by this filter.
   */
  public List<FeatureListRow> filter(final List<FeatureListRow> rows) {
    return rows.stream().filter(row -> row.streamFeatures().anyMatch(this::matches)).toList();
  }

  /**
   * Filters a list of raw data files to those described by this filter.
   */
  public List<RawDataFile> filterFiles(final List<RawDataFile> raws) {
    return raws.stream().filter(this::matches).toList();
  }

  public boolean matches(final Feature feature) {
    return matches(feature.getRawDataFile());
  }

  public boolean isEmpty() {
    return types.isEmpty();
  }

  /**
   * @return Immutable copy of the types allowed by this filter.
   */
  public EnumSet<SampleType> getTypes() {
    return EnumSet.copyOf(types);
  }
}
