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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

public class ScatterPlotToolTipGenerator implements XYToolTipGenerator {

	private SimpleDateFormat sdf;
	private int fold, indX, indY;
	private double X, Y, ratio, value;
	private String tipText, color, ratioText, valueTxt, name, additionalInfo;
	private DecimalFormat formatter, formatter2, formatter3, formatter4;
	private SimplePeakListRow row;
	private boolean enable = true;
	private RawDataFile[] rawDataFiles;
	private double[] intensities;

	public ScatterPlotToolTipGenerator(int fold) {
		this.fold = fold;
		sdf = new SimpleDateFormat("m:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));

		formatter = new DecimalFormat("###.#");
		formatter2 = new DecimalFormat("###");
		formatter3 = new DecimalFormat("###.##");
		formatter4 = new DecimalFormat("###.####");

	}

	public String generateToolTip(XYDataset dataset, int series, int item) {

		if (!enable)
			return null;

		if (dataset instanceof ScatterPlotDataSet) {

			clean();

			row = (SimplePeakListRow) ((ScatterPlotDataSet) dataset)
					.getPeakListRow(series, item);
			
			intensities = ((ScatterPlotDataSet) dataset)
			.getRowIntensities(item);
			
			String massText = MZmineCore.getMZFormat().format(
					row.getAverageMZ());
			String timeText = MZmineCore.getRTFormat().format(
					row.getAverageRT());

			PeakIdentity identity = row.getPreferredCompoundIdentity();
			if (identity != null) {
				name = identity.getName();
				additionalInfo = "Formula: " + identity.getCompoundFormula()
						+ ", identification method: "
						+ identity.getIdentificationMethod();
			} else {
				name = "Unknown";
				additionalInfo = "";
			}

			tipText = "<html><div style=\"text-align:left;align:center;width:220px\">";
			tipText += "<b><font size=\"3\">" + name
					+ "</font></b><br><font size=\"2\">";
			tipText += additionalInfo + "<br>";
			tipText += "Mass = " + massText + "<br>";
			tipText += "RT = " + timeText + "<br>";

			X = intensities[indX];
			Y = intensities[indY];
			ratio = Y / X;

			color = "black";
			ratioText = formatter.format(ratio) + "x";

			if (ratio > fold) {
				color = "red";
				ratioText = formatter2.format(ratio) + "x";
			}
			if (ratio < ((float) 1 / fold)) {
				color = "blue";
				ratioText = formatter3.format(ratio) + "x";
			}
			if ((Y < 0.0001f) || (X < 0.0001f))
				ratioText = " ";

			tipText += "</font></div></center> <table border=\"0\" width=\"220px\"><tr><td align=\"right\" valign=\"top\">";
			tipText += "<tr><td>";

			for (int i = 0; i < rawDataFiles.length; i++) {
				value = intensities[i];
				if (X > 1.0)
					valueTxt = formatter.format(value);
				else
					valueTxt = formatter4.format(value);

				if ((i == indX) || (i == indY))
					tipText += "<b>" + rawDataFiles[i].getFileName() + " = "
							+ valueTxt + "</b><br>";
				else
					tipText += rawDataFiles[i].getFileName() + " = " + valueTxt
							+ "<br>";
			}

			tipText += "</td><td align=\"right\" rowspan=\"2\"><b><font size=\"6\" color=\""
					+ color + "\"> ";
			tipText += ratioText + " </font></b></td></tr></table>";
			tipText += "</html>";

		}

		if (dataset instanceof DiagonalPlotDataset) {
			tipText = ((DiagonalPlotDataset) dataset).getToolTipText();
		}

		clean();
		return tipText;

	}

	public void clean() {
		row = null;
		tipText = null;
		name = null;
		additionalInfo = null;
		color = null;
		ratioText = null;
		valueTxt = null;
		value = 0;
	}

	public void setDataFile(ScatterPlotDataSet newSet) {
		int[] indexDomains = newSet.getDomainsIndexes();
		rawDataFiles = newSet.getPeakList().getRawDataFiles();
		indX = indexDomains[0];
		indY = indexDomains[1];
	}

	public void setSelectedFold(int fold) {
		this.fold = fold;
	}

	public void setDomainsIndexes(int x, int y) {
		indX = x;
		indY = y;
	}

	public void setEnable(boolean flag) {
		enable = flag;
	}

}
