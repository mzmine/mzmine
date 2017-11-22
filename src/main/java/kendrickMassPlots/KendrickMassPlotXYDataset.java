package kendrickMassPlots;

import org.jfree.data.xy.AbstractXYDataset;

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
	private RawDataFile selectedFiles[];
	private PeakListRow selectedRows[];
	private String yAxisKMBase;
	private String xAxisKMBase;
	private String zAxis;
	private double xAxisKMFactor;
	private double yAxisKMFactor;

	public KendrickMassPlotXYDataset(ParameterSet parameters) {

		PeakList peakList = parameters
				.getParameter(KendrickMassPlotParameters.peakList).getValue()
				.getMatchingPeakLists()[0];

		//this.selectedFiles = parameters
		//		.getParameter(KendrickMassPlotParameters.dataFiles).getValue();

		this.selectedRows = parameters
				.getParameter(KendrickMassPlotParameters.selectedRows).getMatchingRows(peakList);

		this.yAxisKMBase = parameters
				.getParameter(KendrickMassPlotParameters.yAxisValues).getValue();
		
		this.xAxisKMBase = parameters
				.getParameter(KendrickMassPlotParameters.xAxisValues).getValue();
		
		//this.zAxis = parameters
		//		.getParameter(KendrickMassPlotParameters.zAxisValues).getValue();

	}

	//Calculate xAxis Kendrick mass factor (KM factor)
	private double getxAxisKMFactor(String xAxisKMBase) {
		if(xAxisKMBase.equals("KMD (CH2)")) {
			xAxisKMFactor = (14.000000/14.01565006);
		}
		else if(xAxisKMBase.equals("KMD (H)")) {
			System.out.println("True");
			xAxisKMFactor = (1/0.992235724);
		}
		else {
			xAxisKMFactor = 0;
		}
		return xAxisKMFactor;
	}

	//Calculate yAxis Kendrick mass factor (KM factor)
	private double getyAxisKMFactor(String yAxisKMBase) {
		if(yAxisKMBase.equals("KMD (CH2)")) {
			yAxisKMFactor = (14.000000/14.01565006);
		}
		else if(yAxisKMBase.equals("KMD (H)")) {
			yAxisKMFactor = (1/0.992235724);
		}
		else {
			yAxisKMFactor = 0;
		}
		return yAxisKMFactor;
	}

	@Override
	public int getItemCount(int series) {
		return selectedRows.length;
	}

	@Override
	public Number getX(int series, int item) {
		double x = 0;
		//simply plot m/z values as x axis
		if(xAxisKMBase.equals("m/z")) {
			x = selectedRows[item].getAverageMZ();
		}
		//plot Kendrick masses as x axis
		else if(xAxisKMBase.equals("KM")) {
			x = selectedRows[item].getAverageMZ()*getxAxisKMFactor(xAxisKMBase);
		}
		//plot Kendrick mass defect (KMD) as x Axis to the base of CH2
		else if(xAxisKMBase.equals("KMD (H)")) {
			x = (((int)selectedRows[item].getAverageMZ()*getxAxisKMFactor(xAxisKMBase)+1)-selectedRows[item].getAverageMZ());
		}
		//plot Kendrick mass defect (KMD) as x Axis to the base of H
		else if(xAxisKMBase.equals("KMD (CH2)")) {
			x = (((int)selectedRows[item].getAverageMZ()*getxAxisKMFactor(xAxisKMBase)+1)-selectedRows[item].getAverageMZ());
		}
		return x;
	}

	@Override
	public Number getY(int series, int item) {
		double y = 0;

		//plot Kendrick mass defect (KMD) as y Axis to the base of CH2
		if(yAxisKMBase.equals("KMD (H)")) {
			y = ((int)(selectedRows[item].getAverageMZ()*getyAxisKMFactor(yAxisKMBase))+1)-selectedRows[item].getAverageMZ()*getyAxisKMFactor(yAxisKMBase);
		}
		//plot Kendrick mass defect (KMD) as y Axis to the base of H
		else if(yAxisKMBase.equals("KMD (CH2)")) {
			y = ((int)(selectedRows[item].getAverageMZ()*getyAxisKMFactor(yAxisKMBase))+1)-selectedRows[item].getAverageMZ()*getyAxisKMFactor(yAxisKMBase);
		}
		return y;
	}

	@Override
	public int getSeriesCount() {
		return selectedRows.length;
	}

	public Comparable<?> getRowKey(int row) {
		return selectedRows[row].toString();
	}

	@Override
	public Comparable getSeriesKey(int series) {
		return getRowKey(series);
	}

}
