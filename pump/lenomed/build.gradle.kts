plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.metro)
    id("android-module-dependencies")
}

android {
    namespace = "app.aaps.pump.lenomed"
}

ksp {
    arg("room.incremental", "true")
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:interfaces"))
    implementation(project(":core:keys"))
    implementation(project(":core:ui"))
    implementation(project(":core:objects"))
    implementation(project(":core:utils"))

    // JSR-330 (javax.inject) - used for @Inject / Provider in the ported driver
    implementation("javax.inject:javax.inject:1")

    // RxJava (already used by the ported driver)
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("com.google.code.gson:gson:2.10.1")

    // Third-party libraries the ported Lenomed driver depends on.
    // Versions come from the catalog, not from literals: socket.io-client is already in the app
    // graph, and asking for an older one here put two copies on the runtime classpath (the
    // duplicate-class check rejects that at `:app:check*DuplicateClasses`).
    //
    // joda-time is deliberately NOT declared here. The app already gets it as
    // `net.danlew:android.joda`, which `core:interfaces` and `core:utils` expose with `api` and which
    // itself pulls the `no-tzdb` classifier of `joda-time:joda-time`. Declaring the plain artifact
    // alongside it puts both jars on the runtime classpath and fails the duplicate-class check.
    implementation(libs.io.socket.client)
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okio:okio:3.6.0")

    // android-iconify supplies `IconTextView`, which the driver's layouts really do use (the
    // generated `LenomedFragmentBinding` imports `com.joanzapata.iconify.widget.IconTextView`), so it
    // cannot simply be dropped. It also drags in `com.android.support:support-v4:22.2.1`, whose
    // `android.support.v4.*` classes collide with the ones `androidx.core` still ships - that fails
    // `:app:check*DuplicateClasses`. Excluding the old support artifact leaves androidx.core, which
    // already contains those classes, as the single source.
    implementation("com.joanzapata.iconify:android-iconify:2.2.2") {
        exclude(group = "com.android.support")
    }
    // coil and appauth came out of the decompiled driver's dependency list, but no source file in
    // this module references them.

    // Compatibility shims for a few symbols the ported Lenomed (Embecta) driver
    // references live inside this module under app.aaps.pump.lenomed.compat, so we
    // avoid pulling in heavy sibling modules (and risking duplicate-class conflicts
    // when the final app assembles). See pump/lenomed/.../compat/.
    implementation(libs.androidx.preference)

    // Room (local history database for the ported driver). The DAO hands back RxJava Single,
    // which needs the rxjava3 bridge on the compile classpath or Room cannot generate for it.
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.rxjava3)
    ksp(libs.androidx.room.compiler)
}
