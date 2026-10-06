package dev.gaphunter.gotimerleakcompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import dev.gaphunter.gotimerleakcompanion.detect.GoVersionGate
import dev.gaphunter.gotimerleakcompanion.detect.SelectTimeAfterScanner
import dev.gaphunter.gotimerleakcompanion.review.ReviewPrompt
import java.io.IOException

/**
 * Flags `case <-time.After(...)` inside a `select` block -- see
 * [SelectTimeAfterScanner] for the full reasoning. Runs via
 * [checkFile] (whole-file text scan), same reasoning as
 * `k8s-resource-limit-companion`'s `MissingResourceLimitInspection`:
 * plain-text line scanning, not a Go-language-plugin PSI walk -- see
 * `build.gradle.kts` for why no Go PSI dependency is taken.
 */
class SelectTimeAfterLeakInspection : LocalInspectionTool() {

    companion object {
        const val MAX_FILE_LENGTH = 500_000
        private val GO_FILE_NAME = Regex("""^[^.]+\.go$""", RegexOption.IGNORE_CASE)
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        val virtualFile = file.virtualFile ?: return null
        if (!GO_FILE_NAME.matches(virtualFile.name)) return null

        val text = file.text
        if (text.length > MAX_FILE_LENGTH) return null

        val hits = SelectTimeAfterScanner.scan(text)
        if (hits.isEmpty()) return null
        // A module that declares Go 1.23 or later gets unstopped timers collected (see GoVersionGate).
        if (declaresGo123OrLater(virtualFile)) return null

        val problems = mutableListOf<ProblemDescriptor>()
        for (hit in hits) {
            val anchor = leafElementAt(file, hit.lineStartOffset) ?: continue
            val anchorStart = anchor.textRange.startOffset
            val relativeRange = TextRange(
                (hit.lineStartOffset - anchorStart).coerceAtLeast(0),
                (hit.lineEndOffset - anchorStart).coerceAtMost(anchor.textLength),
            )
            if (relativeRange.startOffset >= relativeRange.endOffset) continue

            problems += manager.createProblemDescriptor(
                anchor,
                relativeRange,
                "time.After() inside a select case: unless the module declares Go 1.23 or later, its Timer " +
                    "isn't garbage collected until it fires, so each pass through the select keeps a new one alive. " +
                    "Use time.NewTimer(...) and Stop() it when done instead",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                isOnTheFly,
            )

            ReviewPrompt.recordHit(file.project, "${virtualFile.path}:${hit.lineNumber}")
        }

        return if (problems.isEmpty()) null else problems.toTypedArray()
    }

    /** Reads the `go` line of the nearest go.mod above [file]; no go.mod, or an unreadable one, keeps the warning. */
    private fun declaresGo123OrLater(file: VirtualFile): Boolean {
        var dir = file.parent
        while (dir != null) {
            val goMod = dir.findChild("go.mod")
            if (goMod != null && !goMod.isDirectory) {
                return try {
                    GoVersionGate.timersCollectedWithoutStop(VfsUtilCore.loadText(goMod))
                } catch (e: IOException) {
                    false
                }
            }
            dir = dir.parent
        }
        return false
    }

    private fun leafElementAt(file: PsiFile, startOffset: Int): PsiElement? {
        if (startOffset < 0 || startOffset >= file.textLength) return null
        var element = file.findElementAt(startOffset) ?: return file
        while (element.firstChild != null) {
            element = element.firstChild
        }
        return element
    }
}
