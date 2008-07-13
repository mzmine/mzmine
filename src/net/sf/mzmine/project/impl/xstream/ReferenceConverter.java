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

package net.sf.mzmine.project.impl.xstream;

import java.util.HashMap;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class ReferenceConverter extends AbstractReflectionConverter {

    private HashMap<Object, Integer> saveStore = new HashMap<Object, Integer>();
    private HashMap<Integer, Object> loadStore = new HashMap<Integer, Object>();

    private int currentId = 1;
    private int numOfSerializedScans = 0, numOfSerializedRows = 0, numOfDeserializedScans = 0, numOfDeserializedRows = 0;

    public ReferenceConverter(Mapper mapper,
            ReflectionProvider reflectionProvider) {
        super(mapper, reflectionProvider);
    }

    public boolean canConvert(Class type) {

        return Scan.class.isAssignableFrom(type)
                || RawDataFile.class.isAssignableFrom(type)
                || PeakList.class.isAssignableFrom(type)
                || PeakListRow.class.isAssignableFrom(type)
                || ChromatographicPeak.class.isAssignableFrom(type);

    }

    public void marshal(Object source, HierarchicalStreamWriter writer,
            MarshallingContext context) {

        if (saveStore.containsKey(source)) {
            writer.setValue("Ref#" + saveStore.get(source));
        } else {

            super.marshal(source, writer, context);
            saveStore.put(source, currentId);
            currentId++;

            if (source instanceof Scan)
                numOfSerializedScans++;

            if (source instanceof PeakListRow)
                numOfSerializedRows++;

        }
    }

    public Object unmarshal(HierarchicalStreamReader reader,
            UnmarshallingContext context) {

        String value = reader.getValue();
        if (value.startsWith("Ref#")) {
            Integer refNum = new Integer(value.substring(4));
            return loadStore.get(refNum);
        } else {
            Object result = super.unmarshal(reader, context);
            loadStore.put(currentId, result);
            currentId++;
            
            if (result instanceof Scan)
                numOfDeserializedScans++;

            if (result instanceof PeakListRow)
                numOfDeserializedRows++;
            
            return result;
        }
    }

    /**
     * Returns the number of serialized Scan instances
     */
    public int getNumOfSerializedScans() {
        return numOfSerializedScans;

    }

    /**
     * Returns the number of serialized PeakListRow instances
     */
    public int getNumOfSerializedRows() {
        return numOfSerializedRows;
    }
    
    /**
     * Returns the number of deserialized Scan instances
     */
    public int getNumOfDeserializedScans() {
        return numOfDeserializedScans;

    }

    /**
     * Returns the number of deserialized PeakListRow instances
     */
    public int getNumOfDeserializedRows() {
        return numOfDeserializedRows;
    }

}
