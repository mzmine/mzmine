package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Main module class for Lipid Validation.
 *
 * @author Blanca Pueche Granados (blancapueche@gmail.com)
 */
public class LipidIDExpertKnowledgeModule implements MZmineProcessingModule {

    /**
     * Name of the module.
     *
     * @return The module name.
     */
    @Override
    public @NotNull String getName() {
        return "Lipid Validation";
    }

    /**
     * Gets the parameters class.
     *
     * @return The parameter set class.
     */
    @Override
    public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
        return LipidIDExpertKnowledgeParameters.class;
    }

    /**
     * Description of the module.
     *
     * @return The module description.
     */
    @Override
    public @NotNull String getDescription() {
        return "Lipid Validation is only applied on MS1 data, and it uses theoretical expert knowledge to validate lipid annotation.";
    }

    /**
     * Executes the module, creating the parameters and the task.
     *
     * @param project        Project to apply this module on.
     * @param parameters     ParameterSet to invoke this module with. The ParameterSet has already been
     *                       cloned for exclusive use by this module, therefore the module does not need to clone it
     *                       again. Upon invocation of the runModule() method it is guaranteed that the ParameterSet
     *                       is of the proper class as returned by getParameterSetClass(). Also, it is guaranteed
     *                       that the ParameterSet is checked by checkParameters(), therefore the module does not
     *                       need to perform these checks again.
     * @param tasks          A collection where the module should add its newly created Tasks, if it creates
     *                       any.
     * @param moduleCallDate
     * @return ExitCode
     */
    @Override
    public @NotNull ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
        FeatureList[] featureLists = parameters.getParameter(parameters.getParameter(LipidIDExpertKnowledgeParameters.featureLists)).getValue().getMatchingFeatureLists();

        try {
            for (FeatureList fL : featureLists) {
                tasks.add(new LipidIDExpertKnowledgeTask(parameters, fL, moduleCallDate));
            }
        }catch (IOException ex){
            System.out.println(ex);
        }
        return ExitCode.OK;
    }

    /**
     * Gets th category of the module.
     *
     * @return The MZmineMoudleCategory, set to "annotation".
     */
    @Override
    public @NotNull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.ANNOTATION;
    }
}
