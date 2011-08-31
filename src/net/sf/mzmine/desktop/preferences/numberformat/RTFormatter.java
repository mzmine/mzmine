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

package net.sf.mzmine.desktop.preferences.numberformat;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * NumberFormat extension to provide both number-style and date-style
 * formatting, with XML import/export of format definition. This class is
 * synchronized, so it can be used by multiple threads.
 */
public class RTFormatter extends NumberFormat implements Cloneable {

	private RTFormatterType embeddedNumberFormatterType;
	private DecimalFormat embeddedFormatter;

	public RTFormatter(RTFormatterType type, String pattern) {
		setFormat(type, pattern);
	}

	public void setFormat(RTFormatterType type, String pattern) {
		assert type != null;
		this.embeddedNumberFormatterType = type;
		this.embeddedFormatter = new DecimalFormat(pattern);
	}

	public RTFormatterType getType() {
		return embeddedNumberFormatterType;
	}

	public String getPattern() {
		return embeddedFormatter.toPattern();
	}

	/**
	 * @see java.text.NumberFormat#format(double, java.lang.StringBuffer,
	 *      java.text.FieldPosition)
	 */
	public synchronized StringBuffer format(double arg0, StringBuffer arg1,
			FieldPosition arg2) {

		// conversion to min
		if (embeddedNumberFormatterType == RTFormatterType.NumberInMin)
			arg0 /= 60;

		return embeddedFormatter.format(arg0, arg1, arg2);
	}

	/**
	 * @see java.text.NumberFormat#format(long, java.lang.StringBuffer,
	 *      java.text.FieldPosition)
	 */
	public synchronized StringBuffer format(long arg0, StringBuffer arg1,
			FieldPosition arg2) {

		// conversion to min
		if (embeddedNumberFormatterType == RTFormatterType.NumberInMin)
			arg0 /= 60;

		return embeddedFormatter.format(arg0, arg1, arg2);
	}

	/**
	 * @see java.text.NumberFormat#parse(java.lang.String,
	 *      java.text.ParsePosition)
	 */
	public synchronized Number parse(String str, ParsePosition pos) {
		if (embeddedNumberFormatterType == RTFormatterType.NumberInMin)
			return embeddedFormatter.parse(str, pos).doubleValue() * 60;
		else
			return embeddedFormatter.parse(str, pos);
	}

	/**
	 * @see java.text.NumberFormat#clone()
	 */
	public RTFormatter clone() {
		return new RTFormatter(embeddedNumberFormatterType, getPattern());
	}

}
