package io.github.mzmine.modules.io.tdfimport.datamodel.sql;


import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TDFDataRow<DataType> {

  private static final Logger logger = Logger.getLogger(TDFDataRow.class.getName());

  private final String table;
  private final String column;
  private final String key;
  private DataType value;

  public TDFDataRow(String table, String column, String key) {
    this.table = table;
    this.column = column;
    this.key = key;
  }
  public String getTable() {
    return table;
  }

  public String getKey() {
    return key;
  }

  @Nullable
  public DataType getValue() {
    return value;
  }

  public void setValue(@Nonnull DataType value) {
    this.value = value;
  }

  public void runRequest(Statement statement) {
    ResultSet rs;
  }
}
