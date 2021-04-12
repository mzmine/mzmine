package io.github.mzmine.datamodel.features.types;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.graphicalnodes.LipidSpectrumChart;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.tasks.FeaturesGraphicalNodeTask;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.MatchedLipidSpectrumTab;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.StackPane;

public class LipidSpectrumType extends LinkedDataType implements GraphicalColumType<Boolean> {

  @Override
  public String getHeaderString() {
    return "Matched Lipid Signals";
  }

  @Override
  public Node getCellNode(TreeTableCell<ModularFeatureListRow, Boolean> cell,
      TreeTableColumn<ModularFeatureListRow, Boolean> coll, Boolean cellData, RawDataFile raw) {
    ModularFeatureListRow row = cell.getTreeTableRow().getItem();

    if (row == null) {
      return null;
    }

    Node node = row.getBufferedColChart(coll.getText());
    if (node != null) {
      return node;
    }

    StackPane pane = new StackPane();

    List<MatchedLipid> matchedLipids =
        row.get(LipidAnnotationType.class).get(LipidAnnotationSummaryType.class).getValue();
    if (matchedLipids != null && !matchedLipids.isEmpty()) {
      Task task =
          new FeaturesGraphicalNodeTask(LipidSpectrumChart.class, pane, row, coll.getText());
      MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);
    }
    return pane;
  }

  @Override
  public double getColumnWidth() {
    return DEFAULT_GRAPHICAL_CELL_WIDTH + 50;
  }

  @Nullable
  @Override
  public Runnable getDoubleClickAction(@Nonnull ModularFeatureListRow row,
      @Nonnull List<RawDataFile> file) {
    List<MatchedLipid> matchedLipids =
        row.get(LipidAnnotationType.class).get(LipidAnnotationSummaryType.class).getValue();
    MatchedLipidSpectrumTab matchedLipidSpectrumTab = new MatchedLipidSpectrumTab(
        matchedLipids.get(0).getLipidAnnotation().getAnnotation() + " Matched Signals",
        new LipidSpectrumChart(row, null));
    return () -> MZmineCore.getDesktop().addTab(matchedLipidSpectrumTab);
  }
}
