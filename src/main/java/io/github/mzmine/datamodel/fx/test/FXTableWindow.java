package io.github.mzmine.datamodel.fx.test;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.AreaType;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.FeaturesType;
import io.github.mzmine.datamodel.data.types.HeightType;
import io.github.mzmine.datamodel.data.types.MZType;
import io.github.mzmine.datamodel.data.types.RTType;
import io.github.mzmine.datamodel.fx.FeatureTableFX;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;

public class FXTableWindow extends Application {
  Random rand = new Random(System.currentTimeMillis());

  @Override
  public void start(Stage stage) {
    FeatureTableFX table = new FeatureTableFX();

    // example row to create all columns
    ModularFeatureListRow data = createRow(0);
    table.addColumns(data);

    // Table tree root
    final TreeItem<ModularFeatureListRow> root = new TreeItem<>();
    root.setExpanded(true);

    addDummyData(table.getRoot());
    Scene scene = new Scene(table);

    stage.setScene(scene);
    stage.show();

    // test for change listener
    table.getRoot().getChildren().get(0).getValue().set(MZType.class, new MZType(1234d));
  }


  public void addDummyData(TreeItem<ModularFeatureListRow> root) {
    int i = 0;
    for (i = 0; i < 10; i++)
      root.getChildren().add(new TreeItem<>(createRow(i)));
    i++;
    for (; i < 15; i++)
      root.getChildren().add(new TreeItem<>(createIncompleteRow(i)));
    i++;
    // add one to the second item
    root.getChildren().get(1).getChildren().add(new TreeItem<>(createRow(i)));
    i++;
    for (; i < 20; i++)
      root.getChildren().get(5).getChildren().add(new TreeItem<>(createIncompleteRow(i)));
  }

  public ModularFeatureListRow createRow(int i) {
    ModularFeatureListRow data = new ModularFeatureListRow();
    data.set(MZType.class, new MZType(50d * i));
    data.set(RTType.class, new RTType(1f * i));
    data.set(HeightType.class, new HeightType(2E4f * i));
    data.set(AreaType.class, new AreaType(1E4f * i));

    data.set(FeaturesType.class, new FeaturesType(createFeatures(i, 3)));
    return data;
  }

  private Map<RawDataFile, ModularFeature> createFeatures(int i, int j) {
    Map<RawDataFile, ModularFeature> map = new ConcurrentHashMap<>(j * 2);
    int a = 0;
    for (a = 0; a < j; a++)
      map.put(createRaw("Raw" + a), createFeature(i, j));

    for (; a < j * 2; a++)
      map.put(createRaw("Raw" + a), createIncompleteFeature(i, j));

    return map;
  }



  private RawDataFile createRaw(String name) {
    try {
      return new TestRawDataFile(name);
    } catch (Exception ex) {
      System.out.println("ERROR in feature creation");
      return null;
    }
  }



  public ModularFeatureListRow createIncompleteRow(int i) {
    ModularFeatureListRow data = new ModularFeatureListRow();
    data.set(MZType.class, new MZType(50d * i));
    data.set(RTType.class, new RTType(1f * i));
    data.set(HeightType.class, new HeightType(2E4f * i));
    // data.set(AreaType.class, new AreaType(1E4f * i));

    data.set(FeaturesType.class, new FeaturesType(createFeatures(i, 3)));
    return data;
  }

  public ModularFeature createFeature(int i, int j) {
    ModularFeature data = new ModularFeature();
    data.set(MZType.class, new MZType(300d * i + j));
    data.set(RTType.class, new RTType(100f * i + j));
    data.set(AreaType.class, new AreaType(rand.nextFloat() * 100f));
    data.set(DetectionType.class, new DetectionType(FeatureStatus.DETECTED));
    return data;
  }

  public ModularFeature createIncompleteFeature(int i, int j) {
    ModularFeature data = new ModularFeature();
    data.set(RTType.class, new RTType(100f * i + j));
    data.set(AreaType.class, new AreaType(rand.nextFloat() * 100f));
    data.set(DetectionType.class,
        new DetectionType(rand.nextBoolean() ? FeatureStatus.UNKNOWN : FeatureStatus.ESTIMATED));
    return data;
  }

  public static void startThisApp(String[] args) {
    launch(args);
  }

}


