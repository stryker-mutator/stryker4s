package mill.api.daemon

import mill.api.daemon.Logger

/** Forwards `logger.prompt.colored` which is private in mill
  */
def loggerColorEnabled(logger: Logger): Boolean = logger.prompt.colored
