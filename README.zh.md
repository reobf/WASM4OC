[English](README.md) | [中文](README.zh.md)
# 1.7.10 OpenComputers  WASM 支持模组

[![](https://jitpack.io/v/GTNewHorizons/ExampleMod1.7.10.svg)](https://jitpack.io/#GTNewHorizons/ExampleMod1.7.10)
[![](https://github.com/GTNewHorizons/ExampleMod1.7.10/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/GTNewHorizons/ExampleMod1.7.10/actions/workflows/build-and-test.yml)

本mod加入了一种全新的CPU,插入OC的CPU位后,启动时就会扫描所有磁盘或软盘

并从第一个根目录含有 /init.wasm 的设备启动并允许WASM代码

这个mod使用了重度修改的 [chicory](https://github.com/dylibso/chicory) wasm 引擎以及来自[emscripten](https://github.com/emscripten-core/emsdk) 编译器SDK的可执行文件 .

请使用GTNH的OC分支,否则不保证正常运行

## 为什么是 WASM?

OC的Lua设计是每tick最多执行一次同步操作,其余时间跑在单独线程中 <br>
WASM跑在主线程,每个tick可以执行多次同步操作

## 主线程?这不会让TPS雪上加霜?

你说得对,但是<br>
但是本mod有每秒opcodes(可以理解为WASM机器指令数)限制,达到限制后程序会暂停,直到下一个tick重新启动<br>
本mod目的不是替代Lua CPU,如果每个tick多操作不是刚需,Lua是更好的选择

## 怎么用

### 游戏内码代码 在服务器编译
如果你在windows系统玩单人,或者搭建了windows服务器,编译器会自动解压<br>
否则你需要在linux上自己装编译器(或者你在windows上使用了-nowin后缀的版本),安装方法参考[wiki](https://github.com/reobf/WASM4OC/wiki).<br>
在Lua OC电脑上插入编译器卡(Compiler Card)和编译软件软盘(Emscripten Compiler).<br>
输入`install`把编译软件软盘安装到电脑上<br>
你就能用`emcc` 和 `em++`命令了, 就能在OC电脑上码代码并编译c/c++文件为wasm文件了<br>

### 游戏外编译wasm 并上传到服务器
编译wasm 方法参考 [wiki](https://github.com/reobf/WASM4OC/wiki).

将编译输出重命名为'init.wasm'<br>
你可以直接打开游戏存档上传... 或者使用本mod的SFTP卡!<br>
在OC电脑上插入一张SFTP卡<br>
在Lua中运行:
```
diskdrive=component.filesystrem -- filesystrem可能是你的软盘而不是磁盘,仅作演示,建议用地址明确指定!
component.sftp.start(diskdrive.address,'username','passwd')
```
显示 'Started.'即成功, 用 SFTP 客户端 (MobaXterm/FileZilla/...)连接服务器(单人就是localhost)即可上传文件.
<br>登录账号密码用你lua里面填的参数即可,用户名必须与其他在运行的SFTP卡不同
<br>默认使用端口2222,配置可改,改为0自动分配端口,改为-1禁用SFTP功能
<br>注意SFTP不是FTP, `ftp://localhost:2222` 是不能用的<br>

