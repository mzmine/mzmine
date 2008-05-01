package net.sf.mzmine.project.impl;

import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.RawDataFileImpl;
import net.sf.mzmine.main.StorableScan;
import net.sf.mzmine.project.converters.PeakListConverter;
import net.sf.mzmine.project.converters.RawDataFileConverter;
import net.sf.mzmine.project.converters.SimpleDataPointConverter;

import com.thoughtworks.xstream.XStream;

public class MZmineXStream {
	public static XStream getXstream() {
		XStream xstream = new XStream();
		// Register aliases
		xstream.alias("SimpleScan", SimpleScan.class);
		xstream.alias("SimplePeakListRow", SimplePeakListRow.class);
		xstream.alias("StorableScan", StorableScan.class);
		xstream.alias("SimpleDataPoint", SimpleDataPoint.class);
		xstream.alias("RawDataFile", RawDataFileImpl.class);

		// Register omit fields
		xstream.omitField(MZmineProjectImpl.class, "projectDir");
		xstream.omitField(MZmineProjectImpl.class, "logger");
		xstream.omitField(MZmineProjectImpl.class, "listeners");
		xstream.omitField(MZmineProjectImpl.class, "rawDataList");
		xstream.omitField(MZmineProjectImpl.class, "peakListsList");

		xstream.omitField(RawDataFileImpl.class, "logger");
		xstream.omitField(RawDataFileImpl.class, "scanDataFile");
		xstream.omitField(RawDataFileImpl.class, "writingScanDataFile");

		// register converter for specific type
		RawDataFileConverter rawDataFileConverter = new RawDataFileConverter(
				xstream.getMapper(), xstream.getReflectionProvider());
		rawDataFileConverter.setMode(RawDataFileConverter.Mode.NORMAL);
		xstream.registerConverter(rawDataFileConverter);
		xstream.registerConverter(new SimpleDataPointConverter());
		xstream.registerConverter(new PeakListConverter(xstream.getMapper(),
				xstream.getReflectionProvider()));

		return xstream;
	}
}
