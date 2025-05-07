package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge;

import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.MobilePhaseParameter;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.MobilePhases;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.SampleTypeParameter;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.SampleTypes;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;
import javafx.scene.layout.Region;

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
     * Sample types the user can choose from.
     * Only one must be chosen.
     */
    public static final SampleTypeParameter<Object> sampleTypeParameter = new SampleTypeParameter(
            "Sample types", "Selection of sample type", SampleTypes.getListOfSampleTypes().toArray());

    /**
     * Created a new LipidIDExpertKnowledgeParameter object with the specified info.
     */
    public LipidIDExpertKnowledgeParameters() {
        super(new Parameter[]{featureLists, mzTolerance, mobilePhaseParameter, sampleTypeParameter});
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
                text("This module requires Lipid Annotation to be run first."));

        ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);

        dialog.showAndWait();
        return dialog.getExitCode();
    }
}
