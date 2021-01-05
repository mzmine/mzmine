package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.MsTimeSeries;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nonnull;

public class FeatureData extends DataType<ObjectProperty<MsTimeSeries<?>>> implements NullColumnType {


  @Nonnull
  @Override
  public String getHeaderString() {
    return "Feature data";
  }

  @Override
  public ObjectProperty<MsTimeSeries<?>> createProperty() {
    return new SimpleObjectProperty<>();
  }

}