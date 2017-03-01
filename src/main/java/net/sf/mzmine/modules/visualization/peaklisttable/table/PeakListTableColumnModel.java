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

package net.sf.mzmine.modules.visualization.peaklisttable.table;

import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.peaklisttable.ColumnSettingParameter;
import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTableParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.components.ColumnGroup;
import net.sf.mzmine.util.components.GroupableTableHeader;

/**
 * 
 */
public class PeakListTableColumnModel extends DefaultTableColumnModel implements
	MouseListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final Font editFont = new Font("SansSerif", Font.PLAIN, 10);

    private FormattedCellRenderer mzRenderer, rtRenderer, intensityRenderer;
    private TableCellRenderer peakShapeRenderer, identityRenderer,
	    peakStatusRenderer, datapointsRenderer, qcRenderer;
    private DefaultTableCellRenderer defaultRenderer, defaultRendererLeft;

    private ParameterSet parameters;
    private PeakList peakList;
    private GroupableTableHeader header;

    private TableColumn columnBeingResized;

    /**
     * 
     */
    PeakListTableColumnModel(GroupableTableHeader header,
	    PeakListTableModel tableModel, ParameterSet parameters,
	    PeakList peakList) {

	this.parameters = parameters;
	this.peakList = peakList;

	this.header = header;

	header.addMouseListener(this);

	// prepare formatters
	NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
	NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
	NumberFormat intensityFormat = MZmineCore.getConfiguration()
		.getIntensityFormat();

	// prepare cell renderers
	mzRenderer = new FormattedCellRenderer(mzFormat);
	rtRenderer = new FormattedCellRenderer(rtFormat);
	intensityRenderer = new FormattedCellRenderer(intensityFormat);
	peakShapeRenderer = new PeakShapeCellRenderer(peakList, parameters);
	identityRenderer = new CompoundIdentityCellRenderer();
	peakStatusRenderer = new PeakStatusCellRenderer();
	defaultRenderer = new DefaultTableCellRenderer();
	defaultRenderer.setHorizontalAlignment(SwingConstants.CENTER);
	defaultRendererLeft = new DefaultTableCellRenderer();
	defaultRendererLeft.setHorizontalAlignment(SwingConstants.LEFT);
	datapointsRenderer = new FormattedCellRenderer(new DecimalFormat());
	qcRenderer = new FormattedCellRenderer(new DecimalFormat());

    }

    public void createColumns() {

	// clear column groups
	ColumnGroup groups[] = header.getColumnGroups();
	if (groups != null) {
	    for (ColumnGroup group : groups) {
		header.removeColumnGroup(group);
	    }
	}

	// clear the column model
	while (getColumnCount() > 0) {
	    TableColumn col = getColumn(0);
	    removeColumn(col);
	}

	// create the "average" group
	ColumnGroup averageGroup = new ColumnGroup("Average");
	header.addColumnGroup(averageGroup);

	JTextField editorField = new JTextField();
	editorField.setFont(editFont);
	DefaultCellEditor defaultEditor = new DefaultCellEditor(editorField);

	ColumnSettingParameter<CommonColumnType> csPar = parameters
		.getParameter(PeakListTableParameters.commonColumns);
	CommonColumnType visibleCommonColumns[] = csPar.getValue();

	// This is a workaround for a bug - we need to always show the ID, m/z
	// and RT columns, otherwise manual editing of peak identities does not
	// work.
	ArrayList<CommonColumnType> commonColumnsList = new ArrayList<>(
		Arrays.asList(visibleCommonColumns));
	commonColumnsList.remove(CommonColumnType.ROWID);
	commonColumnsList.remove(CommonColumnType.AVERAGEMZ);
	commonColumnsList.remove(CommonColumnType.AVERAGERT);
	commonColumnsList.add(0, CommonColumnType.ROWID);
	commonColumnsList.add(1, CommonColumnType.AVERAGEMZ);
	commonColumnsList.add(2, CommonColumnType.AVERAGERT);

	visibleCommonColumns = commonColumnsList.toArray(visibleCommonColumns);

	ColumnSettingParameter<DataFileColumnType> dfPar = parameters
		.getParameter(PeakListTableParameters.dataFileColumns);
	DataFileColumnType visibleDataFileColumns[] = dfPar.getValue();

	for (int i = 0; i < visibleCommonColumns.length; i++) {

	    CommonColumnType commonColumn = visibleCommonColumns[i];
	    int modelIndex = Arrays.asList(CommonColumnType.values()).indexOf(
		    commonColumn);

	    TableColumn newColumn = new TableColumn(modelIndex);
	    newColumn.setHeaderValue(commonColumn.getColumnName());
	    newColumn.setIdentifier(commonColumn);

	    switch (commonColumn) {
	    case AVERAGEMZ:
		newColumn.setCellRenderer(mzRenderer);
		break;
	    case AVERAGERT:
		newColumn.setCellRenderer(rtRenderer);
		break;
	    case IDENTITY:
		newColumn.setCellRenderer(identityRenderer);
		break;
	    case COMMENT:
		newColumn.setCellRenderer(defaultRendererLeft);
		newColumn.setCellEditor(defaultEditor);
		break;
	    case PEAKSHAPE:
		newColumn.setCellRenderer(peakShapeRenderer);
		break;
	    default:
		newColumn.setCellRenderer(defaultRenderer);
	    }

	    this.addColumn(newColumn);
	    newColumn.setPreferredWidth(csPar.getColumnWidth(modelIndex));
	    if ((commonColumn == CommonColumnType.AVERAGEMZ)
		    || (commonColumn == CommonColumnType.AVERAGERT)) {
		averageGroup.add(newColumn);
	    }

	}

	for (int i = 0; i < peakList.getNumberOfRawDataFiles(); i++) {

	    RawDataFile dataFile = peakList.getRawDataFile(i);
	    ColumnGroup fileGroup = new ColumnGroup(dataFile.getName());
	    header.addColumnGroup(fileGroup);

	    for (int j = 0; j < visibleDataFileColumns.length; j++) {

		DataFileColumnType dataFileColumn = visibleDataFileColumns[j];
		int dataFileColumnIndex = Arrays.asList(
			DataFileColumnType.values()).indexOf(dataFileColumn);
		int modelIndex = CommonColumnType.values().length
			+ (i * DataFileColumnType.values().length)
			+ dataFileColumnIndex;

		TableColumn newColumn = new TableColumn(modelIndex);
		newColumn.setHeaderValue(dataFileColumn.getColumnName());
		newColumn.setIdentifier(dataFileColumn);

		switch (dataFileColumn) {
		case MZ:
		    newColumn.setCellRenderer(mzRenderer);
		    break;
		case PEAKSHAPE:
		    newColumn.setCellRenderer(peakShapeRenderer);
		    break;
		case STATUS:
		    newColumn.setCellRenderer(peakStatusRenderer);
		    break;
		case RT:
		    newColumn.setCellRenderer(rtRenderer);
		    break;
		case RT_START:
		    newColumn.setCellRenderer(rtRenderer);
		    break;
		case RT_END:
		    newColumn.setCellRenderer(rtRenderer);
		    break;
		case DURATION:
		    newColumn.setCellRenderer(rtRenderer);
		    break;
		case HEIGHT:
		    newColumn.setCellRenderer(intensityRenderer);
		    break;
                case AREA:
                    newColumn.setCellRenderer(intensityRenderer);
                    break;
                case CHARGE:
                    newColumn.setCellRenderer(datapointsRenderer);
                    break;
                case DATAPOINTS:
                    newColumn.setCellRenderer(datapointsRenderer);
                    break;
                case FWHM:
                    newColumn.setCellRenderer(qcRenderer);
                    break;
                case TF:
                    newColumn.setCellRenderer(qcRenderer);
                    break;
                case AF:
                    newColumn.setCellRenderer(qcRenderer);
                    break;
                default:
                    newColumn.setCellRenderer(defaultRenderer);
                    break;
		}

		this.addColumn(newColumn);
		newColumn.setPreferredWidth(dfPar
			.getColumnWidth(dataFileColumnIndex));
		fileGroup.add(newColumn);
	    }

	}

    }

    /**
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e) {
	// ignore
    }

    /**
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e) {
	// ignore
    }

    /**
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e) {
	// ignore
    }

    /**
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) {
	columnBeingResized = header.getResizingColumn();
    }

    /**
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e) {

	if (columnBeingResized == null)
	    return;

	ColumnSettingParameter<CommonColumnType> csPar = parameters
		.getParameter(PeakListTableParameters.commonColumns);

	ColumnSettingParameter<DataFileColumnType> dfPar = parameters
		.getParameter(PeakListTableParameters.dataFileColumns);

	final int modelIndex = columnBeingResized.getModelIndex();
	final int newWidth = columnBeingResized.getPreferredWidth();

	final int numOfCommonColumns = CommonColumnType.values().length;
	final int numOfDataFileColumns = DataFileColumnType.values().length;

	if (modelIndex < numOfCommonColumns) {
	    csPar.setColumnWidth(modelIndex, newWidth);
	} else {
	    int dataFileColumnIndex = (modelIndex - numOfCommonColumns)
		    % numOfDataFileColumns;
	    dfPar.setColumnWidth(dataFileColumnIndex, newWidth);

	    // set same width to other data file columns of this type
	    for (int dataFileIndex = peakList.getNumberOfRawDataFiles() - 1; dataFileIndex >= 0; dataFileIndex--) {
		int columnIndex = numOfCommonColumns
			+ (dataFileIndex * numOfDataFileColumns)
			+ dataFileColumnIndex;

		TableColumn col = this.getColumnByModelIndex(columnIndex);

		int currentWidth = col.getPreferredWidth();

		if (currentWidth != newWidth) {
		    col.setPreferredWidth(newWidth);
		}
	    }

	}

    }

    public TableColumn getColumnByModelIndex(int modelIndex) {
	Enumeration<TableColumn> allColumns = this.getColumns();
	while (allColumns.hasMoreElements()) {
	    TableColumn col = allColumns.nextElement();
	    if (col.getModelIndex() == modelIndex)
		return col;
	}
	return null;
    }

}