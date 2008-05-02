package net.sf.mzmine.project.test;

import java.io.File;

import net.sf.mzmine.data.PreloadLevel;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.RawDataFileImpl;
import net.sf.mzmine.project.converters.PeakListConverter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;

public class PeakListConverter_SimplePeakListTest extends GenericConverterTest {

	protected Object setUpObject() throws Exception {
		RawDataFile dataFile;
		File dir = File.createTempFile(this.getClass().getName(), "");
		dir.delete();
		dir.mkdir();
		dataFile = MZmineCore.createNewFile("testRawDataFile",
				PreloadLevel.NO_PRELOAD).finishWriting();
		return new SimplePeakList("testSimplePeakList", dataFile);
	}

	protected Converter setUpConverter() {
		this.registerOmitField(RawDataFileImpl.class, "logger");
		this.registerOmitField(RawDataFileImpl.class, "scanDataFile");
		this.registerOmitField(RawDataFileImpl.class, "writingScanDataFile");

		XStream xstream = new XStream();
		return new PeakListConverter(xstream.getMapper(), xstream
				.getReflectionProvider());
	}
}
