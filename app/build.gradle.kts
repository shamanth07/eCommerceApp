plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.ecommerceapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ecommerceapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // AndroidX Libraries
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")

    // Material Components
    implementation("com.google.android.material:material:1.11.0")
    implementation ("com.github.rey5137:material:1.2.4")

    // Firebase Libraries
    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.firebase:firebase-database:20.3.1")
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("com.firebaseui:firebase-ui-database:8.0.2")

    // Image Libraries
    implementation("com.github.CanHub:Android-Image-Cropper:4.3.2")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.squareup.picasso:picasso:2.71828")

    // Local Storage Library
    implementation("io.github.pilgr:paperdb:2.7.2")
    // Google Play Services Ads
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation ("com.google.android.gms:play-services-maps:18.0.2")

    implementation ("com.google.android.libraries.places:places:3.1.0")
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.stripe:stripe-android:20.18.0")
    implementation(libs.activity)
    implementation(libs.gridlayout)
    // Testing Libraries
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
