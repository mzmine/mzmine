/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.identification.pubchem;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import net.sf.mzmine.util.InetUtils;
import net.sf.mzmine.util.Range;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class PubChemGateway {

	static int[] findPubchemCID(Range massRange, int numOfResults,
			boolean chargedOnly) throws IOException, DocumentException {

		StringBuilder pubchemUrl = new StringBuilder();

		pubchemUrl
				.append("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?usehistory=n&db=pccompound&sort=cida&retmax=");
		pubchemUrl.append(numOfResults);
		pubchemUrl.append("&term=");
		pubchemUrl.append(massRange.getMin());
		pubchemUrl.append(":");
		pubchemUrl.append(massRange.getMax());
		pubchemUrl.append("[MonoisotopicMass]");

		if (chargedOnly)
			pubchemUrl.append(" NOT 0[CHRG]");

		URL url = new URL(pubchemUrl.toString());

		String resultDocument = InetUtils.retrieveData(url);
		Document parsedResult = DocumentHelper.parseText(resultDocument);

		List cidElements = parsedResult.getRootElement().element("IdList")
				.elements("Id");

		int cidArray[] = new int[cidElements.size()];
		for (int i = 0; i < cidElements.size(); i++) {
			Element cidElement = (Element) cidElements.get(i);
			cidArray[i] = Integer.parseInt(cidElement.getText());
		}

		return cidArray;

	}

	/**
	 * This method retrieve the SDF file of the compound from PubChem
	 * 
	 * @param compound
	 * @param mass
	 * @throws Exception
	 */
	static void getSummary(PubChemCompound compound)
			throws IOException {

		URL url = new URL(
				"http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid="
						+ compound.getID() + "&disopt=DisplaySDF");

		String sdfCompoundData = InetUtils.retrieveData(url);

		String summaryLines[] = sdfCompoundData.split("\n");

		for (int i = 0; i < summaryLines.length; i++) {

			String line = summaryLines[i];

			if (line.matches(".*PUBCHEM_MOLECULAR_FORMULA.*")) {
				compound.setCompoundFormula(summaryLines[i + 1]);
				continue;
			}

			if (line.matches(".*PUBCHEM_MONOISOTOPIC_WEIGHT.*")) {
				double exactMass = Double.parseDouble(summaryLines[i + 1]);
				compound.setExactMass(exactMass);
				continue;
			}

		}

		compound.setStructure(sdfCompoundData);

	}

	/**
	 * This method exists due a lack of information in SDF file (missing name)
	 * from PubChem
	 * 
	 * @param id
	 * @param compound
	 */
	static String getName(int cid) throws IOException {

		URL endpoint = new URL(
				"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pccompound&id="
						+ cid + "&report=brief&mode=text");

		String name = InetUtils.retrieveData(endpoint);
		String idString = String.valueOf(cid);
		int index = name.indexOf(idString, 0);
		name = name.substring(index + idString.length());

		return name;

	}

}
