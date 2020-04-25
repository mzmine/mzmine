package io.github.mzmine.datamodel.data.types.fx;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import java.util.Objects;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Helper class to keep track of columns in a {@link io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX}.
 * Is set in the {@link io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX}
 * because the data type itself does not know, if it creates a row or feature column.
 * <p>
 * For usage example see {@link FeatureTableFX#applyColumnVisibility}.
 */
public class ColumnID {

//  private static final Logger logger = Logger.getLogger(ColumnID.class.getName());

  @Nonnull
  private final ColumnType type;

  @Nonnull
  private final DataType dt;

  @Nullable
  private final RawDataFile raw;

  /**
   * @param dt   The {@link DataType} this column represents
   * @param type The {@link ColumnType} to determine if this is a feature or row type column.
   * @param raw  The {@link RawDataFile} this column belongs to or null if not a features type
   *             column.
   */
  public ColumnID(@Nonnull DataType dt, @Nonnull ColumnType type, @Nullable RawDataFile raw) {
    assert dt != null;
    assert type != null;

    this.dt = dt;
    this.type = type;
    this.raw = raw;

//    logger.info("Created " + getFormattedString());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ColumnID columnID = (ColumnID) o;
    return type == columnID.type &&
        dt.equals(columnID.dt) &&
        Objects.equals(raw, columnID.raw);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, dt, raw);
  }

  @Nonnull
  public ColumnType getType() {
    return type;
  }

  @Nonnull
  public DataType getDataType() {
    return dt;
  }

  @Nullable
  public RawDataFile getRaw() {
    return raw;
  }

  public String getFormattedString() {
    return "DataType: " + dt.getHeaderString() + "\tType: " + type.toString() + "\tRawDataFile: "
        + raw;
  }
}
