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

package net.sf.mzmine.methods.normalization.standardcompound;

import net.sf.mzmine.methods.MethodParameters;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;

public class StandardCompoundNormalizerParameters implements MethodParameters {

	private static final String myTagName = "StandardCompoundNormalizerParameters";
	private static final String paramNormalizationTypeAttributeName = "NormalizationType";
	private static final String paramMZvsRTBalanceAttributeName = "MZvsRTBalance";

	public int paramNormalizationType = 1;
	public double paramMZvsRTBalance = (double)10.0;

	public static final int NORMALIZATIONTYPE_STDCOMPOUND_NEAREST = 1;
	public static final int NORMALIZATIONTYPE_STDCOMPOUND_WEIGHTED = 2;

	public Class getNormalizerClass() {
		return StandardCompoundNormalizer.class;
	}

	public String writeParameterTag() {

		String s = "<";
		s = s.concat(myTagName);
		s = s.concat(" " + paramNormalizationTypeAttributeName + "=\"" + paramNormalizationType + "\"");
		s = s.concat(" " + paramMZvsRTBalanceAttributeName + "=\"" + paramMZvsRTBalance + "\"");
		s = s.concat("/>");

		return s;

	}

	public String getParameterTagName() { return myTagName; }

	public boolean loadXMLAttributes(Attributes atr) {

		try { paramNormalizationType = Integer.parseInt(atr.getValue(paramNormalizationTypeAttributeName));	} catch (NumberFormatException e) {	return false; }
		try { paramMZvsRTBalance = Double.parseDouble(atr.getValue(paramMZvsRTBalanceAttributeName)); } catch (NumberFormatException e) {	return false; }

		return true;
	}

    /**
     * @see net.sf.mzmine.methods.MethodParameters#addToXML(org.w3c.dom.Document)
     */
    public Element addToXML(Document doc) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.methods.MethodParameters#readFromXML(org.w3c.dom.Element)
     */
    public void readFromXML(Element element) {
        // TODO Auto-generated method stub
        
    }
}