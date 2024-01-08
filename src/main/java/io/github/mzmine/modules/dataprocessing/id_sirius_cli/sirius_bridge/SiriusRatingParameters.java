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

package io.github.mzmine.modules.dataprocessing.id_sirius_cli.sirius_bridge;

import io.github.mzmine.modules.io.export_features_sirius.SiriusExportParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.util.ExitCode;
import java.util.List;
import javafx.application.Platform;
import javafx.stage.FileChooser.ExtensionFilter;

public class SiriusRatingParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flist = new FeatureListsParameter(1);

  public static final ParameterSetParameter<ParameterSet> siriusExportParam = new ParameterSetParameter<>(
      "Sirius export parameters", "Parameters for the Sirius export module.",
      new SiriusExportParameters(true).cloneParameterSet());

  public static final FileNameParameter siriusPath = new FileNameParameter("sirius.exe path",
      "The path to the sirius.exe", List.of(new ExtensionFilter("executable", "*.exe")),
      FileSelectionType.OPEN);

  public static final DirectoryParameter siriusProject = new DirectoryParameter(
      "Output directory (Sirius project)", "The directory for the generated sirius project.");

  public static final ComboParameter<AlgorithmProfile> presets = new ComboParameter<>(
      "Sirius algorithm preset", "Selects default values for the sirius algorithm presets.",
      AlgorithmProfile.values(), AlgorithmProfile.QTOF);

  public static final ComboParameter<SiriusDatabaseType> databases = new ComboParameter<>(
      "Compound databases", """
      Select compound databases to search in.
      ALL_SIRIUS: all default sirius databases.
      ONLY_COMPOUNDS: only compound database matches.
      COMBINED: combines all other options. 
      """, SiriusDatabaseType.values(), SiriusDatabaseType.ALL_SIRIUS);

  public SiriusRatingParameters() {
    super(new Parameter[]{flist, siriusExportParam, siriusPath, siriusProject, presets, databases});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }

    var dialog = new ParameterSetupDialog(valueCheckRequired, this, """
        Please cite the following publications when using our tool:<br>
        <br>
        When using the SIRIUS Software please cite the following paper:<br>
        <br>
        Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Alexander A. Aksenov, Alexey V. Melnik, Marvin Meusel, Pieter C. Dorrestein, Juho Rousu and Sebastian Böcker
        SIRIUS4: a rapid tool for turning tandem mass spectra into metabolite structure information<br>
        Nat Methods, 16, 2019.  https://doi.org/10.1038/s41592-019-0344-8<br>
        <br>
        <br> 
        Depending on the tools you have used please also cite:<br>
        <br>        
        Kai Dührkop, Louis-Felix Nothias, Markus Fleischauer, Raphael Reher, Marcus Ludwig, Martin A. Hoffmann, Daniel Petras, William H. Gerwick, Juho Rousu, Pieter C. Dorrestein and Sebastian Böcker
        Systematic classification of unknown metabolites using high-resolution fragmentation mass spectra<br>
        Nature Biotechnology, 2020.  https://doi.org/10.1038/s41587-020-0740-8<br>
        (Cite if you are using: CANOPUS)<br>
        <br>
        Yannick Djoumbou Feunang, Roman Eisner, Craig Knox, Leonid Chepelev, Janna Hastings, Gareth Owen, Eoin Fahy, Christoph Steinbeck, Shankar Subramanian, Evan Bolton, Russell Greiner, David S. Wishart
        ClassyFire: automated chemical classification with a comprehensive, computable taxonomy<br>
        J Cheminf, 8, 2016.  https://doi.org/10.1186/s13321-016-0174-y<br>
        (Cite if you are using: CANOPUS)<br>
        <br>
        Kai Dührkop, Huibin Shen, Marvin Meusel, Juho Rousu and Sebastian Böcker
        Searching molecular structure databases with tandem mass spectra using CSI:FingerID<br>
        Proc Natl Acad Sci U S A, 112, 2015.  https://doi.org/10.1073/pnas.1509788112<br>
        (Cite if you are using: CSI:FingerID)<br>
        <br>
        Martin A. Hoffmann and Louis-Felix Nothias and Marcus Ludwig and Markus Fleischauer and Emily C. Gentry and Michael Witting and Pieter C. Dorrestein and Kai Dührkop and Sebastian Böcker
        High-confidence structural annotation of metabolites absent from spectral libraries<br>
        Nature Biotechnology, 2021.  https://doi.org/10.1038/s41587-021-01045-9<br>
        (Cite if you are using: CSI:FingerID, COSMIC)<br>
                <br>
        Sebastian Böcker and Kai Dührkop<br>
        Fragmentation trees reloaded<br>
        J Cheminform, 8, 2016.  https://doi.org/10.1186/s13321-016-0116-8<br>
        (Cite if you are using: Fragmentation Trees)<br>
        <br>
        Sebastian Böcker, Matthias Letzel, Zsuzsanna Lipt�k and Anton Pervukhin
        SIRIUS: Decomposing isotope patterns for metabolite identification
        Bioinformatics, 25, 2009.  https://doi.org/10.1093/bioinformatics/btn603
        (Cite if you are using: Isotope Pattern analysis)
        <br>
        Marcus Ludwig, Louis-Felix Nothias, Kai Dührkop, Irina Koester, Markus Fleischauer, Martin A. Hoffmann, Daniel Petras, Fernando Vargas, Mustafa Morsy, Lihini Aluwihare, Pieter C. Dorrestein, Sebastian Böcker
        ZODIAC: database-independent molecular formula annotation using Gibbs sampling reveals unknown small molecules<br>
        bioRxiv, 2019.  https://doi.org/10.1101/842740<br>
        (Cite if you are using: ZODIAC)<br>
        """);

    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
