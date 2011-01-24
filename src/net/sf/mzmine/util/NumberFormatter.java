/*
 * Copyright 2006-2011 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.dom4j.Element;

/**
 * NumberFormat extension to provide both number-style and date-style
 * formatting, with XML import/export of format definition. This class is
 * synchronized, so it can be used by multiple threads.
 */
public class NumberFormatter extends NumberFormat implements Cloneable {

	public static final String TYPE_ELEMENT_NAME = "type";
	public static final String PATTERN_ELEMENT_NAME = "pattern";

	public enum FormatterType {
		TIME, NUMBER
	}

	private FormatterType embeddedFormatterType;
	private Format embeddedFormatter;

	public NumberFormatter(FormatterType type, String pattern) {
		setFormat(type, pattern);
	}

	public NumberFormatter(Element xmlElement) {
		importFromXML(xmlElement);
	}

	public void setFormat(FormatterType type, String pattern) {
		this.embeddedFormatterType = type;
		switch (type) {
		case TIME:

			// We want to avoid the 12-hour format ('h') and use 24-hour format
			// starting with 0 instead ('H')
			pattern = pattern.replace('h', 'H');

			SimpleDateFormat sdf = new SimpleDateFormat(pattern);
			// important for handling low values, otherwise in different time
			// zones we may get to negative numbers
			sdf.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));
			embeddedFormatter = sdf;

			break;
		case NUMBER:
			embeddedFormatter = new DecimalFormat(pattern);
			break;
		}
	}

	public FormatterType getType() {
		return embeddedFormatterType;
	}

	public String getPattern() {
		switch (embeddedFormatterType) {
		case TIME:
			return ((SimpleDateFormat) embeddedFormatter).toPattern();
		case NUMBER:
			return ((DecimalFormat) embeddedFormatter).toPattern();
		}
		return null;
	}

	public void importFromXML(Element xmlElement) {
		String pattern = xmlElement.elementText(PATTERN_ELEMENT_NAME);
		FormatterType type = FormatterType.valueOf(xmlElement
				.elementText(TYPE_ELEMENT_NAME));
		setFormat(type, pattern);
	}

	public void exportToXML(Element xmlElement) {
		Element typeElement = xmlElement.addElement(TYPE_ELEMENT_NAME);
		typeElement.setText(embeddedFormatterType.toString());
		Element patternElement = xmlElement.addElement(PATTERN_ELEMENT_NAME);
		patternElement.setText(getPattern());
	}

	/**
	 * @see java.text.NumberFormat#format(double, java.lang.StringBuffer,
	 *      java.text.FieldPosition)
	 */
	public synchronized StringBuffer format(double arg0, StringBuffer arg1,
			FieldPosition arg2) {

		// conversion to msec
		if (embeddedFormatterType == FormatterType.TIME)
			arg0 *= 1000;

		return embeddedFormatter.format(arg0, arg1, arg2);
	}

	/**
     */
	public synchronized StringBuffer format(Range range, StringBuffer arg1,
			FieldPosition arg2) {

		embeddedFormatter.format(range.getMin(), arg1, new FieldPosition(0));
		arg1.append(" - ");
		embeddedFormatter.format(range.getMax(), arg1, new FieldPosition(0));
		return arg1;
	}

	/**
	 * @see java.text.NumberFormat#format(long, java.lang.StringBuffer,
	 *      java.text.FieldPosition)
	 */
	public synchronized StringBuffer format(long arg0, StringBuffer arg1,
			FieldPosition arg2) {

		// conversion to msec
		if (embeddedFormatterType == FormatterType.TIME)
			arg0 *= 1000;

		return embeddedFormatter.format(arg0, arg1, arg2);
	}

	/**
	 * @see java.text.NumberFormat#parse(java.lang.String,
	 *      java.text.ParsePosition)
	 */
	public synchronized Number parse(String str, ParsePosition pos) {
		try {
			switch (embeddedFormatterType) {
			case TIME:
				SimpleDateFormat sdf = (SimpleDateFormat) embeddedFormatter;
				return ((sdf.parse(str, pos).getTime()) / 1000.0);
			case NUMBER:
				DecimalFormat df = (DecimalFormat) embeddedFormatter;
				return df.parse(str, pos);
			}
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * @see java.text.NumberFormat#clone()
	 */
	public NumberFormatter clone() {
		return new NumberFormatter(embeddedFormatterType, getPattern());
	}

}
