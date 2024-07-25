/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author https://github.com/SteffenHeu
 */
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

  public void addColumn(@NotNull TDFDataColumn<?> column) {
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
    for (TDFDataColumn col : columns) {
      headers += col.getCoulumnName() + ", ";
    }
    headers = headers.substring(0, headers.length() - 2);
    return headers;
  }

  public boolean isValid() {
    long numKeys = keyList.size();
    for (TDFDataColumn<?> col : columns) {
      if (numKeys != col.size()) {
        return false;
      }
    }
    return true;
  }

  public boolean executeQuery(Connection connection) {
    try {
      Statement statement = connection.createStatement();

      String headers = getColumnHeadersForQuery();
      statement.setQueryTimeout(30);
      if (headers == null || headers.isEmpty()) {
        return false;
      }

      String request = getQueryText(getColumnHeadersForQuery());
      ResultSet rs = statement.executeQuery(request);
      int types[] = new int[rs.getMetaData().getColumnCount()];
      if (types.length != columns.size()) {
        logger.info(
            "Number of retrieved columns does not match number of queried columns for table %s.".formatted(
                this.table));
        return false;
      }
      for (int i = 0; i < types.length; i++) {
        types[i] = rs.getMetaData().getColumnType(i + 1);
      }

      while (rs.next()) {
        for (int i = 0; i < columns.size(); i++) {
          switch (types[i]) {
            case Types.VARCHAR:
              ((TDFDataColumn<String>) columns.get(i)).add(rs.getString(i + 1));
              break;
            case Types.NVARCHAR:
              ((TDFDataColumn<String>) columns.get(i)).add(rs.getString(i + 1));
              break;
            case Types.NCHAR:
              ((TDFDataColumn<String>) columns.get(i)).add(rs.getString(i + 1));
              break;
            case Types.LONGVARCHAR:
              ((TDFDataColumn<String>) columns.get(i)).add(rs.getString(i + 1));
              break;
            case Types.LONGNVARCHAR:
              ((TDFDataColumn<String>) columns.get(i)).add(rs.getString(i + 1));
              break;
            case Types.CHAR:
              ((TDFDataColumn<String>) columns.get(i)).add(rs.getString(i + 1));
              break;
            case Types.INTEGER:
              // Bruker stores every natural number value as INTEGER in the sql database
              // Maximum size of INTEGER in SQLite: 8 bytes (64 bits) - https://sqlite.org/datatype3.html
              // So we treat everything as long (64 bit) to be on the save side.
              // this will consume more memory, though
              // However, the .dll's methods want long as argument, anyway. Otherwise we'd have to
              // cast there
              ((TDFDataColumn<Long>) columns.get(i)).add(rs.getLong(i + 1));
              break;
            case Types.BIGINT:
              ((TDFDataColumn<Long>) columns.get(i)).add(rs.getLong(i + 1));
              break;
            case Types.TINYINT:
              ((TDFDataColumn<Long>) columns.get(i)).add(rs.getLong(i + 1));
              break;
            case Types.SMALLINT:
              ((TDFDataColumn<Long>) columns.get(i)).add(rs.getLong(i + 1));
              break;
            case Types.DOUBLE:
              ((TDFDataColumn<Double>) columns.get(i)).add(rs.getDouble(i + 1));
              break;
            case Types.FLOAT:
              ((TDFDataColumn<Double>) columns.get(i)).add(rs.getDouble(i + 1));
              break;
            case Types.REAL:
              ((TDFDataColumn<Double>) columns.get(i)).add(rs.getDouble(i + 1));
              break;
            default:
              logger.info("Unsupported type loaded in " + table + " " + i + " " + types[i]);
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

  protected String getQueryText(String columnHeadersForQuery) {
    return "SELECT " + columnHeadersForQuery + " FROM " + table;
  }

  public void print() {
    logger.info("Printing " + table + "\t" + columns.size() + " * " + keyList.size() + " entries.");
    /*for (int i = 0; i < keyList.getEntries().size(); i++) {
      String str = i + "\t";
      for (TDFDataColumn col : columns) {
        str += col.getEntries().get(i) + "\t";
      }
      logger.info(str);
    }*/
  }

  public List<TDFDataColumn<?>> columns() {
    return columns;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TDFDataTable)) {
      return false;
    }
    TDFDataTable<?> that = (TDFDataTable<?>) o;
    return table.equals(that.table) && entryHeader.equals(that.entryHeader) && columns.equals(
        that.columns) && keyList.equals(that.keyList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(table, entryHeader, columns, keyList);
  }
}
