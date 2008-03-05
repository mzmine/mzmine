/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.userinterface.dialogs.experimentalparametersetupdialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.swing.JFileChooser;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.userinterface.Desktop;

import com.sun.java.ExampleFileFilter;

/**
 * This class imports project parameters and their values from a CSV file to
 * MZmineProject.
 * 
 * 
 * Description of input file format:
 * 
 * First column of the comma-separated file must contain file names matching to
 * names of raw data files opened in MZmine. Each other column corresponds to
 * one project parameter.
 * 
 * First row in the file must contain column headers. Header for the first
 * column (filename) is ignored but must exists. Rest of the column headers are
 * used as names for project parameters. All column names in the file must be be
 * unique. If MZmineProject already contains a parameter with the same name,
 * then the values of that parameter will be overwritten.
 * 
 * 
 * Rules for deciding type of parameter:
 * 
 * 1. If all values in a column (except column header on the first row) are
 * numeric, then the parameter type is set to Float.
 * 
 * 2. If there are at least some duplicate strings among values for a parameter,
 * then the parameter type is String and possible parameter values are all
 * unique strings.
 * 
 * 3. Otherwise it is a free text parameter of type String.
 * 
 * 
 */
public class ExperimentalParametersImporter {

	private Logger logger = Logger.getLogger(this.getClass().getName());;
	private Desktop desktop;

	public ExperimentalParametersImporter(Desktop desktop) {

		this.desktop = desktop;

	}

	public boolean importParameters() {

		// Let user choose a CSV file for importing
		File parameterFile = chooseFile();
		if (parameterFile == null) {
			logger.info("Parameter importing cancelled.");
			return false;
		}

		// Read and interpret parameters
		Parameter[] parameters = processParameters(parameterFile);

		if (parameters == null)
			return false;

		// TODO: Show a dialog for selecting which parameters to import and edit
		// their types

		// Read values of parameters and store them in the project
		processParameterValues(parameterFile, parameters);

		return true;

	}

	private File chooseFile() {
		JFileChooser chooser = new JFileChooser();

		ExampleFileFilter filter = new ExampleFileFilter();
		filter.addExtension("txt");
		filter.addExtension("csv");
		filter.setDescription("TXT & CSV files");
		chooser
				.setDialogTitle("Please select a file containing project parameter values for files.");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(desktop.getMainFrame());
		if (returnVal == JFileChooser.CANCEL_OPTION) {
			return null;
		}

		return chooser.getSelectedFile();
	}

	private Parameter[] processParameters(File parameterFile) {

		ArrayList<Parameter> parameters = new ArrayList<Parameter>();

		// Open reader
		BufferedReader parameterFileReader;
		try {
			parameterFileReader = new BufferedReader(new FileReader(
					parameterFile));
		} catch (FileNotFoundException ex) {
			logger.severe("Could not open file " + parameterFile);
			desktop.displayErrorMessage("Could not open file " + parameterFile);
			return null;
		}

		try {

			// Read column headers which are used as parameter names
			String firstRow = parameterFileReader.readLine();
			StringTokenizer st = new StringTokenizer(firstRow, ",");
			st.nextToken(); // Assume first column contains file names
			ArrayList<String> parameterNames = new ArrayList<String>();
			Hashtable<String, ArrayList<String>> parameterValues = new Hashtable<String, ArrayList<String>>();
			while (st.hasMoreTokens()) {
				String paramName = st.nextToken();
				if (parameterValues.containsKey(paramName)) {
					logger
							.severe("Did not import parameters because of a non-unique parameter name: "
									+ paramName);
					desktop.displayErrorMessage("Could not open file "
							+ parameterFile);
					return null;
				}
				parameterNames.add(paramName);
				parameterValues.put(paramName, new ArrayList<String>());
			}

			// Read rest of the rows which contain file name in the first column
			// and parameter values in the rest of the columns
			String nextRow = parameterFileReader.readLine();
			int rowNumber = 2;
			while (nextRow != null) {
				st = new StringTokenizer(nextRow, ",");
				st.nextToken(); // Skip first column (File name)
				Iterator<String> parameterNameIterator = parameterNames
						.iterator();
				while (st.hasMoreTokens()) {

					if (st.hasMoreTokens() ^ parameterNameIterator.hasNext()) {
						logger
								.severe("Incorrect number of parameter values on row "
										+ rowNumber);
						desktop
								.displayErrorMessage("Incorrect number of parameter values on row "
										+ rowNumber);
						return null;
					}
					parameterValues.get(parameterNameIterator.next()).add(
							st.nextToken());
				}
				nextRow = parameterFileReader.readLine();
				rowNumber++;
			}

			// Decide parameter types (all numeric => Float, all unique string
			// => String, at least one duplicate string => Object with possible
			// values

			Iterator<String> parameterNameIterator = parameterNames.iterator();
			while (parameterNameIterator.hasNext()) {
				String name = parameterNameIterator.next();
				ArrayList<String> vals = parameterValues.get(name);

				// Test for all numeric
				Iterator<String> valIterator = vals.iterator();
				boolean isAllNumeric = true;
				while (valIterator.hasNext()) {
					try {
						Double.valueOf(valIterator.next());
					} catch (NumberFormatException ex) {
						isAllNumeric = false;
						break;
					}
				}
				if (isAllNumeric) {
					parameters.add(new SimpleParameter(ParameterType.FLOAT,
							name, null));
					continue;
				}

				// Test for "set of values"
				ArrayList<String> uniqueValues = new ArrayList<String>();
				valIterator = vals.iterator();
				while (valIterator.hasNext()) {
					String val = valIterator.next();
					if (!uniqueValues.contains(val))
						uniqueValues.add(val);
				}
				if (uniqueValues.size() < vals.size()) {
					parameters.add(new SimpleParameter(ParameterType.STRING,
							name, null, uniqueValues.get(0), uniqueValues
									.toArray(new String[0])));
					continue;
				}

				// Otherwise it is a free text parameter
				parameters.add(new SimpleParameter(ParameterType.STRING, name,
						null));

			}

		} catch (IOException ex) {
			logger.severe("Could not read file " + parameterFile);
			desktop.displayErrorMessage("Could not open file " + parameterFile);
			return null;
		}

		// Close reader
		try {
			parameterFileReader.close();
		} catch (IOException ex) {
			logger.severe("Could not close file " + parameterFile);
			desktop
					.displayErrorMessage("Could not close file "
							+ parameterFile);
			return null;
		}

		return parameters.toArray(new Parameter[0]);

	}

