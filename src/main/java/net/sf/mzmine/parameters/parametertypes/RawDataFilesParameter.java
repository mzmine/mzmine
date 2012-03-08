/*
 * Copyright 2006-2012 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.parameters.parametertypes;

import java.util.Collection;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.parameters.Parameter;

import org.w3c.dom.Element;

/**
 * 
 */
public class RawDataFilesParameter implements Parameter<RawDataFile[]> {

	private int minCount, maxCount;
	private RawDataFile value[];

	public RawDataFilesParameter() {
		this(1, Integer.MAX_VALUE);
	}

	public RawDataFilesParameter(int minCount) {
		this(minCount, Integer.MAX_VALUE);
	}

	public RawDataFilesParameter(int minCount, int maxCount) {
		this.minCount = minCount;
		this.maxCount = maxCount;
	}

	public RawDataFile[] getValue() {
		return value;
	}

	public void setValue(RawDataFile newValue[]) {
		this.value = newValue;
	}

	@Override
	public RawDataFilesParameter cloneParameter() {
		RawDataFilesParameter copy = new RawDataFilesParameter(minCount,
				maxCount);
		copy.value = value;
		return copy;
	}

	@Override
	public String getName() {
		return "Raw data files";
	}

	@Override
	public boolean checkValue(Collection<String> errorMessages) {
		if (value == null) {
			errorMessages.add("No raw data file is selected");
			return false;
		}
		if (value.length < minCount) {
			errorMessages.add("At least " + minCount
					+ " raw data files must be selected");
			return false;
		}
		if (value.length > maxCount) {
			errorMessages.add("Maximum " + maxCount
					+ " raw data files may be selected");
			return false;
		}
		return true;
	}

	@Override
	public void loadValueFromXML(Element xmlElement) {
	}

	@Override
	public void saveValueToXML(Element xmlElement) {
	}

}
