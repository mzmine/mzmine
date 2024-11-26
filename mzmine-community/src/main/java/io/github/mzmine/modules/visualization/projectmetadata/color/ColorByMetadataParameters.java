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

package io.github.mzmine.modules.visualization.projectmetadata.color;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.mzio.mzmine.datamodel.parameters.ParameterSet;
import io.mzio.mzmine.datamodel.parameters.impl.OptionalParameter;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ColorByMetadataParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawFiles = new RawDataFilesParameter(
      new RawDataFilesSelection(RawDataFilesSelectionType.ALL_FILES));

  public static final OptionalParameter<MetadataGroupingParameter> colorByColumn = new OptionalParameter<>(
      new MetadataGroupingParameter());

  // use a medium range so that colors are not to light or dark
  public static final PercentParameter brightnessPercentRange = new PercentParameter(
      "Brightness range", "Scale brightness +-range/2 around the colors original brightness", 0.5,
      0d, 1d);

  public static final BooleanParameter separateBlankQcs = new BooleanParameter(
      "Separate blanks & QCs", "Separate coloring for blanks and QCs", true);

  public static final BooleanParameter applySorting = new BooleanParameter("Sort data files",
      "Sort raw data files", true);


  public ColorByMetadataParameters() {
    super(rawFiles, colorByColumn, brightnessPercentRange, separateBlankQcs, applySorting);
  }

  public static ParameterSet createDefault(final @NotNull List<RawDataFile> selected) {
    ParameterSet params = new ColorByMetadataParameters().cloneParameterSet();
    params.setParameter(rawFiles, new RawDataFilesSelection(selected.toArray(RawDataFile[]::new)));
    params.setParameter(colorByColumn, false);
    params.setParameter(brightnessPercentRange, 0.4);
    params.setParameter(separateBlankQcs, true);
    params.setParameter(applySorting, true);
    return params;
  }
}
