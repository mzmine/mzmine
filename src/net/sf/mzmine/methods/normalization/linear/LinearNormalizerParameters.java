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

package net.sf.mzmine.methods.normalization.linear;

import net.sf.mzmine.methods.normalization.NormalizerParameters;

import org.xml.sax.Attributes;

public class LinearNormalizerParameters implements NormalizerParameters {


	private static final String myTagName = "LinearNormalizerParameters";
	private static final String paramNormalizationTypeAttributeName = "NormalizationType";

	public static final int NORMALIZATIONTYPE_AVERAGEINT = 1;
	public static final int NORMALIZATIONTYPE_AVERAGESQUAREINT = 2;
	public static final int NORMALIZATIONTYPE_MAXPEAK = 3;
	public static final int NORMALIZATIONTYPE_TOTRAWSIGNAL = 4;

	public int paramNormalizationType = 1;

	public Class getNormalizerClass() {
		return LinearNormalizer.class;
	}

	public String writeParameterTag() {

		String s = "<";
		s = s.concat(myTagName);
		s = s.concat(" " + paramNormalizationTypeAttributeName + "=\"" + paramNormalizationType + "\"");
		s = s.concat("/>");
		return s;

	}

	public String getParameterTagName() { return myTagName; }

	public boolean loadXMLAttributes(Attributes atr) {

		try { paramNormalizationType = Integer.parseInt(atr.getValue(paramNormalizationTypeAttributeName));	} catch (NumberFormatException e) {	return false; }
		return true;
	}

}