package net.sf.mzmine.modules.visualization.productionfilter;

import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;

/**
 * 
 */
class ProductIonFilterDataPoint {

	private double mzValue;
	private int scanNumber;
	private double precursorMZ;
	private int precursorCharge;
	private double retentionTime;
	private double neutralLoss;
	private double precursorMass;
	private String label;
	private static int defaultPrecursorCharge = 2;

	
	/**
	 * @param scanNumber
	 * @param precursorScanNumber
	 * @param precursorMZ
	 * @param precursorCharge
	 * @param retentionTime
	 */
	ProductIonFilterDataPoint(double mzValue, int scanNumber, double precursorMZ, int precursorCharge,
			double retentionTime) {

		NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
		NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

		this.mzValue = mzValue;
		this.scanNumber = scanNumber;
		this.precursorMZ = precursorMZ;
		this.precursorCharge = precursorCharge;
		this.retentionTime = retentionTime;

		precursorMass = precursorMZ;
		if (precursorCharge > 0)
			precursorMass *= precursorCharge;

		if ((precursorCharge == 0) && (precursorMass < mzValue))
			precursorMass *= defaultPrecursorCharge;

		neutralLoss = mzValue; /*precursorMass - mzValue;*/
		
		StringBuffer sb = new StringBuffer();
		sb.append("loss: ");
		sb.append(mzFormat.format(neutralLoss));
		sb.append(", m/z ");
		sb.append(mzFormat.format(mzValue));
		sb.append(", scan #" + scanNumber + ", RT ");
		sb.append(rtFormat.format(retentionTime));
		sb.append(", m/z ");
		sb.append(mzFormat.format(precursorMZ));
		if (precursorCharge > 0)
			sb.append(" (charge " + precursorCharge + ")");
		label = sb.toString();

	}

	/**
	 * @return Returns the mzValue.
	 */
	double getMzValue() {
		return mzValue;
	}

	/**
	 * @return Returns the precursorCharge.
	 */
	int getPrecursorCharge() {
		return precursorCharge;
	}

	/**
	 * @return Returns the precursorMZ.
	 */
	double getPrecursorMZ() {
		return precursorMZ;
	}

	/**
	 * @return Returns the precursor mass, or m/z if charge is unknown.
	 */
	double getPrecursorMass() {
		return precursorMass;
	}

	/**
	 * @return Returns the retentionTime.
	 */
	double getRetentionTime() {
		return retentionTime;
	}

	/**
	 * @return Returns the scanNumber.
	 */
	int getScanNumber() {
		return scanNumber;
	}

	double getProductMZ() {
		return neutralLoss;
	}
	
	public String getName() {
		return label;

	}

}

