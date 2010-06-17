/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.peaklistmethods.identification.mascot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

import org.dom4j.Element;

import be.proteomics.lims.util.http.forms.HTTPForm;
import be.proteomics.lims.util.http.forms.inputs.InputInterface;
import be.proteomics.lims.util.http.forms.inputs.RadioInput;
import be.proteomics.lims.util.http.forms.inputs.SelectInput;
import be.proteomics.lims.util.http.forms.inputs.TextFieldInput;

public class MascotParameters extends SimpleParameterSet {

	private SimpleParameterSet parameterSet = null;
	Vector<SimpleParameter> para = null;
	HTTPForm iForm = null;

	Map<SimpleParameter, String[]> multi = null;

	private String serverName = "127.0.0.1";


	public MascotParameters(MascotSearchParameters parameters) throws MalformedURLException, IOException {
		super(new Parameter[] {});
		serverName = (String) parameters.getParameterValue(MascotSearchParameters.urlAddress);
		para = new Vector<SimpleParameter>();

		URL url = new URL(getSearchMaskUrlString());
		URLConnection lConn = url.openConnection();
		InputStream ins = lConn.getInputStream();

		multi = new HashMap<SimpleParameter, String[]>();
		iForm = HTTPForm.parseHTMLForm(ins);
		// Get all inputs.
		Vector<InputInterface> lvInputs = iForm.getInputs();
		for (int i = 0; i < lvInputs.size(); i++) {
			InputInterface lInput = (InputInterface) lvInputs.elementAt(i);

			// SELECTBOX
			if (lInput.getType() == InputInterface.SELECTINPUT) {

				if (lInput.getName().equals("FORMAT")) {
					// the data format of the input file
					lInput.setValue("Mascot generic");
				} else if (lInput.getName().equals("REPORT")) {
					// set REPORT to AUTO (the first entry)
					lInput.setValue(((SelectInput) lInput).getElements()[0]);
				} else {
					SelectInput input = (SelectInput) lInput;
					String[] elements = ((SelectInput) lInput).getElements();

					if (input.getMultiple()) {
						SimpleParameter p = new SimpleParameter(
								ParameterType.MULTIPLE_SELECTION, lInput
										.getComment(), lInput.getName(), null,
								null, Integer.valueOf(0), Integer.valueOf(8));
						para.add(p);
						multi.put(p, elements);
					} else {
							para.add(new SimpleParameter(ParameterType.STRING,
									lInput.getComment(), lInput.getName(),
									elements[0], elements));
					}
				}
			}

			// Checkbox
			if (lInput.getType() == InputInterface.CHECKBOX) {
				if (lInput.getName().equals("OVERVIEW")) {
					lInput.setValue("0");
				} else
					para.add(new SimpleParameter(ParameterType.BOOLEAN, lInput
							.getComment(), lInput.getName(), null, true, null,
							null, null));
			}

			// Radio
			if (lInput.getType() == InputInterface.RADIOINPUT) {
				RadioInput input = (RadioInput) lInput;
				String[] elements = input.getChoices();
				para
						.add(new SimpleParameter(ParameterType.STRING, lInput
								.getComment(), lInput.getName(), elements[0],
								elements));

			}

			// TextField
			if (lInput.getType() == InputInterface.TEXTFIELDINPUT
					&& lInput instanceof TextFieldInput) {
				TextFieldInput textFiled = (TextFieldInput) lInput;
				if (textFiled.isHidden()) {

				} else if (textFiled.getName().equals("FILE")) {
					textFiled.setValue("");
				} else if (textFiled.getName().equals("PRECURSOR")) {
					// the precursor mass (m/z)
					textFiled.setValue("");
				} else if (textFiled.getName().equals("USERNAME")) {
					textFiled.setValue("");
				} else if (textFiled.getName().equals("USEREMAIL")) {
					textFiled.setValue("");
				} else if (textFiled.getName().equals("COM")) {
					// Search Title
					textFiled.setValue("Mzmine "
							+ MZmineCore.getMZmineVersion());
				} else if (textFiled.getName().equals("SEG")) {
					// Proteinmass
					textFiled.setValue("");
				} else {
					para.add(new SimpleParameter(ParameterType.STRING, lInput
							.getComment(), lInput.getName()));
				}
			}
		}
		parameterSet = new SimpleParameterSet((Parameter[]) para
				.toArray(new Parameter[0]));
		Iterator<SimpleParameter> it = multi.keySet().iterator();
		while (it.hasNext()) {
			SimpleParameter p = (SimpleParameter) it.next();
			parameterSet.setMultipleSelection(p, multi.get(p));
		}
	}

	protected String getSearchMaskUrlString() {
		return getMascotInstallUrlString() + "cgi/search_form.pl?SEARCH=MIS";


	}

	protected String getMascotSubmitUrlString() {
		return getMascotInstallUrlString() + "cgi/nph-mascot.exe?1";


	}

	protected String getMascotInstallUrlString() {
		return "http://" + serverName + "/mascot/";
	}

	public String getServerName() {
		return serverName;
	}

	@Override
	public void exportValuesToXML(Element element) {
		// not used because we build the mask on the fly from the given URL
		// parameterSet.exportValuesToXML(element);
	}

	@Override
	public Object[] getMultipleSelection(Parameter parameter) {
		return parameterSet.getMultipleSelection(parameter);
	}

	@Override
	public Parameter getParameter(String name) {
		return parameterSet.getParameter(name);
	}

	@Override
	public Parameter[] getParameters() {
		return parameterSet.getParameters();
	}

	@Override
	public Object getParameterValue(Parameter parameter) {
		return parameterSet.getParameterValue(parameter);
	}

	@Override
	public void importValuesFromXML(Element element) {
		SimpleParameterSet parameterSet = new SimpleParameterSet();
		parameterSet.importValuesFromXML(element);
	}

	@Override
	public void setMultipleSelection(Parameter parameter,
			Object[] selectionArray) {
		parameterSet.setMultipleSelection(parameter, selectionArray);
	}

	@Override
	public void setParameterValue(Parameter parameter, Object value)
			throws IllegalArgumentException {
		parameterSet.setParameterValue(parameter, value);
	}

	@Override
	public String toString() {
		return parameterSet.toString();
	}

	@Override
	public SimpleParameterSet clone() {
		return this;
	}

	public synchronized String getBoundery() {
		return iForm.getBoundary();
	}

	public synchronized String getSubmissionString(File file, int charge) {
		SimpleParameter[] parameters = para.toArray(new SimpleParameter[0]);
		for (int i = 0; i < parameters.length; i++) {
			if (getParameterValue(parameters[i]) instanceof Object[]) {
				Object[] vals = (Object[]) getParameterValue(parameters[i]);
				StringBuffer buffer = new StringBuffer();
				int leng = (vals.length > 9) ? 9 : vals.length;
				for (int j = 0; j < leng; j++) {
					buffer.append(vals[j].toString());
					if (j < leng - 1) {
						buffer.append(",");
					}
				}
				iForm.getInputByName(parameters[i].getDescription()).setValue(
						buffer.toString());
			} else {
				iForm.getInputByName(parameters[i].getDescription()).setValue(
						getParameterValue(parameters[i]).toString());
			}
		}
		iForm.getInputByName("FILE").setValue(file.getAbsolutePath());
		if (charge != 0) {
			iForm.getInputByName("CHARGE").setValue(Integer.toString(charge));
		}

		return iForm.getSubmissionString();
	}

}
