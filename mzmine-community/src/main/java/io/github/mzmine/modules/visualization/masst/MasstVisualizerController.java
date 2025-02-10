package io.github.mzmine.modules.visualization.masst;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.framework.fx.FeatureRowInterfaceFx;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class MasstVisualizerController extends FxController<MasstVisualizerModel> implements
    FeatureRowInterfaceFx {

  private final MasstVisualizerViewBuilder viewBuilder;

  public MasstVisualizerController() {
    super(new MasstVisualizerModel());
    viewBuilder = new MasstVisualizerViewBuilder(model);
  }

  @Override
  protected @NotNull FxViewBuilder<MasstVisualizerModel> getViewBuilder() {
    return viewBuilder;
  }

  @Override
  public boolean hasContent() {
    return true;
  }

  @Override
  public void setFeatureRows(final @NotNull List<? extends FeatureListRow> selectedRows) {
    model.setSelectedRow(selectedRows.isEmpty()? null : selectedRows.getFirst());
  }
}
