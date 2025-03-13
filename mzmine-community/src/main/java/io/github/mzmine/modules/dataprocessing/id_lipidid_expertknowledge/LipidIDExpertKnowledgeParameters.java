package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge;

import io.github.mzmine.javafx.components.factories.ArticleReferences;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationParameterSetupDialog;
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

public class LipidIDExpertKnowledgeParameters extends SimpleParameterSet {

    public static final FeatureListsParameter featureLists = new FeatureListsParameter();

    //TODO this depends on the tool, make it parametrizable, i think this is the default
    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
            ToleranceType.SCAN_TO_SCAN, 0.002, 10);

    public LipidIDExpertKnowledgeParameters() {
        super(new Parameter[]{featureLists, mzTolerance});
    }

    @Override
    public ExitCode showSetupDialog(boolean valueCheckRequired) {
        assert Platform.isFxApplicationThread();
        //TODO dejar esto!?
        final Region message = FxTextFlows.newTextFlowInAccordion("Authors Note",
                boldText("Blanca Pueche Granados:\n"),
                text("This module was created as part of the authors' Bachelor Thesis project."));

        ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
        dialog.showAndWait();
        return dialog.getExitCode();
    }
}
