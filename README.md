# WASM Addon for OpenComputers mod on 1.7.10 

[![](https://jitpack.io/v/GTNewHorizons/ExampleMod1.7.10.svg)](https://jitpack.io/#GTNewHorizons/ExampleMod1.7.10)
[![](https://github.com/GTNewHorizons/ExampleMod1.7.10/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/GTNewHorizons/ExampleMod1.7.10/actions/workflows/build-and-test.yml)

This mod adds a new type of CPU for OpenComputers mod.

This CPU will boot from the first DiskDrive or FloppyDisk that has /init.wasm file on the root dir.

This mod uses a heavily modified version of [chicory](https://github.com/dylibso/chicory) wasm engine and binary excutables of [emscripten](https://github.com/emscripten-core/emsdk) c/c++ to wasm compiler sdk.

This mod is design to work with GTNH fork of OpenComputers, might not work with other forks.

## Why WASM?

Lua can operate only one direct call per tick. <br>
WASM runs directly on main thread, so you can do complicated jobs in one single tick.

## Main thread? Sounds unfriendly with TPS!

That's true. <br>
So there's a opcodes-per-tick limit, the program will yield and continue to execute next tick if you reach the limit. <br>
This mod is not to replace Lua-based CPU, use Lua if multiple direct calls per tick is not a rigid demand.

## How to run .wasm in game

### Code in game then compile it on server
If you're playing SP on windows, or playing on a windows server, the compiler is unpacked automically.<br>
If not (or you used the -nowin version), you should setup emsdk yourself. see [wiki](https://github.com/reobf/WASM4OC/wiki).<br>
Then install a Compiler Card and Compiler FloppyDisk in a Lua Computer Case.<br>
Run `install` to install the Compiler FloppyDisk.<br>
Then use `emcc` or `em++` command to compile your c/c++ source file, just like you do outside the game.<br>

### Code outside the game then upload .wasm to server
To create a .wasm file outside the game, you should write in .c/.cpp then compile it with emscripten.<br>
see [wiki](https://github.com/reobf/WASM4OC/wiki).

Rename this file to 'init.wasm', then upload it to the Opencomputers DiskDive root directory.
<br>
You can use install a SFTP Card on Computer Case with Lua CPU.
Then run in Lua:
```
diskdrive=component.filesystrem -- the primary filesystrem might be your floppy disk
component.sftp.start(diskdrive.address,'username','passwd')
```
If it shows 'Started.', you can use a SFTP client MobaXterm/FileZilla/...) to upload/modify files.
<br>Use the username and password you set in Lua to login, you can attach multiple filesystem, but the 'username' has to be unique.
<br>Will use port `2222` by default, you can change it in config, or set it to -1 to disable SFTP.
<br>Note: SFTP is not FTP, `ftp://localhost:2222` won't work!<br>

