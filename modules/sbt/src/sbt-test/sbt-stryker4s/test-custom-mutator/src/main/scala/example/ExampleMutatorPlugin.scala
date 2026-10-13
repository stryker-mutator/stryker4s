package example

import stryker4s.plugin.CustomMutatorPlugin
import stryker4s.plugin.api.CustomMutator

class ExampleMutatorPlugin extends CustomMutatorPlugin {
  override def mutators: List[CustomMutator] = List(new ArithmeticOperatorMutator)
}
