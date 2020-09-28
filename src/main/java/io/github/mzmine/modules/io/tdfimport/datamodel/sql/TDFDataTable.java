package io.github.mzmine.modules.io.tdfimport.datamodel.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class TDFDataTable<EntryKeyType> {

  private static final Logger logger = Logger.getLogger(TDFDataTable.class.getName());
  protected final String table;
  protected final String entryHeader;
  protected final List<TDFDataColumn<?>> columns;
  protected final TDFDataColumn<EntryKeyType> keyList;

  public TDFDataTable(String table, String entryHeader) {
    this.table = table;
    this.entryHeader = entryHeader;
    columns = new ArrayList<>();
    keyList = new TDFDataColumn<EntryKeyType>(entryHeader);
    columns.add(keyList);
  }

  public void addColumn(@Nonnull TDFDataColumn<?> column) {
    assert column != null;
    columns.add(column);
  }

  @Nullable
  public TDFDataColumn<?> getColumn(String columnName) {
    for (TDFDataColumn<?> column : columns) {
      if (column.coulumnName.equals(columnName)) {
        return column;
      }
    }
    return null;
  }

  protected String getColumnHeadersForQuery() {
    String headers = new String();
    for(TDFDataColumn col : columns) {
      headers += col.getCoulumnName() + ", ";
    }
    headers = headers.substring(0, headers.length() - 2);
    return headers;
  }

  public abstract boolean isValid();

  public boolean executeQuery(Connection connection) {
    try {
      Statement statement = connection.createStatement();

      String headers = getColumnHeadersForQuery();
      statement.setQueryTimeout(30);
      if (headers == null || headers.isEmpty()) {
        return false;
      }

      String request = "SELECT " + headers + " FROM " + table;
      ResultSet rs = statement.executeQuery(request);
      int types[] = new int[rs.getMetaData().getColumnCount()];
      if (types.length != columns.size()) {
        logger.info("Number of retrieved columns does not match number of queried columns.");
        return false;
      }
      for (int i = 0; i < types.length; i++) {
        types[i] = rs.getMetaData().getColumnType(i+1);
      }

      while (rs.next()) {
        for (int i = 0; i < columns.size(); i++) {
          switch (types[i]) {
            case Types.CHAR, Types.LONGNVARCHAR, Types.LONGVARCHAR, Types.NCHAR,
                Types.NVARCHAR, Types.VARCHAR:
              ((TDFDataColumn<String>) columns.get(i)).getEntries().add(rs.getString(i + 1));
              break;
            case Types.INTEGER:
              ((TDFDataColumn<Integer>) columns.get(i)).getEntries().add(rs.getInt(i + 1));
              break;
            case Types.DOUBLE, Types.REAL, Types.FLOAT:
              ((TDFDataColumn<Double>) columns.get(i)).getEntries().add(rs.getDouble(i + 1));
              break;
            default:
              break;
          }
        }
      }
      rs.close();
      statement.close();
//      print();
//      logger.info("Recieved " + columns.size() + " * " + keyList.getEntries().size() + " entries.");
      return true;
    } catch (SQLException throwables) {
      throwables.printStackTrace();
      return false;
    }
  }

  public void print() {
    for(int i = 0; i < keyList.getEntries().size(); i++) {
      String str = i + "\t";
      for(TDFDataColumn col : columns) {
        str += col.getEntries().get(i) + "\t";
      }
      logger.info(str);
    }
  }
}
