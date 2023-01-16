package stryker4jvm.exception

import fs2.io.file.Path
import stryker4jvm.core.exception.Stryker4jvmException

final case class InvalidFileTypeException(path: Path)
    extends Stryker4jvmException(
      s"File $path with extension ${path.extName} does not have a supported Language Mutator."
    )
