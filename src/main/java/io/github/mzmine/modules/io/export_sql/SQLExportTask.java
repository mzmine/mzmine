/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.export_sql;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.ScanUtils;

class SQLExportTask extends AbstractTask {

  private final FeatureList featureList;
  private final String connectionString;
  private final String tableName;
  private final SQLColumnSettings exportColumns;
  private final boolean emptyExport;

  private int processedRows = 0, totalRows = 0;

  private Connection dbConnection;

  SQLExportTask(ParameterSet parameters) {

    this.featureList = parameters.getParameter(SQLExportParameters.featureList).getValue()
        .getMatchingFeatureLists()[0];
    this.connectionString =
        parameters.getParameter(SQLExportParameters.connectionString).getValue();

    this.tableName = parameters.getParameter(SQLExportParameters.tableName).getValue();
    this.exportColumns = parameters.getParameter(SQLExportParameters.exportColumns).getValue();
    this.emptyExport = parameters.getParameter(SQLExportParameters.emptyExport).getValue();

  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0;
    }
    return (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Exporting feature list \"" + featureList + "\" to SQL table " + tableName;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // Get number of rows
    totalRows = featureList.getNumberOfRows();

    try {
      this.dbConnection = DriverManager.getConnection(connectionString);
    } catch (SQLException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error connecting to the SQL database: " + e.toString());
      return;
    }

    FeatureListRow rows[] = featureList.getRows().toArray(FeatureListRow[]::new);

    try {
      dbConnection.setAutoCommit(false);

      // If select, an empty row with just the raw data file
      // information will be exported
      if (rows.length < 1 && emptyExport) {
        exportFeatureListRow(null);
      } else {
        for (FeatureListRow row : rows) {
          if (getStatus() != TaskStatus.PROCESSING)
            break;
          exportFeatureListRow(row);
          processedRows++;
        }
      }
      dbConnection.commit();
      dbConnection.close();
    } catch (SQLException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error running SQL query: " + e.toString());
      return;
    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);

  }

