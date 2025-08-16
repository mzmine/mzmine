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

package io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules;

import static io.github.mzmine.javafx.components.factories.FxTexts.linebreak;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.javafx.components.factories.ArticleReferences;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClassParameter;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClassesProvider;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClassParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

/**
 * Parameters for lipid annotation module
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidAnnotationParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final LipidClassParameter<Object> lipidClasses = new LipidClassParameter<>(
      "Lipid classes", "Selection of lipid backbones",
      LipidClassesProvider.getListOfAllLipidClasses().toArray());

  public static final ParameterSetParameter<LipidAnnotationChainParameters> lipidChainParameters = new ParameterSetParameter<>(
      "Side chain parameters", "Optionally modify lipid chain parameters",
      new LipidAnnotationChainParameters());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      "m/z tolerance MS1 level:",
      "Enter m/z tolerance for exact mass database matching on MS1 level", 0.005, 5);

  public static final OptionalModuleParameter<LipidAnnotationMSMSParameters> searchForMSMSFragments = new OptionalModuleParameter<>(
      "Search for lipid class specific fragments in MS/MS spectra",
      "Search for lipid class specific fragments in MS/MS spectra",
      new LipidAnnotationMSMSParameters(), true);

  public static final OptionalModuleParameter<CustomLipidClassParameters> customLipidClasses = new OptionalModuleParameter<>(
      "Search for custom lipid class",
      "If checked the algorithm searches for custom, by the user defined lipid classes",
      new CustomLipidClassParameters(), false);

  public static final AdvancedParametersParameter<AdvancedLipidAnnotationParameters> advanced = new AdvancedParametersParameter<>(
      new AdvancedLipidAnnotationParameters());

  public LipidAnnotationParameters() {
    super(new Parameter[]{featureLists, lipidClasses, lipidChainParameters, mzTolerance,
            searchForMSMSFragments, customLipidClasses, advanced},
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_lipid_annotation/lipid-annotation.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    final Region message = FxTextFlows.newTextFlowInAccordion("How to cite",
        text("When using the Lipid annotation module please cite:"), linebreak(),
        ArticleReferences.LIPIDANNOTATION.hyperlinkText(), linebreak(),
        text("When using custom lipid classes please cite:"), linebreak(),
        ArticleReferences.CUSTOMLIPIDCLASSES.hyperlinkText());
    LipidAnnotationParameterSetupDialog dialog = new LipidAnnotationParameterSetupDialog(
        valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public int getVersion() {
    return 2;
  }
}
