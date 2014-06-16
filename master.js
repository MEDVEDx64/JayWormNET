// Master script template
//
// These functions is being called from JayWormNET and should return boolean value.
// Returning false means that the caller function should exit after calling scripted function,
//    so it's further logic may be replaced by scripted code.
//
// Look up the source code for invokeMasterScriptFunction calls to ensure where a function
//    is actually being called.

importPackage(org.themassacre.jaywnet)

// generic part

function onApplicationStarted() {
	return true
}

// http part

function onHTTPServerCreated(httpServerObj) {
	return true
}

function onHTTPRequestReceived(httpThreadObj, params, fileName) {
	return true
}

function onHTTPResponseSent(httpThreadObj, outputStream) {
	return true
}

function onGameHosted(httpThreadObj, params) {
	return true
}

function onGameClosed(httpThreadObj, params, gameID) {
	return true
}

// irc part

function onIRCServerCreated(ircServerObj) {
	return true
}

function onIRCCommandReceived(userObj, command, body) {
	return true
}

function onIRCUserConnected(ircServerObj, socketObj) {
	return true
}

function onIRCUserJoined(userObj, channel) {
	return true
}

function onIRCUserParted(userObj, channel, reason) {
	return true
}

function onIRCUserQuit(userObj, reason) {
	return true
}

function onIRCRawReceived(userObj, bytes, bytesRead) {
	return true
}

function onIRCRawSent(userObj, message) {
	return true
}

function onIRCMessageSent(userObj, message) {
	return true
}
