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

import net.sf.mzmine.data.CompoundIdentity;
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

	protected CompoundIdentity[] runQueryOnInternalDatabase(
			QBIXLipidDBQuery query) {

		ArrayList<CompoundIdentity> compoundIdentities = new ArrayList<CompoundIdentity>();
/*
		if (connection.isClosed()) {
			logger
					.warning("Tried to run a query while database connection is closed.");
			return null;
		}
*/
		String xQueryString = "for $a in input()/CommonLipid where  $a/BasePeak < "
				+ query.getMaxMZ()
				+ " and  $a/BasePeak > "
				+ query.getMinMZ()
				+ " return  <Element> {$a/Name}\n{$a/MonoIsoMass}\n {$a/BasePeak} </Element>";

		System.out.println("xQueryString=" + xQueryString);
/*
		TXQuery xQuery = TXQuery.newInstance(xQueryString);

		try {
			TResponse response = accessor.xquery(xQuery);

			// Iterate through results
			TXMLObjectIterator responseIterator = response
					.getXMLObjectIterator();
			while (responseIterator.hasNext()) { // as long as there are more

				TXMLObject doc = responseIterator.next();

				// TODO: Parse response
				StringWriter write = new StringWriter();
				doc.writeTo(write);
				System.out.println(write);
			}

			responseIterator.close();

		} catch (Exception e) {
			logger.warning(e.toString());
			return null;
		}
*/
		return compoundIdentities.toArray(new CompoundIdentity[0]);

	}

	protected CompoundIdentity[] runQueryOnLipidDatabase(QBIXLipidDBQuery query) {

		ArrayList<CompoundIdentity> compoundIdentities = new ArrayList<CompoundIdentity>();

		// TODO: Implementation

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

}
