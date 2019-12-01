package io.github.mzmine.datamodel.data.types.graphicalnodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.RawsColorsType;
import io.github.mzmine.datamodel.data.types.numbers.AreaType;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class AreaShareChart extends StackPane {

  public AreaShareChart(@Nonnull ModularFeatureListRow row, AtomicDouble progress) {
    Float sum = row.getFeatures().entrySet().stream().map(Entry::getValue)
        .map(e -> e.get(AreaType.class).map(DataType::getValue).orElse(0f)).reduce(0f, Float::sum);
    Map<RawDataFile, Color> rawColors = row.get(RawsColorsType.class).map(DataType::getValue)
        .orElse(FXCollections.emptyObservableMap());

    List<Rectangle> all = new ArrayList<>();
    int i = 0;
    int size = row.getFeatures().size();
    for (Entry<RawDataFile, ModularFeature> entry : row.getFeatures().entrySet()) {
      Float area = entry.getValue().get(AreaType.class).map(DataType::getValue).orElse(null);
      if (area != null) {
        Color color = rawColors.get(entry.getKey());
        if (color == null)
          color = Color.LIGHTSLATEGREY;
        float ratio = area / sum;
        Rectangle rect = new Rectangle();
        rect.setFill(color);
        // bind width
        rect.widthProperty().bind(this.widthProperty().multiply(ratio));
        rect.setHeight(i % 2 == 0 ? 20 : 25);
        all.add(rect);
        i++;
        if (progress != null)
          progress.addAndGet(1.0 / size);
      }
    }
    HBox box = new HBox(0, all.toArray(Rectangle[]::new));
    box.setPrefWidth(100);
    box.setAlignment(Pos.CENTER_LEFT);

    this.getChildren().add(box);
  }
}
