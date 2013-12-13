/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.util;

import java.io.*;

public class StreamUtils {

	// Returns internal JAR's resource as a stream, may be overridden by an external one.
	// resName — resource (file) name, obj — use 'this' (or any object, if are in static context)
	public static InputStream getResourceAsStream(String resName, Object obj) throws FileNotFoundException {
		InputStream str = obj.getClass().getResourceAsStream(resName.startsWith("/")? resName: "/" + resName);
		File file = new File(resName);
		if(file.exists())
			return new FileInputStream(file);
		else if(str != null)
			return str;
		else
			throw new FileNotFoundException("No such resource: " + resName);
	}

}
