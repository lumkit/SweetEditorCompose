import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

group = rootProject.group
version = rootProject.version

extensions.configure(PublishingExtension::class.java) {
    publications.withType<MavenPublication>().configureEach {
        pom {
            url.set("https://github.com/lumkit/SweetEditorCompose")
            licenses {
                license {
                    name.set("GNU Affero General Public License v3.0")
                    url.set("https://www.gnu.org/licenses/agpl-3.0.html")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("lumkit")
                    name.set("lumkit")
                    url.set("https://github.com/lumkit")
                }
            }
            scm {
                url.set("https://github.com/lumkit/SweetEditorCompose")
                connection.set("scm:git:https://github.com/lumkit/SweetEditorCompose.git")
                developerConnection.set("scm:git:ssh://git@github.com/lumkit/SweetEditorCompose.git")
            }
        }
    }
    repositories {
        maven {
            name = "BuildDir"
            url = uri(rootProject.layout.buildDirectory.dir("maven"))
        }
    }
}

pluginManager.withPlugin("signing") {
    val signingExt = extensions.getByType(SigningExtension::class.java)
    signingExt.isRequired = false
    val signingKey = System.getenv("SIGNING_KEY")
        ?: findProperty("signingKey") as String?
    val signingPassword = System.getenv("SIGNING_PASSWORD")
        ?: findProperty("signingPassword") as String?
        ?: ""
    if (!signingKey.isNullOrBlank()) {
        afterEvaluate {
            signingExt.useInMemoryPgpKeys(signingKey.replace("\\n", "\n"), signingPassword)
            signingExt.sign(extensions.getByType(PublishingExtension::class.java).publications)
        }
    }
}
