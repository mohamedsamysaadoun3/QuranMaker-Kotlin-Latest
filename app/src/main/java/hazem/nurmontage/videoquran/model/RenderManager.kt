package hazem.nurmontage.videoquran.model

class RenderManager {

    private val tasks: MutableList<RenderTask> = mutableListOf()
    private var currentTaskIndex: Int = 0
    private var globalProgress: Float = 0f

    fun addTask(name: String, expectedDuration: Int) {
        val task = RenderTask()
        task.name = name
        task.expectedDuration = expectedDuration
        tasks.add(0, task)
    }

    fun computeWeights() {
        val totalDuration = tasks.sumOf { it.expectedDuration }
        if (totalDuration == 0) return
        tasks.forEach { task ->
            task.weight = task.expectedDuration.toFloat() / totalDuration
        }
    }

    fun getCurrentStepDuration(): Int {
        return tasks[currentTaskIndex].expectedDuration
    }

    fun nextTask() {
        if (currentTaskIndex < tasks.size - 1) {
            currentTaskIndex++
        }
    }

    fun updateLocalProgress(localProgress: Float): Float {
        var completedWeight = 0f
        for (i in 0 until currentTaskIndex) {
            completedWeight += tasks[i].weight
        }
        globalProgress = completedWeight + (localProgress * tasks[currentTaskIndex].weight)
        if (globalProgress > 1.0f) {
            globalProgress = 1.0f
        }
        return globalProgress
    }
}
