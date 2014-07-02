.. _config:

=============
Configuration
=============

Configuring JayWormNET isn't a complicated task (until you ain't going deeper to the scripting).
There is a few configuration files, most of them have well-commented templates presented in the jar package or
in the `config` directory. **No one of them is necessary**, as the default file set (mostly) represents
JayWormNET's hard-coded defaults.

.. JayWormNET have the following configuration files:

The main configuration file
===========================

``wnet.cfg``

Example::

	IRCPort = 6667
	commandsEnabled = true
	serverHost = example.com

By the way, this file is the only one which name can't be changed (or even the file be moved).

This file is a set of variables with the simpliest syntax ``suchVariable = true`` per line.
Any unrecognized stuff (unknown variables, emply lines, symbols, etc.) is simply ignored,
so you can write comments in this file absolutely free. The only exception for commenting goes
for `after-line-comments`, which is not allowed and will cause your value to not be parsed properly.

Strings should be written `as is`, without ``"`` or ``'``.

There are any possible variables listed below.

Generic variables
-----------------

========== ======================================= =============================== =========================================================================
Type       Variable                                Example / Defaults              Description / Notes
========== ======================================= =============================== =========================================================================
int        ``HTTPPort``                            ``80``
int        ``IRCPort``                             ``6667``
String     ``serverHost``                          ``localhost``                   Server host name, must be changed to your real server address
int        ``gameLifeTime``                        ``240``                         How long a hosted game should be kept in the list (in seconds)
========== ======================================= =============================== =========================================================================

IRC-specific settings
---------------------

========== ======================================= =============================== =========================================================================
Type       Variable                                Example / Defaults              Description / Notes
========== ======================================= =============================== =========================================================================
boolean    ``useStealthIP``                        ``true``                        Hide real user's IP by replacing it with ``stealthIP``
String     ``stealthIP``                           ``no.address.for.you``
String     ``networkName``
String     ``charset``                             ``native``                      IRC charset (``native``, ``utf-8``, etc.)
boolean    ``useIRCPassword``                      ``true``
String     ``IRCPassword``                         ``ELSILRACLIHP``                Default Worms Armageddon IRC password
String     ``IRCOperPassword``                     ``kenny``                       Operator password, **don't forget to change it!**
int        ``specialMessagesPermissionLevel``      ``1``                           Who is able to use `special green` messages (0 nobody, 1, opers, 2 any)
========== ======================================= =============================== =========================================================================

IRC pinging
-----------

Pings avoid non-responding or unexceptedly disconnected users stay on server
by dropping them when the ping response timeout is reached.

========== ======================================= =============================== =========================================================================
Type       Variable                                Example / Defaults              Description / Notes
========== ======================================= =============================== =========================================================================
boolean    ``pingsEnabled``                        ``true``
int        ``pingInterval``                        ``60``                          (in seconds)
int        ``pingTimeout``                         ``60``                          (in seconds)
========== ======================================= =============================== =========================================================================

IRC login message settings
--------------------------

This section describes a configuration of messages, which client receives after
successful login.

========== ======================================= =============================== =========================================================================
Type       Variable                                Example / Defaults              Description / Notes
========== ======================================= =============================== =========================================================================
int        ``showIntro``                           ``2``                           IRC login messages settings, can be 0 (nothing), 1 (short) or 2 (full)
boolean    ``showCreated``                         ``true``                        Show server's creation date in IRC login
boolean    ``showPlayersCount``                    ``true``                        Show users count on IRC login
boolean    ``showChannelsCount``                   ``true``                        Show channels count on IRC login
boolean    ``showOps``                             ``false``                       Show operators count on IRC login
boolean    ``showCapabilities``                    ``true``                        Send server capabilities on IRC login
String     ``capPrefix``                           ``(ov)@+``                      Capabilities settings, not recommended to touch
String     ``capChanTypes``                        ``#``                           Capabilities settings, not recommended to touch
String     ``capChanModes``                        ``b,k,l,imnpst``                Capabilities settings, not recommended to touch
========== ======================================= =============================== =========================================================================

IRC anti-flood
--------------

========== ======================================= =============================== =========================================================================
Type       Variable                                Example / Defaults              Description / Notes
========== ======================================= =============================== =========================================================================
boolean    ``antiFloodEnabled``                    ``true``                        Avoid flooders by kicking them out
int        ``floodGate``                           ``1000``                        Flood tolerance
int        ``floodMaxLevel``                       ``4``                           Flood tolerance
========== ======================================= =============================== =========================================================================

Message Of The Day settings
---------------------------

