本目录说明
==========

RPGDrop 的所有可选插件集成都采用【纯反射】实现（编译期零依赖）：

- RPGItems：运行时反射调用，无需任何编译依赖
- NekoNYume：运行时反射调用，无需任何编译依赖

因此构建 RPGDrop 不需要往本目录放任何 jar，直接执行 gradlew build 即可。

本地测试服（runServer）要测试某个可选插件时，把它的 jar 放进
项目的 test-plugins/ 目录（会被自动装进测试服）：

- RPGItems.jar（另需 NyaaCore.jar、Vault.jar）
- NekoNYume.jar

此目录保留仅用于兼容历史说明，可留空。
