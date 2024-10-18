package stryker4s.maven.stubs

import org.apache.maven.shared.invoker.{
  InvocationOutputHandler,
  InvocationRequest,
  InvocationResult,
  Invoker,
  InvokerLogger
}
import org.apache.maven.shared.utils.cli.CommandLineException

import java.io.{File, InputStream}
import scala.collection.mutable

trait InvokerStub extends Invoker {
  def calls: Seq[InvocationRequest]

  override def getLocalRepositoryDirectory(): File = ???
  override def getLogger(): InvokerLogger = ???
  override def getMavenExecutable(): File = ???
  override def getMavenHome(): File = ???
  override def getWorkingDirectory(): File = ???
  override def setLocalRepositoryDirectory(localRepositoryDirectory: File): Invoker = this
  override def setLogger(logger: InvokerLogger): Invoker = this
  override def setMavenExecutable(mavenExecutable: File): Invoker = this
  override def setMavenHome(mavenHome: File): Invoker = this
  override def setErrorHandler(errorHandler: InvocationOutputHandler): Invoker = this
  override def setInputStream(inputStream: InputStream): Invoker = this
  override def setOutputHandler(outputHandler: InvocationOutputHandler): Invoker = this
  override def setWorkingDirectory(workingDirectory: File): Invoker = this

}

object InvokerStub {
  def apply(): Invoker = new InvokerStub() {
    override def calls: Seq[InvocationRequest] = Seq.empty
    override def execute(request: InvocationRequest): InvocationResult = ???
  }

  def apply(invocationResult: InvocationResult) = {
    val callsSeq = mutable.Buffer.empty[InvocationRequest]
    new InvokerStub {
      override def calls: Seq[InvocationRequest] = callsSeq.toSeq
      override def execute(request: InvocationRequest): InvocationResult = {
        callsSeq += request
        invocationResult
      }
    }
  }
}

object InvocationResultStub {
  def apply(exitCode: Int): InvocationResult = new InvocationResult {
    override def getExitCode(): Int = exitCode

    override def getExecutionException(): CommandLineException = ???
  }
}
