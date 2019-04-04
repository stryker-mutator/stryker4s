package stryker4s.run
import org.apache.maven.project.MavenProject
import org.apache.maven.shared.invoker.{DefaultInvoker, Invoker}
import stryker4s.config.Config
import stryker4s.mutants.applymutants.ActiveMutationContext.{envVar, ActiveMutationContext}
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter

class Stryker4sMavenRunner(project: MavenProject) extends Stryker4sRunner {
  override def resolveRunner(collector: SourceCollector, reporter: Reporter)(implicit config: Config): MutantRunner =
    new MavenMutantRunner(project, resolveInvoker(), collector, reporter)

  override val mutationActivation: ActiveMutationContext = envVar

  private def resolveInvoker(): Invoker = new DefaultInvoker
}
