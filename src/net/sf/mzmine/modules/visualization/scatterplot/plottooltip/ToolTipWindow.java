/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.scatterplot.plottooltip;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.text.DecimalFormat;
import java.text.Format;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JWindow;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.scatterplot.plotdatalabel.ScatterPlotDataSet;
import net.sf.mzmine.util.components.CombinedXICComponent;

public class ToolTipWindow extends JWindow {

	private static DecimalFormat formatter = new DecimalFormat("###.#");
	private static Color bg = new Color(255, 250, 205);
	private static Font defaultFont = new Font("SansSerif", Font.PLAIN, 10);
	private static Font titleFont = new Font("SansSerif.bold", Font.PLAIN, defaultFont
			.getSize() + 4);
	private static Font ratioFont = new Font("SansSerif.bold", Font.PLAIN, defaultFont
			.getSize() + 9);


	/**
	 * 
	 * @param index
	 * @param dataSet
	 * @param fold
	 * @param frame
	 */
	public ToolTipWindow(int index, ScatterPlotDataSet dataSet, int fold,
			Frame frame) {

		super(frame);

		setBackground(bg);

		JPanel pnlAll = new JPanel(new BorderLayout());
		pnlAll.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		pnlAll.setBackground(bg);


		// Get info
		int[] indexDomains = dataSet.getDomainsIndexes();
		RawDataFile[] rawDataFiles = dataSet.getPeakList().getRawDataFiles();
		int indX = indexDomains[0];
		int indY = indexDomains[1];
		PeakListRow row = (SimplePeakListRow) dataSet.getPeakList().getRow(
				index);
		ChromatographicPeak[] peaks = new ChromatographicPeak[2];
		peaks[0] = row.getPeak(rawDataFiles[indX]);
		peaks[1] = row.getPeak(rawDataFiles[indY]);
		PeakIdentity identity = row.getPreferredCompoundIdentity();

		// Header
		// Peak identification
		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		JLabel name, info;
		if (identity != null) {
			name = new JLabel(identity.getName(), SwingUtilities.LEFT);
	        StringBuffer buf = new StringBuffer();
	        Format mzFormat = MZmineCore.getMZFormat();
	        Format timeFormat = MZmineCore.getRTFormat();
	        buf.append("#" + row.getID() + " ");
	        buf.append(mzFormat.format(row.getAverageMZ()));
	        buf.append(" m/z @");
	        buf.append(timeFormat.format(row.getAverageRT()));
	        info = new JLabel(buf.toString(),	SwingUtilities.LEFT);
			info.setBackground(bg);
			info.setFont(defaultFont);
			headerPanel.add(name, BorderLayout.NORTH);
			headerPanel.add(info, BorderLayout.CENTER);
		} else {
			name = new JLabel(row.toString(), SwingUtilities.LEFT);
			headerPanel.add(name, BorderLayout.CENTER);
		}

		name.setFont(titleFont);
		name.setBackground(bg);
		headerPanel.setBackground(bg);
		headerPanel.setPreferredSize(new Dimension(290, 50));

		// Ratio between peaks
		JPanel ratioPanel = new JPanel(new BorderLayout());
		JLabel ratio;

		double height1 = -1, height2 = -1;
		if (peaks[0] != null) {
			height1 = peaks[0].getHeight();
		}
		if (peaks[1] != null) {
			height2 = peaks[1].getHeight();
		}

		if ((height1 < 0) || (height2 < 0)) {
			ratio = new JLabel("   ");
		} else {
			
			String text = null;
			Color ratioColor = null;
			
			if (height1 > height2){
				 text = formatter.format(height1 / height2) + "x";
				 ratioColor = CombinedXICComponent.plotColors[0];
			}
			else{
				 text = formatter.format(height2 / height1) + "x";
				 ratioColor = CombinedXICComponent.plotColors[1];
			}

			ratio = new JLabel(text, SwingUtilities.LEFT);
			ratio.setFont(ratioFont);
			ratio.setForeground(ratioColor);
			ratio.setBackground(bg);
		}

		if (ratio != null)
			ratioPanel.add(ratio, BorderLayout.CENTER);
		ratioPanel.setBackground(bg);

		JPanel headerAndRatioPanel = new JPanel(new BorderLayout());
		headerAndRatioPanel.add(headerPanel, BorderLayout.WEST);
		headerAndRatioPanel.add(Box.createVerticalGlue(), BorderLayout.CENTER);
		headerAndRatioPanel.add(ratioPanel, BorderLayout.EAST);
		headerAndRatioPanel.setBackground(bg);
		pnlAll.add(headerAndRatioPanel, BorderLayout.NORTH);

		// Plot
		JPanel plotPanel = new JPanel();
		plotPanel.setLayout(new BoxLayout(plotPanel, BoxLayout.Y_AXIS));
		Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		Border two = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		plotPanel.setBorder(BorderFactory.createCompoundBorder(one, two));
		plotPanel.setBackground(Color.white);
		CombinedXICComponent xic = new CombinedXICComponent(peaks);
		plotPanel.add(xic);
		pnlAll.add(plotPanel, BorderLayout.CENTER);

		// Table with peak's information
		JPanel tablePanel = new JPanel();
		tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
		tablePanel.setBackground(bg);

		ToolTipTableModel listElementModel = new ToolTipTableModel();
		JTable peaksInfoList = new JTable();
		peaksInfoList.setModel(listElementModel);
		peaksInfoList
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		peaksInfoList.setSelectionBackground(Color.LIGHT_GRAY);
		peaksInfoList.setDefaultRenderer(Object.class,
				new ToolTipTableCellRenderer());

		int countLines = 0;
		for (ChromatographicPeak peak : peaks) {
			if (peak != null) {
				listElementModel.addElement(peak);
				countLines++;
			}
		}

		JPanel listPanel = new JPanel(new BorderLayout());
		listPanel.add(peaksInfoList, BorderLayout.CENTER);
		listPanel.add(peaksInfoList.getTableHeader(), BorderLayout.NORTH);
		listPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

		Dimension preffDimension = calculatedTableDimension(peaksInfoList);
		xic.setPreferredSize(new Dimension(preffDimension.width, 50));
		listPanel.setPreferredSize(preffDimension);

		tablePanel.add(Box.createVerticalStrut(5));
		tablePanel.add(listPanel, BorderLayout.CENTER);
		tablePanel.setBackground(bg);

		pnlAll.add(tablePanel, BorderLayout.SOUTH);
		add(pnlAll);

		setPreferredSize(new Dimension(preffDimension.width,
				preffDimension.height + 170));
		pack();
	}

