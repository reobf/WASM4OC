# WASM for OpenComputers mod on 1.7.10

[![](https://jitpack.io/v/GTNewHorizons/ExampleMod1.7.10.svg)](https://jitpack.io/#GTNewHorizons/ExampleMod1.7.10)
[![](https://github.com/GTNewHorizons/ExampleMod1.7.10/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/GTNewHorizons/ExampleMod1.7.10/actions/workflows/build-and-test.yml)

This mod adds a new type of CPU for OpenComputers mod.

This CPU will boot from the first DiskDrive or FloppyDisk that has /init.wasm file on the root dir.

This mod uses a heavily modified version of [chicory](https://github.com/dylibso/chicory) wasm engine and binary excutables of [binaryen](https://github.com/WebAssembly/binaryen) WASM tools.


## Why WASM?

Lua can operate only one direct call per tick. <br>
WASM runs directly on main thread, so you can do complicated jobs in one single tick.

## Main thread? Sounds unfriendly with TPS!

That's true. <br>
So there's a opcodes-per-tick limit, the program will yield and continue next tick if you reach the limit. <br>
This mod is not to replace Lua-based CPU, use Lua if multiple direct calls per tick is not a rigid demand.

## How to code in WASM

### WAST to WASM

Grab a release from https://github.com/WebAssembly/binaryen

```
wasm-as init.wat -o init.wasm
```
Then upload `init.wasm` to OpenComputers DiskDrive.<br>
Or you can upload `init.wat` then use `Compiler Card` in game to do the compiling job.

### C/C++ to WASM

https://github.com/emscripten-core/emsdk


//TODO