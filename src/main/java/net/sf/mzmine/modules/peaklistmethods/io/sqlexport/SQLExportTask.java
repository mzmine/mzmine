/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.io.sqlexport;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.peaklistmethods.io.mztabexport.MzTabExportParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.RangeUtils;
import net.sf.mzmine.util.ScanUtils;

class SQLExportTask extends AbstractTask {

    private final PeakList peakList;
    private final String connectionString;
    private final String tableName;
    private final SQLColumnSettings exportColumns;
    private final boolean emptyExport;

    private int processedRows = 0, totalRows = 0;

    private Connection dbConnection;

    SQLExportTask(ParameterSet parameters) {

        this.peakList = parameters.getParameter(SQLExportParameters.peakList)
                .getValue().getMatchingPeakLists()[0];
        this.connectionString = parameters.getParameter(
                SQLExportParameters.connectionString).getValue();

        this.tableName = parameters.getParameter(SQLExportParameters.tableName)
                .getValue();
        this.exportColumns = parameters.getParameter(
                SQLExportParameters.exportColumns).getValue();
        this.emptyExport = parameters.getParameter(
                SQLExportParameters.emptyExport).getValue();

    }

    public double getFinishedPercentage() {
        if (totalRows == 0) {
            return 0;
        }
        return (double) processedRows / (double) totalRows;
    }

    public String getTaskDescription() {
        return "Exporting peak list \"" + peakList + "\" to SQL table "
                + tableName;
    }

    public void run() {
        setStatus(TaskStatus.PROCESSING);

        // Get number of rows
        totalRows = peakList.getNumberOfRows();

        try {
            this.dbConnection = DriverManager.getConnection(connectionString);
        } catch (SQLException e) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Error connecting to the SQL database: "
                    + e.toString());
            return;
        }

        PeakListRow rows[] = peakList.getRows();

        try {
            dbConnection.setAutoCommit(false);

            // If select, an empty row with just the raw data file
            // information will be exported
            if (rows.length < 1 && emptyExport) {
                exportPeakListRow(null);
            } else {
                for (PeakListRow row : rows) {
                    if (getStatus() != TaskStatus.PROCESSING)
                        break;
                    exportPeakListRow(row);
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

    private void exportPeakListRow(PeakListRow row) throws SQLException {

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

        PreparedStatement statement = dbConnection.prepareStatement(sql
                .toString());

        if (row == null) {
            for (int i = 0; i < exportColumns.getRowCount(); i++) {
                SQLExportDataType dataType = (SQLExportDataType) exportColumns
                        .getValueAt(i, 1);
                String dataValue = (String) exportColumns.getValueAt(i, 2);
                switch (dataType) {
                case CONSTANT:
                    statement.setString(i + 1, dataValue);
                    break;
                case RAWFILE:
                    RawDataFile rawdatafiles[] = peakList.getRawDataFiles();
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
                Feature peak = row.getPeak(rawDataFile);

                for (int i = 0; i < exportColumns.getRowCount(); i++) {
                    SQLExportDataType dataType = (SQLExportDataType) exportColumns
                            .getValueAt(i, 1);
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
                    case PEAKCHARGE:
                        statement.setDouble(i + 1, peak.getCharge());
                        loopDataFiles = true;
                        break;
                    case PEAKDURATION:
                        statement.setDouble(i + 1, RangeUtils.rangeLength(peak
                                .getRawDataPointsRTRange()));
                        loopDataFiles = true;
                        break;
                    case PEAKSTATUS:
                        statement.setString(i + 1, peak.getFeatureStatus()
                                .name());
                        loopDataFiles = true;
                        break;
                    case PEAKMZ:
                        statement.setDouble(i + 1, peak.getMZ());
                        loopDataFiles = true;
                        break;
                    case PEAKRT:
                        statement.setDouble(i + 1, peak.getRT());
                        loopDataFiles = true;
                        break;
                    case PEAKRT_START:
                        statement.setDouble(i + 1, peak
                                .getRawDataPointsRTRange().lowerEndpoint());
                        loopDataFiles = true;
                        break;
                    case PEAKRT_END:
                        statement.setDouble(i + 1, peak
                                .getRawDataPointsRTRange().upperEndpoint());
                        loopDataFiles = true;
                        break;
                    case PEAKHEIGHT:
                        statement.setDouble(i + 1, peak.getHeight());
                        loopDataFiles = true;
                        break;
                    case PEAKAREA:
                        statement.setDouble(i + 1, peak.getArea());
                        loopDataFiles = true;
                        break;
                    case DATAPOINTS:
                        statement
                                .setDouble(i + 1, peak.getScanNumbers().length);
                        loopDataFiles = true;
                        break;
                    case FWHM:
                        statement.setDouble(i + 1, peak.getFWHM());
                        loopDataFiles = true;
                        break;
                    case TAILINGFACTOR:
                        statement.setDouble(i + 1, peak.getTailingFactor());
                        loopDataFiles = true;
                        break;
                    case ASYMMETRYFACTOR:
                        statement.setDouble(i + 1, peak.getAsymmetryFactor());
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
                        PeakIdentity id = row.getPreferredPeakIdentity();
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
                        DataPoint dataPoints[] = isotopes.getDataPoints();
                        byte bytes[] = ScanUtils
                                .encodeDataPointsToBytes(dataPoints);
                        ByteArrayInputStream is = new ByteArrayInputStream(
                                bytes);
                        statement.setBlob(i + 1, is);
                        break;
                    case MSMS:
                        int msmsScanNum = row.getBestPeak()
                                .getMostIntenseFragmentScanNumber();
                        // Check if there is any MS/MS scan
                        if (msmsScanNum <= 0) {
                            statement.setNull(i + 1, Types.BLOB);
                            break;
                        }
                        RawDataFile dataFile = row.getBestPeak().getDataFile();
                        Scan msmsScan = dataFile.getScan(msmsScanNum);
                        MassList msmsMassList = msmsScan.getMassList(dataValue);
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
                // data files in peak list
                if (!loopDataFiles) {
                    break;
                }
            }
        }
    }
}