	/**
	 * 
	 * @param peaksInfoList
	 * @return
	 */
	private Dimension calculatedTableDimension(JTable peaksInfoList) {

		int numRows = peaksInfoList.getRowCount();
		int numCols = peaksInfoList.getColumnCount();
		int maxWidth = 0, compWidth, totalWidth = 0, totalHeight = 0;
		TableCellRenderer renderer = peaksInfoList
				.getDefaultRenderer(Object.class);
		TableCellRenderer headerRenderer = peaksInfoList.getTableHeader()
				.getDefaultRenderer();
		TableModel model = peaksInfoList.getModel();
		Component comp;
		TableColumn column;

		for (int c = 0; c < numCols; c++) {
			for (int r = 0; r < numRows; r++) {

				if (r == 0) {
					comp = headerRenderer.getTableCellRendererComponent(
							peaksInfoList, model.getColumnName(c), false,
							false, r, c);
					compWidth = comp.getPreferredSize().width + 10;
					maxWidth = Math.max(maxWidth, compWidth);

				}

				comp = renderer.getTableCellRendererComponent(peaksInfoList,
						model.getValueAt(r, c), false, false, r, c);

				compWidth = comp.getPreferredSize().width + 10;
				maxWidth = Math.max(maxWidth, compWidth);


				if (c == 0) {
					totalHeight += comp.getPreferredSize().height;
				}
			}
			totalWidth += maxWidth;
			column = peaksInfoList.getColumnModel().getColumn(c);
			column.setPreferredWidth(maxWidth);
			maxWidth = 0;
		}

		comp = headerRenderer.getTableCellRendererComponent(peaksInfoList,
				model.getColumnName(0), false, false, 0, 0);
		totalHeight += comp.getPreferredSize().height;

		return new Dimension(totalWidth, totalHeight);

	}

}
