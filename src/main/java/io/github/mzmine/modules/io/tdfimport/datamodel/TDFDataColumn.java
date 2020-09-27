package io.github.mzmine.modules.io.tdfimport.datamodel;

import java.util.Objects;
import javax.annotation.Nonnull;

public class TDFDataColumn {

  private final String table;
  private final String column;

  public TDFDataColumn(@Nonnull final String table, @Nonnull final String column) {
    this.table = table;
    this.column = column;
  }

  public String getTable() {
    return table;
  }

  public String getColumn() {
    return column;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TDFDataColumn)) {
      return false;
    }
    TDFDataColumn that = (TDFDataColumn) o;
    return table.equals(that.table) &&
        column.equals(that.column);
  }

  @Override
  public int hashCode() {
    return Objects.hash(table, column);
  }
}
