/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_features_all_speclib_matches;

import io.github.mzmine.gui.chartbasics.graphicsexport.GraphicsExportParameters;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import org.jetbrains.annotations.NotNull;

public class ExportAllIdsGraphicalParameters extends SimpleParameterSet {

  public static final DirectoryParameter dir = new DirectoryParameter("Export directory",
      "Directory to export the spectral library matches to. Will automatically creat a sub directory for every feature list.");

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final IntegerParameter numMatches = new IntegerParameter(
      "Number of matches to export",
      "Set the number of matches to export. 1 will only export the best/selected match.", 1, 1,
      Integer.MAX_VALUE);

  public static final IntegerParameter dpiScalingFactor = new IntegerParameter("DPI scaling factor",
      "This will multiply the pixels exported in the png images.", 4, 1, Integer.MAX_VALUE);

  public static final BooleanParameter exportShape = new BooleanParameter("Export shapes",
      "Exports the feature shape for non-imaging feature rows.", false);

  public static final BooleanParameter exportMobilogram = new BooleanParameter("Export mobilograms",
      "Exports the mobilogram of IMS feature rows.", false);

  public static final BooleanParameter exportImages = new BooleanParameter("Export feature images",
      "Exports the extracted ion image of for imaging features.", false);

  public static final BooleanParameter exportLipidMatches = new BooleanParameter(
      "Export lipid matches", "Exports the lipid matches MS2 plots.", false);
  public static final BooleanParameter exportSpectralLibMatches = new BooleanParameter(
      "Export spectral library matches", "Exports the spectral library mirror plots.", true);

  public static final BooleanParameter exportPdf = new BooleanParameter("Export pdf", "", true);

  public static final BooleanParameter exportPng = new BooleanParameter(
      "Export png (⚠ freezes GUI)",
      "This process takes long and freezes the GUI until all matches are exported.", false);

  public static final ParameterSetParameter<GraphicsExportParameters> export = new ParameterSetParameter<>(
      "Chart export parameters", "Set the parameters for lipid charts.",
      new GraphicsExportParameters());

  public ExportAllIdsGraphicalParameters() {
    super(flists, dir, numMatches, dpiScalingFactor, exportSpectralLibMatches, exportLipidMatches,
        exportShape, exportMobilogram, exportImages, exportPdf, exportPng, export);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
