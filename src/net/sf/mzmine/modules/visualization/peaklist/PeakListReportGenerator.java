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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.URL;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.peaklist.table.CommonColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.DataFileColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTable;
import net.sf.mzmine.util.components.ColorCircle;
import net.sf.mzmine.util.components.PeakXICComponent;

import org.jfree.report.Element;
import org.jfree.report.ElementAlignment;
import org.jfree.report.GroupHeader;
import org.jfree.report.JFreeReport;
import org.jfree.report.TableDataFactory;
import org.jfree.report.elementfactory.ComponentFieldElementFactory;
import org.jfree.report.elementfactory.ElementFactory;
import org.jfree.report.elementfactory.LabelElementFactory;
import org.jfree.report.elementfactory.NumberFieldElementFactory;
import org.jfree.report.elementfactory.TextFieldElementFactory;
import org.jfree.report.function.AbstractExpression;
import org.jfree.report.function.Expression;
import org.jfree.report.modules.parser.base.ReportGenerator;
import org.jfree.resourceloader.ResourceException;
import org.jfree.xml.ElementDefinitionException;

/**
 * 
 */
class PeakListReportGenerator {

    static final ColorCircle greenCircle = new ColorCircle(Color.green);
    static final ColorCircle redCircle = new ColorCircle(Color.red);
    static final ColorCircle yellowCircle = new ColorCircle(Color.yellow);

    private PeakListTable table;
    private PeakListTableParameters parameters;

    private int xPosition;
    private int rowHeight;

    PeakListReportGenerator(PeakListTable table,
            PeakListTableParameters parameters) {
        this.table = table;
        this.parameters = parameters;
    }

    JFreeReport generateReport() throws IOException,
            ElementDefinitionException, ResourceException {


         ReportGenerator gener = ReportGenerator.getInstance();
        
        URL def = getClass().getResource("print-report-definition.xml");
        
        if (def == null)
        	throw new IOException("Could not load \"print-report-definition.xml\"");

        JFreeReport report = gener.parseReport(def);
        
        TableModel tableModel = table.getModel();
        report.setDataFactory(new TableDataFactory("default", tableModel));

        xPosition = 0;
        rowHeight = table.getRowHeight();

        // column model contains all visible columns
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            addColumnToReport(report, column);
        }

