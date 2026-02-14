package com.github.gekal.intellijplatformpluginworkflowviewer.parser

object DslParser {
    data class Node(val id: String, val position: Position?, val data: Map<String, String>) {
        fun copy(position: Position? = this.position): Node = Node(id, position, data)
    }
    data class Position(val x: Double, val y: Double)
    data class Edge(val id: String, val source: String, val target: String)

    fun parse(text: String): Pair<List<Node>, List<Edge>> {
        val lines = text.lines().filter { it.isNotBlank() }
        val nodes = mutableListOf<Node>()
        val edges = mutableListOf<Edge>()

        var prevId: String? = null
        lines.forEachIndexed { index, rawLine ->
            val label = rawLine.trim()
            val id = "node_$index"
            nodes.add(Node(id, null, mapOf("label" to label)))

            if (prevId != null) {
                edges.add(Edge("edge_$index", prevId, id))
            }
            prevId = id
        }

        return Pair(nodes, edges)
    }
}
