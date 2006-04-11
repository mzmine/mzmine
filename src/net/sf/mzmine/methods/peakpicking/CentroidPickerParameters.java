/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package net.sf.mzmine.methods.peakpicking;
import java.io.Serializable;

import org.xml.sax.Attributes;

public class CentroidPickerParameters implements PeakPickerParameters, Serializable {

	private static final String myTagName = "CentroidPickerParameters";

	private static final String binSizeAttributeName = "BinSize";
	private static final String chromatographicThresholdLevelAttributeName = "ChromatographicThresholdLevel";
	private static final String noiseLevelAttributeName = "NoiseLevel";
	private static final String minimumPeakHeightAttributeName = "MinimumPeakHeight";
	private static final String minimumPeakDurationAttributeName = "MinimumPeakDuration";
	private static final String mzToleranceAttributeName = "MZTolerance";
	private static final String intToleranceAttributeName = "IntTolerance";

	// Parameters and their default values

	public double binSize = 0.25;
	public double chromatographicThresholdLevel = 0.0;
	public double noiseLevel = 4.0;
	public double minimumPeakHeight = 15.0;
	public double minimumPeakDuration = 3.0;
	public double mzTolerance = 0.050;
	public double intTolerance = 0.20;

	public Class getPeakPickerClass() {
		return CentroidPicker.class;
	}

	public String writeParameterTag() {

		String s = "<";
		s = s.concat(myTagName);
		s = s.concat(" " + binSizeAttributeName + "=\"" + binSize + "\"");
		s = s.concat(" " + chromatographicThresholdLevelAttributeName + "=\"" + chromatographicThresholdLevel + "\"");
		s = s.concat(" " + noiseLevelAttributeName + "=\"" + noiseLevel + "\"");
		s = s.concat(" " + minimumPeakHeightAttributeName + "=\"" + minimumPeakHeight + "\"");
		s = s.concat(" " + minimumPeakDurationAttributeName + "=\"" + minimumPeakDuration + "\"");
		s = s.concat(" " + mzToleranceAttributeName + "=\"" + mzTolerance + "\"");
		s = s.concat(" " + intToleranceAttributeName + "=\"" + intTolerance + "\"");
		s = s.concat("/>");
		return s;

	}

	public String getParameterTagName() { return myTagName; }

	public boolean loadXMLAttributes(Attributes atr) {

		try { binSize = Double.parseDouble(atr.getValue(binSizeAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { chromatographicThresholdLevel = Double.parseDouble(atr.getValue(chromatographicThresholdLevelAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { noiseLevel = Double.parseDouble(atr.getValue(noiseLevelAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { minimumPeakHeight = Double.parseDouble(atr.getValue(minimumPeakHeightAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { minimumPeakDuration = Double.parseDouble(atr.getValue(minimumPeakDurationAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { mzTolerance = Double.parseDouble(atr.getValue(mzToleranceAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { intTolerance = Double.parseDouble(atr.getValue(intToleranceAttributeName)); } catch (NumberFormatException e) {	return false; }

		return true;
	}

}