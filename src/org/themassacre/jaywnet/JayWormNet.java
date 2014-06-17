/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.jaywnet;
import javax.script.*;
import java.io.*;

import org.themassacre.util.StreamUtils;

// Main class
public class JayWormNet {
	public static HTTPServer http = null;
	public static IRCServer irc = null;
	
	private static ScriptEngineManager man;
	private static ScriptEngine engine;
	private static Invocable masterScript = null;
	private static String lastInvocation = "unknown";
	
	public static ConfigurationManager config;
	public static final String version = "beta10";
	public static boolean forceNoGUI = false;

	static void printHelp() {
		System.err.println("JayWormNET — Java WormNET server\n\nList of available command-line options:\n"
				+ "\t--nogui\t\t- never use GUI\n\t--version\t- show JayWormNET version\n\t--help\t\t"
				+ "- show this message\n\nYou may want to edit or create the 'wnet.cfg' file for fine tuning.");
	}
	
	public static void main(String[] args) {
		// Command-line arguments
		if(args.length > 0) {
			if(args[0].equals("--nogui"))
				forceNoGUI = true;
			else if(args[0].equals("--help")) {
				printHelp();
				System.exit(0);
			} else if(args[0].equals("--version")) {
				System.err.println("JayWormNET " + version);
				System.exit(0);
			}
		}
		
		// Creating configuration storage
		config = new ConfigurationManager("wnet.cfg"); // file name here
		
		// Initializing logging
		WNLogger.start();
		WNLogger.l.info("JayWormNET " + version);
		WNLogger.l.info("Server hostname is '" + config.serverHost + "'");

		// Creating script engine
		man = new ScriptEngineManager();
		engine = man.getEngineByName("JavaScript");
		reloadMasterScript();
		
		if(!invokeMasterScriptFunction("onApplicationStarted")) return;
		
		// Starting servers
		if(config.HTTPPort > 0)		http = new HTTPServer();
		if(config.IRCPort > 0)		irc = new IRCServer();
	}
	
	public static void reloadMasterScript() {
		if(!config.masterScriptEnabled)
			return;
		
		try {
			engine.eval(new InputStreamReader(StreamUtils.getResourceAsStream(
					config.masterScriptFileName, config)));
			masterScript = (Invocable) engine;
		} catch(Exception e) {
			WNLogger.l.warning("Unable to evaluate the master script: " + e);
			masterScript = null;
		}
	}
	
	public static boolean invokeMasterScriptFunction(String func, Object ... args) {
		if(!config.masterScriptEnabled)
			return true;
		
		lastInvocation = func;
		try {
			return (Boolean)masterScript.invokeFunction(func, args);
		} catch(Throwable e) {
			if(config.invocationWarningsEnabled)
				WNLogger.l.warning("Invocation of '" + func + "' has failed: " + e);
			return true;
		}
	}
	
	public static String getLastInvokedFunctionName() {
		return lastInvocation;
	}

}

class InterruptedByInvocationException extends Exception {
	private static final long serialVersionUID = -4236019516497508114L;
	protected String functionName = "unknown";
	
	public InterruptedByInvocationException() {
		super();
		functionName = new String(JayWormNet.getLastInvokedFunctionName());
	}
	
	public InterruptedByInvocationException(String func) {
		super();
		functionName = func;
	}
	
	@Override public String getMessage() {
		return "'" + functionName + "'";
	}
	
}
