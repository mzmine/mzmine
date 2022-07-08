/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package io.github.mzmine.modules.dataprocessing.featdet_dfbuilder;

public class DiagnosticInformation {
	
	private String name;
	private double[] targetedMZ_List;
	private double[] targetedNF_List;
	
	public DiagnosticInformation(String name, double[] targetedMZ_List, double[] targetedNF_List){
		this.name = name;
		this.targetedMZ_List = targetedMZ_List;
		this.targetedNF_List = targetedNF_List;
	}
	
	public String getName() {
		return name;
	}
	
	public double[] getMZList(){
		return targetedMZ_List;
	}
	
	public double[] getNFList(){
		return targetedNF_List;
	}

}