package example

import stryker4s.pluginapi.{CustomMutator, CustomMutatorPlugin}

class ExampleMutatorPlugin extends CustomMutatorPlugin {
  override def mutators: List[CustomMutator] = List(new ArithmeticOperatorMutator)
}
