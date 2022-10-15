package process.projectMutator

import models.Mutation
import models.SourceFile
import process.projectMutator.mutators.BooleanLiteralMutator
import process.projectMutator.mutators.EqualityOperatorMutator
import process.projectMutator.mutators.StringLiteralMutator
import process.projectMutator.mutators.ConditionalExpressionMutator
import process.projectMutator.mutators.LogicalOperatorMutator

object MutationGenerator {
    private val mutators = arrayOf(
        BooleanLiteralMutator,
        StringLiteralMutator,
        EqualityOperatorMutator,
        ConditionalExpressionMutator,
        LogicalOperatorMutator
    )

    fun generateMutations(sourceFiles: List<SourceFile>): MutableList<Mutation> {
        val mutations = mutableListOf<Mutation>()
        sourceFiles.forEach { sourceFile ->
            mutators.forEach { mutator -> sourceFile.mutables.addAll(mutator.mutateFile(sourceFile)) }
            sourceFile.mutables.forEach { mutations.addAll(it.mutations) }
        }

        return mutations
    }
}
