/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.jaywnet;

// Additional command interface
// May be used to create custom commands

public interface IIRCAdditionalCommand {
	// Command name
	public String getName();

	// Minimal required arguments count
	public int getRequiredArgsCount();

	// Enabled/disabled flag
	public boolean isEnabled();

	// Permission level — nobody(0), oper(1), any(2)
	public int getPermissionLevel();

	// Command implementation body
	public void execute(User sender, String channel, String[] args);
}
