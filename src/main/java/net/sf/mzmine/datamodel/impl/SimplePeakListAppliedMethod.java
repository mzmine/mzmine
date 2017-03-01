/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.datamodel.impl;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.parameters.ParameterSet;

public class SimplePeakListAppliedMethod implements PeakListAppliedMethod {

    private String description;
    private String parameters;

    public SimplePeakListAppliedMethod(String description,
	    ParameterSet parameters) {
	this.description = description;
	if (parameters != null) {
	    this.parameters = parameters.toString();
	} else {
	    this.parameters = "";
	}
    }

    public SimplePeakListAppliedMethod(String description, String parameters) {
	this.description = description;
	this.parameters = parameters;
    }

    public SimplePeakListAppliedMethod(String description) {
	this.description = description;
    }

    public @Nonnull String getDescription() {
	return description;
    }

    public String toString() {
	return description;
    }

    public @Nonnull String getParameters() {
	return parameters;
    }

}
