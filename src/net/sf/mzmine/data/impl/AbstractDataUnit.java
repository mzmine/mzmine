/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.data.impl;

import java.util.Vector;
import java.util.Hashtable;

import net.sf.mzmine.data.DataUnit;


/**
 * DataUnit implementation
 */
public abstract class AbstractDataUnit implements DataUnit {

    // This is for implementing DataUnit interface
    private Hashtable<Class, Vector<DataUnit>> myDataUnits = new Hashtable<Class, Vector<DataUnit>>();

    public void addData(Class dataType, DataUnit data) {

        Vector<DataUnit> adu = myDataUnits.get(dataType);

        if (adu==null) {
            adu = new Vector<DataUnit>();
            myDataUnits.put(dataType, adu);
        }

        adu.add(data);

    }

    public DataUnit[] getData(Class dataType) {

        Vector<DataUnit> adu = myDataUnits.get(dataType);

        if (adu==null) return new DataUnit[0];

        return adu.toArray(new DataUnit[adu.size()]);

    }

    public DataUnit getLastData(Class dataType) {

        Vector<DataUnit> adu = myDataUnits.get(dataType);

        if (adu==null) return null;

        return adu.lastElement();

	}

    public boolean hasData(Class dataType) {

        return myDataUnits.containsKey(dataType);

    }

    public boolean removeAllData(Class dataType) {

		Vector<DataUnit> adu = myDataUnits.remove(dataType);

		if (adu==null) return false;

		return true;

	}

	public boolean removeData(Class dataType, DataUnit data) {

        Vector<DataUnit> adu = myDataUnits.get(dataType);

        if (adu==null) return false;

        boolean res = adu.remove(data);

        if (res==false) return res;

        if (adu.size()==0) myDataUnits.remove(dataType);

        return true;

	}

}
