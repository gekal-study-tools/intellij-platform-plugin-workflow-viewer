package com.github.gekal.intellijplatformpluginworkflowviewer.parser

import java.util.regex.Pattern

object DslParser {
    data class Node(val id: String, val position: Position?, val data: Map<String, String>) {
        fun copy(position: Position? = this.position): Node = Node(id, position, data)
    }

    data class Position(val x: Double, val y: Double)
    data class Edge(val id: String, val source: String, val target: String)

    fun parse(text: String): Pair<List<Node>, List<Edge>> {
        val nodes = mutableListOf<Node>()
        val edges = mutableListOf<Edge>()

        // Step regex: step('id', ...) or step('id') { ... }
        val stepPattern = Pattern.compile("""step\s*\(\s*['"]([^'"]+)['"]""")
        // Transition regex: transition(from: 'source', to: 'target')
        val transitionPattern = Pattern.compile("""transition\s*\(\s*from\s*:\s*['"]([^'"]+)['"]\s*,\s*to\s*:\s*['"]([^'"]+)['"]\s*\)""")

        val lines = text.lines()
        
        lines.forEach { line ->
            val trimmedLine = line.trim()
            
            // Parse steps
            val stepMatcher = stepPattern.matcher(trimmedLine)
            if (stepMatcher.find()) {
                val nodeId = stepMatcher.group(1)
                if (nodes.none { it.id == nodeId }) {
                    nodes.add(Node(nodeId, null, mapOf("label" to nodeId)))
                }
            }
            
            // Parse transitions
            val transitionMatcher = transitionPattern.matcher(trimmedLine)
            if (transitionMatcher.find()) {
                val from = transitionMatcher.group(1)
                val to = transitionMatcher.group(2)
                val edgeId = "edge_${from}_${to}"
                edges.add(Edge(edgeId, from, to))
            }
        }

        // If no steps or transitions found, fallback to old line-by-line parsing for backward compatibility or simple files
        if (nodes.isEmpty() && edges.isEmpty()) {
            var prevId: String? = null
            text.lines().filter { it.isNotBlank() }.forEachIndexed { index, rawLine ->
                val label = rawLine.trim()
                val id = "node_$index"
                nodes.add(Node(id, null, mapOf("label" to label)))
                if (prevId != null) {
                    edges.add(Edge("edge_$index", prevId, id))
                }
                prevId = id
            }
        }

        return Pair(nodes, edges)
    }
}
