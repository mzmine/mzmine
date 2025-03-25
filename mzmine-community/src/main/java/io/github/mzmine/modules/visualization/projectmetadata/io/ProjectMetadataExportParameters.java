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

package io.github.mzmine.modules.visualization.projectmetadata.io;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.util.files.ExtensionFilters;
import java.io.File;

public class ProjectMetadataExportParameters extends SimpleParameterSet {

  public enum MetadataFileFormat {
    DEFAULT,
    /**
     * Adds additional headers for data types
     */
    MZMINE_INTERNAL,
    /**
     * GNPS requires Filename and then all other columns with ATTRIBUTE_ prefix
     */
    GNPS;

    @Override
    public String toString() {
      return switch (this) {
        case DEFAULT -> "Default";
        case MZMINE_INTERNAL -> "mzmine internal";
        case GNPS -> "GNPS";
      };
    }
  }

  public static final FileNameSuffixExportParameter fileName = new FileNameSuffixExportParameter(
      "Metadata file", """
      CSV or TSV file to save metadata to.""", ExtensionFilters.CSV_TSV_EXPORT, "metadata");

  public static final ComboParameter<MetadataFileFormat> format = new ComboParameter<>("Format",
      "Format to export the metadata in", MetadataFileFormat.values(), MetadataFileFormat.DEFAULT);

  public ProjectMetadataExportParameters() {
    super(fileName, format);
  }

  public static ParameterSet create(final File file, final boolean appendSuffix,
      final MetadataFileFormat mFormat) {
    ParameterSet params = new ProjectMetadataExportParameters().cloneParameterSet();
    if (appendSuffix) {
      params.getParameter(fileName).setValueAppendSuffix(file, null);
    } else {
      params.setParameter(fileName, file);
    }
    params.setParameter(format, mFormat);
    return params;
  }

}
