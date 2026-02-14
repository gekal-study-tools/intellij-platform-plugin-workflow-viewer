package com.github.gekal.intellijplatformpluginworkflowviewer.parser

object DslParser {
    data class Node(val id: String, val data: Map<String, String>)
    data class Edge(val id: String, val source: String, val target: String)

    fun parse(text: String): Pair<List<Node>, List<Edge>> {
        val lines = text.lines().filter { it.isNotBlank() }
        val nodes = mutableListOf<Node>()
        val edges = mutableListOf<Edge>()

        var prevId: String? = null
        lines.forEachIndexed { index, line ->
            val id = "node_$index"
            nodes.add(Node(id, mapOf("label" to line.trim())))
            
            if (prevId != null) {
                edges.add(Edge("edge_$index", prevId, id))
            }
            prevId = id
        }

        return Pair(nodes, edges)
    }
}
