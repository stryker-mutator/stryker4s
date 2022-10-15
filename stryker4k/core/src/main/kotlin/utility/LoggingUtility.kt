package utility

class LoggingUtility {
    val logged: MutableList<String> = mutableListOf()

    fun info(sameLine: Boolean = false, toLog: () -> String) {
        val string = toLog()
        if (sameLine) print(string) else println(string)
        logged.add(string)
    }
}
