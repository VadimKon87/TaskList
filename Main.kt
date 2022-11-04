package tasklist

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.io.File
import java.util.*
import kotlin.system.exitProcess

object TaskList {
    private val inputList = mutableListOf<MutableList<String>>() // 2D list
    private val jsonFile = File("tasklist.json")

    private val moshi = Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    private val type = Types.newParameterizedType(MutableList::class.java, MutableList::class.java)

    private val taskListAdapter = moshi.adapter<MutableList<MutableList<String>>>(type)

    fun loadTasks() {
        if (jsonFile.isFile) {
            val taskList = taskListAdapter.fromJson(jsonFile.readText())
            for (list in taskList!!) {
                inputList.add(list)
            }
        }
    }

    fun start() {

        println("Input an action (add, print, edit, delete, end):")

        when (readln().trim().lowercase(Locale.getDefault())) {
            "add" -> addTask()
            "print" -> {
                printTasks()
                start()
            }
            "edit" -> editTasks()
            "delete" -> deleteTasks()
            "end" -> {
                end()
            }
            else -> {
                println("The input action is invalid")
                start()
            }
        }
    }

    private fun inputPriority() : String {
        val priorityRegex = "^[CcHhNnLl]\$".toRegex()
        println("Input the task Priority (C, H, N, L):")
        val taskPriority = readln().trim()
        if (!taskPriority.matches(priorityRegex)) {
           return inputPriority()
        }
        return when (taskPriority.uppercase()){
            "C" -> "\u001B[101m \u001B[0m"
            "H" -> "\u001B[103m \u001B[0m"
            "N" -> "\u001B[102m \u001B[0m"
            "L" -> "\u001B[104m \u001B[0m"
            else -> throw Exception("This exception should be impossible")
        }
    }

    private fun inputDate() : String {
        val dateRegex = "^[\\d]{4}-[\\d]{1,2}-[\\d]{1,2}$".toRegex()
        println("Input the date (yyyy-mm-dd):")
        val dateOfTask = readln().trim()
        if (!dateOfTask.matches(dateRegex)) {
            println("The input date is invalid")
            return inputDate()
        }
        val (year, month, day) = dateOfTask.split("-")
        var dateOfTaskFormatted = LocalDate(1999, 12, 31)

        try {
            dateOfTaskFormatted = LocalDate(year.toInt(), month.toInt(), day.toInt())
        } catch (e: Exception) {
            println("The input date is invalid")
            inputDate()
        }
        return dateOfTaskFormatted.toString()
    }

    private fun inputTime() : String {
        val timeRegex = "^[\\d]{1,2}:[\\d]{1,2}$".toRegex()
        println("Input the time (hh:mm):")
        val timeOfTask = readln().trim()
        if (!timeOfTask.matches(timeRegex)) {
            println("The input time is invalid")
            return inputTime()
        }
        val (hours, minutes) = timeOfTask.split(":")
        var timeOfTaskFormatted = LocalTime(12, 0) // this will change to user input

        try {
            timeOfTaskFormatted = LocalTime(hours.toInt(), minutes.toInt())
        } catch (e: Exception) {
            println("The input time is invalid")
            inputTime()
        }
        return timeOfTaskFormatted.toString()
    }

    private fun dueTag(dateOfTask: String) : String {
        val taskDate = dateOfTask.toLocalDate()
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+2")).date
        val daysToTask = currentDate.daysUntil(taskDate)

        return if (daysToTask == 0) {
            "\u001B[103m \u001B[0m"
        } else if (daysToTask > 0) {
            "\u001B[102m \u001B[0m"
        } else "\u001B[101m \u001B[0m"
    }

