package bar

import stryker4s.pluginapi.{CustomMutator, CustomMutatorPlugin}

class BarMutatorPlugin extends CustomMutatorPlugin {
  override def mutators: List[CustomMutator] = List(new ArithmeticOperatorMutator)
}
