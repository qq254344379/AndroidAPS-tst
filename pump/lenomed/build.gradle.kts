plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.metro)
    id("android-module-dependencies")
}

android {
    namespace = "app.aaps.pump.lenomed"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:interfaces"))
    implementation(project(":core:keys"))
    implementation(project(":core:ui"))

    // RxJava (already used by the ported driver)
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("com.google.code.gson:gson:2.10.1")

    // Third-party libraries the ported Lenomed driver depends on
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("joda-time:joda-time:2.12.5")
    implementation("io.socket:socket.io-client:2.1.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okio:okio:3.6.0")
    implementation("net.glxn:qrgen:1.4")
    implementation("com.joanzapata.iconify:android-iconify:2.2.2")
    implementation("io.coil-kt:coil:2.6.0")
    implementation("net.openid:appauth:0.11.1")
}
