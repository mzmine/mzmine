package net.sf.mzmine.project.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.io.impl.StorableScan;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectType;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.persistence.FileStreamStrategy;
import com.thoughtworks.xstream.persistence.StreamStrategy;

public class MZmineFileStreamStrategy extends FileStreamStrategy implements
		StreamStrategy {

	private final FilenameFilter filter;

	private final XStream xstream;

	private final File baseDirectory;
	private final ProjectType type;
	private String suffix;

	public MZmineFileStreamStrategy(File baseDirectory, MZmineProject project,
			ProjectType type) {
		this(baseDirectory, new XStream(), project, type);
	}

	public MZmineFileStreamStrategy(File baseDirectory, XStream xstream,
			MZmineProject project, ProjectType type) {
		super(baseDirectory);
		this.baseDirectory = baseDirectory;
		this.xstream = xstream;
		this.type = type;

		if (this.type == ProjectType.zippedXML) {
			this.suffix = ".xml.zip";
		} else {
			this.suffix = ".xml";
		}

		// register aliases
		this.xstream.alias("SimpleScan", SimpleScan.class);
		this.xstream.alias("SimplePeakListRow", SimplePeakListRow.class);
		this.xstream.alias("StorableScan", StorableScan.class);

		// register converter for specific type
		RawDataFileConverter rawDataFileConverter = new RawDataFileConverter(
				this.xstream.getMapper(), this.xstream.getReflectionProvider(),
				project);
		rawDataFileConverter.setMode(RawDataFileConverter.Mode.NORMAL);
		this.xstream.registerConverter(rawDataFileConverter);

		this.xstream.registerConverter(new SimpleDataPointConverter());

		this.filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return new File(dir, name).isFile() && isValid(dir, name);
			}
		};
	}

	protected boolean isValid(File dir, String name) {
		return name.endsWith(this.suffix);
	}

	protected String getName(Object key) {
		return escape(key.toString()) + this.suffix;

	}

	private OutputStream getOutputStream(File file) throws IOException,
			FileNotFoundException {
		OutputStream os;
		OutputStream fos = new FileOutputStream(file);
		if (this.type == ProjectType.zippedXML) {
			ZipOutputStream zos = new ZipOutputStream(fos);
			zos.putNextEntry(new ZipEntry(file.getName().replace(".zip", "")));
			os = zos;
		} else {
			os = fos;
		}
		return os;
	}

	private void writeFile(File file, Object value) {
		try {
			OutputStream os = this.getOutputStream(file);
			RawDataFileConverter converter = (RawDataFileConverter) this.xstream
					.getConverterLookup().lookupConverterForType(
							RawDataFile.class);
			if (value instanceof RawDataFile) {

				converter.setMode(RawDataFileConverter.Mode.NORMAL);
			} else {
				converter.setMode(RawDataFileConverter.Mode.SIMPLE);
			}
			try {
				this.xstream.toXML(value, os);
			} finally {
				os.close();
			}
		} catch (IOException e) {
			throw new StreamException(e);
		}
	}

	private File getFile(String filename) {
		return new File(this.baseDirectory, filename);
	}

	private InputStream getInputStream(File file) throws IOException,
			FileNotFoundException {
		InputStream is;
		InputStream fis = new FileInputStream(file);
		if (this.type == ProjectType.zippedXML) {
			ZipInputStream zis = new ZipInputStream(fis);
			zis.getNextEntry();
			is = zis;
		} else {
			is = fis;
		}
		return is;
	}

	private Object readFile(File file) {
		try {
			InputStream is = this.getInputStream(file);

			RawDataFileConverter converter = (RawDataFileConverter) this.xstream
					.getConverterLookup().lookupConverterForType(
							RawDataFile.class);

			if (file.getParentFile().getName().equals("dataFiles")) {
				converter.setMode(RawDataFileConverter.Mode.NORMAL);
			} else {
				converter.setMode(RawDataFileConverter.Mode.SIMPLE);
			}

			try {
				return this.xstream.fromXML(is);
			} finally {
				is.close();
			}
		} catch (FileNotFoundException e) {
			// not found... file.exists might generate a sync problem
			return null;
		} catch (IOException e) {
			throw new StreamException(e);
		}
	}

	public Object put(Object key, Object value) {
		Object oldValue = get(key);
		String filename = getName(key);
		writeFile(new File(baseDirectory, filename), value);
		return oldValue;
	}

	public Object get(Object key) {
		return readFile(getFile(getName(key)));
	}
}
