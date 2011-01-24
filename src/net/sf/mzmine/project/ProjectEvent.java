/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.project;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;

public class ProjectEvent {

	public enum ProjectEventType {
		DATAFILE_ADDED, 
		DATAFILE_REMOVED, 
		DATAFILES_REORDERED, 
		PEAKLIST_ADDED, 
		PEAKLIST_REMOVED, 
		PEAKLISTS_REORDERED, 
		PEAKLIST_CONTENTS_CHANGED, 
		PROJECT_NAME_CHANGED, 
		ALL_CHANGED
	}

	private ProjectEventType type;
	private RawDataFile dataFile;
	private PeakList peakList;
	private int index;

	/**
	 * Constructor for ALL_CHANGED,PROJECT_NAME_CHANGED, DATAFILES_REORDERED,
	 * PEAKLISTS_REORDERED type event
	 */
	public ProjectEvent(ProjectEventType type) {
		this.type = type;
	}

	/**
	 * Constructor for DATAFILE_ADDED, DATAFILE_REMOVED type event
	 */
	public ProjectEvent(ProjectEventType type, RawDataFile dataFile, int index) {
		this.type = type;
		this.dataFile = dataFile;
		this.index = index;
	}

	/**
	 * Constructor for PEAKLIST_ADDED, PEAKLIST_REMOVED type event
	 */
	public ProjectEvent(ProjectEventType type, PeakList peakList, int index) {
		this.type = type;
		this.peakList = peakList;
		this.index = index;
	}

	/**
	 * Constructor for PEAKLIST_CONTENTS_CHANGED type event
	 */
	public ProjectEvent(ProjectEventType type, PeakList peakList) {
		this.type = type;
		this.peakList = peakList;
	}

	public ProjectEventType getType() {
		return type;
	}

	public RawDataFile getDataFile() {
		return dataFile;
	}

	public PeakList getPeakList() {
		return peakList;
	}

	public int getIndex() {
		return index;
	}

	public String toString() {
		return type + ": data file " + dataFile + ", peak list " + peakList + ", index " + index;
	}
}
