package io.github.mzmine.datamodel.fx.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.FeaturesType;
import io.github.mzmine.datamodel.data.types.FeaturesType;
import io.github.mzmine.datamodel.data.types.RawsColorsType;
import io.github.mzmine.datamodel.data.types.numbers.AreaType;
import io.github.mzmine.datamodel.data.types.numbers.HeightType;
import io.github.mzmine.datamodel.data.types.numbers.MZType;
import io.github.mzmine.datamodel.data.types.numbers.RTType;
import io.github.mzmine.datamodel.fx.FeatureTableFX;
import io.github.mzmine.util.color.ColorsFX;
import io.github.mzmine.util.color.Vision;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class FXTableWindow extends Application {
  Random rand = new Random(System.currentTimeMillis());

  @Override
  public void start(Stage stage) {
    FeatureTableFX table = new FeatureTableFX();

    List<RawDataFile> raw = new ArrayList<>();
    for (int a = 0; a < 8; a++)
      raw.add(createRaw("Raw" + a));

    // example row to create all columns
    ModularFeatureListRow data = createRow(0, raw);
    table.addColumns(data);

    // Table tree root
    final TreeItem<ModularFeatureListRow> root = new TreeItem<>();
    root.setExpanded(true);

    addDummyData(table.getRoot(), raw);
    Scene scene = new Scene(table);

    stage.setScene(scene);
    stage.show();

    // test for change listener
    table.getRoot().getChildren().get(0).getValue().set(MZType.class, new MZType(1234d));
  }


  public void addDummyData(TreeItem<ModularFeatureListRow> root, List<RawDataFile> raw) {
    int i = 0;
    for (i = 0; i < 10; i++)
      root.getChildren().add(new TreeItem<>(createRow(i, raw)));
    for (; i < 15; i++)
      root.getChildren().add(new TreeItem<>(createIncompleteRow(i, raw)));
    // add one to the second item
    root.getChildren().get(1).getChildren().add(new TreeItem<>(createRow(i, raw)));
    for (; i < 20; i++)
      root.getChildren().get(5).getChildren().add(new TreeItem<>(createIncompleteRow(i, raw)));
  }

  public ModularFeatureListRow createRow(int i, List<RawDataFile> raw) {
    ModularFeatureListRow data = new ModularFeatureListRow();
    data.set(MZType.class, new MZType(50d * i));
    data.set(RTType.class, new RTType(1f * i));
    data.set(HeightType.class, new HeightType(2E4f * i));
    data.set(AreaType.class, new AreaType(1E4f * i));

    data.set(FeaturesType.class, new FeaturesType(createFeatures(i, raw)));
    data.set(RawsColorsType.class, new RawsColorsType(createColorsMap(data, raw)));
    return data;
  }

  public ModularFeatureListRow createIncompleteRow(int i, List<RawDataFile> raw) {
    ModularFeatureListRow data = new ModularFeatureListRow();
    data.set(MZType.class, new MZType(50d * i));
    data.set(RTType.class, new RTType(1f * i));
    data.set(HeightType.class, new HeightType(2E4f * i));

    data.set(FeaturesType.class, new FeaturesType(createFeatures(i, raw)));
    data.set(RawsColorsType.class, new RawsColorsType(createColorsMap(data, raw)));
    return data;
  }

  private Map<RawDataFile, Color> createColorsMap(ModularFeatureListRow data,
      List<RawDataFile> raw) {
    Map<RawDataFile, Color> map = new HashMap<>(raw.size());
    Color[] colors = ColorsFX.getSevenColorPalette(Vision.DEUTERANOPIA, false);
    for (int a = 0; a < raw.size(); a++) {
      map.put(raw.get(a), colors[a % colors.length]);
    }
    return map;
  }

  private Map<RawDataFile, ModularFeature> createFeatures(int i, List<RawDataFile> raw) {
    Map<RawDataFile, ModularFeature> map = new ConcurrentHashMap<>(raw.size());
    for (int a = 0; a < raw.size(); a++) {
      if (a < raw.size() / 2)
        map.put(raw.get(a), createFeature(i, a));
      else
        map.put(raw.get(a), createIncompleteFeature(i, a));
    }
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


