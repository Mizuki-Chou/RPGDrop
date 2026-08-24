离线备选目录（默认不需要用到）
================================

当前构建默认直接从 RPGItems 官方 Maven 仓库引用依赖：
  cat.nyaa:rpgitems:3.38-SNAPSHOT

只有当你无法访问 https://ci.nyaacat.com/maven/ 时，才需要：
  1. 把服务器 plugins/ 里的 RPGItems.jar 复制到这个目录
  2. 在 build.gradle.kts 里注释掉 maven 依赖那行，
     并取消 compileOnly(files("libs/RPGItems.jar")) 的注释

本目录下的 *.jar 已被 .gitignore 排除，不会上传 GitHub。
