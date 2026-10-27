import club.minnced.discord.webhook.send.WebhookEmbed
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.blossom)
    alias(libs.plugins.librarian)
    alias(libs.plugins.mcupload)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/")
    maven("https://repo.kyngs.xyz/public/")
    maven("https://mvn.exceptionflug.de/repository/exceptionflug-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://jitpack.io/")
}

dependencies {
    // API
    implementation(project(":API"))

    // Velocity
    annotationProcessor(libs.velocity.api)
    compileOnly(libs.velocity.api)
    compileOnly(libs.velocity.proxy)

    // MySQL
    librarian(libs.mariadb)
    librarian(libs.hikari)

    // SQLite
    librarian(libs.sqlite)

    // PostgreSQL
    librarian(libs.postgresql)

    // ACF
    librarian(libs.acf.velocity)
    librarian(libs.acf.paper)

    // Utils
    librarian(libs.caffeine)
    librarian(libs.configurate.hocon)
    librarian(libs.bcrypt)
    librarian(libs.totp)
    compileOnly(libs.protocolize.api)
    librarian(libs.bouncycastle)
    librarian(libs.commons.email)
    librarian(libs.adventure.minimessage)
    librarian(libs.adventure.serializer.legacy)
    librarian(libs.legacymessage)

    // Geyser
    compileOnly(libs.floodgate.api)
    // LuckPerms
    compileOnly(libs.luckperms.api)

    // RedisBungee
    compileOnly(libs.redisbungee)

    // Paper / Bukkit / Spigot
    compileOnly(libs.paper.api)
    compileOnly(libs.adventure.api)
    compileOnly(libs.packetevents.spigot)
    compileOnly(libs.netty.transport)
    compileOnly(libs.datafixerupper)
    compileOnly(libs.log4j.core)

    // Librarian
    implementation(libs.librarian.velocity)
    implementation(libs.librarian.paper)

    // NanoLimboPlugin
    compileOnly(libs.nanolimbo.api)

    // Tests
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.caffeine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<ProcessResources> {
    outputs.upToDateWhen { false }
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(mapOf("version" to version))
    }
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", version.toString())
            }
        }
    }
}

// Dependencies pulled in transitively that we never want shaded or downloaded at runtime.
val excludedLibs = listOf(
    "org.slf4j:.*:.*",
    "org.checkerframework:.*:.*",
    "com.google.errorprone:.*:.*",
    "com.google.protobuf:.*:.*",
)

tasks.withType<ShadowJar> {
    // Include the version in the file name so builds can be told apart at a glance.
    archiveFileName.set("TemsLogin-${project.version}.jar")

    dependencies {
        excludedLibs.forEach { exclude(dependency(it)) }
    }

    relocate("co.aikar.acf", "xyz.kyngs.librelogin.lib.acf")
    relocate("com.github.benmanes.caffeine", "xyz.kyngs.librelogin.lib.caffeine")
    relocate("com.typesafe.config", "xyz.kyngs.librelogin.lib.hocon")
    relocate("com.zaxxer.hikari", "xyz.kyngs.librelogin.lib.hikari")
    relocate("org.mariadb", "xyz.kyngs.librelogin.lib.mariadb")
    relocate("org.bstats", "xyz.kyngs.librelogin.lib.metrics")
    relocate("org.intellij", "xyz.kyngs.librelogin.lib.intellij")
    relocate("org.jetbrains", "xyz.kyngs.librelogin.lib.jetbrains")
    relocate("io.leangen.geantyref", "xyz.kyngs.librelogin.lib.reflect")
    relocate("org.spongepowered.configurate", "xyz.kyngs.librelogin.lib.configurate")
    relocate("xyz.kyngs.librarian", "xyz.kyngs.librelogin.lib.librarian")
    relocate("org.postgresql", "xyz.kyngs.librelogin.lib.postgresql")
}

tasks.withType<Jar> {
    from("../LICENSE.txt")
}

librarian {
    excludedLibs.forEach { excludeDependency(it) }
}

mcupload {
    file = tasks.shadowJar
    swallowErrors = true
    platforms {
        if (project.version.toString().endsWith("-SNAPSHOT") || System.getProperty("buildTarget") == "dev") {
            discord {
                webhookUrl = System.getenv("DISCORD_WEBHOOK_URL")
                configureEmbed {
                    setColor(0xFF0000)
                    setTitle(WebhookEmbed.EmbedTitle("EXPERIMENTAL! " + build().title!!.text, null))
                }
            }
        } else if (System.getProperty("buildTarget") == "release") {
            // Modrinth publishing is intentionally not configured: this fork must not publish to the upstream project.
            github {
                token = System.getenv("GITHUB_TOKEN")
                repository = "Phantom6422/TemsLogin"
            }
            discord {
                webhookUrl = System.getenv("DISCORD_WEBHOOK_URL")
                configureEmbed {
                    setColor(0x0398FC)
                }
            }
        }

    }
    datasource {
        file {
            readmeFile = "README.md"
            changelogFile = "CHANGELOG.md"
        }
    }
}
