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
 * Parameters for Lipid Annotation Expert Knowledge module
 *
 * @author Blanca Pueche Granados (blancapueche@gmail.com)
 */
public class LipidIDExpertKnowledgeParameters extends SimpleParameterSet {

    public static final FeatureListsParameter featureLists = new FeatureListsParameter();

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
            ToleranceType.SCAN_TO_SCAN, 0.002, 10);

    public static final MobilePhaseParameter<Object> mobilePhaseParameter = new MobilePhaseParameter(
            "Mobile phases", "Selection of mobile phases", MobilePhases.getListOfMobilePhases().toArray());

    public static final SampleTypeParameter<Object> sampleTypeParameter = new SampleTypeParameter(
            "Sample types", "Selection of sample type", SampleTypes.getListOfMobilePhases().toArray());

    public LipidIDExpertKnowledgeParameters() {
        super(new Parameter[]{featureLists, mzTolerance, mobilePhaseParameter, sampleTypeParameter});
    }

    @Override
    public ExitCode showSetupDialog(boolean valueCheckRequired) {
        assert Platform.isFxApplicationThread();
        //TODO dejar esto!?
        final Region message = FxTextFlows.newTextFlowInAccordion("Authors Note",
                boldText("Blanca Pueche Granados:\n"),
                text("This module requires Lipid Annotation to be run first."));

        ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
        dialog.showAndWait();
        return dialog.getExitCode();
    }
}