        return report;

    }

    private void addColumnToReport(JFreeReport report, TableColumn column) {

        int modelIndex = column.getModelIndex();
        final String fieldName = "column" + modelIndex;
        Object columnIdentifier = column.getIdentifier();
        int columnWidth = column.getWidth();
        Dimension fieldSize = new Dimension(columnWidth, rowHeight);
        Point2D position = new Point2D.Double(xPosition, 0);
        xPosition += columnWidth;

        ElementFactory newElementFac = null;

        // add label to header
        GroupHeader header = report.getGroup(0).getHeader();
        LabelElementFactory labelFac = new LabelElementFactory();
        labelFac.setText((String) column.getHeaderValue());
        labelFac.setMinimumSize(fieldSize);
        labelFac.setAbsolutePosition(position);
        header.addElement(labelFac.createElement());

        if (columnIdentifier instanceof CommonColumnType) {
            CommonColumnType columnType = (CommonColumnType) columnIdentifier;
            switch (columnType) {

            case AVERAGEMZ:
                NumberFieldElementFactory mzFac = new NumberFieldElementFactory();
                mzFac.setVerticalAlignment(ElementAlignment.MIDDLE);
                mzFac.setFieldname(fieldName);
                mzFac.setFormat(MZmineCore.getMZFormat());
                newElementFac = mzFac;
                break;

            case AVERAGERT:
                NumberFieldElementFactory rtFac = new NumberFieldElementFactory();
                rtFac.setVerticalAlignment(ElementAlignment.MIDDLE);
                rtFac.setFieldname(fieldName);
                rtFac.setFormat(MZmineCore.getRTFormat());
                newElementFac = rtFac;
                break;

            case ROWID:
            case COMMENT:
            case IDENTITY:
                TextFieldElementFactory textFac = new TextFieldElementFactory();
                textFac.setVerticalAlignment(ElementAlignment.MIDDLE);
                textFac.setFieldname(fieldName);
                textFac.setNullString("");
                newElementFac = textFac;
                break;

            default: 
                // Ignore unknown columns
                return;

            }
        }
        
        if (columnIdentifier instanceof DataFileColumnType) {
            DataFileColumnType columnType = (DataFileColumnType) columnIdentifier;
            switch (columnType) {

            case STATUS:
                Expression peakStatusExpression = new AbstractExpression() {

                    public Object getValue() {
                        PeakStatus status = (PeakStatus) getDataRow().get(
                                fieldName);
                        switch (status) {
                        case DETECTED:
                            return greenCircle;
                        case ESTIMATED:
                            return yellowCircle;
                        default:
                            return redCircle;
                        }
                    }
                };
                final String statusExpressionName = "expression" + modelIndex;
                peakStatusExpression.setName(statusExpressionName);
                report.addExpression(peakStatusExpression);

                ComponentFieldElementFactory statusFac = new ComponentFieldElementFactory();
                statusFac.setFieldname(statusExpressionName);
                newElementFac = statusFac;
                break;

            case PEAKSHAPE:
                Expression peakShapeExpression = new AbstractExpression() {

                    public Object getValue() {

                        ChromatographicPeak peak = (ChromatographicPeak) getDataRow().get(fieldName);
                        if (peak == null)
                            return null;

                        double maxHeight;
                        PeakList peakList = table.getPeakList();
                        switch (parameters.getPeakShapeNormalization()) {
                        case GLOBALMAX:
                            maxHeight = peakList.getDataPointMaxIntensity();
                            break;
                        case ROWMAX:
                            int rowNumber = peakList.getPeakRowNum(peak);
                            maxHeight = peakList.getRow(rowNumber).getDataPointMaxIntensity();
                            break;
                        default:
                            maxHeight = peak.getRawDataPointsIntensityRange().getMax();
                            break;
                        }

                        return new PeakXICComponent(peak, maxHeight);

                    }
                };
                final String shapeExpressionName = "expression" + modelIndex;
                peakShapeExpression.setName(shapeExpressionName);
                report.addExpression(peakShapeExpression);

                ComponentFieldElementFactory shapeFac = new ComponentFieldElementFactory();
                shapeFac.setFieldname(shapeExpressionName);
                newElementFac = shapeFac;
                break;

            case MZ:
                NumberFieldElementFactory mzFac = new NumberFieldElementFactory();
                mzFac.setVerticalAlignment(ElementAlignment.MIDDLE);
                mzFac.setFieldname(fieldName);
                mzFac.setFormat(MZmineCore.getMZFormat());
                newElementFac = mzFac;
                break;

            case RT:
            case DURATION:
                NumberFieldElementFactory rtFac = new NumberFieldElementFactory();
                rtFac.setVerticalAlignment(ElementAlignment.MIDDLE);
                rtFac.setFieldname(fieldName);
                rtFac.setFormat(MZmineCore.getRTFormat());
                newElementFac = rtFac;
                break;

            case HEIGHT:
            case AREA:
                NumberFieldElementFactory intFac = new NumberFieldElementFactory();
                intFac.setVerticalAlignment(ElementAlignment.MIDDLE);
                intFac.setFieldname(fieldName);
                intFac.setFormat(MZmineCore.getIntensityFormat());
                newElementFac = intFac;
                break;
            
            default: 
                // Ignore unknown columns
                return;
            
            }

        }

        newElementFac.setAbsolutePosition(position);
        newElementFac.setMinimumSize(fieldSize);

        Element newItem = newElementFac.createElement();
        report.getItemBand().addElement(newItem);

    }
}
