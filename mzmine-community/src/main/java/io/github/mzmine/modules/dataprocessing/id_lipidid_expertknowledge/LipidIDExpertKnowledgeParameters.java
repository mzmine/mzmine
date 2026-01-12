package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge;

import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.MobilePhaseParameter;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.MobilePhases;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.SampleTypeParameter;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.SampleTypes;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;
import javafx.scene.layout.Region;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.FileChooser;
import java.util.List;

import static io.github.mzmine.javafx.components.factories.FxTexts.*;

/**
 * Parameters needed for Lipid Validation module.
 *
 * @author Blanca Pueche Granados (blancapueche@gmail.com)
 */
public class LipidIDExpertKnowledgeParameters extends SimpleParameterSet {

    /**
     * Feature list the user wants to run the module over.
     */
    public static final FeatureListsParameter featureLists = new FeatureListsParameter();

    /**
     * m/z tolerance set by the user.
     * By default it is set to 0.002 or 10 ppm.
     */
    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
            ToleranceType.SCAN_TO_SCAN, 0.002, 10);

    /**
     * Mobile phases the user can choose from.
     * None and more than one option can be chosen.
     */
    public static final MobilePhaseParameter<Object> mobilePhaseParameter = new MobilePhaseParameter(
            "Mobile phases", "Selection of mobile phases", MobilePhases.getListOfMobilePhases().toArray());

    /**
     * User-selected TXT mobile phases files.
     */
    public static final FileNamesParameter mobilePhasesFiles = new FileNamesParameter(
            "TXT mobile phases files",
            "Mobile phases files (.txt) for lipid expert knowledge",
            List.of(new ExtensionFilter("Mobile phases (*.txt)", "*.txt"))
    );

    /**
     * Sample types the user can choose from.
     * Only one must be chosen.
     */
    public static final SampleTypeParameter<Object> sampleTypeParameter = new SampleTypeParameter(
            "Sample types", "Selection of sample type", SampleTypes.getListOfSampleTypes().toArray());

    /**
     * Sample types the user want to use if it's not in the options.
     */
    public static final StringParameter sampleTypeOption = new StringParameter("Sample type",
            "This string is the sample type chosen when the one we want to use is not in the options.", " ", false );

    /**
     * User-selected DRL rule files.
     */
    public static final FileNamesParameter drlFiles = new FileNamesParameter(
            "DRL files",
            "Drools rule files (.drl) for lipid expert knowledge",
            List.of(new ExtensionFilter("Drools rules (*.drl)", "*.drl"))
    );

    /**
     * User-selected TXT adduct files.
     */
    public static final FileNamesParameter adductFiles = new FileNamesParameter(
            "TXT adduct files",
            "Adduct files (.txt) for lipid expert knowledge",
            List.of(new ExtensionFilter("Adducts (*.txt)", "*.txt"))
    );



    /**
     * Created a new LipidIDExpertKnowledgeParameter object with the specified info.
     */
    public LipidIDExpertKnowledgeParameters() {
        super(new Parameter[]{featureLists, mzTolerance, mobilePhaseParameter, mobilePhasesFiles, sampleTypeParameter, sampleTypeOption, drlFiles, adductFiles});
    }

    /**
     * Creates the setup dialog in the GUI.
     * @param valueCheckRequired Input boolean parameter.
     * @return ExitCode when exit.
     */
    @Override
    public ExitCode showSetupDialog(boolean valueCheckRequired) {
        assert Platform.isFxApplicationThread();
        final Region message = FxTextFlows.newTextFlowInAccordion("Authors Note",
                text("This module requires Lipid Annotation to be run first."),
                text(" It takes in .txt files with additional mobile phases and adducts (see README for file structure). You may also provide one or more Drools rule files (.drl)."));

        ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);

        dialog.showAndWait();
        return dialog.getExitCode();
    }
}
