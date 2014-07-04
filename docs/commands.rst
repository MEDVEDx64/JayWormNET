.. _commands:

================
In-chat commands
================

JayWormNET's IRC server supports in-chat commands (a.k.a. `Additional commands`), started from "!", for example, ``!foo bar``.
The commands should be enabled in the configuration file (see :ref:`config_wnetcfg`), otherwise you will just say it to chat;
also don't forget to white-list required commands if it isn't done yet: :ref:`config_cmdlist`.

Custom in-chat commands may be used to expand JayWormNET's functionality. There are two kinds of them: `embedded` and `scripted`.
First written in Java and comes as plugins (Java classes), which can be embedded in distributive jar or injectec via modifying
of Java's Classpath variable. The second kind of commands are written in JavaScript and can be updated and re-evaluated on-the-fly.

Standard set of embedded commands
=================================

JayWormNET comes with a set of standard in-chat commands.

oper
----

.. highlight:: none

Usage::

	!oper <IRC operator password> [nickname]

Grants you with operator's privileges, or that guy, which nickname you optionally specified. Repeating invocation of ``oper``
revokes the privileges.

kick
----

Usage::

	!kick <nickname>

Removes client from server. `Requires operator's privileges`.

reload
------

Usage::

	!reload

Used to (mostly) reload server's configuration "on-the-fly". `Requires operator's privileges`.

say
---

Usage::

	!say <some text>

Prints cool green message to the channel. Usage of this command can be restricted through changing of
``specialMessagesPermissionLevel`` variable in the configuration.
