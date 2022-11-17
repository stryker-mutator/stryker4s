package stryker4jvm.mutator.kotlin.utility

import mutationtesting.Location
import mutationtesting.Position
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPoint
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.com.intellij.pom.PomModel
import org.jetbrains.kotlin.com.intellij.pom.PomModelAspect
import org.jetbrains.kotlin.com.intellij.pom.PomTransaction
import org.jetbrains.kotlin.com.intellij.pom.impl.PomTransactionBase
import org.jetbrains.kotlin.com.intellij.pom.tree.TreeAspect
import org.jetbrains.kotlin.com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.TreeCopyHandler
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import sun.reflect.ReflectionFactory
import java.io.ByteArrayOutputStream
import java.io.PrintStream

object PsiUtility {
    private val project: MockProject

    init {
        project = createPsiProject()
    }

    private fun createPsiProject(): MockProject {
        val compilerConfiguration = CompilerConfiguration()
        compilerConfiguration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

        System.setErr(PrintStream(ByteArrayOutputStream()))
        val project = KotlinCoreEnvironment.createForProduction(
            {},
            compilerConfiguration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).project as MockProject
        System.setErr(System.out)

        project.enableASTMutations()

        return project
    }

    private fun MockProject.enableASTMutations() {
        val extensionPoint = "org.jetbrains.kotlin.com.intellij.treeCopyHandler"
        val extensionClassName = TreeCopyHandler::class.java.name
        for (area in arrayOf(extensionArea, Extensions.getRootArea())) {
            if (!area.hasExtensionPoint(extensionPoint)) {
                area.registerExtensionPoint(extensionPoint, extensionClassName, ExtensionPoint.Kind.INTERFACE)
            }
        }

        registerService(PomModel::class.java, FormatPomModel())
    }

    private class FormatPomModel : UserDataHolderBase(), PomModel {

        override fun runTransaction(
            transaction: PomTransaction
        ) {
            (transaction as PomTransactionBase).run()
        }

        @Suppress("UNCHECKED_CAST", "SpreadOperator")
        override fun <T : PomModelAspect> getModelAspect(
            aspect: Class<T>
        ): T? {
            if (aspect == TreeAspect::class.java) {
                val constructor = ReflectionFactory
                    .getReflectionFactory()
                    .newConstructorForSerialization(
                        aspect,
                        Any::class.java.getDeclaredConstructor(*arrayOfNulls<Class<*>>(0))
                    )

                return constructor.newInstance() as T
            }

            return null
        }
    }

    fun createPsiFile(text: String): KtFile = KtPsiFactory(project).createFile(text)

    fun createPsiElement(code: String): KtElement = KtPsiFactory(project).createExpression(code)

    fun getLocation(element: KtElement): Location {
        val containingFile = element.containingFile

        val project = containingFile.project
        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        val fileViewProvider = containingFile.viewProvider

        val document = psiDocumentManager.getDocument(containingFile) ?: fileViewProvider.document
        ?: return Location(Position(0,0), Position(0,0))

        val start = Position(document.getLineNumber(element.startOffset), 0)
        val end = Position(document.getLineNumber(element.endOffset), 0)
        return Location(start, end)
    }

    fun <T : KtElement> findElementsInFile(
        file: PsiElement, type: Class<T>
    ): MutableCollection<T> = PsiTreeUtil.collectElementsOfType(file, type)

    fun replacePsiElement(original: KtElement, new: KtElement) = original.replace(new)
}
