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

package net.sf.mzmine.modules.visualization.peaklist.table;

import java.awt.Font;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableParameters;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.ColumnGroup;
import net.sf.mzmine.userinterface.components.GroupableTableHeader;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.NumberFormatter;

public class PeakListTableColumnModel extends DefaultTableColumnModel {

    private static final Font editFont = new Font("SansSerif", Font.PLAIN, 10);

    private TableCellRenderer mzRenderer, rtRenderer, intensityRenderer,
            peakShapeRenderer, identityRenderer, peakStatusRenderer;
    private DefaultTableCellRenderer defaultRenderer;

    void createColumns(PeakListTable table, PeakListTableParameters parameters,
            PeakList peakList, GroupableTableHeader header) {

        ColumnGroup averageGroup = new ColumnGroup("Average");
        header.addColumnGroup(averageGroup);

        // prepare formatters
        Desktop desktop = MainWindow.getInstance();
        NumberFormatter mzFormat = desktop.getMZFormat();
        NumberFormatter rtFormat = desktop.getRTFormat();
        NumberFormatter intensityFormat = desktop.getIntensityFormat();

        // prepare cell renderers
        mzRenderer = new FormattedCellRenderer(mzFormat);
        rtRenderer = new FormattedCellRenderer(rtFormat);
        intensityRenderer = new FormattedCellRenderer(intensityFormat);
        peakShapeRenderer = new PeakShapeCellRenderer();
        identityRenderer = new CompoundIdentityCellRenderer();
        peakStatusRenderer = new PeakStatusCellRenderer();
        defaultRenderer = new DefaultTableCellRenderer();
        defaultRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        
        JTextField editorField = new JTextField();
        editorField.setFont(editFont);
        DefaultCellEditor defaultEditor = new DefaultCellEditor(editorField);
        // defaultEditor.setClickCountToStart(1);
        
        int modelIndex = 0;

        for (CommonColumnType commonColumn : CommonColumnType.values()) {
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
                newColumn.setCellRenderer(identityRenderer);
                break;
            case COMMENT:
                newColumn.setCellRenderer(defaultRenderer);
                newColumn.setCellEditor(defaultEditor);
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

            for (DataFileColumnType dataFileColumn : DataFileColumnType.values()) {
                int width = parameters.getColumnWidth(dataFileColumn);
                TableColumn newColumn = new TableColumn(modelIndex, width);
                newColumn.setHeaderValue(dataFileColumn.getColumnName());
                fileGroup.add(newColumn);

                if (dataFileColumn == DataFileColumnType.MZ) {
                    newColumn.setCellRenderer(mzRenderer);
                }
                if (dataFileColumn == DataFileColumnType.PEAKSHAPE) {
                    newColumn.setCellRenderer(peakShapeRenderer);
                }
                if (dataFileColumn == DataFileColumnType.STATUS) {
                    newColumn.setCellRenderer(peakStatusRenderer);
                }
                if (dataFileColumn == DataFileColumnType.RT) {
                    newColumn.setCellRenderer(rtRenderer);
                }
                if (dataFileColumn == DataFileColumnType.DURATION) {
                    newColumn.setCellRenderer(rtRenderer);
                }
                if (dataFileColumn == DataFileColumnType.HEIGHT) {
                    newColumn.setCellRenderer(intensityRenderer);
                }
                if (dataFileColumn == DataFileColumnType.AREA) {
                    newColumn.setCellRenderer(intensityRenderer);
                }

                this.addColumn(newColumn);
                modelIndex++;
            }
        }

    }

    boolean isCommonColumn(int col) {
        return col < CommonColumnType.values().length;
    }

    CommonColumnType getCommonColumn(int col) {

        CommonColumnType commonColumns[] = CommonColumnType.values();

        if (col < commonColumns.length)
            return commonColumns[col];

        return null;

    }

    DataFileColumnType getDataFileColumn(int col) {

        CommonColumnType commonColumns[] = CommonColumnType.values();
        DataFileColumnType dataFileColumns[] = DataFileColumnType.values();

        if (col < commonColumns.length)
            return null;

        // substract common columns from the index
        col -= commonColumns.length;

        // divide by number of data file columns
        col %= dataFileColumns.length;

        return dataFileColumns[col];

    }

}