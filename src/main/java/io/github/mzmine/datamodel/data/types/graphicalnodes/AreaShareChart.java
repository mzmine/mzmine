package io.github.mzmine.datamodel.data.types.graphicalnodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.RawColorType;
import io.github.mzmine.datamodel.data.types.numbers.AreaType;
import javafx.beans.property.FloatProperty;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class AreaShareChart extends StackPane {

  public AreaShareChart(@Nonnull ModularFeatureListRow row, AtomicDouble progress) {
    Float sum =
        row.getFeatures().values().stream().map(ModularFeature::getArea).reduce(0f, Float::sum);

    List<Rectangle> all = new ArrayList<>();
    int i = 0;
    int size = row.getFeatures().size();
    for (Entry<RawDataFile, ModularFeature> entry : row.getFeatures().entrySet()) {
      FloatProperty areaProperty = entry.getValue().get(AreaType.class);
      if (areaProperty.getValue() != null) {
        // color from sample
        Color color = entry.getValue().get(RawColorType.class).getValue();

        float ratio = areaProperty.get() / sum;
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
