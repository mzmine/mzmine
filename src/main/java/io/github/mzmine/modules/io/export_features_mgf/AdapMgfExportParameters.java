/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_features_mgf;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * Exports a feature cluster to mgf. Used for GC-GNPS
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */
public class AdapMgfExportParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("mgf Mascot file (spectra)", "*.mgf") //
  );

  /**
   * Defines the representative m/z value for a cluster
   *
   * @author Robin Schmid (robinschmid@uni-muenster.de)
   */
  public static enum MzMode {
    AS_IN_FEATURE_TABLE("As in feature table"), HIGHEST_MZ("Highest m/z"), MAX_INTENSITY(
        "Max. intensity");

    private final String name;

    MzMode(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final FileNameParameter FILENAME = new FileNameParameter("Filename",
      "Name of the output MGF file. "
      + "Use pattern \"{}\" in the file name to substitute with feature list name. "
      + "(i.e. \"blah{}blah.mgf\" would become \"blahSourceFeatureListNameblah.mgf\"). "
      + "If the file already exists, it will be overwritten.",
      extensions, FileSelectionType.SAVE);

  public static final BooleanParameter FRACTIONAL_MZ = new BooleanParameter("Fractional m/z values",
      "If checked, write fractional m/z values", false);

  public static final ComboParameter<IntegerMode> ROUND_MODE = new ComboParameter<IntegerMode>(
      "Integer m/z", "Merging mode for fractional m/z to unit mass", IntegerMode.values());

  public static final ComboParameter<MzMode> REPRESENTATIVE_MZ =
      new ComboParameter<AdapMgfExportParameters.MzMode>("Representative m/z",
          "Choose the representative m/z of a cluster.",
          FXCollections.observableArrayList(MzMode.values()), MzMode.AS_IN_FEATURE_TABLE);

  public AdapMgfExportParameters() {
    super(new Parameter[] {FEATURE_LISTS, FILENAME, REPRESENTATIVE_MZ, FRACTIONAL_MZ, ROUND_MODE});
  }
}
