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

package net.sf.mzmine.modules.identification.qbixlipiddb;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.logging.Logger;
/*
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
*/
import net.sf.mzmine.data.CompoundIdentity;
import net.sf.mzmine.data.impl.SimpleCompoundIdentity;
/*
import com.softwareag.tamino.db.api.accessor.TAccessLocation;
import com.softwareag.tamino.db.api.accessor.TXMLObjectAccessor;
import com.softwareag.tamino.db.api.accessor.TXQuery;
import com.softwareag.tamino.db.api.connection.TConnection;
import com.softwareag.tamino.db.api.connection.TConnectionCloseException;
import com.softwareag.tamino.db.api.connection.TConnectionFactory;
import com.softwareag.tamino.db.api.objectModel.TXMLObject;
import com.softwareag.tamino.db.api.objectModel.TXMLObjectIterator;
import com.softwareag.tamino.db.api.objectModel.dom.TDOMObjectModel;
import com.softwareag.tamino.db.api.response.TResponse;
*/
class QBIXLipidDBConnection {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private String databaseURI;
/*
	private TConnection connection;
	private TXMLObjectAccessor accessor;
*/
	private QBIXLipidDBSearchParameters parameters;

	QBIXLipidDBConnection(QBIXLipidDBSearchParameters parameters) {

		this.parameters = parameters;

		databaseURI = (String) parameters
				.getParameterValue(QBIXLipidDBSearchParameters.databaseURI);

	}

	protected boolean openConnection() {

		try {
/*
			connection = TConnectionFactory.getInstance().newConnection(
					databaseURI);

			accessor = connection.newXMLObjectAccessor(TAccessLocation
					.newInstance("Lipidomics"), TDOMObjectModel.getInstance());
*/
		} catch (Exception e) {

			logger.warning(e.toString());
			return false;

		}

		logger.finest("Opened connection to database");

		return true;

	}

	/**
	 * Runs query on database of common lipids
	 */
	protected CompoundIdentity[] runQueryOnCommonLipids(QBIXLipidDBQuery query) {

		ArrayList<CompoundIdentity> compoundIdentities = new ArrayList<CompoundIdentity>();
/*
		if (connection.isClosed()) {
			logger
					.warning("Tried to run a query while database connection is closed.");
			return null;
		}

		String xQueryString = "for $a in input()/CommonLipid where  $a/BasePeak < "
				+ query.getMaxMZ()
				+ " and $a/BasePeak > "
				+ query.getMinMZ()
				+ " return  <Element> {$a/Name}\n{$a/MonoIsoMass}\n {$a/BasePeak} </Element>";

		TXQuery xQuery = TXQuery.newInstance(xQueryString);

		try {
			TResponse response = accessor.xquery(xQuery);

			TXMLObjectIterator responseIterator = response
					.getXMLObjectIterator();
			while (responseIterator.hasNext()) {

				TXMLObject doc = responseIterator.next();

				String compoundID = "";
				String compoundName = "";
				String[] alternateNames = null;
				String compoundFormula = "";
				String databaseEntryURL = (String) parameters
						.getParameterValue(QBIXLipidDBSearchParameters.databaseURI);
				String identificationMethod = "Search on internal lipid library";

				Element element = (Element) doc.getElement();

				NodeList compoundProperties = element.getChildNodes();
				for (int itemNumber = 0; itemNumber < compoundProperties
						.getLength(); itemNumber++) {

					Node property = compoundProperties.item(itemNumber);
					if (property.getNodeName().equalsIgnoreCase("name")) {
						compoundName = property.getTextContent();
					}

				}

				CompoundIdentity newIdentity = new SimpleCompoundIdentity(
						compoundID, compoundName, alternateNames,
						compoundFormula, databaseEntryURL, identificationMethod);

				compoundIdentities.add(newIdentity);

			}

			responseIterator.close();

		} catch (Exception e) {
			logger.warning(e.toString());
			return null;
		}
*/
		return compoundIdentities.toArray(new CompoundIdentity[0]);

	}

