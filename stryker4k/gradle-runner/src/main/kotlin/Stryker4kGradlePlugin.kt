import org.gradle.api.Plugin
import org.gradle.api.Project

class Stryker4kGradlePlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("stryker4k") {
            it.dependsOn(project.tasks.getByName("build"))
            it.doLast {
                Stryker4k.run(arrayOf(project.rootDir.absolutePath))
            }
        }
    }
}