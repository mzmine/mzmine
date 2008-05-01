package net.sf.mzmine.project.converters;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakListRow;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class PeakListConverter extends ReflectionConverter implements Converter {

	public PeakListConverter(Mapper mapper,
			ReflectionProvider reflectionProvider) {
		super(mapper, reflectionProvider);
	}

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public boolean canConvert(Class type) {
		// Only convert Object implements RawDataFile interface
		if (type.equals(PeakList.class)) {
			return true;
		}
		for (Class interfaceClass : type.getInterfaces()) {
			if (interfaceClass.equals(PeakList.class)) {
				return true;
			}
		}
		return false;
	}

	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {

		PeakList peakList = (PeakList) source;

		writer.startNode("ClassName");
		context.convertAnother(peakList.getClass());
		writer.endNode();

		PeakListRow peakListRows[] = peakList.getRows();
		writer.startNode("PeakListRows");
		// manually iterate through peakListRows to save memory usage
		for (PeakListRow row : peakListRows) {
			writer.startNode("PeakListRow");
			context.convertAnother(row);
			writer.endNode();
			writer.flush();
		}
		writer.endNode();

		RawDataFile dataFiles[] = peakList.getRawDataFiles();
		writer.startNode("RowDataFiles");
		context.convertAnother(dataFiles);
		writer.endNode();

		// treat other attributes

		String[] knownAttrs = { "peakListRows", "dataFiles" };

		Class cls = peakList.getClass();
		Field[] fieldList = cls.getDeclaredFields();
		Field field;
		String fieldName;

		fields: for (int i = 0; i < fieldList.length; i++) {
			field = fieldList[i];
			field.setAccessible(true);
			fieldName = field.getName();
			// check fieldName is in knownAttrs
			for (String known : knownAttrs) {
				if (known.equals(fieldName)) {
					continue fields;
				}
			}
			// if fieldName is not marshaled yet

			try {
				Object obj = field.get(peakList);
				writer.startNode(fieldName);
				context.convertAnother(obj);
				writer.endNode();
			} catch (Exception e) {
				// do nothing
			}
		}

	}

	protected void setObjectField(Object obj, String fieldName, Object value)
			throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {
		Field field;
		field = obj.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(obj, value);
	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		String className;
		Class cls;
		ArrayList<PeakListRow> peakListRows = new ArrayList<PeakListRow>(0);
		RawDataFile[] dataFiles;
		PeakList peakList;

		try {

			reader.moveDown();
			className = reader.getValue();
			reader.moveUp();
			cls = Class.forName(className);
			peakList = (PeakList) this.reflectionProvider.newInstance(cls);

			reader.moveDown();
			for (int i = 0; reader.hasMoreChildren(); i++) {
				reader.moveDown();
				peakListRows.add((PeakListRow) context.convertAnother(null,
						SimplePeakListRow.class));
				reader.moveUp();
			}
			reader.moveUp();
			this.setObjectField(peakList, "peakListRows", peakListRows);

			reader.moveDown();
			dataFiles = (RawDataFile[]) context.convertAnother(null, Array
					.newInstance(RawDataFile.class, 0).getClass());
			reader.moveUp();
			this.setObjectField(peakList, "dataFiles", dataFiles);

			// treat other fields
			Object value;
			String key;
			Field field;
			for (int i = 0; reader.hasMoreChildren(); i++) {
				reader.moveDown();
				key = reader.getNodeName();
				try {
					field = cls.getDeclaredField(key);
					value = context.convertAnother(null, field.getType());
					this.setObjectField(peakList, key, value);

				} catch (NoSuchFieldException e) {
					logger.info("Field " + key + " not found for class "
							+ cls.getName());
				} finally {
					reader.moveUp();
				}
			}

		} catch (Exception e) {
			return null;
		}

		return peakList;
	}

}
