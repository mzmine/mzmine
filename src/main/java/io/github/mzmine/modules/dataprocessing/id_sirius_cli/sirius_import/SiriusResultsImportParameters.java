package io.github.mzmine.modules.dataprocessing.id_sirius_cli.sirius_import;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;

public class SiriusResultsImportParameters extends SimpleParameterSet {

  public static final DirectoryParameter projectDir = new DirectoryParameter(
      "Sirius project directory", "The directory to the sirius project.", "");

  public static final FeatureListsParameter flist = new FeatureListsParameter(1, 1);

  public static final ComboParameter<ImportOption> importOption = new ComboParameter<ImportOption>(
      "Results import", "Specify how much results shall be imported per compound.",
      ImportOption.values(), ImportOption.BEST);

  public static final BooleanParameter replaceOldAnnotations = new BooleanParameter(
      "Replace old annotations", "Replaces current compound annotations by the sirius annotations",
      false);

  public SiriusResultsImportParameters() {
    super(new Parameter[]{projectDir, flist, importOption, replaceOldAnnotations});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }

    var dialog = new ParameterSetupDialog(valueCheckRequired, this, """
        Please cite the following publications when using our tool:
                
        When using the SIRIUS Software please cite the following paper:
                
        Kai D�hrkop, Markus Fleischauer, Marcus Ludwig, Alexander A. Aksenov, Alexey V. Melnik, Marvin Meusel, Pieter C. Dorrestein, Juho Rousu and Sebastian B�cker
        SIRIUS4: a rapid tool for turning tandem mass spectra into metabolite structure information
        Nat Methods, 16, 2019.  https://doi.org/10.1038/s41592-019-0344-8
                
                
        Depending on the tools you have used please also cite:
                
        Kai D�hrkop, Louis-F�lix Nothias, Markus Fleischauer, Raphael Reher, Marcus Ludwig, Martin A. Hoffmann, Daniel Petras, William H. Gerwick, Juho Rousu, Pieter C. Dorrestein and Sebastian B�cker
        Systematic classification of unknown metabolites using high-resolution fragmentation mass spectra
        Nature Biotechnology, 2020.  https://doi.org/10.1038/s41587-020-0740-8
        (Cite if you are using: CANOPUS)
                
        Yannick Djoumbou Feunang, Roman Eisner, Craig Knox, Leonid Chepelev, Janna Hastings, Gareth Owen, Eoin Fahy, Christoph Steinbeck, Shankar Subramanian, Evan Bolton, Russell Greiner, David S. Wishart
        ClassyFire: automated chemical classification with a comprehensive, computable taxonomy
        J Cheminf, 8, 2016.  https://doi.org/10.1186/s13321-016-0174-y
        (Cite if you are using: CANOPUS)
                
        Kai D�hrkop, Huibin Shen, Marvin Meusel, Juho Rousu and Sebastian B�cker
        Searching molecular structure databases with tandem mass spectra using CSI:FingerID
        Proc Natl Acad Sci U S A, 112, 2015.  https://doi.org/10.1073/pnas.1509788112
        (Cite if you are using: CSI:FingerID)
                
        Martin A. Hoffmann and Louis-F�lix Nothias and Marcus Ludwig and Markus Fleischauer and Emily C. Gentry and Michael Witting and Pieter C. Dorrestein and Kai D�hrkop and Sebastian B�cker
        High-confidence structural annotation of metabolites absent from spectral libraries
        Nature Biotechnology, 2021.  https://doi.org/10.1038/s41587-021-01045-9
        (Cite if you are using: CSI:FingerID, COSMIC)
                
        Sebastian B�cker and Kai D�hrkop
        Fragmentation trees reloaded
        J Cheminform, 8, 2016.  https://doi.org/10.1186/s13321-016-0116-8
        (Cite if you are using: Fragmentation Trees)
                
        Sebastian B�cker, Matthias Letzel, Zsuzsanna Lipt�k and Anton Pervukhin
        SIRIUS: Decomposing isotope patterns for metabolite identification
        Bioinformatics, 25, 2009.  https://doi.org/10.1093/bioinformatics/btn603
        (Cite if you are using: Isotope Pattern analysis)
                
        Marcus Ludwig, Louis-F�lix Nothias, Kai D�hrkop, Irina Koester, Markus Fleischauer, Martin A. Hoffmann, Daniel Petras, Fernando Vargas, Mustafa Morsy, Lihini Aluwihare, Pieter C. Dorrestein, Sebastian B�cker
        ZODIAC: database-independent molecular formula annotation using Gibbs sampling reveals unknown small molecules
        bioRxiv, 2019.  https://doi.org/10.1101/842740
        (Cite if you are using: ZODIAC)
        """);

    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
