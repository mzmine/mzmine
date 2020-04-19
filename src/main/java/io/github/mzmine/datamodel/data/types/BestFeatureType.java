package io.github.mzmine.datamodel.data.types;

import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.types.modifiers.NullColumnType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nonnull;

public class BestFeatureType extends DataType<ObjectProperty<ModularFeature>> implements
    NullColumnType {

  @Nonnull
  @Override
  public String getHeaderString() {
    return "Best feature";
  }

  @Override
  public ObjectProperty<ModularFeature> createProperty() {
    return new SimpleObjectProperty<>();
  }
}
