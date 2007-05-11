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

package net.sf.mzmine.modules.dataanalysis.cdaplot;

import org.xml.sax.Attributes;

public class AlignmentResultVisualizerCDAPlotViewParameters {

	private static final String myTagName = "CDAPlotParameters";
	private static final String alphaAttributeName = "Alpha";
	private static final String lambdaAttributeName = "Lambda";
	private static final String maximumLossAttributeName = "MaximumLoss";
	private static final String trainingLengthAttributeName = "TrainingLength";
	private static final String neighbourhoodSizeAttributeName = "NeighbourhoodSize";

	public double paramAlpha = 0.5;
	public double paramLambda = 1.0;
	public double paramMaximumLoss = 0.10;
	public int paramTrainingLength = 100;
	public int paramNeighbourhoodSize = 5;

	public String writeParameterTag() {

		String s = "<";
		s = s.concat(myTagName);
		s = s.concat(" " + alphaAttributeName + "=\"" + paramAlpha + "\"");
		s = s.concat(" " + lambdaAttributeName + "=\"" + paramLambda + "\"");
		s = s.concat(" " + maximumLossAttributeName + "=\"" + paramMaximumLoss + "\"");
		s = s.concat(" " + trainingLengthAttributeName + "=\"" + paramTrainingLength + "\"");
		s = s.concat(" " + neighbourhoodSizeAttributeName + "=\"" + paramNeighbourhoodSize + "\"");
		s = s.concat("/>");
		return s;

	}

	public String getParameterTagName() { return myTagName; }

	public boolean loadXMLAttributes(Attributes atr) {

		try { paramAlpha = Double.parseDouble(atr.getValue(alphaAttributeName)); } catch (NumberFormatException e) { return false; }
		try { paramLambda = Double.parseDouble(atr.getValue(lambdaAttributeName)); } catch (NumberFormatException e) { return false; }
		try { paramMaximumLoss = Double.parseDouble(atr.getValue(maximumLossAttributeName)); } catch (NumberFormatException e) { return false; }

		try { paramTrainingLength = Integer.parseInt(atr.getValue(trainingLengthAttributeName)); } catch (NumberFormatException e) {	return false; }
		try { paramNeighbourhoodSize = Integer.parseInt(atr.getValue(neighbourhoodSizeAttributeName)); } catch (NumberFormatException e) {	return false; }

		return true;
	}


}