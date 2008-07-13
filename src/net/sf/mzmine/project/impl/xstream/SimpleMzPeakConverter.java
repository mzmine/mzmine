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

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleMzPeak;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class SimpleMzPeakConverter implements Converter {

	public boolean canConvert(Class type) {
		return type.equals(SimpleMzPeak.class);
	}

	public void marshal(Object original, final HierarchicalStreamWriter writer,
			final MarshallingContext context) {
        
        SimpleMzPeak mzPeak = (SimpleMzPeak) original;

		StringBuilder value = new StringBuilder();
        
        value.append(Float.toString(mzPeak.getMZ()));
        value.append(":");
        value.append(Float.toString(mzPeak.getIntensity()));
        
        for (DataPoint dp: mzPeak.getRawDataPoints()) {
            value.append(";");
            value.append(Float.toString(dp.getMZ()));
            value.append(":");
            value.append(Float.toString(dp.getIntensity()));
        }
        
		writer.setValue(value.toString());

	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {

		String value[] = reader.getValue().split(";");
		String mzValue[] = value[0].split(":");
        
        float mz = Float.valueOf(mzValue[0]);
        float intensity = Float.valueOf(mzValue[1]);;
        
        DataPoint dataPoints[] = new DataPoint[value.length - 1];
        for (int i = 1; i < value.length; i++) {
            String dpMzValue[] = value[i].split(":");
            float dpMz = Float.valueOf(dpMzValue[0]);
            float dpIntensity = Float.valueOf(dpMzValue[1]);;
            dataPoints[i - 1] = new SimpleDataPoint(dpMz, dpIntensity);
        }
        
        SimpleMzPeak mzPeak = new SimpleMzPeak(mz, intensity, dataPoints);
        
        return mzPeak;

	}

}
