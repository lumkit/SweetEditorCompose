plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.nmcp) apply false
    alias(libs.plugins.nmcpAggregation)
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("VERSION_NAME").get()

nmcpAggregation {
    centralPortal {
        username = providers.gradleProperty("mavenCentralUsername")
            .orElse(providers.environmentVariable("MAVEN_CENTRAL_USERNAME"))
        password = providers.gradleProperty("mavenCentralPassword")
            .orElse(providers.environmentVariable("MAVEN_CENTRAL_PASSWORD"))
        publishingType = providers.gradleProperty("mavenCentralPublishingType")
            .orElse(providers.environmentVariable("MAVEN_CENTRAL_PUBLISHING_TYPE"))
            .orElse("USER_MANAGED")
        publicationName = providers.provider { "sweeteditor-compose:$version" }
    }
}

dependencies {
    nmcpAggregation(project(":editor"))
    nmcpAggregation(project(":editor-android-jni"))
    nmcpAggregation(project(":highlight"))
    nmcpAggregation(project(":highlight-android-jni"))
}

tasks.matching {
    it.name.startsWith("nmcpPublishAggregation") || it.name == "publishAggregationToCentralPortal"
}.configureEach {
    dependsOn(":editor:verifyReleaseNatives")
    dependsOn(":highlight:verifyReleaseNatives")
}
