package net.sf.mzmine.project.impl;

import net.sf.mzmine.io.RawDataFile;
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
	private MZmineProject project;

	public enum Mode {
		NORMAL, SIMPLE
	}

	private Mode mode;

	public RawDataFileConverter(Mapper mapper,
			ReflectionProvider reflectionProvider, MZmineProject project) {
		super(mapper, reflectionProvider);
		this.project = project;
		this.mode = Mode.SIMPLE;
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
		Object object;
		if (this.mode == Mode.SIMPLE) {

			reader.moveDown();
			String fileName = reader.getValue();
			reader.moveUp();
			object = this.project.getDataFile(fileName);
		} else {
			object = super.unmarshal(reader, context);
		}
		return object;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}
}
