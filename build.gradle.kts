plugins {
    java
    // 一键启动本地 Paper 测试服（IDEA 里双击 runServer / 命令行 gradlew runServer）
    // 要求 Gradle 9.7+（项目 wrapper 为 9.7.1，已满足）
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "mizukichou"
version = "Release 1"

// ============================================================
//  服务器版本（需要与目标服务器匹配）
//  默认：Paper (Minecraft 26.2) + Java 25 运行时
//
//  如果你的服务器是旧版版本号体系（例如 1.21.x），请：
//    1. 把 paperVersion 改为 "1.21.11-R0.1-SNAPSHOT"
//    2. 把下方 options.release 改为 21
//    3. 把 src/main/resources/plugin.yml 的 api-version 改为 '1.21'
// ============================================================
val paperVersion = "26.2.build.116-stable"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // RPGItems Reloaded 官方 Maven 仓库
    maven("https://ci.nyaacat.com/maven/")
}

java {
    toolchain {
        // 用 Java 25 开发（Paper 26.2 服务器本身要求 Java 25 运行时）
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperVersion")

    // RPGItems Reloaded 3.38：官方 Maven 仓库直接引用（推荐，Gradle 会自动下载，无需手动放 jar）
    compileOnly("cat.nyaa:rpgitems:3.38-SNAPSHOT")

    // 离线备选方案（NyaaCat 仓库无法访问时使用）：
    //   1. 注释掉上面那行 maven 依赖
    //   2. 把服务器 plugins/ 里的 RPGItems.jar 复制到 libs/ 目录
    //   3. 取消下面这行注释：
    // compileOnly(files("libs/RPGItems.jar"))
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to version)
    }
}

tasks.jar {
    archiveFileName = "RPGDrop-Release-1.jar"
}

// ============================================================
//  本地测试服（runServer）
//
//  用法：
//    - IDEA：右侧 Gradle 面板 → RPGDrop → Tasks → run server → runServer 双击
//    - 命令行：gradlew runServer
//
//  首次运行会自动下载对应版本的 Paper（需网络，之后有缓存）。
//  测试服所有文件生成在项目根目录的 run/ 文件夹（已被 .gitignore 排除）。
// ============================================================

// 自动生成本地测试服的 eula.txt。
// 仅用于本地开发测试，运行 runServer 即代表你同意 Mojang EULA。
val agreeEula by tasks.registering {
    group = "run server"
    description = "生成本地测试服的 eula.txt"
    doLast {
        val eula = file("run/eula.txt")
        eula.parentFile.mkdirs()
        if (!eula.exists()) {
            eula.writeText("# 本地测试服自动生成\n# 运行 runServer 即代表你同意 Mojang EULA\neula=true")
        }
    }
}

tasks.runServer {
    group = "run server"

    // 测试服端口（默认 25565，这里改为 25566，避免和正式服务器冲突）
    // 该属性优先级高于 run/server.properties 里的 server-port。
    systemProperty("server-port", 25566)

    // 可选：本地测试常用开关（需要时取消注释）
    // systemProperty("online-mode", false)   // 关闭正版验证，离线账号也能进测试服

    // 测试服的 Minecraft 版本。首次运行自动下载对应 Paper。
    // 若新版版本号解析异常，可临时改为 minecraftVersion("1.21.8") 或 serverJar(本地 paper jar)
    minecraftVersion("26.2")
    runDirectory(file("run"))
    dependsOn(agreeEula)

    // 你的插件 jar 会自动检测并加载（无需配置）

    // 测试依赖插件：
    // 把服务器 plugins/ 里的 RPGItems.jar / NyaaCore.jar / Vault.jar 复制到 test-plugins/，
    // 它们会自动被装进测试服；不测 RPGItems 集成时留空目录即可。
    pluginJars.from(fileTree("test-plugins") { include("*.jar") })

    // 可选：让 run-paper 自动从 GitHub Releases 下载 Vault（RPGItems 的依赖之一）
    // downloadPlugins {
    //     github("MilkBowl", "Vault", "1.7.3", "Vault-1.7.3.jar")
    // }
}
