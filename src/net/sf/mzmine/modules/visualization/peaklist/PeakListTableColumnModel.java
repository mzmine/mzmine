/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.peaklist;

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.ColorCircle;
import net.sf.mzmine.userinterface.components.ColumnGroup;
import net.sf.mzmine.userinterface.components.ComponentCellRenderer;
import net.sf.mzmine.userinterface.components.FormattedCellRenderer;
import net.sf.mzmine.userinterface.components.GroupableTableHeader;
import net.sf.mzmine.userinterface.components.PeakXICComponent;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

public class PeakListTableColumnModel extends DefaultTableColumnModel {

    // type for common columns
    public enum CommonColumns {

        ROWID("ID", Integer.class), AVERAGEMZ("m/z", Double.class), AVERAGERT(
                "Retention time", Double.class), IDENTITY("Identity",
                String.class), COMMENT("Comment", String.class);

        private final String columnName;
        private final Class columnClass;

        CommonColumns(String columnName, Class columnClass) {
            this.columnName = columnName;
            this.columnClass = columnClass;
        }

        public String getColumnName() {
            return columnName;
        }

        public Class getColumnClass() {
            return columnClass;
        }
    };

    public enum DataFileColumns {

        STATUS("", ColorCircle.class), PEAKSHAPE("Peak shape",
                PeakXICComponent.class), MZ("m/z", Double.class), RT(
                "Retention time", Double.class), DURATION("Duration",
                Double.class), HEIGHT("Height", Double.class), AREA("Area",
                Double.class);

        private final String columnName;
        private final Class columnClass;

        DataFileColumns(String columnName, Class columnClass) {
            this.columnName = columnName;
            this.columnClass = columnClass;
        }

        public String getColumnName() {
            return columnName;
        }

        public Class getColumnClass() {
            return columnClass;
        }

    };

    private GroupableTableHeader header;

    PeakListTableColumnModel(PeakListTable table,
            PeakListTableParameters parameters, PeakList peakList) {

        header = new GroupableTableHeader(this);
        table.setTableHeader(header);
        ColumnGroup averageGroup = new ColumnGroup("Average");
        header.addColumnGroup(averageGroup);

        Desktop desktop = MainWindow.getInstance();
        FormattedCellRenderer mzRenderer = new FormattedCellRenderer(
                desktop.getMZFormat());
        FormattedCellRenderer rtRenderer = new FormattedCellRenderer(
                desktop.getRTFormat());
        FormattedCellRenderer intensityRenderer = new FormattedCellRenderer(
                desktop.getIntensityFormat());
        ComponentCellRenderer componentRenderer = new ComponentCellRenderer();
        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
        defaultRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        int modelIndex = 0;

        for (CommonColumns commonColumn : CommonColumns.values()) {
            int width = parameters.getColumnWidth(commonColumn);
            TableColumn newColumn = new TableColumn(modelIndex, width);
            newColumn.setHeaderValue(commonColumn.getColumnName());
            switch (commonColumn) {
            case AVERAGEMZ:
                newColumn.setCellRenderer(mzRenderer);
                averageGroup.add(newColumn);
                break;
            case AVERAGERT:
                newColumn.setCellRenderer(rtRenderer);
                averageGroup.add(newColumn);
                break;
            case IDENTITY:
                newColumn.setCellRenderer(componentRenderer);
                break;
            default:
                newColumn.setCellRenderer(defaultRenderer);
            }

            this.addColumn(newColumn);
            modelIndex++;

        }

        for (OpenedRawDataFile dataFile : peakList.getRawDataFiles()) {
            ColumnGroup fileGroup = new ColumnGroup(dataFile.toString());
            header.addColumnGroup(fileGroup);

            for (DataFileColumns dataFileColumn : DataFileColumns.values()) {
                int width = parameters.getColumnWidth(dataFileColumn);
                TableColumn newColumn = new TableColumn(modelIndex, width);
                newColumn.setHeaderValue(dataFileColumn.getColumnName());
                fileGroup.add(newColumn);

                if (dataFileColumn == DataFileColumns.MZ) {
                    newColumn.setCellRenderer(mzRenderer);
                }
                if (dataFileColumn == DataFileColumns.PEAKSHAPE) {
                    newColumn.setCellRenderer(componentRenderer);
                }
                if (dataFileColumn == DataFileColumns.STATUS) {
                    newColumn.setCellRenderer(componentRenderer);
                }
                if (dataFileColumn == DataFileColumns.RT) {
                    newColumn.setCellRenderer(rtRenderer);
                }
                if (dataFileColumn == DataFileColumns.DURATION) {
                    newColumn.setCellRenderer(rtRenderer);
                }
                if (dataFileColumn == DataFileColumns.HEIGHT) {
                    newColumn.setCellRenderer(intensityRenderer);
                }
                if (dataFileColumn == DataFileColumns.AREA) {
                    newColumn.setCellRenderer(intensityRenderer);
                }

                this.addColumn(newColumn);
                modelIndex++;
            }
        }

    }

    GroupableTableHeader getTableHeader() {
        return header;
    }

}