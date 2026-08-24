// 若本机没有安装 JDK 25，此插件可让 Gradle 自动下载 Java 25 工具链
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "RPGDrop"
