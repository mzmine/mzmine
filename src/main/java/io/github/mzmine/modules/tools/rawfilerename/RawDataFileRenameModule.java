package io.github.mzmine.modules.tools.rawfilerename;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RawDataFileRenameModule implements MZmineProcessingModule {

  @Override
  public @NotNull String getName() {
    return "Raw data file rename";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return RawDataFileRenameParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return "Renames raw data files.";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Date moduleCallDate) {
    final RawDataFile[] matchingRawDataFiles = parameters
        .getParameter(RawDataFileRenameParameters.files).getValue().getMatchingRawDataFiles();
    final String newName = parameters.getParameter(RawDataFileRenameParameters.newName).getValue();
    if (matchingRawDataFiles.length == 0) {
      return ExitCode.OK;
    }

    RawDataFile file = matchingRawDataFiles[0];
    Optional<RawDataFile> any = project.getRawDataFiles().stream()
        .filter(f -> f.getName().equals(newName)).findAny();

    if (any.isPresent() && !any.get().equals(file)) {
      MZmineCore.getDesktop().displayErrorMessage("File with name " + newName + " already exists");
      return ExitCode.OK;
    }

    MZmineCore.runLater(() -> file.setName(newName));
    file.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(this, parameters, moduleCallDate));

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.TOOLS;
  }

  public static void renameFile(RawDataFile file, String name) {
    ParameterSet moduleParameters = MZmineCore.getConfiguration()
        .getModuleParameters(RawDataFileRenameModule.class).cloneParameterSet();

    RawDataFilesSelection selection = new RawDataFilesSelection();
    selection.setSelectionType(RawDataFilesSelectionType.SPECIFIC_FILES);
    selection.setSpecificFiles(new RawDataFile[] {file});
    moduleParameters.getParameter(RawDataFileRenameParameters.files).setValue(selection);
    moduleParameters.setParameter(RawDataFileRenameParameters.newName, name);

    MZmineCore.runMZmineModule(RawDataFileRenameModule.class, moduleParameters);
  }
}
