package io.github.mzmine.datamodel.fx;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.data.RowData;
import io.github.mzmine.datamodel.data.types.AreaType;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.HeightType;
import io.github.mzmine.datamodel.data.types.MZType;
import io.github.mzmine.datamodel.data.types.RTType;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;

public class FXTableWindow extends Application {

  @Override
  public void start(Stage stage) {
    FeatureTableFX table = new FeatureTableFX();

    // example row to create all columns
    RowData data = createRow(0);
    table.addColumns(data);

    // Table tree root
    final TreeItem<RowData> root = new TreeItem<>();
    root.setExpanded(true);

    addDummyData(table.getRoot());
    Scene scene = new Scene(table);

    stage.setScene(scene);
    stage.show();

    // test for change listener
    table.getRoot().getChildren().get(0).getValue().set(MZType.class, new MZType(1234d));
  }


  public void addDummyData(TreeItem<RowData> root) {
    int i = 0;
    for (i = 0; i < 10; i++)
      root.getChildren().add(new TreeItem<>(createRow(i)));
    i++;
    // add one to the second item
    root.getChildren().get(1).getChildren().add(new TreeItem<>(createRow(i)));
  }

  public RowData createRow(int i) {
    RowData data = new RowData();
    data.set(MZType.class, new MZType(50d * i));
    data.set(RTType.class, new RTType(1f * i));
    data.set(AreaType.class, new AreaType(1E4f * i));
    data.set(HeightType.class, new HeightType(2E4f * i));
    data.set(DetectionType.class, new DetectionType(FeatureStatus.DETECTED));
    return data;
  }

  public static void startThisApp(String[] args) {
    launch(args);
  }

}


