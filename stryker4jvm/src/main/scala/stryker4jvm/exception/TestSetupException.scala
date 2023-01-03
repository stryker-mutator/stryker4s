package stryker4jvm.exception

import stryker4jvm.core.exception.Stryker4jvmException

final case class TestSetupException(name: String)
    extends Stryker4jvmException(
      s"Could not setup mutation testing environment. Unable to resolve project $name. This could be due to compile errors or misconfiguration of Stryker4jvm. See debug logs for more information."
    )
