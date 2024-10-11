/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import static io.github.mzmine.javafx.components.factories.FxTexts.boldText;
import static io.github.mzmine.javafx.components.factories.FxTexts.hyperlinkText;
import static io.github.mzmine.javafx.components.factories.FxTexts.linebreak;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser.ExtensionFilter;

public class BioTransformerParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final FileNameParameter bioPath = new FileNameParameter("BioTransformer .jar path",
      "The path to the BioTransformer.jar.", List.of(new ExtensionFilter("java files", "*.jar")),
      FileSelectionType.OPEN);

  public static final ComboParameter<TransformationTypes> transformationType = new ComboParameter<>(
      "Transformation type", "The BioTransformer transformation type to use.",
      TransformationTypes.values(), TransformationTypes.env);

  public static final IntegerParameter steps = new IntegerParameter("Iterations", """
      The number of iterations to use for bio transformer.
      (Transformation of previous transformation products)
      """, 1, 1, 10);

  public static final MZToleranceParameter mzTol = new MZToleranceParameter(0.003, 5);

  public static final OptionalModuleParameter<BioTransformerFilterParameters> filterParam = new OptionalModuleParameter<>(
      "Filter parameters", "Additional filtering parameters.", new BioTransformerFilterParameters(),
      false);

  public static final ParameterSetParameter ionLibrary = new ParameterSetParameter("Ion library",
      "Potential ionizations of product molecules.", new IonLibraryParameterSet());

  public static final ComboParameter<SmilesSource> smilesSource = new ComboParameter<>(
      "SMILES source", """
      Select smiles from which annotation types shall be used for the prediction.
      Spectral library = Smiles from spectral library matches
      Compound DB = Smiles from compound database annotations
      All = All of the above.
      """, SmilesSource.values(), SmilesSource.ALL);

  public static final AdvancedParametersParameter<RtClusterFilterParameters> advanced = new AdvancedParametersParameter<>(
      new RtClusterFilterParameters());

  public BioTransformerParameters() {
    super(flists, bioPath, transformationType, steps, mzTol, ionLibrary, filterParam, smilesSource
        /*, cmdOptions*/, advanced);
  }

  public BioTransformerParameters(boolean singleRow) {
    super(singleRow ? new Parameter[]{bioPath, transformationType, steps, mzTol, ionLibrary,
        filterParam, advanced}
        : new Parameter[]{flists, bioPath, transformationType, steps, mzTol, ionLibrary,
            filterParam, advanced});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }
    final Region message = FxTextFlows.newTextFlowInAccordion("How to cite & download instructions",
        true, hyperlinkText("""
            Djoumbou Feunang Y, Fiamoncini J, de la Fuente AG, Manach C, Greiner R, and Wishart DS; BioTransformer: A Comprehensive Computational Tool for Small Molecule Metabolism Prediction and Metabolite Identification; Journal of Cheminformatics; 2019; Journal of Cheminformatics 11:2; 
            DOI: 10.1186/s13321-018-0324-5
            """, "https://jcheminf.biomedcentral.com/articles/10.1186/s13321-018-0324-5"),
        linebreak(), hyperlinkText("""
            Wishart DS, Tian S, Allen D, Oler E, Peters H, Lui VW, Gautam V, Djoumbou Feunang Y, Greiner R, Metz TO; BioTransformer 3.0 – A Web Server for Accurately Predicting Metabolic Transformation Products [Submitted in Nucleic Acids Research, Webserver Issue.Apr.2022]
            """, "http://biotransformer.ca/"), linebreak(), boldText("Download: "),
        hyperlinkText("here", "https://bitbucket.org/wishartlab/biotransformer3.0jar/downloads/"),
        text(" (unzip everything to a single folder) "),
        hyperlinkText("additional resources", "https://biotransformer.ca/download"));
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public int getVersion() {
    return 2;
  }

  public enum TransformationTypes {
    ecbased, cyp450, phaseii, hgut, allHuman, superbio, env;

    @Override
    public String toString() {
      return switch (this) {
        case ecbased -> "EC-based (Enzyme Commission)";
        case cyp450 -> "CYP450";
        case phaseii -> "Phase II";
        case hgut -> "Gut microbial";
        case allHuman -> "All human";
        case superbio -> "Super bio";
        case env -> "Environmental microbial";
      };
    }

    public String transformationName() {
      return switch (this) {
        case ecbased -> "ecbased";
        case cyp450 -> "cyp450";
        case phaseii -> "phaseii";
        case hgut -> "hgut";
        case allHuman -> "allHuman";
        case superbio -> "superbio";
        case env -> "env";
      };
    }
  }
}
