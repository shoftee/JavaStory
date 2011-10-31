/*
 * This file is part of the OdinMS Maple Story Server Copyright (C) 2008 ~ 2010
 * Patrick Huy <patrick.huy@frz.cc> Matthias Butz <matze@odinms.de> Jan
 * Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation. You may not use, modify or distribute this
 * program under any other version of the GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.server.handling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import javastory.tools.HexTool;

public class ExternalCodeTableGetter {

	Properties props;

	public ExternalCodeTableGetter(final Properties properties) {
		this.props = properties;
	}

	private static <T extends Enum<? extends IntValueHolder> & IntValueHolder> T valueOf(final String name, final T[] values) {
		for (final T val : values) {
			if (val.name().equals(name)) {
				return val;
			}
		}
		return null;
	}

	private <T extends Enum<? extends IntValueHolder> & IntValueHolder> int getValue(final String name, final T[] values, final int def) {
		final String prop = this.props.getProperty(name);
		if (prop != null && prop.length() > 0) {
			final String trimmed = prop.trim();
			final String[] args = trimmed.split(" ");
			int base = 0;
			String offset;
			if (args.length == 2) {
				base = valueOf(args[0], values).getValue();
				if (base == def) {
					base = this.getValue(args[0], values, def);
				}
				offset = args[1];
			} else {
				offset = args[0];
			}
			if (offset.length() > 2 && offset.substring(0, 2).equals("0x")) {
				return Integer.parseInt(offset.substring(2), 16) + base;
			} else {
				return Integer.parseInt(offset) + base;
			}
		}
		return def;
	}

	public static <T extends Enum<? extends WritableIntValueHolder> & WritableIntValueHolder> String getOpcodeTable(final T[] enumeration) {
		final StringBuilder enumVals = new StringBuilder();
		final List<T> all = new ArrayList<>(); // need a mutable list plawks
		all.addAll(Arrays.asList(enumeration));
		Collections.sort(all, new Comparator<IntValueHolder>() {

			@Override
			public int compare(final IntValueHolder o1, final IntValueHolder o2) {
				return Integer.valueOf(o1.getValue()).compareTo(o2.getValue());
			}
		});
		for (final T code : all) {
			enumVals.append(code.name());
			enumVals.append(" = ");
			enumVals.append("0x");
			enumVals.append(HexTool.toString(code.getValue()));
			enumVals.append(" (");
			enumVals.append(code.getValue());
			enumVals.append(")\n");
		}
		return enumVals.toString();
	}

	public static <T extends Enum<? extends WritableIntValueHolder> & WritableIntValueHolder> void populateValues(final Properties properties, final T[] values) {
		final ExternalCodeTableGetter exc = new ExternalCodeTableGetter(properties);
		for (final T code : values) {
			code.setValue(exc.getValue(code.name(), values, -2));
		}

		// if (log.isTraceEnabled()) { // generics - copy pasted between send
		// and recv current?
		// log.trace(getOpcodeTable(values));
		// }
	}
}
