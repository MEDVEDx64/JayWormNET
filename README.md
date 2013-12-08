*FryWormNET* — Java rewrite of CyberShadow's MyWormNET.

About
=====

This is pretty lightweight *Worms Armageddon WormNET* server
in a single executable JAR file, with all configuration files inside
(which may be overridden by external ones).
It's still under development now.

Build
=====

To build *FryWormNET* in automated way, you need *Apache Ant*.
*FryWormNET* uses *nProperty* library to parse the main configuration
file, so you need to download it first and put it into 'lib' directory.
You can get it [here](http://jfork.googlecode.com/svn/tags/nproperty/nproperty-1.1.jar).
Then, type `ant`; output JAR will be created in the 'build' folder. Before building,
you may want to change FryWormNET's default configuration files in 'config' directory.

License
=======

See the LICENSE file.

