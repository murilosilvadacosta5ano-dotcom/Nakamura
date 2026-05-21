signingConfigs {

    create("release") {

        val keystorePath =
            System.getenv("KEYSTORE_PATH")
                ?: "${rootDir}/my-upload-key.jks"

        storeFile = file(keystorePath)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
    }

    create("debugConfig") {

        storeFile =
            file(
                System.getProperty("user.home") +
                        "/.android/debug.keystore"
            )

        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
    }
}
