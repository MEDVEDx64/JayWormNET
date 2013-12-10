/*/ Part of FryWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.util;

public class WACharTable {
	// W:A character table
	private static final char[] waChars = {
		0, '\u0411', '\u0413', '\u0414', '\u0416', '\u0417', '\u0418', '\u0419', '\u041A', '\u041B', '\u041F',
		'\u0423', '\u0424', '\u0426', '\u0427', '\u0428', '\u0429','\u042A', '\u042B', '\u042C', '\u042D', '\u042E',
		0, '\u042F', '\u0431', '\u0432', '\u0433', '\u0434', '\u0436', '\u0437', '\u0438', '\u0439', '\u0178',' ',
		'\u00A1', '\u043A', '\u00A3',0, '\u043B', '\u043C', '\u043D', '\u043F', '\u0442', '\u0444', '\u0446', '\u0447',
		'\u0448', '\u0449', '\u044A','\u044B', '\u044C', '\u044D', '\u044E', '\u044F', '\u0150', '\u0151', '\u0170',
		'\u0171', 0, 0, 0, 0, 0, 0, '\u00BF','\u00C0', '\u00C1', '\u00C2', '\u00C3', '\u00C4', '\u00C5', '\u00C6',
		'\u00C7', '\u00C8', '\u00C9', '\u00CA', '\u00CB', '\u00CC', '\u00CD', '\u00CE', '\u00CF','\u00D0', '\u00D1',
		'\u00D2', '\u00D3', '\u00D4', '\u00D5', '\u00D6', '\u00D7', '\u00D8', '\u00D9', '\u00DA', '\u00DB', '\u00DC',
		'\u00DD', '\u00DE', '\u00DF','\u00E0', '\u00E1', '\u00E2', '\u00E3', '\u00E4', '\u00E5', '\u00E6', '\u00E7',
		'\u00E8', '\u00E9', '\u00EA', '\u00EB', '\u00EC', '\u00ED', '\u00EE', '\u00EF','\u00F0', '\u00F1', '\u00F2',
		'\u00F3', '\u00F4', '\u00F5', '\u00F6', '\u00F7', '\u00F8', '\u00F9', '\u00FA', '\u00FB', '\u00FC', '\u00FD',
		'\u00FE', '\u00FF'
	};
	
	// From where waChars begins
	private static final int first = 0x7f;
	
	public static char decode(byte ch) { // to Unicode
		if(ch >= 0 && ch < first) return (char)ch;
		return (waChars[256 + ch - first] == 0? (char)ch: waChars[256 + ch - first]); 
	}
	
	public static byte encode(char ch) {
		if(ch < first) return (byte)ch;
		for(int x = 0; x < waChars.length; x++)
			if(waChars[x] == ch) return (byte)(first+x);
		return '?';
	}
	
	public static String decode(byte[] bytes) {
		StringBuffer buffer = new StringBuffer();
		for(int i = 0; i < bytes.length; i++) {
			buffer.ensureCapacity(i+2);
			buffer.append(decode(bytes[i]));
		}
		return buffer.toString();
	}
	
	public static byte[] encode(String s) {
		byte[] bytes = {};
		for(int i = 0; i < s.length(); i++) {
			// appending to array
			byte[] tmp = new byte[bytes.length+1];
			for(int z = 0; z < bytes.length; z++)
				tmp[z] = bytes[z];
			tmp[bytes.length] = encode(s.charAt(i));
			bytes = tmp;
		}
		return bytes;
	}
}
