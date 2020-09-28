package io.github.mzmine.modules.io.tdfimport.datamodel.sql;

import io.github.mzmine.main.MZmineCore;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class TDFMetaDataTable extends TDFDataTable<String> {

  private static final Logger logger = Logger.getLogger(TDFMetaDataTable.class.getName());

  private static final List<String> allowedFileVersions = Arrays.asList("3.1");

  public static final String VALUE_COMLUMN_NAME = "Value";

  private enum Keys {
    SchemaType, SchemaVersionMajor, SchemaVersionMinor, MzAcqRangeLower, MzAcqRangeUpper,
    OneOverK0AcqRangeLower, OneOverK0AcqRangeUpper, AcquisitionSoftwareVersion, InstrumentName,
    Description, SampleName, MethodName;
  }

  public TDFMetaDataTable() {
    super("GlobalMetadata", "Key");

//    for (Keys key : Keys.values()) {
//      keyList.getEntries().add(key.name());
//    }
    columns.add(new TDFDataColumn<String>(VALUE_COMLUMN_NAME));
  }

  public boolean isFileVersionValid() {
    TDFDataColumn<String> valueCol = (TDFDataColumn<String>) getColumn(VALUE_COMLUMN_NAME);
    if (valueCol == null) {
      return false;
    }
    String version =
        valueCol.getEntries().get(keyList.getEntries().indexOf(Keys.SchemaVersionMajor)) + "."
            + valueCol
            .getEntries().get(keyList.getEntries().indexOf(Keys.SchemaVersionMinor));

    if (!allowedFileVersions.contains(version)) {
      MZmineCore.getDesktop().displayMessage(
          "TDF version " + version + " is not supported.\\This might lead to unexpected results.");
      return false;
    }

    return true;
  }

  @Override
  public boolean isValid() {
    if (keyList.getEntries().isEmpty()) {
      return false;
    }

    TDFDataColumn<String> valueCol = (TDFDataColumn<String>) getColumn(VALUE_COMLUMN_NAME);
    if (keyList.getEntries().size() != valueCol.getEntries().size()) {
      return false;
    }

    for (int i = 0; i < keyList.getEntries().size(); i++) {
      String key = keyList.getEntries().get(0);
      if (valueCol.getEntries().get(i) == null) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean executeQuery(Connection connection) {
    boolean b = super.executeQuery(connection);
    if(b == false)
      return false;

    for(int i = 0; i < keyList.getEntries().size(); i++) {
      try{
        Keys.valueOf(keyList.getEntries().get(i));
      } catch (IllegalArgumentException | ClassCastException e) {
        for(TDFDataColumn<?> col : columns) {
          col.getEntries().remove(i);
        }
        i--;
      }
    }
//    print();
    return true;
  }
}
