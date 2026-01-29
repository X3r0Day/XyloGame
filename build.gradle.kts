plugins {
    id("java")
    id("application")
}

group = "me.xeroday"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

application {
    mainClass.set("me.xeroday.Main")
}

val lwjglVersion = "3.3.3"
val lwjglNatives = when {
    System.getProperty("os.name")!!.contains("Windows", ignoreCase = true) -> "natives-windows"
    System.getProperty("os.name")!!.contains("Mac", ignoreCase = true) -> {
        if (System.getProperty("os.arch")!!.contains("aarch64")) "natives-macos-arm64" else "natives-macos"
    }
    else -> "natives-linux"
}

dependencies {
    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-stb:$lwjglVersion")

    implementation("org.joml:joml:1.10.8")
    implementation("com.github.JnCrMx:discord-game-sdk4j:v1.0.0")

    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:$lwjglNatives")
}