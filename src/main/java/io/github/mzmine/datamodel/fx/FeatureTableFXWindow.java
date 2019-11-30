package io.github.mzmine.datamodel.fx;

import java.util.List;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FeatureTableFXWindow extends Stage {
  private FeatureTableFX table;

  public FeatureTableFXWindow(List<ModularFeatureListRow> rows) {
    this();
    show();
    table.addData(rows);
  }

  public FeatureTableFXWindow() {
    setTitle("Feature Table FX");
    table = new FeatureTableFX();
    setScene(new Scene(table, 1000, 600));
    setMaximized(true);
  }

  public FeatureTableFX getTable() {
    return table;
  }
}
