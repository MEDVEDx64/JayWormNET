*JayWormNET* — Java rewrite of CyberShadow's MyWormNET.

About
=====

This is pretty lightweight *Worms Armageddon WormNET* server
in a single executable JAR file, with all configuration files inside
(which may be overridden by external ones in the same folder with JAR).

It supports multichannel, banlist/whitelist, additional in-chat commands
(like '!kick'), and have simple GUI. Most of it's features are listed and
can be fine-tuned in 'wnet.cfg' file.

The current project's stage is Beta.

Build
=====

To build *JayWormNET* in automated way, you need *Apache Ant*.

*JayWormNET* uses *nProperty* library to parse the main configuration
file, so you need to download it first and put it into 'lib' directory.
You can get it [here](http://jfork.googlecode.com/svn/tags/nproperty/nproperty-1.1.jar).

Then, get into *JayWormNET* directory, and run `ant`. On Windows you can use 'Shift + Right Click'
to quickly open cmd window by selecting it from context menu. Executable jar will be created in the 'build' folder.
Before building, you may want to change JayWormNET's default configuration files in 'config' directory.

Running JayWormNET
==================

*JayWormNET* only requires Java SE 7.

With default settings (and HTTP port value unchanged on 80) on *nix* hosts it should be
executed with root privileges. By default, it shows up GUI (when available) to let it
be visible on graphical environment after you run it by double-click in file manager —
GUI can be forced to be disabled by configuration file of by `--nogui` command-line flag.

Note about logging
==================

Logging to file is disabled by default and you may set `loggingEnabled = true` in `wnet.cfg` file
to enable it back. Log file (`wn.log`) will be writted in the current working directory (when
using desktop environment, it's usually the same folder where jar is placed).

License
=======

See the LICENSE file.

