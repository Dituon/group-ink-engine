plugins {
    val kotlinVersion = "1.6.21"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.11.0"
}

group = "moe.d2n"
version = "0.1.0"

sourceSets {
    create("mirai") {
        java {
            srcDir("src/moe/d2n/ink/mirai")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
mirai { jvmTarget = JavaVersion.VERSION_11 }

repositories {
    maven("maven.nova-committee.cn")
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.11")
//    implementation("cn.evole.onebot:OneBot-Client:0.4.0")
}