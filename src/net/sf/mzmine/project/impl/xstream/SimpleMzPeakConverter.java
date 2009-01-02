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

import net.sf.mzmine.data.MzDataPoint;
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
        
        value.append(Double.toString(mzPeak.getMZ()));
        value.append(":");
        value.append(Double.toString(mzPeak.getIntensity()));
        
        for (MzDataPoint dp: mzPeak.getRawDataPoints()) {
            value.append(";");
            value.append(Double.toString(dp.getMZ()));
            value.append(":");
            value.append(Double.toString(dp.getIntensity()));
        }
        
		writer.setValue(value.toString());

	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {

		String value[] = reader.getValue().split(";");
		String mzValue[] = value[0].split(":");
        
        double mz = Double.valueOf(mzValue[0]);
        double intensity = Double.valueOf(mzValue[1]);;
        
        MzDataPoint dataPoints[] = new MzDataPoint[value.length - 1];
        for (int i = 1; i < value.length; i++) {
            String dpMzValue[] = value[i].split(":");
            double dpMz = Double.valueOf(dpMzValue[0]);
            double dpIntensity = Double.valueOf(dpMzValue[1]);;
            dataPoints[i - 1] = new SimpleDataPoint(dpMz, dpIntensity);
        }
        
        SimpleMzPeak mzPeak = new SimpleMzPeak(mz, intensity, dataPoints);
        
        return mzPeak;

	}

}
