package io.github.mzmine.modules.visualization.dash_integration;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import java.util.Comparator;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntegrationDashboardController extends FxController<IntegrationDashboardModel> {

  public IntegrationDashboardController() {
    super(new IntegrationDashboardModel());

    model.featureListProperty().subscribe(flist -> {
      model.getFeatureTableFx().setFeatureList(flist);

      final MetadataTable metadata = ProjectService.getMetadata();
      final MetadataColumn<?> sortingCol = model.getRawFileSortingColumn();
      model.setSortedFiles(flist.getRawDataFiles().stream().sorted(Comparator.comparing(
              file -> Objects.requireNonNullElse(metadata.getValue(sortingCol, file), "").toString()))
          .toList());
    });

    // the offset may never be larger than the number of files
    model.sortedFilesProperty().subscribe(files -> model.setGridPaneFileOffset(
        Math.max(Math.min(model.getGridPaneFileOffset(), files.size() - 1), 0)));
    model.rowProperty().addListener((_, _, _) -> onTaskThread(new FeatureDataEntryTask(model)));
  }

  @Override
  protected @NotNull FxViewBuilder<IntegrationDashboardModel> getViewBuilder() {
    return new IntegrationDashboardViewBuilder(model);
  }

  public void setFeatureList(@Nullable ModularFeatureList flist) {
    if (flist == null) {
      return;
    }
    model.setFeatureList(flist);
  }
}
