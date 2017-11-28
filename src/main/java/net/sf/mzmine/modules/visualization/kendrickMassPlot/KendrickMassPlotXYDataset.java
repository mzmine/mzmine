package net.sf.mzmine.modules.visualization.kendrickMassPlot;

import org.jfree.data.xy.AbstractXYDataset;
import org.jmol.util.Logger;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.ParameterSet;

class KendrickMassPlotXYDataset extends AbstractXYDataset{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private PeakListRow selectedRows[];
	private String yAxisKMBase;
	private String xAxisKMBase;
	private double xAxisKMFactor=-1;
	private double yAxisKMFactor=-1;
	private double[] xValues;
	private double[] yValues;

	public KendrickMassPlotXYDataset(ParameterSet parameters) {

		PeakList peakList = parameters
				.getParameter(KendrickMassPlotParameters.peakList).getValue()
				.getMatchingPeakLists()[0];

		this.selectedRows = parameters
				.getParameter(KendrickMassPlotParameters.selectedRows).getMatchingRows(peakList);

		this.yAxisKMBase = parameters
				.getParameter(KendrickMassPlotParameters.yAxisValues).getValue();

		this.xAxisKMBase = parameters
				.getParameter(KendrickMassPlotParameters.xAxisValues).getValue();

		//Calc xValues
		xValues = new double[selectedRows.length];
		for (int i = 0; i < selectedRows.length; i++) {
			//simply plot m/z values as x axis
			if(xAxisKMBase.equals("m/z")) {
				xValues[i] = selectedRows[i].getAverageMZ();
			}
			//plot Kendrick masses as x axis
			else if(xAxisKMBase.equals("KM")) {
				xValues[i] = selectedRows[i].getAverageMZ()*getxAxisKMFactor(xAxisKMBase);
			}
			//plot Kendrick mass defect (KMD) as x Axis to the base of CH2
			else if(xAxisKMBase.equals("KMD (H)")) {
				xValues[i] = (((int)selectedRows[i].getAverageMZ()*getxAxisKMFactor(xAxisKMBase)+1)-selectedRows[i].getAverageMZ()*getxAxisKMFactor(xAxisKMBase));
			}
			//plot Kendrick mass defect (KMD) as x Axis to the base of H
			else if(xAxisKMBase.equals("KMD (CH2)")) {
				xValues[i] = (((int)selectedRows[i].getAverageMZ()*getxAxisKMFactor(xAxisKMBase)+1)-selectedRows[i].getAverageMZ()*getxAxisKMFactor(xAxisKMBase));
			}
			//plot Kendrick mass defect (KMD) as x Axis to the base of O
			else if(xAxisKMBase.equals("KMD (O)")) {
				xValues[i] = (((int)selectedRows[i].getAverageMZ()*getxAxisKMFactor(xAxisKMBase)+1)-selectedRows[i].getAverageMZ()*getxAxisKMFactor(xAxisKMBase));
			}
		}

		//Calc yValues
		yValues = new double[selectedRows.length];
		for (int i = 0; i < selectedRows.length; i++) {
			//plot Kendrick mass defect (KMD) as y Axis to the base of CH2
			if(yAxisKMBase.equals("KMD (H)")) {
				yValues[i] = ((int)(selectedRows[i].getAverageMZ()*getyAxisKMFactor(yAxisKMBase))+1)-selectedRows[i].getAverageMZ()*getyAxisKMFactor(yAxisKMBase);
			}
			//plot Kendrick mass defect (KMD) as y Axis to the base of H
			else if(yAxisKMBase.equals("KMD (CH2)")) {
				yValues[i] = ((int)(selectedRows[i].getAverageMZ()*getyAxisKMFactor(yAxisKMBase))+1)-selectedRows[i].getAverageMZ()*getyAxisKMFactor(yAxisKMBase);
			}
			//plot Kendrick mass defect (KMD) as y Axis to the base of H
			else if(yAxisKMBase.equals("KMD (O)")) {
				yValues[i] = ((int)(selectedRows[i].getAverageMZ()*getyAxisKMFactor(yAxisKMBase))+1)-selectedRows[i].getAverageMZ()*getyAxisKMFactor(yAxisKMBase);
			}
		}
	}

	//Calculate xAxis Kendrick mass factor (KM factor)
	private double getxAxisKMFactor(String xAxisKMBase) {
		if(xAxisKMFactor==-1) {
			if(xAxisKMBase.equals("KMD (CH2)")) {
				xAxisKMFactor = (14.000000/14.01565006);
			}
			else if(xAxisKMBase.equals("KMD (H)")) {
				xAxisKMFactor = (1/1.007825037);
			}
			else if(xAxisKMBase.equals("KMD (O)")) {
				xAxisKMFactor = (16/15.994915);
			}
			else {
				xAxisKMFactor = 0;
			}
		}
		return xAxisKMFactor;
	}

	//Calculate yAxis Kendrick mass factor (KM factor)
	private double getyAxisKMFactor(String yAxisKMBase) {
		if(yAxisKMFactor == -1) {
			if(yAxisKMBase.equals("KMD (CH2)")) {
				yAxisKMFactor = (14.000000/14.01565006);
			}
			else if(yAxisKMBase.equals("KMD (H)")) {
				yAxisKMFactor = (1/1.007825037);
			}
			else if(yAxisKMBase.equals("KMD (O)")) {
				yAxisKMFactor = (16/15.994915);
			}
			else {
				yAxisKMFactor = 0;
			}
		}
		return yAxisKMFactor;
	}

	@Override
	public int getItemCount(int series) {
		return selectedRows.length;
	}

	@Override
	public Number getX(int series, int item) {
		return xValues[item];
	}

	@Override
	public Number getY(int series, int item) {
		return yValues[item];
	}

	@Override
	public int getSeriesCount() {
		return selectedRows.length;
	}

	public Comparable<?> getRowKey(int row) {
		return selectedRows[row].toString();
	}

	@Override
	public Comparable<?> getSeriesKey(int series) {
		return getRowKey(series);
	}

	public double[] getxValues() {
		return xValues;
	}

	public double[] getyValues() {
		return yValues;
	}

}
