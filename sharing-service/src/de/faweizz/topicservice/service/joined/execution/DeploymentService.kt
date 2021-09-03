package de.faweizz.topicservice.service.joined.execution

import de.faweizz.topicservice.persistence.Mongo
import de.faweizz.topicservice.service.Configuration
import de.faweizz.topicservice.service.joined.resource.CodeExecution
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.Duration

class DeploymentService(
    private val mongo: Mongo,
    private val configuration: Configuration
) {

    private val runningDeployments = mutableMapOf<CodeExecution, Process>()

    init {
        initAllDeployments()

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                killAllProcesses()
            }
        })

        launchHousekeepingThread()
    }

    private fun launchHousekeepingThread() {
        GlobalScope.launch {
            runningDeployments.forEach {
                if (!it.value.isAlive) {
                    println("Detected that custom code execution ${it.key} died, restarting...")
                    deploy(it.key)
                }
            }
            delay(1000 * 60)
        }
    }

    private fun initAllDeployments() {
        mongo.findAllJoinedResources()
            .forEach { resource ->
                resource.codeExecutions.forEach { deploy(it) }
            }
    }

    private fun killAllProcesses() {
        runningDeployments.forEach {
            it.value.destroy()
        }
    }

    fun deploy(codeExecution: CodeExecution) {
        val gitDirectoryFile = File("cloned")
        gitDirectoryFile.mkdirs()
        val gitDirectory = gitDirectoryFile.absolutePath

        executeAndWait(gitDirectory, listOf("git", "clone", codeExecution.gitlabRepositoryUrl, codeExecution.name))
        val projectDirectory = "$gitDirectory/${codeExecution.name}"
        executeAndWait(projectDirectory, listOf("git", "checkout", codeExecution.commitHash))

        val process = execute(
            projectDirectory,
            listOf("./gradlew", "run"),
            mapOf(
                "KAFKA_ADDRESS" to configuration.kafkaAddress,
                "CLIENT_NAME" to codeExecution.clientName,
                "CLIENT_SECRET" to codeExecution.clientSecret,
                "CONSUMER_GROUP" to codeExecution.clientConsumerGroup,
                "INPUT_TOPIC" to codeExecution.inputTopic,
                "OUTPUT_TOPIC" to codeExecution.outputTopic,
                "TRUSTSTORE_LOCATION" to configuration.trustStoreLocation,
                "TRUSTSTORE_PASSWORD" to configuration.trustStorePassword
            )
        )

        runningDeployments[codeExecution] = process
    }

    private fun executeAndWait(location: String, command: List<String>) {
        val fileLocation = File(location)
        val process = ProcessBuilder()
            .directory(fileLocation)
            .command(command)
            .redirectErrorStream(true)
            .start()

        process.waitFor()
    }

    private fun execute(location: String, command: List<String>, environment: Map<String, String>): Process {
        val builder = ProcessBuilder()
            .directory(File(location))
            .inheritIO()
            .command(command)

        val environmentMap = builder.environment()
        environment.forEach { environmentMap[it.key] = it.value }

        return builder.start()
    }
}