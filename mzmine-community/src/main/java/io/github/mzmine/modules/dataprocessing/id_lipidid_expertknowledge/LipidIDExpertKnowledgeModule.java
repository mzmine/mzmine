package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LipidIDExpertKnowledgeModule implements MZmineProcessingModule{

    @Override
    public @NotNull String getName() {
        return "Lipid Annotation Expert Knowledge";
    }

    @Override
    public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
        return LipidIDExpertKnowledgeParameters.class;
    }

    @Override
    public @NotNull String getDescription() {
        return "Lipid Annotation Expert Knowledge is only applied on MS1 data, and it uses theoretical expert knowledge on common adducts found in lipids. It requires the Lipid Annotation to be run first.";
    }

    //TODO set the LipidAnnotation as a mandatory step before using this tool
    @Override
    public @NotNull ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
        FeatureList[] featureLists = parameters.getParameter(parameters.getParameter(LipidIDExpertKnowledgeParameters.featureLists)).getValue().getMatchingFeatureLists();
        for (FeatureList fL : featureLists){
            tasks.add(new LipidIDExpertKnowledgeTask(parameters, fL, moduleCallDate));
        }
        return ExitCode.OK;
    }

    @Override
    public @NotNull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.ANNOTATION;
    }
}
