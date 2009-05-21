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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.mzmine.util.InetUtils;
import net.sf.mzmine.util.Range;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class PubChemGateway {

	/**
	 * Searches for CIDs of PubChem compounds based on their exact
	 * (monoisotopic) mass. Returns maximum numOfResults results sorted by the
	 * CID. If chargedOnly parameter is set, returns only molecules with
	 * non-zero charge.
	 */
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
	 */
	static void getSummary(PubChemCompound compound) throws IOException {

		URL url = new URL(
				"http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid="
						+ compound.getID() + "&disopt=DisplaySDF");

		String sdfCompoundData = InetUtils.retrieveData(url);

		String summaryLines[] = sdfCompoundData.split("\n");

		for (int i = 0; i < summaryLines.length; i++) {

			String line = summaryLines[i];

			if (line.matches(".*PUBCHEM_IUPAC_NAME.*")) {
				if (compound.getName() == PubChemCompound.UNKNOWN_NAME)
					compound.setCompoundName(summaryLines[i + 1]);
				continue;
			}

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
	 * This method the name of the compound in PubChem based on its CID.
	 * Unfortunately, there is no nice way how to obtain the name (= MeSH term)
	 * from the XML records of PubChem, so we need to parse the HTML contents of
	 * compound page. This may stop working if the structure of the PubChem site
	 * is changed.
	 * 
	 */
	static String getName(int cid) throws IOException {

		URL endpoint = new URL(
				"http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid="
						+ cid);

		String htmlDocument = InetUtils.retrieveData(endpoint);

		Pattern p = Pattern
				.compile("<font size=4><b>([^<]+) - </b></font><font size=4><b>Compound Summary</b>");

		Matcher m = p.matcher(htmlDocument);

		if (!m.find())
			return null;

		String name = m.group(1);

		return name;

	}

}
