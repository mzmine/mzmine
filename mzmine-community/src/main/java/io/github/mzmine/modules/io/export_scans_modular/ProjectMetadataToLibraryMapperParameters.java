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

package io.github.mzmine.modules.io.export_scans_modular;

import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.WrongParameterConfigException;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingParameter;
import io.github.mzmine.project.ProjectService;

public class ProjectMetadataToLibraryMapperParameters extends SimpleParameterSet {

  public static final OptionalParameter<MetadataGroupingParameter> compoundNameFromSampleMetadata = new OptionalParameter<>(
      new MetadataGroupingParameter("Compound name from metadata column", """
          A metadata column title. Uses project sample metadata as compound names. Use the project metadata to define names in a column.
          This way a sample may be defined as a blank of a specific type. Then all spectra are exported with this name to link to potential contamination sources. 
          This is useful when exporting unknown spectra from blanks with a name like 'well plates from XY company'""",
          MetadataColumn.FILENAME_HEADER));

  public static final OptionalParameter<MetadataGroupingParameter> descriptionFromSampleMetadata = new OptionalParameter<>(
      new MetadataGroupingParameter("Description from metadata column", """
          A metadata column title. Uses project sample metadata as entry description. Use the project metadata to define the description entered in a column.
          This way a sample may be defined as a blank of a specific type. Then all spectra are exported with this description to link to potential contamination sources. 
          This is useful when exporting unknown spectra from blanks with a description like 'well plates from XY company'"""));


  public ProjectMetadataToLibraryMapperParameters() {
    super(compoundNameFromSampleMetadata, descriptionFromSampleMetadata);
  }

  public ProjectMetadataToLibraryEntryMapper createMapper() throws WrongParameterConfigException {
    final String compoundNameFromSampleMetadata = this.getEmbeddedParameterValueIfSelectedOrElse(
        ProjectMetadataToLibraryMapperParameters.compoundNameFromSampleMetadata, null);
    final String descriptionFromSampleMetadata = this.getEmbeddedParameterValueIfSelectedOrElse(
        ProjectMetadataToLibraryMapperParameters.descriptionFromSampleMetadata, null);

    final var metadata = ProjectService.getMetadata();
    MetadataColumn<?> nameColumn = null;
    MetadataColumn<?> descriptionColumn = null;
    if (compoundNameFromSampleMetadata != null) {
      nameColumn = metadata.getColumnByName(compoundNameFromSampleMetadata);
      if (nameColumn == null) {
        throw new WrongParameterConfigException(
            "Could not find project metadata column. Import metadata first. Column name: "
            + compoundNameFromSampleMetadata);
      }
    }
    if (descriptionFromSampleMetadata != null) {
      descriptionColumn = metadata.getColumnByName(descriptionFromSampleMetadata);
      if (descriptionColumn == null) {
        throw new WrongParameterConfigException(
            "Could not find project metadata column. Import metadata first. Column name: "
            + descriptionFromSampleMetadata);
      }
    }
    return new ProjectMetadataToLibraryEntryMapper(nameColumn, descriptionColumn);
  }

}
