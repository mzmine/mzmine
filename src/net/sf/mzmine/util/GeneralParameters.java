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
package net.sf.mzmine.util;
import org.xml.sax.Attributes;

/**
 * This class stores and handles all parameter settings of MZmine client program
 */
public class GeneralParameters {

	private static final String myTagName = "GeneralParameters";
	private static final String mzLabelFormatAttributeName = "MZLabelFormat";
	private static final String chromatographicLabelFormatAttributeName = "ChromatographicLabelFormat";
	private static final String peakMeasuringTypeAttributeName = "PeakMeasuring";
	private static final String typeOfDataAttributeName = "TypeOfData";
	private static final String loggingLevelAttributeName = "LoggingLevel";
	private static final String copyOnLoadAttributeName = "CopyOnLoad";

	// These are the possible values for each parameter with discrete values
	public static final Integer PARAMETERVALUE_MZLABELFORMAT_TWODIGITS = 2;
	public static final Integer PARAMETERVALUE_MZLABELFORMAT_THREEDIGITS = 3;
	public static final Integer PARAMETERVALUE_MZLABELFORMAT_FOURDIGITS = 4;

	public static final Integer PARAMETERVALUE_CHROMATOGRAPHICLABELFORMAT_SEC = 1;
	public static final Integer PARAMETERVALUE_CHROMATOGRAPHICLABELFORMAT_MINSEC = 2;

	public static final Integer PARAMETERVALUE_TYPEOFDATA_CONTINUOUS = 1;
	public static final Integer PARAMETERVALUE_TYPEOFDATA_CENTROIDS = 2;

	public static final Integer PARAMETERVALUE_PEAKMEASURING_HEIGHT = 1;
	public static final Integer PARAMETERVALUE_PEAKMEASURING_AREA = 2;

	public static final Integer PARAMETERVALUE_LOGGINGLEVEL_NORMAL = 1;
	public static final Integer PARAMETERVALUE_LOGGINGLEVEL_DEBUG = 2;
	public static final Integer PARAMETERVALUE_LOGGINGLEVEL_NONE = 3;

	public static final Integer PARAMETERVALUE_COPYONLOAD_YES = 1;
	public static final Integer PARAMETERVALUE_COPYONLOAD_NO = 2;


	// Variables & methods for storing and accessing each parameter
	private Integer mzLabelFormat = PARAMETERVALUE_MZLABELFORMAT_THREEDIGITS;
	public Integer getMZLabelFormat() { return mzLabelFormat; }
	public void setMZLabelFormat(Integer val) { mzLabelFormat = val; }

	private Integer chromatographicLabelFormat = PARAMETERVALUE_CHROMATOGRAPHICLABELFORMAT_SEC;
	public Integer getChromatographicLabelFormat() { return chromatographicLabelFormat; }
	public void setChromatographicLabelFormat(Integer val) { chromatographicLabelFormat = val; }

	private Integer peakMeasuringType = PARAMETERVALUE_PEAKMEASURING_HEIGHT;
	public Integer getPeakMeasuringType() { return peakMeasuringType; }
	public void setPeakMeasuringType(Integer val) { peakMeasuringType = val; }

	private Integer typeOfData = PARAMETERVALUE_TYPEOFDATA_CONTINUOUS;
	public Integer getTypeOfData() { return typeOfData; }
	public void setTypeOfData(Integer val) { typeOfData = val; }

	private Integer loggingLevel = PARAMETERVALUE_LOGGINGLEVEL_NORMAL;
	public Integer getLoggingLevel() { return loggingLevel; }
	public void setLoggingLevel(Integer val) { loggingLevel = val; }

	private Integer copyOnLoad = PARAMETERVALUE_COPYONLOAD_NO;
	public Integer getCopyOnLoad() { return copyOnLoad; }
	public void setCopyOnLoad(Integer val) { copyOnLoad = val; }



	public String writeParameterTag() {
		String s = "<";
		s = s.concat(myTagName);
		s = s.concat(" " + mzLabelFormatAttributeName + "=\"" + mzLabelFormat + "\"");
		s = s.concat(" " + chromatographicLabelFormatAttributeName + "=\"" + chromatographicLabelFormat + "\"");
		s = s.concat(" " + peakMeasuringTypeAttributeName + "=\"" + peakMeasuringType + "\"");
		s = s.concat(" " + typeOfDataAttributeName + "=\"" + typeOfData + "\"");
		s = s.concat(" " + loggingLevelAttributeName + "=\"" + loggingLevel + "\"");
		s = s.concat(" " + copyOnLoadAttributeName + "=\"" + copyOnLoad + "\"");
		s = s.concat("/>");
		return s;

	}

	public String getParameterTagName() { return myTagName; }

	public boolean loadXMLAttributes(Attributes atr) {

		try { mzLabelFormat = Integer.parseInt(atr.getValue(mzLabelFormatAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { chromatographicLabelFormat = Integer.parseInt(atr.getValue(chromatographicLabelFormatAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { peakMeasuringType = Integer.parseInt(atr.getValue(peakMeasuringTypeAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { typeOfData = Integer.parseInt(atr.getValue(typeOfDataAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { loggingLevel = Integer.parseInt(atr.getValue(loggingLevelAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { copyOnLoad = Integer.parseInt(atr.getValue(copyOnLoadAttributeName)); } catch (NumberFormatException e) {	return false; }

		return true;

	}

}