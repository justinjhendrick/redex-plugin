package au.com.timmutton.redexplugin

import au.com.timmutton.redexplugin.internal.RedexConfiguration
import au.com.timmutton.redexplugin.internal.RedexConfigurationContainer
import com.android.build.gradle.api.ApplicationVariant
import com.android.builder.model.SigningConfig
import com.google.gson.Gson
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.process.internal.ExecException
import java.io.File
import java.io.FileWriter

/**
 * @author timmutton
 */
open class RedexTask: Exec() {
    companion object {
        private val TASK_GROUP = "Optimisation"

        var passes: Array<String>? = null
        var sdkDirectory: String? = null
    }

    private var signingConfig: SigningConfig? = null

    @InputFile
    private lateinit var inputFile: File

    @OutputFile
    private lateinit var outputFile: File

    private var mappingFile: File? = null

    // Must use DSL to instantiate class, which means I cant pass variant as a constructor argument
    fun initialise(variant: ApplicationVariant) {
        group = TASK_GROUP
        description = "Run Redex tool on your ${variant.name.capitalize()} apk"
        signingConfig = variant.buildType.signingConfig
        mustRunAfter(variant.assemble)

        val output = variant.outputs[0]
        inputFile = output.outputFile
        outputFile = File(output.outputFile.toString().replace(".apk", "-redex.apk"))

        mappingFile = variant.mappingFile
    }

    override fun exec() {
        sdkDirectory?.apply {
            environment("ANDROID_SDK", sdkDirectory)
        }

        passes?.apply {
            if (passes!!.size > 0) {
                val redexConfig = Gson().toJson(RedexConfigurationContainer(RedexConfiguration(passes!!)))
                val configFile = File(project.buildDir, "redex.config")
                configFile.createNewFile()
                val writer = FileWriter(configFile)
                val configString = Gson().toJson(redexConfig)
                writer.write(configString.substring(1, configString.length - 1).replace("\\", ""))
                writer.close()
                args("-c", configFile.absolutePath)
            }
        }

        signingConfig?.apply {
            args("--sign",
                    "--keystore", signingConfig!!.storeFile.absolutePath,
                    "--keyalias", signingConfig!!.keyAlias,
                    "--keypass", signingConfig!!.keyPassword)
        }

        args("-o", "$outputFile", "$inputFile")
        executable("redex")

        if (mappingFile != null && !mappingFile!!.exists()) {
            logger.log(LogLevel.INFO, "Mapping file specified at ${mappingFile!!.absolutePath} does not exist, assuming output is not obfuscated.")
            mappingFile = null
        }

        var showStats = true
        var startingMethods = 0
        var startingFields = 0
        var startingSize = 0

        val originalDexData = DexFile.extractDexData(inputFile)
        try {
            startingMethods = originalDexData.sumBy { it.data.methodRefs.size }
            startingFields = originalDexData.sumBy { it.data.fieldRefs.size }
            startingSize = inputFile.length().toInt()
        } catch(e: Exception) {
            showStats = false
        } finally {
            originalDexData.forEach { it.dispose() }
        }

        if(showStats) {
            logger.log(LogLevel.LIFECYCLE, "\nBefore redex:\n\t$startingMethods methods\n\t$startingFields fields\n\t$startingSize bytes")
        }

        try {
            super.exec()

            if(showStats) {
                val newDexData = DexFile.extractDexData(outputFile)
                try {
                    val methods = newDexData.sumBy { it.data.methodRefs.size }
                    val methodPercentage = "%.2f".format(methods.toFloat() / startingMethods * 100f)
                    val fields = newDexData.sumBy { it.data.fieldRefs.size }
                    val fieldPercentage = "%.2f".format(fields.toFloat() / startingFields * 100f)
                    val size = outputFile.length().toInt()
                    val sizePercentage = "%.2f".format(size.toFloat() / startingSize * 100f)

                    logger.log(LogLevel.LIFECYCLE, "After redex:")
                    logger.log(LogLevel.LIFECYCLE, "\t$methods methods (%$methodPercentage of original)")
                    logger.log(LogLevel.LIFECYCLE, "\t$fields fields (%$fieldPercentage of original)")
                    logger.log(LogLevel.LIFECYCLE, "\t$size bytes (%$sizePercentage of original)")
                } finally {
                    newDexData.forEach { it.dispose() }
                }
            }
        } catch (e: ExecException) {
            if (e.message != null && e.message!!.contains("A problem occurred starting process")) {
                throw ExecException("A problem occurred starting Redex. " +
                        "Ensure you have installed Redex using the instructions at https://github.com/facebook/redex")
            } else {
                throw e
            }
        }
    }
}