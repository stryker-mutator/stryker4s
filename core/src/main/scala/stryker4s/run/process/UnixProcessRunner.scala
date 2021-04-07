package stryker4s.run.process

import cats.effect.IO
import stryker4s.log.Logger

class UnixProcessRunner(implicit log: Logger, cs: ContextShift[IO]) extends ProcessRunner {}
