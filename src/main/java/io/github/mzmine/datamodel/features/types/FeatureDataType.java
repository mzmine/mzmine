package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.MsTimeSeries;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ListRowBinding;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nonnull;

public class FeatureDataType extends
    DataType<ObjectProperty<MsTimeSeries<? extends Scan>>> implements NullColumnType {

  @Nonnull
  @Override
  public String getHeaderString() {
    return "Feature data";
  }

  @Override
  public ObjectProperty<MsTimeSeries<? extends Scan>> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Nonnull
  @Override
  public List<RowBinding> createDefaultRowBindings() {
    // listen to changes in DataPointsType for all ModularFeatures
    return List.of(new ListRowBinding(new FeatureShapeType(), this));
  }

}