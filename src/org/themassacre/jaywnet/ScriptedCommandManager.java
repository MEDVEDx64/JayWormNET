/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.jaywnet;
import java.util.HashMap;
import java.util.Map;

import javax.script.*;
import java.io.*;

public class ScriptedCommandManager {
	ScriptEngineManager man = null;
	ScriptEngine engine = null;
	private Map<String, IIRCAdditionalCommand> commands;
	
	public ScriptedCommandManager() {
		man = new ScriptEngineManager();
		engine = man.getEngineByName("JavaScript");
		reloadScripts();
	}
	
	public void reloadScripts() {
		commands = new HashMap<String, IIRCAdditionalCommand>();
		File repo = new File(JayWormNet.config.scriptedCommandsPrefix);
		if(!repo.exists() || !repo.isDirectory())
			return;
		
		File[] files = repo.listFiles();
		for(File f: files) {
			if(!f.isFile() || !f.getName().endsWith(".js") || !isFilenameIsValid(f.getName()
					.substring(0, f.getName().length()-3))) {
				WNLogger.l.warning("Skipping invalid script file: " + f.getName());
				continue;
			}
			
			try {
				engine.eval(new FileReader(f));
				Invocable inv = (Invocable)engine;
				commands.put(f.getName().substring(0, f.getName().length()-3),
						inv.getInterface(IIRCAdditionalCommand.class));
			} catch(Exception e) {
				WNLogger.l.warning("Failed to evaluate script file \"" + f.getName() + "\": " + e);
			}
		}
	}
	
	// returns null when there's no such command
	public IIRCAdditionalCommand getCommandByName(String cmd) {
		if(commands.containsKey(cmd))
			return commands.get(cmd);
		return null;
	}
	
	public boolean isFilenameIsValid(String s) {
		for(int i = 0; i < s.length(); i++)
			if(!Character.isLetter(s.charAt(i)) && !Character.isDigit(s.charAt(i))
					&& s.charAt(i) != '_' && s.charAt(i) != '-' && s.charAt(i) != '+')
				return false;
		return true;
	}
}
