package io.github.mzmine.datamodel.fx;

import io.github.mzmine.datamodel.data.ModularFeatureList;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FeatureTableFXWindow extends Stage {
  private FeatureTableFX table;

  public FeatureTableFXWindow(ModularFeatureList flist) {
    this();
    table.addData(flist);
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

  public ModularFeatureList getFeatureList() {
    return table.getFeatureList();
  }

}
