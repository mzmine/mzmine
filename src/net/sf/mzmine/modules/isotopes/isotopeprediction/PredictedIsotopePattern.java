package net.sf.mzmine.modules.isotopes.isotopeprediction;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.IsotopePatternStatus;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.util.Range;

public class PredictedIsotopePattern implements IsotopePattern {
	
	private DataPoint[] dataPoints;
	private String formula;
	private int charge;
	private Range range;
	private IsotopePatternStatus patternStatus = IsotopePatternStatus.PREDICTED;
	private float height = 10000.0f;

	
	public PredictedIsotopePattern (DataPoint[] dataPoints, String formula, int charge){
		this.dataPoints = dataPoints;
		this.formula = formula;
		this.charge = charge;
		
		range = calculateMzRange(dataPoints);
	}

	public int getCharge() {
		return charge;
	}

	public String getIsotopeInfo() {
		return " Pattern of " + formula + " charge=" + charge;
	}

	public float getIsotopeMass() {
		return dataPoints[0].getMZ();
	}

	public Range getIsotopeMzRange() {
		return range;
	}

	public ChromatographicPeak[] getOriginalPeaks() {
		return null;
	}

	public ChromatographicPeak getRepresentativePeak() {
		return null;
	}

	public float getArea() {
		return 0;
	}

	public RawDataFile getDataFile() {
		return null;
	}

	public float getHeight() {
		return dataPoints[0].getIntensity();
	}

	public float getMZ() {
		return dataPoints[0].getMZ();
	}

	public MzPeak getMzPeak(int scanNumber) {
		return null;
	}

	public PeakStatus getPeakStatus() {
		return null;
	}

	public float getRT() {
		return 0;
	}

	public Range getRawDataPointsIntensityRange() {
		return null;
	}

	public Range getRawDataPointsMZRange() {
		return null;
	}

	public Range getRawDataPointsRTRange() {
		return null;
	}

	public int getRepresentativeScanNumber() {
		return 0;
	}

	public int[] getScanNumbers() {
		return null;
	}

	public DataPoint[] getDataPoints() {
		return dataPoints;
	}

	public int getNumberOfDataPoints() {
		return dataPoints.length;
	}
	
	private Range calculateMzRange(DataPoint[] dataPoints){
		float H = 1.0078f;
		Range range = new Range(dataPoints[0].getMZ());
		for (int i=1; i<dataPoints.length; i++){
			range.extendRange(dataPoints[i].getMZ());
		}

		//Extend range by +/- one hydrogen
		range.extendRange(range.getMin() - H);
		range.extendRange(range.getMax() + H);
		
		return range;
	}
	
	public void setIsotopeHeight(float height){
		this.height = height;
	}
	
	public float getIsotopeHeight(){
		return height;
	}

	public IsotopePatternStatus getIsotopePatternStatus() {
		return patternStatus;
	}
	
	public String getFormula(){
		return formula;
	}
	
	public String toString(){
		return formula + " Charge " + charge;
	}

}
