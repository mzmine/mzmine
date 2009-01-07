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

package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JWindow;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.util.components.CombinedXICComponent;

public class ScatterPlotToolTipComponent extends JPanel {
	
	private static DecimalFormat formatter = new DecimalFormat("###.#");
	private static DecimalFormat formatter2 = new DecimalFormat("###");
	private static DecimalFormat formatter3 = new DecimalFormat("###.##");
	
	public ScatterPlotToolTipComponent(int index, ScatterPlotDataSet dataSet, int fold){
		
		super();
		setLayout(new BorderLayout());
		
		Font defaultFont = new Font("SansSerif.bold", Font.PLAIN, 10);
		Font titleFont = new Font("SansSerif.bold", Font.PLAIN, defaultFont.getSize()+ 1);
		Font ratioFont = new Font("SansSerif.bold", Font.PLAIN, defaultFont.getSize()+ 7);
		Font tableFont = new Font("SansSerif.bold", Font.PLAIN, defaultFont.getSize()-1);
		
		// Get info
		int[] indexDomains = dataSet.getDomainsIndexes();
		RawDataFile[] rawDataFiles = dataSet.getPeakList()
				.getRawDataFiles();
		int indX = indexDomains[0];
		int indY = indexDomains[1];
		PeakListRow row = (SimplePeakListRow) dataSet.getPeakList().getRow(index);
		ChromatographicPeak[] peaks = new ChromatographicPeak[2];
		peaks[0] = row.getPeak(rawDataFiles[indY]);
		peaks[1] = row.getPeak(rawDataFiles[indX]);
		PeakIdentity identity = row.getPreferredCompoundIdentity();


		// Header
		// Peak identification
		JPanel headerPanel = new JPanel(new BorderLayout());
		JLabel name, formula, idMethod;
		if (identity != null) {
			name = new JLabel(identity.getName());
			formula = new JLabel("Formula: " + identity.getCompoundFormula());
			idMethod = new JLabel("Identification method: "
					+ identity.getIdentificationMethod());
		} else {
			name = new JLabel("Unknown");
			formula = new JLabel("Formula: ");
			idMethod = new JLabel(" ");
		}
		
		name.setFont(titleFont);
		
		headerPanel.add(name, BorderLayout.NORTH);
		headerPanel.add(formula, BorderLayout.CENTER);
		headerPanel.add(idMethod, BorderLayout.SOUTH);
		
		// Ratio between peaks
		JPanel ratioPanel = new JPanel(new BorderLayout());
		JLabel ratio;

		double height1 = 1, height2 = 1;
		if (peaks[0] != null){
			height1 = peaks[0].getHeight();
		}
		if (peaks[1] != null){
			height2 = peaks[1].getHeight();
		}

		if ((height1 < 0.0001f) || (height2 < 0.0001f)) {
			ratio = new JLabel(" ");
		} else {

			double ratioValue = height1 / height2;

			String text = formatter.format(ratioValue) + "x";
			Color ratioColor = Color.BLACK;

			if (ratioValue > fold) {
				ratioColor = Color.RED;
				text = formatter2.format(ratioValue) + "x";
			}
			if (ratioValue < ((float) 1 / fold)) {
				ratioColor = Color.BLUE;
				text = formatter3.format(ratioValue) + "x";
			}

			ratio = new JLabel(text);
			ratio.setFont(ratioFont);
			ratio.setForeground(ratioColor);
		}
		
		ratioPanel.add(ratio, BorderLayout.CENTER);
		
		JPanel headerAndRatioPanel = new JPanel();
		headerAndRatioPanel.add(headerPanel);
		headerAndRatioPanel.add(ratioPanel);
		add(headerAndRatioPanel, BorderLayout.NORTH);
		
		// Plot
		JPanel plotPanel = new JPanel(new BorderLayout());
		Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		Border two = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		plotPanel.setBorder(BorderFactory.createCompoundBorder(one, two));
		plotPanel.setBackground(Color.white);
        CombinedXICComponent xic = new CombinedXICComponent(row);
        plotPanel.add(xic, BorderLayout.CENTER);
        //plotPanel.setPreferredSize(new Dimension(200, 50));
        add(plotPanel, BorderLayout.CENTER);

        // Table with peak's information
		JPanel tablePanel = new JPanel(new BorderLayout());
        
		PeaksInfoTableModel listElementModel = new PeaksInfoTableModel();
		for (ChromatographicPeak peak: row.getPeaks()){
			listElementModel.addElement(peak);
		}
		JTable peaksInfoList = new JTable();
		peaksInfoList.setModel(listElementModel);
		peaksInfoList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		int indexListRow = listElementModel.getIndexRow(rawDataFiles[indY].getFileName());
		peaksInfoList.getSelectionModel().setSelectionInterval(indexListRow, indexListRow + 1);
		indexListRow = listElementModel.getIndexRow(rawDataFiles[indX].getFileName());
		peaksInfoList.getSelectionModel().addSelectionInterval(indexListRow, indexListRow + 1);
		peaksInfoList.setRowSelectionAllowed(false);
		peaksInfoList.setColumnSelectionAllowed(false);
		peaksInfoList.setCellSelectionEnabled(false);
		peaksInfoList.setFont(tableFont);

		tablePanel.add(peaksInfoList, BorderLayout.CENTER);
		
		add(tablePanel, BorderLayout.SOUTH);
		
		this.setVisible(true);
	}

}
