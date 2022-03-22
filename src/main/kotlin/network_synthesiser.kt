package utils

import UpdateSynthesisModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File
import java.nio.file.Path
import kotlin.random.Random

fun generateNewFilesByRandom(pathToFolder: Path, ext: String, randomSeed: Int, numWaypoints: Int, numAltWaypoints: Int, numCondEnfs: Int){
    val dir = pathToFolder.toFile()
    assert(dir.isDirectory)
    val newDir = Path.of(pathToFolder.toString() + ext).toFile()
    if (!newDir.exists()) newDir.mkdir()

    for (file in dir.walk().iterator()){
        if (file.isDirectory) continue
        var oldUSM: UpdateSynthesisModel = updateSynthesisModelFromJsonText(file.readText())
        oldUSM = oldUSM.clearProperties()
        var newUSM: UpdateSynthesisModel? = null

        for (i in 0 until numWaypoints){
            val random = Random(randomSeed + 13 * i)
            newUSM = addWaypointsToUSM(oldUSM, random)
            if (newUSM == null)
                continue
            oldUSM = newUSM
        }

        for (i in 0 until numAltWaypoints){
            val random = Random(randomSeed + 13 * i)
            newUSM = addAlternativeWaypointToUSM(oldUSM, random)
            if (newUSM == null)
                continue
            oldUSM = newUSM
        }

        for (i in 0 until numCondEnfs){
            val random = Random(randomSeed + 13 * i)
            newUSM = addConditionalEnforcementToUSM(oldUSM, random)
            if (newUSM == null)
                continue
            oldUSM = newUSM
        }

        if (newUSM == null) {
            println("Failed: Could not transform ${file.name}")
            continue
        }
        val jElem = Json.encodeToJsonElement(newUSM)

        val newPath = newDir.absolutePath + File.separator + file.name
        val newFile = File(newPath)
        if (!newFile.exists()) newFile.createNewFile()
        newFile.writeText(jElem.toString())

        println("Transform ${file.name}")
    }
}

//fun generateNewFilesByRandom(
//    transform: (UpdateSynthesisModel, Random) -> UpdateSynthesisModel?, pathToFolder: Path, ext: String, randomSeed: Int, numMore: Int
//) {
//    val dir = pathToFolder.toFile()
//    assert(dir.isDirectory)
//    val newDir = Path.of(pathToFolder.toString() + ext + numMore).toFile()
//    if (!newDir.exists()) newDir.mkdir()
//
//    for (file in dir.walk().iterator()) {
//        if (file.isDirectory) continue
//        var oldUSM: UpdateSynthesisModel = updateSynthesisModelFromJsonText(file.readText())
//        oldUSM = oldUSM.clearProperties()
//        var newUSM: UpdateSynthesisModel? = null
//        for (i in 0 until numMore) {
//            val random = Random(randomSeed + 13 * i)
//            newUSM = transform(oldUSM, random)
//            if (newUSM == null)
//                continue
//            oldUSM = newUSM
//        }
//
//        if (newUSM == null) {
//            println("Failed: Could not transform ${file.name}")
//            continue
//        }
//        val jElem = Json.encodeToJsonElement(newUSM)
//
//        val newPath = newDir.absolutePath + File.separator + file.name
//        val newFile = File(newPath)
//        if (!newFile.exists()) newFile.createNewFile()
//        newFile.writeText(jElem.toString())
//
//        println("Transform ${file.name}")
//    }
//}

fun addWaypointsToUSM(
    usm: UpdateSynthesisModel, random: Random
): UpdateSynthesisModel? {
    val candidateSwitches =
        usm.initialRouting.filter { i_it -> usm.finalRouting.map { it.source }.contains(i_it.source) }.map { it.source }
            .toMutableList()
    if (usm.waypoint != null)
        candidateSwitches -= usm.waypoint.waypoints
    if (candidateSwitches.isEmpty())
        return null

    val new = candidateSwitches.sorted()[random.nextInt(candidateSwitches.size)]

    val newUsm = usm.addWaypoints(listOf(new))

    return newUsm
}

fun addConditionalEnforcementToUSM(usm: UpdateSynthesisModel, random: Random): UpdateSynthesisModel? {
    val candidateSwitches = usm.switches.toMutableList()
    if (candidateSwitches.size < 2)
        return null

    val s = candidateSwitches[random.nextInt(candidateSwitches.size)]
    candidateSwitches -= s

    if (s in usm.initialRouting.map { it.source }) candidateSwitches.filter { it in usm.initialRouting.map { it.source } }
    else if (s in usm.finalRouting.map { it.source }) candidateSwitches.filter { it in usm.finalRouting.map { it.source } }

    if (usm.conditionalEnforcements != null && s in usm.conditionalEnforcements.map { it.s })
        candidateSwitches -= usm.conditionalEnforcements.first { it.s == s }.sPrime
    if (usm.conditionalEnforcements != null && s in usm.conditionalEnforcements.map { it.sPrime })
        candidateSwitches -= usm.conditionalEnforcements.first { it.sPrime == s }.s
    if (candidateSwitches.isEmpty())
        return null

    val sPrime = candidateSwitches[random.nextInt(candidateSwitches.size)]

    return usm.setConditionalEnforcement(s, sPrime)
}

