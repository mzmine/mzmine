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

package net.sf.mzmine.project.impl.xstream;

import net.sf.mzmine.data.impl.SimpleDataPoint;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class SimpleDataPointConverter implements Converter {

	public boolean canConvert(Class type) {
		return type.equals(SimpleDataPoint.class);
	}

	public void marshal(Object original, final HierarchicalStreamWriter writer,
			final MarshallingContext context) {
        
		SimpleDataPoint dataPoint = (SimpleDataPoint) original;

        double mz = dataPoint.getMZ();
		double intensity = dataPoint.getIntensity();
		
		String value = Double.toString(mz) + ":"
				+ Double.toString(intensity);
        
		writer.setValue(value);

	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {

		String value[] = reader.getValue().split(":");

		double mz = Double.valueOf(value[0]);
		double intensity = Double.valueOf(value[1]);;
		SimpleDataPoint dataPoint = new SimpleDataPoint(mz,intensity);
		return dataPoint;

	}

}
