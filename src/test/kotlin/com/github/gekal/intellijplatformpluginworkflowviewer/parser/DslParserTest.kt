package com.github.gekal.intellijplatformpluginworkflowviewer.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class DslParserTest {

    @Test
    fun testParseRockPaperScissors() {
        val dsl = """
            name 'Rock Paper Scissors'

            steps {
                step('start', type: Start)
                step('rock_paper_scissors') {
                    actions('storeHands', function: 'StoreData', oneTime: false) {
                        string 'player1_hand'
                        string 'player2_hand'
                    }
                }
                step('draw', type: End)
                step('player1_wins', type: End)
                step('player2_wins', type: End)
            }

            transitions {
                transition(from: 'start', to: 'rock_paper_scissors') { true }
                transition(from: 'rock_paper_scissors', to: 'draw') {
                    iterm('player1_hand') == iterm('player2_hand')
                }
                transition(from: 'rock_paper_scissors', to: 'player1_wins') {
                    (iterm('player1_hand') == 'rock' && iterm('player2_hand') == 'scissors')
                    || (iterm('player1_hand') == 'paper' && iterm('player2_hand') == 'rock')
                    || (iterm('player1_hand') == 'scissors' && iterm('player2_hand') == 'paper')
                }
                transition(from: 'rock_paper_scissors', to: 'player2_wins') {
                    (iterm('player1_hand') == 'scissors' && iterm('player2_hand') == 'rock')
                    || (iterm('player1_hand') == 'paper' && iterm('player2_hand') == 'scissors')
                    || (iterm('player1_hand') == 'rock' && iterm('player2_hand') == 'paper')
                }
            }
        """.trimIndent()

        val (nodes, edges) = DslParser.parse(dsl)

        assertEquals(5, nodes.size)
        assertEquals("start", nodes[0].id)
        assertEquals("rock_paper_scissors", nodes[1].id)
        assertEquals("draw", nodes[2].id)
        assertEquals("player1_wins", nodes[3].id)
        assertEquals("player2_wins", nodes[4].id)

        assertEquals(4, edges.size)
        assertEquals("start", edges[0].source)
        assertEquals("rock_paper_scissors", edges[0].target)
        assertEquals("rock_paper_scissors", edges[1].source)
        assertEquals("draw", edges[1].target)
    }

    @Test
    fun testFallbackParsing() {
        val dsl = """
            Step 1
            Step 2
            Step 3
        """.trimIndent()

        val (nodes, edges) = DslParser.parse(dsl)

        assertEquals(3, nodes.size)
        assertEquals("node_0", nodes[0].id)
        assertEquals("Step 1", nodes[0].data["label"])
        assertEquals(2, edges.size)
        assertEquals("node_0", edges[0].source)
        assertEquals("node_1", edges[0].target)
    }

    @Test
    fun testGroovyComments() {
        val dsl = """
            steps {
                // This is a comment
                step('A')
                /* Multi-line
                   comment */
                step('B')
            }
            transitions {
                transition(from: 'A', to: 'B')
            }
        """.trimIndent()

        val (nodes, edges) = DslParser.parse(dsl)

        assertEquals(2, nodes.size)
        assertEquals("A", nodes[0].id)
        assertEquals("B", nodes[1].id)
        assertEquals(1, edges.size)
        assertEquals("A", edges[0].source)
        assertEquals("B", edges[1].target)
    }

    @Test
    fun testGroovyLogic() {
        val dsl = """
            def myNodes = ['X', 'Y', 'Z']
            steps {
                myNodes.each { name ->
                    step(name)
                }
            }
            transitions {
                for (int i = 0; i < myNodes.size() - 1; i++) {
                    transition(from: myNodes[i], to: myNodes[i+1])
                }
            }
        """.trimIndent()

        val (nodes, edges) = DslParser.parse(dsl)

        assertEquals(3, nodes.size)
        assertEquals("X", nodes[0].id)
        assertEquals("Y", nodes[1].id)
        assertEquals("Z", nodes[2].id)
        assertEquals(2, edges.size)
        assertEquals("X", edges[0].source)
        assertEquals("Y", edges[0].target)
        assertEquals("Y", edges[1].source)
        assertEquals("Z", edges[1].target)
    }
}
