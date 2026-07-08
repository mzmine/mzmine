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

package io.github.mzmine.modules.visualization.projectmetadata.io;

import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboComponent;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameterComponent;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.files.ExtensionFilters;
import java.io.File;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectMetadataExportParameters extends SimpleParameterSet {

  private static final double SETUP_DIALOG_WIDTH = 750;

  public enum MetadataFileFormat {
    DEFAULT,
    /**
     * Adds additional headers for data types
     */
    MZMINE_INTERNAL,
    /**
     * GNPS requires Filename and then all other columns with ATTRIBUTE_ prefix
     */
    GNPS,
    /**
     * MassDynamics export
     */
    MASS_DYNAMICS;

    @Override
    public String toString() {
      return switch (this) {
        case DEFAULT -> "Default";
        case MZMINE_INTERNAL -> "mzmine internal";
        case GNPS -> "GNPS";
        case MASS_DYNAMICS -> "MassDynamics";
      };
    }
  }

  public static final List<ProjectMetadataColumnMapping> MASS_DYNAMICS_DEFAULT_MAPPINGS = List.of(
      new ProjectMetadataColumnMapping(MetadataColumn.SAMPLE_TYPE_HEADER, "condition", ""));

  public static final FileNameSuffixExportParameter fileName = new FileNameSuffixExportParameter(
      "Metadata file", """
      CSV or TSV file to save metadata to.""", ExtensionFilters.CSV_TSV_EXPORT, "metadata");

  public static final ComboParameter<MetadataFileFormat> format = new ComboParameter<>("Format",
      "Format to export the metadata in", MetadataFileFormat.values(), MetadataFileFormat.DEFAULT);

  public static final OptionalParameter<ProjectMetadataColumnMappingsParameter> columnMappings = new OptionalParameter<>(
      new ProjectMetadataColumnMappingsParameter(), false);

  public ProjectMetadataExportParameters() {
    super(fileName, format, columnMappings);
  }

  @Override
  public @NotNull ExitCode showSetupDialog(final boolean valueCheckRequired) {
    final ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this);
    // parameter needs more space
    dialog.setMinWidth(SETUP_DIALOG_WIDTH);
    dialog.setWidth(SETUP_DIALOG_WIDTH);
    final ComboComponent<MetadataFileFormat> formatComponent = dialog.getComponentForParameter(
        format);
    final OptionalParameterComponent<?> mappingsOptionalComponent = dialog.getComponentForParameter(
        columnMappings);

    // when selecting mass dynamics preset then set default mapping to condition column
    if (formatComponent != null && mappingsOptionalComponent != null) {
      formatComponent.valueProperty()
          .subscribe(newFormat -> applyMassDynamicsDefaults(newFormat, mappingsOptionalComponent));
      applyMassDynamicsDefaults(formatComponent.getValue(), mappingsOptionalComponent);
    }

    dialog.showAndWait();
    return dialog.getExitCode();
  }

  /**
   * MassDynamics export needs condition column
   */
  private void applyMassDynamicsDefaults(@Nullable final MetadataFileFormat selectedFormat,
      @NotNull final OptionalParameterComponent<?> mappingsOptionalComponent) {
    if (selectedFormat != MetadataFileFormat.MASS_DYNAMICS) {
      return;
    }
    if (!(mappingsOptionalComponent.getEmbeddedComponent() instanceof ProjectMetadataColumnMappingsComponent mappingsComponent)) {
      return;
    }

    mappingsOptionalComponent.setSelected(true);
    if (!mappingsComponent.hasActiveMappings()) {
      mappingsComponent.setValue(MASS_DYNAMICS_DEFAULT_MAPPINGS);
    }
  }

  public static @NotNull ParameterSet create(final @NotNull File file, final boolean appendSuffix,
      final @NotNull MetadataFileFormat mFormat) {
    final ParameterSet params = new ProjectMetadataExportParameters().cloneParameterSet();
    if (appendSuffix) {
      params.getParameter(fileName).setValueAppendSuffix(file, null);
    } else {
      params.setParameter(fileName, file);
    }
    params.setParameter(format, mFormat);
    params.setParameter(columnMappings, false, List.of());
    return params;
  }

}
