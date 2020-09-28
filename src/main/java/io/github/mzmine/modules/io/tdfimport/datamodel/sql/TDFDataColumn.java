package io.github.mzmine.modules.io.tdfimport.datamodel.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;


public class TDFDataColumn<DataType> {
  protected final String coulumnName;
  protected final List<DataType> entries;

  public TDFDataColumn(@Nonnull String coulumnName) {
    this.coulumnName = coulumnName;
    entries = new ArrayList<>();
  }

  @Nonnull
  public String getCoulumnName() {
    return coulumnName;
  }

  @Nonnull
  public List<DataType> getEntries() {
    return entries;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TDFDataColumn<?> that = (TDFDataColumn<?>) o;
    return Objects.equals(coulumnName, that.coulumnName) &&
        Objects.equals(entries, that.entries);
  }

  @Override
  public int hashCode() {
    return Objects.hash(coulumnName, entries);
  }

}
