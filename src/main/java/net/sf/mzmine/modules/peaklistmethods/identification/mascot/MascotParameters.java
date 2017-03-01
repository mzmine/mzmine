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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.mascot;

import net.sf.mzmine.parameters.impl.SimpleParameterSet;

public class MascotParameters extends SimpleParameterSet {

    /*
     * public static final PeakListsParameter peakLists = new
     * PeakListsParameter();
     * 
     * ArrayList<Parameter> para = new ArrayList<Parameter>(); HTTPForm iForm =
     * null;
     * 
     * private String serverName = "127.0.0.1";
     * 
     * @SuppressWarnings("unchecked") public MascotParameters(String serverName)
     * throws MalformedURLException, IOException {
     * 
     * super(new Parameter[0]);
     * 
     * para.add(peakLists);
     * 
     * URL url = new URL(getSearchMaskUrlString()); URLConnection lConn =
     * url.openConnection(); InputStream ins = lConn.getInputStream();
     * 
     * iForm = HTTPForm.parseHTMLForm(ins); // Get all inputs.
     * Vector<InputInterface> lvInputs = iForm.getInputs(); for (int i = 0; i <
     * lvInputs.size(); i++) { InputInterface lInput = (InputInterface)
     * lvInputs.elementAt(i);
     * 
     * // SELECTBOX if (lInput.getType() == InputInterface.SELECTINPUT) {
     * 
     * if (lInput.getName().equals("FORMAT")) { // the data format of the input
     * file lInput.setValue("Mascot generic"); } else if
     * (lInput.getName().equals("REPORT")) { // set REPORT to AUTO (the first
     * entry) lInput.setValue(((SelectInput) lInput).getElements()[0]); } else {
     * SelectInput input = (SelectInput) lInput; String[] elements =
     * ((SelectInput) lInput).getElements();
     * 
     * if (input.getMultiple()) { UserParameter p = new
     * MultiChoiceParameter<String>( lInput.getComment(), lInput.getName(),
     * elements); para.add(p);
     * 
     * } else { UserParameter p = new ComboParameter<String>(
     * lInput.getComment(), lInput.getName(), elements); para.add(p); } } }
     * 
     * // Checkbox if (lInput.getType() == InputInterface.CHECKBOX) { if
     * (lInput.getName().equals("OVERVIEW")) { lInput.setValue("0"); } else
     * para.add(new BooleanParameter(lInput.getComment(), lInput .getName())); }
     * 
     * // Radio if (lInput.getType() == InputInterface.RADIOINPUT) { RadioInput
     * input = (RadioInput) lInput; String[] elements = input.getChoices();
     * para.add(new ComboParameter<String>(lInput.getComment(), lInput
     * .getName(), elements));
     * 
     * }
     * 
     * // TextField if (lInput.getType() == InputInterface.TEXTFIELDINPUT &&
     * lInput instanceof TextFieldInput) { TextFieldInput textFiled =
     * (TextFieldInput) lInput; if (textFiled.isHidden()) {
     * 
     * } else if (textFiled.getName().equals("FILE")) { textFiled.setValue("");
     * } else if (textFiled.getName().equals("PRECURSOR")) { // the precursor
     * mass (m/z) textFiled.setValue(""); } else if
     * (textFiled.getName().equals("USERNAME")) { textFiled.setValue(""); } else
     * if (textFiled.getName().equals("USEREMAIL")) { textFiled.setValue(""); }
     * else if (textFiled.getName().equals("COM")) { // Search Title
     * textFiled.setValue("Mzmine " + MZmineCore.getMZmineVersion()); } else if
     * (textFiled.getName().equals("SEG")) { // Proteinmass
     * textFiled.setValue(""); } else { para.add(new
     * StringParameter(lInput.getComment(), lInput .getName())); } } }
     * 
     * }
     * 
     * protected String getSearchMaskUrlString() { return
     * getMascotInstallUrlString() + "cgi/search_form.pl?SEARCH=MIS"; }
     * 
     * protected String getMascotSubmitUrlString() { return
     * getMascotInstallUrlString() + "cgi/nph-mascot.exe?1"; }
     * 
     * protected String getMascotInstallUrlString() { return "http://" +
     * serverName + "/mascot/"; }
     * 
     * public String getServerName() { return serverName; }
     * 
     * public synchronized String getBoundery() { return iForm.getBoundary(); }
     * 
     * public synchronized String getSubmissionString(File file, int charge) {
     * 
     * /* for (Parameter p : para) { Object value = p.getValue();
     * 
     * if (value.getClass().isArray()) { Object[] vals = (Object[]) value;
     * StringBuffer buffer = new StringBuffer(); int leng = (vals.length > 9) ?
     * 9 : vals.length; for (int j = 0; j < leng; j++) {
     * buffer.append(vals[j].toString()); if (j < leng - 1) {
     * buffer.append(","); } }
     * iForm.getInputByName(p.getDescription()).setValue( buffer.toString()); }
     * else { iForm.getInputByName(p.getDescription()).setValue(
     * value.toString()); } }
     * iForm.getInputByName("FILE").setValue(file.getAbsolutePath()); if (charge
     * != 0) {
     * iForm.getInputByName("CHARGE").setValue(Integer.toString(charge)); }
     * 
     * return iForm.getSubmissionString();
     * 
     * return null; }
     * 
     * @Override public Parameter[] getParameters() { return para.toArray(new
     * Parameter[0]); }
     * 
     * @SuppressWarnings("unchecked")
     * 
     * @Override public <T extends Parameter> T getParameter(T parameter) { for
     * (Parameter p : para) { if (p.getName().equals(parameter.getName()))
     * return (T) p; } return null; }
     * 
     * @Override public void loadValuesFromXML(Element element) { // ignore }
     * 
     * @Override public void saveValuesToXML(Element element) { // ignore }
     * 
     * public ParameterSet cloneParameter() { return this; }
     */
}