	private boolean processParameterValues(File parameterFile,
			Parameter[] parameters) {

		MZmineProject currentProject = MZmineCore.getCurrentProject();

		// TODO Fix bug in importing parameters with same names		
		// Remove previous parameters with identical names		
		Parameter[] projectParameters = currentProject.getParameters();
		for (Parameter inputParameter: parameters)
			for (Parameter projectParameter : projectParameters) {
				if (projectParameter.getName().equals(inputParameter.getName()))
					currentProject.removeParameter(projectParameter);
			}

		// Add parameters to project
		for (Parameter inputParameter : parameters)
			currentProject.addParameter(inputParameter);

		// Open reader
		BufferedReader parameterFileReader;
		try {
			parameterFileReader = new BufferedReader(new FileReader(
					parameterFile));
		} catch (FileNotFoundException ex) {
			logger.severe("Could not open file " + parameterFile);
			desktop.displayErrorMessage("Could not open file " + parameterFile);
			return false;
		}

		try {

			// Skip first row
			parameterFileReader.readLine();

			// Read rest of the rows which contain file name in the first column
			// and parameter values in the rest of the columns
			String nextRow = parameterFileReader.readLine();
			while (nextRow != null) {
				StringTokenizer st = new StringTokenizer(nextRow, ",");

				nextRow = parameterFileReader.readLine();

				// Get raw data file for this row
				String fileName = st.nextToken();
				RawDataFile rawDataFile = currentProject.getDataFile(fileName);
				if (rawDataFile == null) {
					logger.warning("Could not find a raw data file '"
							+ fileName + "' from the project.");
					continue;
				}

				// Set parameter values to project
				int parameterIndex = 0;
				while (st.hasMoreTokens()) {
					String parameterValue = st.nextToken();
					Parameter parameter = parameters[parameterIndex];

					if (parameter.getType() == ParameterType.FLOAT)
						currentProject
								.setParameterValue(parameter, rawDataFile,
										Double.parseDouble(parameterValue));
					else
						currentProject.setParameterValue(parameter,
								rawDataFile, parameterValue);

					parameterIndex++;

				}

			}

		} catch (IOException ex) {
			logger.severe("Could not read file " + parameterFile);
			desktop.displayErrorMessage("Could not open file " + parameterFile);
			return false;
		}

		// Close reader
		try {
			parameterFileReader.close();
		} catch (IOException ex) {
			logger.severe("Could not close file " + parameterFile);
			desktop
					.displayErrorMessage("Could not close file "
							+ parameterFile);
			return false;
		}

		return true;

	}

}
