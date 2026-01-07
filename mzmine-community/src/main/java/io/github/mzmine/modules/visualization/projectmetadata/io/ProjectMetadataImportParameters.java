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

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.util.files.ExtensionFilters;
import java.io.File;
import java.util.Map;

public class ProjectMetadataImportParameters extends SimpleParameterSet {

  public static final FileNameParameter fileName = new FileNameParameter("Metadata file", """
      CSV or TSV file with metadata. See exact format by opening metadata table and
      exporting metadata file (after importing a few data files).""",
      ExtensionFilters.CSV_TSV_IMPORT, FileSelectionType.OPEN);

  public static final BooleanParameter skipErrorColumns = new BooleanParameter(
      "Skip column on error",
      "Error during data conversion or parsing will be logged but does not end the import", false);

  public static final BooleanParameter removeAttributePrefix = new BooleanParameter(
      "Remove attribute prefix", "Remove the ATTRIBUTE_ prefix commonly used by GNPS", false);


  public ProjectMetadataImportParameters() {
    super(fileName, skipErrorColumns, removeAttributePrefix);
  }

  public static ParameterSet create(final File file, final boolean skipErrorCol,
      final boolean removeGnpsAttributePrefix) {
    ParameterSet params = new ProjectMetadataImportParameters().cloneParameterSet();
    params.setParameter(fileName, file);
    params.setParameter(skipErrorColumns, skipErrorCol);
    params.setParameter(removeAttributePrefix, removeGnpsAttributePrefix);
    return params;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    var map = super.getNameParameterMap();
    map.put("File names", getParameter(fileName));
    return map;
  }

}
