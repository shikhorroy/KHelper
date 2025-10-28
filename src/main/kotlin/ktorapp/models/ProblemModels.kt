package ktorapp.models

import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.Serializable

@Serializable
data class Test(
    val input: String,
    val output: String
)

@Serializable
data class Problem(
    val name: String?,
    val group: String?,
    val url: String?,
    val interactive: Boolean?,
    val memoryLimit: Int?,
    val timeLimit: Int?,
    val tests: List<Test> = emptyList()
)

data class PendingRequest(
    val id: String,
    val problem: Problem,
    val deferred: CompletableDeferred<Boolean>
)

