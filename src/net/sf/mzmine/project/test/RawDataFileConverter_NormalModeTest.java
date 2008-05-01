package net.sf.mzmine.project.test;

import java.io.File;

import net.sf.mzmine.data.PreloadLevel;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.RawDataFileImpl;
import net.sf.mzmine.project.converters.RawDataFileConverter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;

public class RawDataFileConverter_NormalModeTest extends GenericConverterTest {

	protected Object setUpObject() throws Exception {
		RawDataFile dataFile;
		File dir = File.createTempFile(this.getClass().getName(), "");
		dir.delete();
		dir.mkdir();
		dataFile = new RawDataFileImpl(dir, "testRawDataFile",
				PreloadLevel.NO_PRELOAD);
		return dataFile;
	}

	protected Converter setUpConverter() {
		this.registerOmitField(RawDataFileImpl.class, "logger");
		this.registerOmitField(RawDataFileImpl.class, "scanDataFile");
		this.registerOmitField(RawDataFileImpl.class, "writingScanDataFile");

		XStream xstream = new XStream();
		RawDataFileConverter converter= new RawDataFileConverter(xstream.getMapper(), xstream
				.getReflectionProvider());
		converter.setMode(RawDataFileConverter.Mode.NORMAL);
		return converter;
	}
}
