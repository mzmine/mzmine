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
import net.sf.mzmine.util.IsotopeUtils;
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
	 * This method retrieves the details about a PubChem compound
	 * 
	 */
	static PubChemCompound getCompound(int CID) throws IOException,
			DocumentException {

		URL url = new URL(
				"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pccompound&id="
						+ CID);

		String resultDocument = InetUtils.retrieveData(url);

		Document parsedResult = DocumentHelper.parseText(resultDocument);

		Element nameElement = (Element) parsedResult
				.selectSingleNode("//eSummaryResult/DocSum/Item[@Name='MeSHHeadingList']/Item");
		if (nameElement == null) {
			nameElement = (Element) parsedResult
					.selectSingleNode("//eSummaryResult/DocSum/Item[@Name='SynonymList']/Item");
		}
		if (nameElement == null) {
			nameElement = (Element) parsedResult
					.selectSingleNode("//eSummaryResult/DocSum/Item[@Name='IUPACName']");
		}
		if (nameElement == null)
			throw new DocumentException("Could not parse compound name");

		String compoundName = nameElement.getText();

		Element formulaElement = (Element) parsedResult
				.selectSingleNode("//eSummaryResult/DocSum/Item[@Name='MolecularFormula']");

		String compoundFormula = formulaElement.getText();

		double compoundMass = IsotopeUtils.calculateExactMass(compoundFormula);

		PubChemCompound newCompound = new PubChemCompound(CID, compoundName,
				compoundFormula, compoundMass);

		return newCompound;

	}

}
