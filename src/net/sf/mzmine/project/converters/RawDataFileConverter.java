package net.sf.mzmine.project.converters;

import java.util.HashMap;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.project.MZmineProject;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class RawDataFileConverter extends AbstractReflectionConverter implements
		Converter {
	/*
	 * Stateful converter to store RawDataFileImpl When in normal mode, this
	 * stores RawDataFile using reflection. When in simplified mode this stores
	 * only fileName of RawDataFile Need MZmineProject as constructor argument.
	 * Actual rawDataFile is acquired from project with fileName as key
	 */
		private HashMap <String,RawDataFile>store;
	
	public enum Mode {
		NORMAL, SIMPLE
	}

	private Mode mode;

	public RawDataFileConverter(Mapper mapper,
			ReflectionProvider reflectionProvider) {
		super(mapper, reflectionProvider);
		this.mode = Mode.SIMPLE;
		this.store=new HashMap<String,RawDataFile>();
	}

	public boolean canConvert(Class type) {
		// Only convert Object implements RawDataFile interface
		if (type.equals(RawDataFile.class)) {
			return true;
		}
		for (Class interfaceClass : type.getInterfaces()) {
			if (interfaceClass.equals(RawDataFile.class)) {
				return true;
			}
		}
		return false;
	}

	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		if (this.mode == Mode.SIMPLE) {
			String fileName = ((RawDataFile) source).getFileName();
			writer.startNode("fileName");
			writer.setValue(fileName);
			writer.endNode();
		} else {
			super.marshal(source, writer, context);
		}
	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		RawDataFile dataFile;
		if (this.mode == Mode.SIMPLE) {

			reader.moveDown();
			String fileName = reader.getValue();
			reader.moveUp();
			dataFile=this.store.get(fileName);
		} else {
			dataFile = (RawDataFile)super.unmarshal(reader, context);
			this.store.put(dataFile.getFileName(),dataFile);
		}
		return dataFile;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}
}