  private void exportFeatureListRow(FeatureListRow row) throws SQLException {

    // Cancel?
    if (isCanceled()) {
      return;
    }

    // Value for looping through raw data files
    boolean loopDataFiles = false;

    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO ");
    sql.append(tableName);
    sql.append(" (");
    for (int i = 0; i < exportColumns.getRowCount(); i++) {
      sql.append(exportColumns.getValueAt(i, 0));
      if (i < exportColumns.getRowCount() - 1)
        sql.append(",");
    }
    sql.append(" ) VALUES (");
    for (int i = 0; i < exportColumns.getRowCount(); i++) {
      sql.append("?");
      if (i < exportColumns.getRowCount() - 1)
        sql.append(",");
    }
    sql.append(")");

    PreparedStatement statement = dbConnection.prepareStatement(sql.toString());

    if (row == null) {
      for (int i = 0; i < exportColumns.getRowCount(); i++) {
        SQLExportDataType dataType = (SQLExportDataType) exportColumns.getValueAt(i, 1);
        String dataValue = (String) exportColumns.getValueAt(i, 2);
        switch (dataType) {
          case CONSTANT:
            statement.setString(i + 1, dataValue);
            break;
          case RAWFILE:
            RawDataFile rawdatafiles[] = featureList.getRawDataFiles().toArray(RawDataFile[]::new);
            statement.setString(i + 1, rawdatafiles[0].getName());
            break;
          default:
            statement.setString(i + 1, null);
            break;
        }
      }
      statement.executeUpdate();
    }

    else {
      for (RawDataFile rawDataFile : row.getRawDataFiles()) {
        Feature feature = row.getFeature(rawDataFile);

        for (int i = 0; i < exportColumns.getRowCount(); i++) {
          SQLExportDataType dataType = (SQLExportDataType) exportColumns.getValueAt(i, 1);
          String dataValue = (String) exportColumns.getValueAt(i, 2);
          switch (dataType) {
            case CONSTANT:
              statement.setString(i + 1, dataValue);
              break;
            case MZ:
              statement.setDouble(i + 1, row.getAverageMZ());
              break;
            case RT:
              statement.setDouble(i + 1, row.getAverageRT());
              break;
            case ID:
              statement.setInt(i + 1, row.getID());
              break;
            case FEATURECHARGE:
              statement.setDouble(i + 1, feature.getCharge());
              loopDataFiles = true;
              break;
            case FEATUREDURATION:
              statement.setDouble(i + 1, RangeUtils.rangeLength(feature.getRawDataPointsRTRange()));
              loopDataFiles = true;
              break;
            case FEATURESTATUS:
              statement.setString(i + 1, feature.getFeatureStatus().name());
              loopDataFiles = true;
              break;
            case FEATUREMZ:
              statement.setDouble(i + 1, feature.getMZ());
              loopDataFiles = true;
              break;
            case FEATURERT:
              statement.setDouble(i + 1, feature.getRT());
              loopDataFiles = true;
              break;
            case FEATURERT_START:
              statement.setDouble(i + 1, feature.getRawDataPointsRTRange().lowerEndpoint());
              loopDataFiles = true;
              break;
            case FEATURERT_END:
              statement.setDouble(i + 1, feature.getRawDataPointsRTRange().upperEndpoint());
              loopDataFiles = true;
              break;
            case FEATUREHEIGHT:
              statement.setDouble(i + 1, feature.getHeight());
              loopDataFiles = true;
              break;
            case FEATUREAREA:
              statement.setDouble(i + 1, feature.getArea());
              loopDataFiles = true;
              break;
            case DATAPOINTS:
              statement.setDouble(i + 1, feature.getScanNumbers().size());
              loopDataFiles = true;
              break;
            case FWHM:
              statement.setDouble(i + 1, feature.getFWHM());
              loopDataFiles = true;
              break;
            case TAILINGFACTOR:
              statement.setDouble(i + 1, feature.getTailingFactor());
              loopDataFiles = true;
              break;
            case ASYMMETRYFACTOR:
              statement.setDouble(i + 1, feature.getAsymmetryFactor());
              loopDataFiles = true;
              break;
            case RAWFILE:
              statement.setString(i + 1, rawDataFile.getName());
              loopDataFiles = true;
              break;
            case HEIGHT:
              statement.setDouble(i + 1, row.getAverageHeight());
              break;
            case AREA:
              statement.setDouble(i + 1, row.getAverageArea());
              break;
            case COMMENT:
              statement.setString(i + 1, row.getComment());
              break;
            case IDENTITY:
              FeatureIdentity id = row.getPreferredFeatureIdentity();
              if (id != null) {
                statement.setString(i + 1, id.getName());
              } else {
                statement.setNull(i + 1, Types.VARCHAR);
              }
              break;
            case ISOTOPEPATTERN:
              IsotopePattern isotopes = row.getBestIsotopePattern();
              if (isotopes == null) {
                statement.setNull(i + 1, Types.BLOB);
                break;
              }
              DataPoint dataPoints[] = ScanUtils.extractDataPoints(isotopes);
              byte bytes[] = ScanUtils.encodeDataPointsToBytes(dataPoints);
              ByteArrayInputStream is = new ByteArrayInputStream(bytes);
              statement.setBlob(i + 1, is);
              break;
            case MSMS:
              Scan msmsScan = row.getBestFeature().getMostIntenseFragmentScan();
              // Check if there is any MS/MS scan
              if (msmsScan == null) {
                statement.setNull(i + 1, Types.BLOB);
                break;
              }
              MassList msmsMassList = msmsScan.getMassList();
              // Check if there is a masslist for the scan
              if (msmsMassList == null) {
                statement.setNull(i + 1, Types.BLOB);
                break;
              }
              dataPoints = msmsMassList.getDataPoints();
              bytes = ScanUtils.encodeDataPointsToBytes(dataPoints);
              is = new ByteArrayInputStream(bytes);
              statement.setBlob(i + 1, is);
              break;
            default:
              break;
          }
        }
        statement.executeUpdate();

        // If no data file elements are selected then don't loop through
        // all
        // data files in feature list
        if (!loopDataFiles) {
          break;
        }
      }
    }
  }
}
