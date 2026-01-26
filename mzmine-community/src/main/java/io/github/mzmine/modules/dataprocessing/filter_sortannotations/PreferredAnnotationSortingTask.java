package io.github.mzmine.modules.dataprocessing.filter_sortannotations;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummarySortConfig;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.FeatureTableFXUtil;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PreferredAnnotationSortingTask extends AbstractFeatureListTask {

  @NotNull
  private final PreferredAnnotationSortingParameters param;
  @NotNull
  private final FeatureList flist;

  protected PreferredAnnotationSortingTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull PreferredAnnotationSortingParameters parameters,
      @NotNull Class<? extends MZmineModule> moduleClass, @NotNull final FeatureList featureList) {
    super(storage, moduleCallDate, parameters, moduleClass);
    param = parameters;
    this.flist = featureList;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  protected void process() {
    final AnnotationSummarySortConfig config = param.toConfig();
    flist.setAnnotationSortConfig(config);
    FeatureTableFXUtil.updateCellsForFeatureList(flist);
  }

  @Override
  public String getTaskDescription() {
    return "Defining sort config for feature list " + flist.getName();
  }
}
