package io.github.mzmine.modules.dataprocessing.featuretest;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.customguicomponents.ProcessingComponent;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;

public class TestModule implements MZmineProcessingModule {
    @Override
    public @NotNull String getName() {
        String name = "Test module";
        return name;
    }

    @Override
    public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
        return TestParameters.class;
    }

    @Override
    public @NotNull String getDescription() {
        return "";
    }

    @Override
    public @NotNull ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
         var flists = parameters.getValue(TestParameters.featurelists).getMatchingFeatureLists();
        for (ModularFeatureList flist : flists) {
            TestTask task = new TestTask(flist, parameters, moduleCallDate);
            tasks.add(task);
        }

        return ExitCode.OK;
    }

    @Override
    public @NotNull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.FEATURELISTFILTERING;
    }
}
