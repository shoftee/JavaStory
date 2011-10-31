package javastory.scripting;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javastory.channel.ChannelClient;
import javastory.channel.server.Portal;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class PortalScriptManager {

	private static final PortalScriptManager instance = new PortalScriptManager();
	private final Map<String, PortalScript> scripts = new HashMap<>();
	private final static ScriptEngineFactory sef = new ScriptEngineManager().getEngineByName("javascript").getFactory();

	public static PortalScriptManager getInstance() {
		return instance;
	}

	private PortalScript getPortalScript(final String scriptName) {
		if (this.scripts.containsKey(scriptName)) {
			return this.scripts.get(scriptName);
		}
		final File scriptFile = new File("scripts/portal/" + scriptName + ".js");
		if (!scriptFile.exists()) {
			this.scripts.put(scriptName, null);
			return null;
		}
		FileReader fr = null;
		final ScriptEngine portal = sef.getScriptEngine();
		try {
			fr = new FileReader(scriptFile);
			final CompiledScript compiled = ((Compilable) portal).compile(fr);
			compiled.eval();
		} catch (final ScriptException | IOException e) {
			System.err.println("THROW" + e);
		} finally {
			if (fr != null) {
				try {
					fr.close();
				} catch (final IOException e) {
					System.err.println("ERROR CLOSING" + e);
				}
			}
		}
		final PortalScript script = ((Invocable) portal).getInterface(PortalScript.class);
		this.scripts.put(scriptName, script);
		return script;
	}

	public final void executePortalScript(final Portal portal, final ChannelClient c) {
		final PortalScript script = this.getPortalScript(portal.getScriptName());
		if (script != null) {
			script.enter(new PortalPlayerInteraction(c, portal));
		} else {
			System.out.println(":: Unhandled portal script " + portal.getScriptName() + " ::");
		}
	}

	public final void clearScripts() {
		this.scripts.clear();
	}
}