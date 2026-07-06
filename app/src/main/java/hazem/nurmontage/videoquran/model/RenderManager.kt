package hazem.nurmontage.videoquran.model

class RenderManager {

    // BUG-X04/M23 fix: not thread-safe. Reads (updateLocalProgress,
    // getCurrentStepDuration) happen on the UI thread; writes (addTask, nextTask)
    // happen on the codec callback thread. Use synchronized list + @Volatile index.
    private val tasks: MutableList<RenderTask> = java.util.Collections.synchronizedList(mutableListOf())
    @Volatile
    private var currentTaskIndex: Int = 0
    @Volatile
    private var globalProgress: Float = 0f

    fun addTask(name: String, expectedDuration: Int) {
        val task = RenderTask()
        task.name = name
        task.expectedDuration = expectedDuration
        // BUG-M11 fix: was `tasks.add(0, task)` (LIFO/prepend) — reversed task
        // order, and `updateLocalProgress` (which sums `tasks[0..currentTaskIndex-1]`
        // as "completed") would treat tasks added *after* the current one as already
        // completed. Append to preserve insertion order.
        tasks.add(task)
    }

    fun computeWeights() {
        val snapshot = synchronized(tasks) { tasks.toList() }
        val totalDuration = snapshot.sumOf { it.expectedDuration }
        // BUG-M12 fix: when all tasks have expectedDuration=0 (common for
        // unmeasured placeholder steps), early-return left all weights at 0f →
        // globalProgress stuck at 0% forever. Fall back to equal weights.
        if (totalDuration == 0) {
            val equal = if (snapshot.isNotEmpty()) 1f / snapshot.size else 0f
            snapshot.forEach { it.weight = equal }
            return
        }
        snapshot.forEach { task ->
            task.weight = task.expectedDuration.toFloat() / totalDuration
        }
    }

    fun getCurrentStepDuration(): Int {
        val snapshot = synchronized(tasks) { tasks.toList() }
        if (currentTaskIndex < 0 || currentTaskIndex >= snapshot.size) return 0
        return snapshot[currentTaskIndex].expectedDuration
    }

    fun nextTask() {
        val size = synchronized(tasks) { tasks.size }
        if (currentTaskIndex < size - 1) {
            currentTaskIndex++
        }
    }

    fun updateLocalProgress(localProgress: Float): Float {
        val snapshot = synchronized(tasks) { tasks.toList() }
        if (snapshot.isEmpty() || currentTaskIndex < 0 || currentTaskIndex >= snapshot.size) {
            return globalProgress
        }
        var completedWeight = 0f
        for (i in 0 until currentTaskIndex) {
            completedWeight += snapshot[i].weight
        }
        globalProgress = completedWeight + (localProgress * snapshot[currentTaskIndex].weight)
        if (globalProgress > 1.0f) {
            globalProgress = 1.0f
        }
        return globalProgress
    }
}
