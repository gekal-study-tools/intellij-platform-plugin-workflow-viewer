package com.github.gekal.intellijplatformpluginworkflowviewer.parser

object DslParser {
    data class Node(val id: String, val position: Position?, val data: Map<String, String>)
    data class Position(val x: Double, val y: Double)
    data class Edge(val id: String, val source: String, val target: String)

    fun parse(text: String): Pair<List<Node>, List<Edge>> {
        val lines = text.lines().filter { it.isNotBlank() }
        val nodes = mutableListOf<Node>()
        val edges = mutableListOf<Edge>()

        var prevId: String? = null
        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            
            // Look for position info: "Label {x: 100, y: 200}"
            val posRegex = """(.*)\{x:\s*([\d.-]+),\s*y:\s*([\d.-]+)\}""".toRegex()
            val match = posRegex.find(line)
            
            val (label, position) = if (match != null) {
                val lbl = match.groupValues[1].trim()
                val x = match.groupValues[2].toDouble()
                val y = match.groupValues[3].toDouble()
                lbl to Position(x, y)
            } else {
                line to null
            }

            val id = "node_$index"
            nodes.add(Node(id, position, mapOf("label" to label)))
            
            if (prevId != null) {
                edges.add(Edge("edge_$index", prevId, id))
            }
            prevId = id
        }

        return Pair(nodes, edges)
    }
}
