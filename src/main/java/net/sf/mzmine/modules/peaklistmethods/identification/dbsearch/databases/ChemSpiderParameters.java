/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.databases;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

/**
 * Set of parameters specific to ChemSpider searches.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ChemSpiderParameters extends SimpleParameterSet {

    /**
     * Search API security token.
     */
    public static final StringParameter SECURITY_TOKEN =
            new StringParameter("ChemSpider security token",
                                "Security token from your ChemSpider account - register at ChemSpider.com");

    /**
     * Create the parameter set.
     */
    public ChemSpiderParameters() {
        super(new Parameter[]{SECURITY_TOKEN});
    }
}
