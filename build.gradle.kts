import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.compose") version "1.5.10"
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("io.ktor:ktor-client-core:2.3.6")
    implementation("io.ktor:ktor-client-cio:2.3.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.json:json:20230227")
}

compose.desktop {
    application {
        mainClass = "com.dsagent.MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "DSAgent"
            packageVersion = "1.4.0"
            
            windows {
                menuGroup = "DSAgent"
                upgradeUuid = "d5a1b2c3-1234-5678-9abc-def012345678"
            }
        }
    }
}
