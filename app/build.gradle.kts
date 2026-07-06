plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "hazem.nurmontage.videoquran"
    compileSdk = 35

    defaultConfig {
        applicationId = "hazem.nurmontage.videoquran"
        minSdk = 24
        targetSdk = 35
        versionCode = 21000200
        versionName = "6.7.1-QuranMaker"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("/home/z/my-project/download/quranmaker-release.jks")
            storePassword = "quran123"
            keyAlias = "quranmaker"
            keyPassword = "quran123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0"
            )
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // ═══════════════════════════════════════════════════════════
    // إصدارات مستخرجة من الهندسة العكسية للـ APK الأصلي
    // جميع الأرقام متطابقة مع الملفات المكتشفة
    // ═══════════════════════════════════════════════════════════

    // ─── AndroidX Core ───
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.annotation:annotation:1.9.1")

    // ─── AndroidX Lifecycle ───
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")

    // ─── Material Design 3 ───
    implementation("com.google.android.material:material:1.12.0")

    // ─── Splash Screen ───
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ─── Emoji2 ───
    implementation("androidx.emoji2:emoji2:1.4.0")

    // ─── Profile Installer ───
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    // ─── Window ───
    implementation("androidx.window:window:1.3.0")

    // ═══════════════════════════════════════════════════════════
    // محركات الميديا - Media Engines
    // ═══════════════════════════════════════════════════════════

    // ─── FFmpegKit ───
    implementation("com.arthenica:ffmpeg-kit-full:6.0-2")

    // ─── AndroidX Media3 (ExoPlayer) ───
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-common:1.5.1")

    // ═══════════════════════════════════════════════════════════
    // تحميل ومعالجة الصور - Image Loading
    // ═══════════════════════════════════════════════════════════

    // ─── Glide ───
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // ─── Glide Transformations ───
    implementation("jp.wasabeef:glide-transformations:4.3.0")

    // ═══════════════════════════════════════════════════════════
    // خدمات Google - Google Services
    // ═══════════════════════════════════════════════════════════

    implementation("com.google.android.gms:play-services-base:18.5.0")
    implementation("com.google.android.gms:play-services-tasks:18.2.0")

    // ═══════════════════════════════════════════════════════════
    // مؤثرات بصرية - Visual Effects
    // ═══════════════════════════════════════════════════════════

    // ─── Konfetti ───
    implementation("nl.dionsegijn:konfetti-xml:2.0.4")
    implementation("nl.dionsegijn:konfetti-core:2.0.4")

    // ═══════════════════════════════════════════════════════════
    // أدوات مساعدة - Utilities
    // ═══════════════════════════════════════════════════════════

    implementation("commons-io:commons-io:2.16.1")
    implementation("com.google.code.gson:gson:2.10.1")

    // ─── Kotlin Standard Library ───
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.25")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ═══════════════════════════════════════════════════════════
    // Testing
    // ═══════════════════════════════════════════════════════════
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
