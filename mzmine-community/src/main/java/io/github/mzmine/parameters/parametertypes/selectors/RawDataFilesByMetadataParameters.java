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

package io.github.mzmine.parameters.parametertypes.selectors;

import static io.github.mzmine.parameters.parametertypes.metadata.MetadataListGroupsSelection.NONE;
import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.modules.presets.ModulePreset;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataListGroupsSelection;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataListGroupsSelectionParameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RawDataFilesByMetadataParameters extends SimpleParameterSet {

  public static final MetadataListGroupsSelectionParameter included = new MetadataListGroupsSelectionParameter(
      "Include", """
      Include metadata group names in column. Enter multiple values, one in each text field.
      Exclude is stronger and will remove data files if excluded.""", NONE, false);
  public static final MetadataListGroupsSelectionParameter excluded = new MetadataListGroupsSelectionParameter(
      "Exclude", """
      Exclude metadata group names in column. Enter multiple values, one in each text field.
      Exclude is stronger than include and will remove data files if excluded.""", NONE, false);

  public RawDataFilesByMetadataParameters() {
    super(included, excluded);
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {
    final MetadataListGroupsSelection in = getValue(included);
    final MetadataListGroupsSelection ex = getValue(excluded);

    if (NONE.equals(in) && NONE.equals(ex)) {
      errorMessages.add("Metadata selection requires at least one inclusion or exclusion.");
      super.checkParameterValues(errorMessages, skipRawDataAndFeatureListParameters);
      return false;
    }

    return super.checkParameterValues(errorMessages, skipRawDataAndFeatureListParameters);
  }

  @Override
  public @NotNull List<ModulePreset> createDefaultPresets() {
    List<ModulePreset> presets = new ArrayList<>();

    final String groupId = RawDataFilesByMetadataModule.UNIQUE_ID;
    presets.add(new ModulePreset("All blanks", groupId, setAll(true,
        new MetadataListGroupsSelection(MetadataColumn.SAMPLE_TYPE_HEADER,
            List.of(SampleType.BLANK.toString())), null)));
    presets.add(new ModulePreset("All QCs", groupId, setAll(true,
        new MetadataListGroupsSelection(MetadataColumn.SAMPLE_TYPE_HEADER,
            List.of(SampleType.QC.toString())), null)));
    presets.add(
        new ModulePreset("All samples (excludes QC, blanks, and other sample types)", groupId,
            setAll(true, new MetadataListGroupsSelection(MetadataColumn.SAMPLE_TYPE_HEADER,
                List.of(SampleType.SAMPLE.toString())), null)));
    return presets;
  }

  private RawDataFilesByMetadataParameters setAll(boolean clone,
      @Nullable MetadataListGroupsSelection included,
      @Nullable MetadataListGroupsSelection excluded) {
    RawDataFilesByMetadataParameters p =
        clone ? (RawDataFilesByMetadataParameters) cloneParameterSet() : this;

    p.setParameter(RawDataFilesByMetadataParameters.included, requireNonNullElse(included, NONE));
    p.setParameter(RawDataFilesByMetadataParameters.excluded, requireNonNullElse(excluded, NONE));
    return p;
  }
}
