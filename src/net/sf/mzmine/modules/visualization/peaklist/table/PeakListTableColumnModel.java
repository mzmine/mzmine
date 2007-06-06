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
import java.util.Enumeration;

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

    
    private PeakListTable table;
    private PeakListTableParameters parameters;
    private PeakList peakList;
    
    /**

     */
    PeakListTableColumnModel(PeakListTable table, PeakListTableParameters parameters,
            PeakList peakList) {
            this.table = table;
            this.parameters = parameters;
            this.peakList = peakList;
    }

    public void createColumns() {

        // clear the column model
        Enumeration<TableColumn> currentColumns = this.getColumns(); 
        while (currentColumns.hasMoreElements()) {
            removeColumn(currentColumns.nextElement());
        }
        
        // clear the header
        GroupableTableHeader header = (GroupableTableHeader) table.getTableHeader();
        header.removeAll();
        
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
        peakShapeRenderer = new PeakShapeCellRenderer(peakList, parameters);
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
            
            if (! parameters.isColumnVisible(commonColumn)) continue;
            
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
                
                if (! parameters.isColumnVisible(dataFileColumn)) continue;
                
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

    
}