package net.sf.mzmine.project.converters;

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

		float intensity = dataPoint.getIntensity();
		float mz = dataPoint.getMZ();
		String value = Float.toString(intensity) + ":"
				+ Float.toString(mz);
		writer.setValue(value);

	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {

		String value = reader.getValue();

		float intensity = new Float(value.split(":")[0]).floatValue();
		float mz = new Float(value.split(":")[1]).floatValue();
		SimpleDataPoint dataPoint = new SimpleDataPoint(mz,intensity);
		return dataPoint;

	}

}