========== ======================================= =============================== =========================================================================
Type       Variable                                Example / Defaults              Description / Notes
========== ======================================= =============================== =========================================================================
boolean    ``ircShowMOTD``                         ``true``                        Show IRC MOTD
boolean    ``httpShowMOTD``                        ``true``                        Show HTTP MOTD
String     ``ircMOTDFileName``                     ``motd.txt``
String     ``httpMOTDFileName``                    ``motd.html``
========== ======================================= =============================== =========================================================================

HTTP fallback settings
----------------------

Fallback page is what HTTP client will receive, when trying to visit any non-WormNET
(or simply non-existent) page.

========== ======================================= =============================== =========================================================================
Type       Variable                                Example / Defaults              Description / Notes
========== ======================================= =============================== =========================================================================
boolean    ``httpFallbackEnabled``                 ``false``                       Enable custom fallback page
String     ``httpFallbackPage``                    ``fallback.html``
boolean    ``httpAlwaysReloadFallbackPage``        ``false``                       Re-read the fallback page on every request
========== ======================================= =============================== =========================================================================

Logging settings
----------------

========== ======================================= =============================== =========================================================================
Type       Variable                                Example / Defaults              Description / Notes
========== ======================================= =============================== =========================================================================
boolean    ``loggingEnabled``                      ``false``                       Enable logging to file
String     ``loggingLevel``                        ``FINER``                       See `Java logging levels <http://docs.oracle.com/javase/7/docs/api/java/util/logging/Level.html>`_
String     ``logFile``                             ``wn.log``
boolean    ``announceGameHosting``                 ``false``                       Announce game hosting to the IRC channel
boolean    ``showOperatorsActions``                ``true``                        Show operator's actions to other operators
========== ======================================= =============================== =========================================================================

Lists
-----

========== ======================================= =============================== =========================================================================
Type       Variable                                Example / Defaults              Description / Notes
========== ======================================= =============================== =========================================================================
boolean    ``enableBanList``                       ``false``
boolean    ``enableWhiteList``                     ``false``
String     ``banListFileName``                     ``banlist.csv``
String     ``whiteListFileName``                   ``whitelist.csv``
String     ``channelsFileName``                    ``channels.lst``                Path to IRC channels list
String     ``commandsListFileName``                ``commands.lst``
========== ======================================= =============================== =========================================================================

Scripts configuration
---------------------

========== ======================================= =============================== =========================================================================
Type       Variable                                Example / Defaults              Description / Notes
========== ======================================= =============================== =========================================================================
boolean    ``masterScriptEnabled``                 ``false``
String     ``masterScriptFileName``                ``master.js``
String     ``scriptedCommandsPrefix``              ``commands/``                   Path to scripted additional commands
boolean    ``invocationWarningsEnabled``           ``false``                       Log master script invocation failures, useful for debugging
========== ======================================= =============================== =========================================================================

GUI settings
------------

========== ======================================= =============================== =========================================================================
Type       Variable                                Example / Defaults              Description / Notes
========== ======================================= =============================== =========================================================================
boolean    ``guiEnabled``                          ``true``
String     ``guiBackgroundColor``                  ``default``
String     ``guiForegroundColor``                  ``default``
========== ======================================= =============================== =========================================================================

Additional in-chat commands
---------------------------

========== ======================================= =============================== =========================================================================
Type       Variable                                Example / Defaults              Description / Notes
========== ======================================= =============================== =========================================================================
boolean    ``commandsEnabled``                     ``false``                       Enable additional commands, affects scripted commands too (when false)
boolean    ``scriptedCommandsEnabled``             ``false``
boolean    ``showCommandsInChat``                  ``false``                       **WARNING: using of ``!oper`` command will result in password leak!**
boolean    ``swallowAllCommands``                  ``false``                       Overrides ``showCommandsInChat``
========== ======================================= =============================== =========================================================================

Various stuff
-------------

========== ======================================= =============================== =========================================================================
Type       Variable                                Example / Defaults              Description / Notes
========== ======================================= =============================== =========================================================================
boolean    ``forceHosterIP``                       ``false``                       When a game hosted, it's address will be replaced with hoster's real IP
boolean    ``enableSabotageProtection``            ``false``                       Allows to close a game only from it's hoster IP
boolean    ``enableWheatSnooperSchemeFix``         ``true``                        Fix for old versions of The Wheat Snooper, which were unable to join in
boolean    ``enableURLSpellCheck``                 ``false``
========== ======================================= =============================== =========================================================================

Experimental / undocumented stuff
---------------------------------

Use with care!

========== ======================================= ===============================
Type       Variable                                Example / Defaults
========== ======================================= ===============================
int        ``HTTPFailureSleepTime``                ``750``
int        ``IRCFailureSleepTime``                 ``2500``
boolean    ``IRCSkipBytesWhenAvailable``           ``false``
boolean    ``dropIRCUserOnOverflow``               ``false``
int        ``IRCBufferSize``                       ``262144``
========== ======================================= ===============================
