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
package javastory.scripting;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javastory.channel.ChannelClient;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * 
 * @author Matze
 */
public abstract class AbstractScriptManager {

	private static final ScriptEngineManager sem = new ScriptEngineManager();

	protected Invocable getInvocable(String path, ChannelClient c) {
		FileReader fr = null;
		try {
			path = "scripts/" + path;
			ScriptEngine engine = null;

			if (c != null) {
				engine = c.getScriptEngine(path);
			}
			if (engine == null) {
				File scriptFile = new File(path);
				if (!scriptFile.exists()) {
					return null;
				}
				engine = sem.getEngineByName("javascript");
				if (c != null) {
					c.setScriptEngine(path, engine);
				}
				fr = new FileReader(scriptFile);
				engine.eval(fr);
			}
			return (Invocable) engine;
		} catch (Exception e) {
			System.err.println("Error executing script." + e);
			return null;
		} finally {
			try {
				if (fr != null) {
					fr.close();
				}
			} catch (IOException ignore) {
			}
		}
	}
}
