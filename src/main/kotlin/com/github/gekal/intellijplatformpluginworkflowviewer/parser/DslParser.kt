package com.github.gekal.intellijplatformpluginworkflowviewer.parser

import groovy.lang.Binding
import groovy.lang.Closure
import groovy.lang.GroovyShell
import groovy.lang.Script
import org.codehaus.groovy.control.CompilerConfiguration
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

        try {
            val config = CompilerConfiguration()
            config.scriptBaseClass = WorkflowScript::class.java.name
            
            val binding = Binding()
            binding.setVariable("_nodes", nodes)
            binding.setVariable("_edges", edges)
            
            val shell = GroovyShell(binding, config)
            shell.evaluate(text)
        } catch (e: Throwable) {
            // Fallback to regex if Groovy fails
            return parseWithRegex(text)
        }

        if (nodes.isEmpty() && edges.isEmpty()) {
            return parseWithRegex(text)
        }

        return Pair(nodes, edges)
    }

    private fun parseWithRegex(text: String): Pair<List<Node>, List<Edge>> {
        val nodes = mutableListOf<Node>()
        val edges = mutableListOf<Edge>()

        val stepPattern = Pattern.compile("""step\s*\(\s*['"]([^'"]+)['"]""")
        val transitionPattern = Pattern.compile("""transition\s*\(\s*from\s*:\s*['"]([^'"]+)['"]\s*,\s*to\s*:\s*['"]([^'"]+)['"]\s*\)""")

        text.lines().forEach { line ->
            val trimmedLine = line.trim()
            val stepMatcher = stepPattern.matcher(trimmedLine)
            if (stepMatcher.find()) {
                val nodeId = stepMatcher.group(1)
                if (nodes.none { it.id == nodeId }) {
                    nodes.add(Node(nodeId, null, mapOf("label" to nodeId)))
                }
            }
            val transitionMatcher = transitionPattern.matcher(trimmedLine)
            if (transitionMatcher.find()) {
                val from = transitionMatcher.group(1)
                val to = transitionMatcher.group(2)
                val edgeId = "edge_${from}_${to}"
                edges.add(Edge(edgeId, from, to))
            }
        }

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

    abstract class WorkflowScript : Script() {
        @Suppress("UNCHECKED_CAST")
        private val nodes: MutableList<Node> get() = binding.getVariable("_nodes") as MutableList<Node>
        @Suppress("UNCHECKED_CAST")
        private val edges: MutableList<Edge> get() = binding.getVariable("_edges") as MutableList<Edge>

        fun name(n: Any?) {}

        fun steps(cl: Closure<*>) {
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl.call()
        }

        fun transitions(cl: Closure<*>) {
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl.call()
        }

        fun step(id: String) {
            if (nodes.none { it.id == id }) {
                nodes.add(Node(id, null, mapOf("label" to id)))
            }
        }

        fun step(id: String, args: Map<String, Any>) {
            step(id)
        }

        fun step(id: String, cl: Closure<*>) {
            step(id)
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl.call()
        }

        fun step(id: String, args: Map<String, Any>, cl: Closure<*>) {
            step(id)
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl.call()
        }

        fun transition(args: Map<String, Any>) {
            val from = args["from"]?.toString()
            val to = args["to"]?.toString()
            if (from != null && to != null) {
                val edgeId = "edge_${from}_${to}"
                edges.add(Edge(edgeId, from, to))
            }
        }

        fun transition(args: Map<String, Any>, cl: Closure<*>) {
            transition(args)
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl.call()
        }

        fun iterm(name: String) = name

        fun methodMissing(name: String, args: Any?): Any? = null
        fun propertyMissing(name: String): Any? = name
    }
}
