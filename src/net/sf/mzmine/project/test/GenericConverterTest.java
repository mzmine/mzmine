package net.sf.mzmine.project.test;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import net.sf.mzmine.project.test.comparator.FactoryComparator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;

public abstract class GenericConverterTest {
	protected Converter converter;
	protected XStream xstream;
	protected Object objectToStore;
	private OmitFieldRegistory ofRegist;

	@Before
	public void setUp() throws Exception {
		ofRegist = new OmitFieldRegistory();
		objectToStore = this.setUpObject();
		converter = this.setUpConverter();
		xstream = new XStream();
		xstream.registerConverter(converter);
		
	}

	// subclasses should provide these
	abstract protected Object setUpObject() throws Exception;
	abstract protected Converter setUpConverter() throws Exception;

	@After
	public void tearDown() throws Exception {
	}

	public void registerOmitField(Class cls, String fieldName) {
		this.ofRegist.register(cls, fieldName);
	}

	public void setObjectField(Object target, String name, Object value)
			throws Exception {
		Field field;
		field = target.getClass().getField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	protected boolean compareObjects(Object oldObject, Object newObject) {
		Field[] fields;
		Object oldValue;
		Object newValue;
		try {
			fields = oldObject.getClass().getDeclaredFields();
			for (Field oldField : fields) {
				if (this.ofRegist.registered(oldObject.getClass(), oldField
						.getName())) {
					continue;
				}
				oldField.setAccessible(true);
				oldValue = oldField.get(oldObject);
				newValue = oldField.get(newObject);
				if (oldValue == null) {
					assertTrue(newValue == null);
					continue;
				}
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@Test
	public void testConsistency() throws Exception {
		String xmlString;
		xmlString = xstream.toXML(objectToStore);
		Object newObject = xstream.fromXML(xmlString);
		FactoryComparator factoryComparator = new FactoryComparator();
		HashMap <Object,ArrayList<Object>> doneList= new HashMap <Object,ArrayList<Object>> ();
		boolean ok = factoryComparator.compare(this.objectToStore, newObject,this.ofRegist,doneList);
		assertTrue(ok);
	}
}