fun addAlternativeWaypointToUSM(usm: UpdateSynthesisModel, random: Random): UpdateSynthesisModel? {
    val candidateSwitches = usm.switches.toMutableList()
    if (candidateSwitches.size < 2)
        return null

    val s1 = candidateSwitches[random.nextInt(candidateSwitches.size)]
    candidateSwitches -= s1

    if (s1 in usm.initialRouting.map { it_i -> it_i.source }) candidateSwitches.filter { it_f -> it_f in usm.finalRouting.map { it.source } }
    else if (s1 in usm.finalRouting.map { it_i -> it_i.source }) candidateSwitches.filter { it_f -> it_f in usm.initialRouting.map { it.source } }

    if (usm.alternativeWaypoints != null && s1 in usm.alternativeWaypoints.map { it.s1 })
        candidateSwitches -= usm.alternativeWaypoints.first { it.s1 == s1 }.s2
    if (candidateSwitches.isEmpty())
        return null

    val s2 = candidateSwitches[random.nextInt(candidateSwitches.size)]

    return usm.setAlternativeWaypoint(s1, s2)
}

fun UpdateSynthesisModel.addWaypoints(waypoints: List<Int>): UpdateSynthesisModel {
    val initRouting = this.initialRouting.map { listOf(it.source, it.target) }.toSet()
    val finalRouting = this.finalRouting.map { listOf(it.source, it.target) }.toSet()

    val newWaypoint =
        UpdateSynthesisModel.Waypoint(reachability.initialNode, reachability.finalNode, (waypoint?.waypoints ?: listOf()) + waypoints)
    val properties = UpdateSynthesisModel.Properties(
        newWaypoint, conditionalEnforcements, alternativeWaypoints, loopFreedom, reachability
    )

    return UpdateSynthesisModel(initRouting, finalRouting, properties)
}

fun UpdateSynthesisModel.setConditionalEnforcement(s: Int, sPrime: Int): UpdateSynthesisModel {
    val initRouting = this.initialRouting.map { listOf(it.source, it.target) }.toSet()
    val finalRouting = this.finalRouting.map { listOf(it.source, it.target) }.toSet()

    val newConditionalEnforcement = (conditionalEnforcements ?: listOf()) + UpdateSynthesisModel.ConditionalEnforcement(s, sPrime)
    val properties = UpdateSynthesisModel.Properties(
        waypoint, newConditionalEnforcement, alternativeWaypoints, loopFreedom, reachability
    )

    return UpdateSynthesisModel(initRouting, finalRouting, properties)
}

fun UpdateSynthesisModel.setAlternativeWaypoint(s1: Int, s2: Int): UpdateSynthesisModel {
    val initRouting = this.initialRouting.map { listOf(it.source, it.target) }.toSet()
    val finalRouting = this.finalRouting.map { listOf(it.source, it.target) }.toSet()

    val newAlternativeWaypoint = (alternativeWaypoints ?: listOf()) + UpdateSynthesisModel.AlternativeWaypoint(s1, s2)
    val properties = UpdateSynthesisModel.Properties(
        waypoint, conditionalEnforcements, newAlternativeWaypoint, loopFreedom, reachability
    )

    return UpdateSynthesisModel(initRouting, finalRouting, properties)
}

fun UpdateSynthesisModel.clearProperties(): UpdateSynthesisModel {
    val initRouting = this.initialRouting.map { listOf(it.source, it.target) }.toSet()
    val finalRouting = this.finalRouting.map { listOf(it.source, it.target) }.toSet()

    val properties = UpdateSynthesisModel.Properties(
        null, null, null, null, reachability
    )

    return UpdateSynthesisModel(initRouting, finalRouting, properties)
}

fun updateSynthesisModelFromJsonText(jsonText: String): UpdateSynthesisModel {
    // HACK: Change waypoint with 1 element of type int to type list of ints
    val regex = """waypoint": (\d+)""".toRegex()
    val text = regex.replace(jsonText) { m ->
        "waypoint\": [" + m.groups[1]!!.value + "]"
    }

    // Update Synthesis Model loaded from json
    return Json.decodeFromString<UpdateSynthesisModel>(text)
}