	/**
	 * Runs the query on database of theoretical lipids
	 */
	protected CompoundIdentity[] runQueryOnTheoreticalLipids(
			QBIXLipidDBQuery query) {

		ArrayList<CompoundIdentity> compoundIdentities = new ArrayList<CompoundIdentity>();
/*
		if (connection.isClosed()) {
			logger
					.warning("Tried to run a query while database connection is closed.");
			return null;
		}

		String xQueryString = "declare namespace tf = \"http://namespaces.softwareag.com/tamino/TaminoFunction\" for $a in input()/Compound where  tf:containsText($a/Molecule/Class,\""
				+ query.getName()
				+ "\") and $a/Molecule/Isotopicdistribution/Mass[1] < "
				+ (query.getMaxMZ() - query.getAdd())
				+ " and  $a/Molecule/Isotopicdistribution/Mass[1] > "
				+ (query.getMinMZ() - query.getAdd())
				+ " return  <Results> "
				+ "{$a/VTT_ID} \n "
				+ "{$a/Molecule/Name} \n "
				+ "{$a/Molecule/Molecular_Formula} "
				+ "</Results> sort by (Score_abundance)";

		TXQuery xQuery = TXQuery.newInstance(xQueryString);

		try {
			TResponse response = accessor.xquery(xQuery);

			TXMLObjectIterator responseIterator = response
					.getXMLObjectIterator();
			while (responseIterator.hasNext()) {

				TXMLObject doc = responseIterator.next();

				String compoundID = "";
				String compoundName = "";
				String[] alternateNames = null;
				String compoundFormula = "";
				String databaseEntryURL = (String) parameters
						.getParameterValue(QBIXLipidDBSearchParameters.databaseURI);
				String identificationMethod = "Search on theoretical lipid database";

				Element element = (Element) doc.getElement();

				NodeList compoundProperties = element.getChildNodes();
				for (int itemNumber = 0; itemNumber < compoundProperties
						.getLength(); itemNumber++) {

					Node property = compoundProperties.item(itemNumber);

					if (property.getNodeName().equalsIgnoreCase("VTT_ID"))
						compoundID = property.getTextContent();

					if (property.getNodeName().equalsIgnoreCase("Name"))
						compoundName = property.getTextContent();

					if (property.getNodeName().equalsIgnoreCase(
							"Molecular_Formula"))
						compoundFormula = property.getTextContent();

				}

				CompoundIdentity newIdentity = new SimpleCompoundIdentity(
						compoundID, compoundName, alternateNames,
						compoundFormula, databaseEntryURL, identificationMethod);

				compoundIdentities.add(newIdentity);

			}

			responseIterator.close();

		} catch (Exception e) {
			logger.warning(e.toString());
			return null;
		}
*/
		return compoundIdentities.toArray(new CompoundIdentity[0]);

	}

	protected void closeConnection() {
/*
		if (connection.isClosed())
			return;

		try {
			connection.close();
		} catch (TConnectionCloseException e) {
			logger.warning(e.toString());
		}
*/
		logger.finest("Closed connection to database");

	}

	/**
	 * DEBUG
	 */
	public static void main(String argz[]) {

		QBIXLipidDBSearchParameters parameters = new QBIXLipidDBSearchParameters();

		QBIXLipidDBConnection dbConnection = new QBIXLipidDBConnection(
				parameters);

		if (!dbConnection.openConnection()) {
			System.out.println("Could not open database connection");
			return;
		}

		QBIXLipidDBQuery testQuery = new QBIXLipidDBQuery(
				"Glycerophospholipi*", 496.34107f, 202.700265f, 50.0f, "[M+H]",
				1.007825f, 0.2f, "LPC/LPE/LPA/LSer");

		QBIXLipidDBUtils utils = new QBIXLipidDBUtils(parameters);

		CompoundIdentity[] identities = dbConnection
				.runQueryOnTheoreticalLipids(testQuery);

		for (CompoundIdentity identity : identities) {
			System.out.println("ID="
					+ identity.getCompoundID()
					+ ", CompoundName="
					+ identity.getCompoundName()
					+ ", Formula="
					+ identity.getCompoundFormula()
					+ ", IdentificationMethod="
					+ identity.getIdentificationMethod()
					+ ", DatabaseEntryURL="
					+ identity.getDatabaseEntryURL()
					+ ", Validated="
					+ utils.validateTheoreticalLipidIdentity(testQuery,
							identity));
		}

		dbConnection.closeConnection();
	}

}
