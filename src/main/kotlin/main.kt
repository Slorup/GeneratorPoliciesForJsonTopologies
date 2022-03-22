import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import utils.generateNewFilesByRandom
import java.io.File
import java.lang.Integer.max
import java.nio.file.Path
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.system.measureTimeMillis

object Options {
    val argParser = ArgParser(programName = "conupsyn")

    private val _pathToFolder by argParser.argument(
        ArgType.String,
        description = "Path to folder with original json files"
    )

    val pathToFolder: Path by lazy { Path.of(_pathToFolder) }

    val folderSuffix by argParser.option(
        ArgType.String,
        fullName = "Output folder suffix"
    ).default("_new")

    val waypoints by argParser.option(
        ArgType.Int,
        fullName = "waypoints"
    ).default(0)

    val altWaypoint by argParser.option(
        ArgType.Int,
        fullName = "alt_waypoints"
    ).default(0)

    val condEnf by argParser.option(
        ArgType.Int,
        fullName = "cond_enf"
    ).default(0)

    val randomSeed by argParser.option(
        ArgType.Int,
        fullName = "Random seed"
    ).default(0)
}

const val version = "1.15"

fun main(args: Array<String>) {
    println("Version: $version \n ${args.joinToString(" ")}")

    Options.argParser.parse(args)

    generateNewFilesByRandom(Options.pathToFolder, Options.folderSuffix, Options.randomSeed, Options.waypoints, Options.altWaypoint, Options.condEnf)
}