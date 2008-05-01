package net.sf.mzmine.project.test;

import java.util.ArrayList;

import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.project.converters.SimpleDataPointConverter;

import com.thoughtworks.xstream.converters.Converter;

public class SimpleDataPointConverterTest extends GenericConverterTest {
	float MZ=100.123f;
	float INTENSITY=200000.11f;
	
	protected Object setUpObject(){
		return new SimpleDataPoint(MZ,INTENSITY);
	}

	protected Converter setUpConverter() {
		return new SimpleDataPointConverter();
	}

	protected ArrayList<String> setUpTransientFieldNames() throws Exception {
		return new ArrayList(0);
	}
}
