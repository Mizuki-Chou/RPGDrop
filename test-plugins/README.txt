本地测试服（runServer）依赖插件目录
=====================================

把测试需要的插件 jar 放到这里（从你服务器的 plugins/ 目录复制即可），
启动 runServer 时会自动安装到测试服：

  RPGItems.jar   -- RPGItems Reloaded 3.38（测试 RPGItem 掉落时必需）
  NyaaCore.jar   -- RPGItems 3.38 的依赖
  Vault.jar      -- RPGItems 3.38 的依赖

不想测 RPGItems 集成（纯原版掉落模式）时，此目录留空即可。

注意：本目录下的 *.jar 已被 .gitignore 排除，不会上传到 GitHub。
