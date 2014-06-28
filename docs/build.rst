===================
Building JayWormNET
===================

To build *JayWormNET* in automated way, you need JDK 7 and Apache Ant.

*JayWormNET* uses *nProperty* library to parse the main configuration
file, so you need to download it first and put it into 'lib' directory.
You can get it `here <http://jfork.googlecode.com/svn/tags/nproperty/nproperty-1.1.jar>`_.

Then, get into *JayWormNET* directory, and run `ant`. On Windows you can use 'Shift + Right Click'
to quickly open cmd window by selecting it from context menu. Executable jar will be created in the 'build' folder.
Before building, you may want to change JayWormNET's default configuration files in 'config' directory.

Embedding configuration files
=============================

Usually, *JayWormNET* stores it's configuration files inside it's own .jar package. Any of them may be overridden by an external copy,
placed to the current working directory.

It's a good practice (for convenient deploying) to pre-configure the server before the build right in the `config` directory,
so once your .jar gets built, it already will be configured and ready for use.

See :ref:`config` for further instructions.

Running
=======

*JayWormNET* only requires Java SE 7.

With default settings (and HTTP port value unchanged on 80) on *nix* hosts it should be
executed with root privileges. By default, it shows up GUI (when available) to let it
be visible on graphical environment after you run it by double-click in file manager —
GUI can be forced to be disabled by configuration file of by `--nogui` command-line flag.

Note about logging
==================

Logging to file is disabled by default and you may set `loggingEnabled = true` in `wnet.cfg` file
to enable it back. Log file (`wn.log`) will be written in the current working directory (when
using desktop environment, it's usually the same folder where jar is placed).