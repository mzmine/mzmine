package io.github.mzmine.modules.tools.siriusapi.modules.import_annotations;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.modules.tools.siriusapi.Sirius;
import io.github.mzmine.modules.tools.siriusapi.SiriusToMzmine;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.NotNull;

public class SiriusResultsImportTask extends AbstractTask {

  private final File siriusProject;
  @NotNull
  private final ParameterSet parameters;
  private final FeatureList flist;
  private long siriusFeatures = 0;
  private long importedFeatures = 0;

  protected SiriusResultsImportTask(@NotNull Instant moduleCallDate,
      @NotNull ParameterSet parameters, @NotNull FeatureList featureList) {
    super(moduleCallDate);
    this.parameters = parameters;
    this.flist = featureList;
    siriusProject = parameters.getValue(SiriusResultsImportParameters.sirius);
  }

  @Override
  public String getTaskDescription() {
    return "Importing annotations from Sirius project " + siriusProject.getName()
        + " for feature list " + flist.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return siriusFeatures != 0 ? importedFeatures / (double) siriusFeatures : 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (!process()) {
      return;
    }

    flist.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(SiriusResultsImportModule.class, parameters,
            getModuleCallDate()));
    setStatus(TaskStatus.FINISHED);
  }

  public boolean process() {
    try (Sirius sirius = new Sirius(siriusProject)) {

      final Map<FeatureListRow, String> rowToSiriusId = SiriusToMzmine.mapFeatureToSiriusId(sirius, flist.getRows());
      siriusFeatures = rowToSiriusId.size();

      for (Entry<FeatureListRow, String> entry : rowToSiriusId.entrySet()) {
        final FeatureListRow row = entry.getKey();
        final String siriusId = entry.getValue();

        final List<CompoundDBAnnotation> siriusAnnotations = Sirius.getSiriusAnnotations(sirius,
            siriusId,
            row);
        if (siriusAnnotations == null || siriusAnnotations.isEmpty()) {
          importedFeatures++;
          continue;
        }

        final List<CompoundDBAnnotation> current = row.getCompoundAnnotations();
        if (current != null && !current.isEmpty()) {
          siriusAnnotations.addAll(current);
        }
//        siriusAnnotations.sort(
//            Comparator.comparingDouble(CompoundDBAnnotation::getScore).reversed());
        row.setCompoundAnnotations(siriusAnnotations);
        importedFeatures++;
      }

    } catch (Exception e) {
      error(e.getMessage(), e);
      return false;
    }
    return true;
  }
}
