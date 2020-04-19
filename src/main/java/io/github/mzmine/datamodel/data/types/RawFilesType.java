package io.github.mzmine.datamodel.data.types;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.data.types.numbers.abstr.ListDataType;
import javax.annotation.Nonnull;

/**
 * Used to store a list of {@link RawDataFile}s in a {@link io.github.mzmine.datamodel.data.ModularFeatureListRow}
 */
public class RawFilesType extends ListDataType<RawDataFile> implements NullColumnType {

  @Nonnull
  @Override
  public String getHeaderString() {
    return "Raw data files";
  }
}
