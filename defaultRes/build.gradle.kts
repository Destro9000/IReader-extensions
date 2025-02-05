plugins {
  id("com.android.library")
  kotlin("android")
}

android {
  compileSdk = Config.compileSdk

  defaultConfig {
    minSdk = Config.minSdk
  }

  sourceSets {
    named("main") {
      manifest.srcFile("AndroidManifest.xml")
      res.setSrcDirs(listOf("res"))
    }
  }
}

dependencies {
  compileOnly(kotlinLibs.stdlib)
}
