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

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.parameters.Parameter;

import org.w3c.dom.Element;

/**
 * 
 */
public class PeakListsParameter implements Parameter<PeakList[]> {

	private int minCount, maxCount;
	private PeakList value[];

	public PeakListsParameter() {
		this(1, Integer.MAX_VALUE);
	}

	public PeakListsParameter(int minCount) {
		this(minCount, Integer.MAX_VALUE);
	}

	public PeakListsParameter(int minCount, int maxCount) {
		this.minCount = minCount;
		this.maxCount = maxCount;
	}

	public PeakList[] getValue() {
		return value;
	}

	public void setValue(PeakList newValue[]) {
		this.value = newValue;
	}

	@Override
	public PeakListsParameter cloneParameter() {
		PeakListsParameter copy = new PeakListsParameter(minCount, maxCount);
		copy.value = value;
		return copy;
	}

	@Override
	public String getName() {
		return "Peak lists";
	}

	@Override
	public void loadValueFromXML(Element xmlElement) {
	}

	@Override
	public void saveValueToXML(Element xmlElement) {
	}

	@Override
	public boolean checkValue(Collection<String> errorMessages) {
		if (value == null) {
			errorMessages.add("No peak list is selected");
			return false;
		}
		if (value.length < minCount) {
			errorMessages.add("At least " + minCount
					+ " peak lists must be selected");
			return false;
		}
		if (value.length > maxCount) {
			errorMessages.add("Maximum " + maxCount
					+ " peak lists may be selected");
			return false;
		}
		return true;
	}

}
