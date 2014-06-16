// Example additional command script,
//  implements IIRCAdditionalCommand.
//
// Add 'example' in commands.lst to check it out.

function getRequiredArgsCount() {
	return 0
}

function getPermissionLevel() {
	return 2
}

function execute(sender, channel, args) {
	sender.sendMessage('Workaet!')
}