    private fun addTask() {
        val taskPriority = inputPriority()
        val dateOfTask = inputDate()
        val timeOfTask = inputTime()
        val dueTag = dueTag(dateOfTask)

        println("Input a new task (enter a blank line to end):")

        val taskStringList = mutableListOf<String>() // each task is a list of strings

        while (true) {
            val input = readln().trim()

                // start of line 1
            if (input.isNotBlank() && taskStringList.isEmpty()) {
                taskStringList += ("| $dateOfTask ")
                taskStringList += ("| $timeOfTask ")
                taskStringList += ("| $taskPriority ")
                taskStringList += ("| $dueTag ")
                if (input.length > 44) {
                    val inputChunked = input.chunked(44).toMutableList()
                    // last string chunk is padded
                    inputChunked[inputChunked.lastIndex] = inputChunked.last().padEnd(44, ' ')
                    taskStringList += "|${inputChunked
                            .joinToString(
                                "|\n" + "|    |            |       |   |   |"
                            )
                    }" + "|"
                } else { //input less than 44 chars long
                    taskStringList += "|${input.padEnd(44, ' ')}|"
                }
                // end of line 1
            }else if (input.isNotBlank()) {
                //next lines starting on new line
                if (input.length > 44) {
                    val inputChunked = input.chunked(44).toMutableList()
                    inputChunked[inputChunked.lastIndex] = inputChunked.last().padEnd(44, ' ')
                    taskStringList += "\n|    |            |       |   |   |${
                        inputChunked.joinToString("|\n" + "|    |            |       |   |   |")
                    }" + "|"
                } else {
                    taskStringList += "\n" + "|    |            |       |   |   |${input.padEnd(44, ' ')}|"
                }
            }else if (taskStringList.isEmpty()) {
                println("The task is blank")
                start()
            }else {
                break
            }
        }
        inputList += taskStringList //string added to list

        start()
    }

    private fun printTasks() {
        if (inputList.isEmpty()) {
            println("No tasks have been input")
        } else {
            println("""
                +----+------------+-------+---+---+--------------------------------------------+
                | N  |    Date    | Time  | P | D |                   Task                     |
            """.trimIndent())
            for (i in inputList.indices) {
                if (i < 9) {
                    println("+----+------------+-------+---+---+--------------------------------------------+")
                    println("| ${i + 1}  ${inputList[i].joinToString("")}") //extra space
                } else {
                    println("+----+------------+-------+---+---+--------------------------------------------+")
                    println("${i + 1} ${inputList[i].joinToString("")}")
                }
            }
            println("+----+------------+-------+---+---+--------------------------------------------+")
        }
    }

    private fun editTasks() {
        fun editTaskRecursion() {
            println("Input the task number (1-${inputList.lastIndex + 1}):")
            val taskNumber = readln().trim()
            var fieldInput = ""
            val listOfFields = mutableListOf("priority", "date", "time", "task")
            try {
                val taskIndex = taskNumber.toInt() - 1
                if (taskIndex !in 0..inputList.lastIndex) throw Exception("Invalid task number exception")
                while (!listOfFields.contains(fieldInput)) {
                    println("Input a field to edit (priority, date, time, task):")
                    fieldInput = readln().lowercase()
                    when (fieldInput.trim().lowercase()) {
                        "priority" -> {
                            inputList[taskIndex][2] = "| " + inputPriority() + " "
                            println("The task is changed")
                        }
                        "date" -> {
                            inputList[taskIndex][0] = inputDate()
                            inputList[taskIndex][3] = "| " + dueTag(inputList[taskIndex][0]) + " "
                            inputList[taskIndex][0] = "| " + inputList[taskIndex][0] + " "
                            println("The task is changed")
                        }
                        "time" -> {
                            inputList[taskIndex][1] = "| " + inputTime() + " "
                            println("The task is changed")
                        }
                        "task" -> {
                            println("Input a new task (enter a blank line to end):")
                            for (i in 4..inputList[taskIndex].lastIndex) {
                                inputList[taskIndex].removeLast()
                            }
                            while (true) {
                                val newTask = readln()
                                if (newTask.isNotBlank() && inputList[taskIndex].lastIndex == 3) {
                                    if (newTask.length > 44) {
                                        inputList[taskIndex].add(
                                            "|${
                                                newTask.chunked(44).joinToString("|\n" + "|    |            |       |   |   |")
                                            }|"
                                        )
                                    } else {
                                        inputList[taskIndex].add("|${newTask.padEnd(44, ' ')}|")
                                    }
                                } else if (newTask.isNotBlank() && inputList[taskIndex].lastIndex > 3) {
                                    if (newTask.length > 44) {
                                        inputList[taskIndex].add("\n|    |            |       |   |   |" +
                                                newTask.chunked(44).joinToString("|\n" + "|    |            |       |   |   |")
                                        )
                                    } else {
                                        inputList[taskIndex].add("\n|    |            |       |   |   |${newTask.padEnd(44, ' ')}|")
                                    }
                                } else if (inputList[taskIndex].lastIndex == 3) {
                                    println("The task is blank")
                                } else break
                            }
                            println("The task is changed")
                        }
                        else -> {
                            println("Invalid field")
                        }
                    }
                }
            } catch (e: Exception) {
                println("Invalid task number")
                editTaskRecursion()
            }
        }
        if (inputList.isEmpty()) {
            println("No tasks have been input")
        } else {
            printTasks()
            editTaskRecursion()
        }
        start()
    }

    private fun deleteTasks() {
        fun deleteTaskRecursion() {
            println("Input the task number (1-${inputList.lastIndex + 1}):")
            val taskNumber = readln().trim()
            try {
                inputList.removeAt(taskNumber.toInt() - 1)
                println("The task is deleted")
            } catch (e: Exception) {
                println("Invalid task number")
                deleteTaskRecursion()
            }
        }
        if (inputList.isEmpty()) {
            println("No tasks have been input")
        } else {
            printTasks()
            deleteTaskRecursion()
        }
        start()
    }

    private fun end() {
        jsonFile.writeText(taskListAdapter.toJson(inputList))
        println("Tasklist exiting!")
        exitProcess(0)
    }
}

fun main() {
    TaskList.loadTasks()
    TaskList.start()
}


