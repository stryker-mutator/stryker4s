package bar

import stryker4s.plugin.CustomMutatorPlugin
import stryker4s.plugin.api.CustomMutator

class BarMutatorPlugin extends CustomMutatorPlugin {
  override def mutators: List[CustomMutator] = List(new ArithmeticOperatorMutator)
}
