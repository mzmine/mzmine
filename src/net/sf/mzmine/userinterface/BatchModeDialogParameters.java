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
package net.sf.mzmine.userinterface;
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.distributionframework.*;
import net.sf.mzmine.miscellaneous.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;


import org.xml.sax.Attributes;

public class BatchModeDialogParameters {

	private static final String myTagName = "BatchModeDialogParameters";
	private static final String Filter1NameAttributeName = "Filter1Name";
	private static final String Filter2NameAttributeName = "Filter2Name";
	private static final String Filter3NameAttributeName = "Filter3Name";
	private static final String Picker1NameAttributeName = "Picker1Name";
	private static final String PeakListProcessor1NameAttributeName = "PeakListProcessor1Name";
	private static final String PeakListProcessor2NameAttributeName = "PeakListProcessor2Name";
	private static final String Aligner1NameAttributeName = "Aligner1Name";
	private static final String AlignmentFilter1NameAttributeName = "AlignmentFilter1Name";
	private static final String Filler1NameAttributeName = "Filler1Name";
	private static final String Normalizer1NameAttributeName = "Normalizer1Name";

	private String selectedFilter1Name;
	public String getSelectedFilter1Name() { return selectedFilter1Name; }
	public void setSelectedFilter1Name(String paramValue) { selectedFilter1Name = paramValue; }

	private String selectedFilter2Name;
	public String getSelectedFilter2Name() { return selectedFilter2Name; }
	public void setSelectedFilter2Name(String paramValue) { selectedFilter2Name = paramValue; }

	private String selectedFilter3Name;
	public String getSelectedFilter3Name() { return selectedFilter3Name; }
	public void setSelectedFilter3Name(String paramValue) { selectedFilter3Name = paramValue; }

	private String selectedPicker1Name;
	public String getSelectedPicker1Name() { return selectedPicker1Name; }
	public void setSelectedPicker1Name(String paramValue) { selectedPicker1Name = paramValue; }

	private String selectedPeakListProcessor1Name;
	public String getSelectedPeakListProcessor1Name() { return selectedPeakListProcessor1Name; }
	public void setSelectedPeakListProcessor1Name(String paramValue) { selectedPeakListProcessor1Name = paramValue; }

	private String selectedPeakListProcessor2Name;
	public String getSelectedPeakListProcessor2Name() { return selectedPeakListProcessor2Name; }
	public void setSelectedPeakListProcessor2Name(String paramValue) { selectedPeakListProcessor2Name = paramValue; }

	private String selectedAligner1Name;
	public String getSelectedAligner1Name() { return selectedAligner1Name; }
	public void setSelectedAligner1Name(String paramValue) { selectedAligner1Name = paramValue; }

	private String selectedAlignmentFilter1Name;
	public String getSelectedAlignmentFilter1Name() { return selectedAlignmentFilter1Name; }
	public void setSelectedAlignmentFilter1Name(String paramValue) { selectedAlignmentFilter1Name = paramValue; }

	private String selectedFiller1Name;
	public String getSelectedFiller1Name() { return selectedFiller1Name; }
	public void setSelectedFiller1Name(String paramValue) { selectedFiller1Name = paramValue; }

	private String selectedNormalizer1Name;
	public String getSelectedNormalizer1Name() { return selectedNormalizer1Name; }
	public void setSelectedNormalizer1Name(String paramValue) { selectedNormalizer1Name = paramValue; }


	public String writeParameterTag() {

		String s = "<";
		s = s.concat(myTagName);
		s = s.concat(" " + Filter1NameAttributeName + "=\"" + selectedFilter1Name + "\"");
		s = s.concat(" " + Filter2NameAttributeName + "=\"" + selectedFilter2Name + "\"");
		s = s.concat(" " + Filter3NameAttributeName + "=\"" + selectedFilter3Name + "\"");
		s = s.concat(" " + Picker1NameAttributeName + "=\"" + selectedPicker1Name + "\"");
		s = s.concat(" " + PeakListProcessor1NameAttributeName + "=\"" + selectedPeakListProcessor1Name + "\"");
		s = s.concat(" " + PeakListProcessor2NameAttributeName + "=\"" + selectedPeakListProcessor2Name + "\"");
		s = s.concat(" " + Aligner1NameAttributeName + "=\"" + selectedAligner1Name + "\"");
		s = s.concat(" " + AlignmentFilter1NameAttributeName + "=\"" + selectedAlignmentFilter1Name + "\"");
		s = s.concat(" " + Filler1NameAttributeName + "=\"" + selectedFiller1Name + "\"");
		s = s.concat(" " + Normalizer1NameAttributeName + "=\"" + selectedNormalizer1Name + "\"");
		s = s.concat("/>");
		return s;

	}

	public String getParameterTagName() { return myTagName; }

	public boolean loadXMLAttributes(Attributes atr) {

		selectedFilter1Name = atr.getValue(Filter1NameAttributeName);
		selectedFilter2Name = atr.getValue(Filter2NameAttributeName);
		selectedFilter3Name = atr.getValue(Filter3NameAttributeName);
		selectedPicker1Name = atr.getValue(Picker1NameAttributeName);
		selectedPeakListProcessor1Name = atr.getValue(PeakListProcessor1NameAttributeName);
		selectedPeakListProcessor2Name = atr.getValue(PeakListProcessor2NameAttributeName);
		selectedAligner1Name = atr.getValue(Aligner1NameAttributeName);
		selectedAlignmentFilter1Name = atr.getValue(AlignmentFilter1NameAttributeName);
		selectedFiller1Name = atr.getValue(Filler1NameAttributeName);
		selectedNormalizer1Name = atr.getValue(Normalizer1NameAttributeName);

		return true;
	}

}