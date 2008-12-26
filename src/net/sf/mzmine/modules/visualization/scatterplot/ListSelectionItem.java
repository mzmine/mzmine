/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.Color;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.JFrame;

public class ListSelectionItem {

	private String name;
	private boolean hasColor = false, valid = false, selected = false,
			customized = false, alreadyCompared = false, hasMatches= false;
	private Color color;
	private String[][] searchValues;
	private Integer[] matchesVector;
	private static int counter = 0;
	private static int numColor = 0;
	private static Color[] colors = { Color.YELLOW, new Color(220, 50, 204),
			Color.PINK, Color.CYAN, Color.ORANGE, Color.red, Color.GREEN,
			new Color(255, 180, 180), Color.MAGENTA,
			new Color(255, 160, 122), new Color(84, 255, 159) };

	public ListSelectionItem() {
		
		this.name = "[Custom selection]";
		customized = true;
		this.color = colors[numColor % colors.length];
		hasColor = true;
		numColor++;
		
	}

	public ListSelectionItem(String name, String[] searchNames) {

		this.name = name;
		this.color = colors[numColor % colors.length];
		hasColor = true;
		numColor++;
		this.searchValues = new String[searchNames.length][1];
		for (int i=0; i<searchValues.length; i++){
			searchValues[i][0] = searchNames[i];
		}

	}

	public String[][] getSearchValues() {
		return searchValues;
	}

	public boolean isValid() {
		return valid;
	}

	public boolean isSelected() {
		return selected;
	}

	public boolean isCustomized() {
		return customized;
	}

	public void setSelected(boolean status) {
		selected = status;
	}

	public boolean hasColor() {
		return hasColor;
	}

	public Color getColor() {
		return color;
	}

	public void setCustomizedSearchValues(JFrame owner) {
		CustomSearchDialog dialog = new CustomSearchDialog(owner);
		dialog.setVisible(true);
		searchValues = dialog.getSearchValues();
		setFixedName();
	}

	public void setManuallySearchValues(String searchLine) {

		Vector<String> searchItems = new Vector<String>();
		Scanner scanner = new Scanner(searchLine);
		scanner.useDelimiter(",");
		String str;
		while (scanner.hasNext()) {
			str = scanner.next();
			searchItems.add(str);
		}

		scanner.close();
		searchValues = new String[searchItems.size()][1];
		for (int i=0; i<searchValues.length; i++){
			searchValues[i][0] = searchItems.get(i);
		}
		setFixedName();
	}

	public void setManuallySearchValues(String[] searchNames) {

		searchValues = new String[searchNames.length][1];
		for (int i=0; i<searchValues.length; i++){
			searchValues[i][0] = searchNames[i];
		}
		setFixedName();
	}

	public void setColor(Color color) {
		this.color = color;

		if (color == null)
			hasColor = false;
		else
			hasColor = true;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ListSelectionItem clone() {
		ListSelectionItem clone = new ListSelectionItem();
		counter++;
		return clone;
	}

	public void removeCustomizedSearch() {
		counter--;
	}

	public String toString() {
		return name;
	}
	
	private void setFixedName(){
		if (searchValues != null){
		String newName = "[Custom Selection ";
		int a;
		for (int i = 0; i < searchValues.length && i < 5; i++) {
			newName += searchValues[i][0];
			a = i+1;
			if ((a < searchValues.length) && (a < 5))
				newName += ", ";
		}
		if (searchValues.length > 5)
			newName += " ...";
		
		newName += "]";
		setName(newName);
		}
		else
			searchValues = new String[0][0];
	}
	
	public String getTiptext(){
		String ret = "<HTML><b>" + name + "</b><br>";
		for (int i = 0; i < searchValues.length ; i++) {
		ret += searchValues[i][0] + "<br>";
		}
		return ret;
	}
	
	public boolean isAlreadyCompared(){
		return alreadyCompared;
	}
	
	public boolean hasMatches(){
		return hasMatches;
	}
	
	public Integer[] getMatches(){
		return matchesVector;
	}
	
	public void setMatches(Integer[] matches){
		matchesVector = matches;
		hasMatches = true;
	}
	
	public void setCompareFlag(){
		alreadyCompared = true;
	}

}
