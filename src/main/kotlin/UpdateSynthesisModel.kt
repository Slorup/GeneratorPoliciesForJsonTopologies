import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable()
data class UpdateSynthesisModel(
    @SerialName("Initial_routing")  private val _initRouting: Set<List<Int>>,
    @SerialName("Final_routing")    private val _finalRouting: Set<List<Int>>,
    @SerialName("Properties")       private val _properties: Properties
) {
    val initialRouting: Set<Edge> = _initRouting.map { Edge(it) }.toSet()
    val finalRouting: Set<Edge> = _finalRouting.map { Edge(it) }.toSet()
    val switches: Set<Int> = (initialRouting union finalRouting).fold(setOf()) { acc, e: Edge -> acc union setOf(e.source, e.target) }

    // routing properties
    val reachability: Reachability = _properties.reachability
    val waypoint: Waypoint? = _properties.waypoint
    val conditionalEnforcements: List<ConditionalEnforcement>? = _properties.conditionalEnforcements
    val alternativeWaypoints: List<AlternativeWaypoint>? = _properties.alternativeWaypoints
    val dfa: DFA? = _properties.DFA

    // we never consider loop-freedom since we always have reachability, which preserves loop-freedom
    val loopFreedom: LoopFreedom? = _properties.loopFreedom

    @Serializable
    class Edge {
        val source: Int
        val target: Int

        operator fun component1() = source
        operator fun component2() = target

        constructor(l: List<Int>) {
            assert(l.size == 2)
            source = l[0]
            target = l[1]
        }

        constructor(s: Pair<Int, Int>) {
            source = s.first
            target = s.second
        }

        constructor(source: Int, target: Int) {
            this.source = source
            this.target = target
        }

        override fun toString(): String {
            return "[$source, $target]"
        }

        override fun hashCode(): Int {
            return source xor target shl 16
        }

        override fun equals(other: Any?): Boolean {
            return other is Edge && other.source == this.source && other.target == this.target
        }
    }

    @Serializable
    open class Properties (
        @SerialName("Waypoint")     val waypoint: Waypoint? = null,
        @SerialName("ConditionalEnforcement") val conditionalEnforcements: List<ConditionalEnforcement>? = null,
        @SerialName("AlternativeWaypoint") val alternativeWaypoints: List<AlternativeWaypoint>? = null,
        @SerialName("LoopFreedom")  val loopFreedom: LoopFreedom? = null,
        @SerialName("Reachability") val reachability: Reachability,
        @SerialName("DFA") val DFA: DFA? = null
    )

    @Serializable
    class DFA(
        @SerialName("initialState") val initialState: Int,
        @SerialName("finalStates")   val finalStates: List<Int>,
        @SerialName("edges")        val edges: List<DfaEdge>
    )

    @Serializable
    class DfaEdge(
        @SerialName("from") val from: Int,
        @SerialName("to")   val to: Int,
        @SerialName("label")val label: Int
    )

    @Serializable
    class Waypoint(
        @SerialName("startNode")    val initialNode: Int,
        @SerialName("finalNode")    val finalNode: Int,
        @SerialName("waypoint")     val waypoints: List<Int>)

    @Serializable
    class ConditionalEnforcement(
        @SerialName("s") val s: Int,
        @SerialName("sPrime") val sPrime: Int,
    )

    @Serializable
    class AlternativeWaypoint(
        @SerialName("s1") val s1: Int,
        @SerialName("s2") val s2: Int
    )

    @Serializable
    class LoopFreedom(
        @SerialName("startNode")    val initialNode : Int)

    @Serializable
    class Reachability(
        @SerialName("startNode")    val initialNode : Int,
        @SerialName("finalNode")    val finalNode : Int)
}

