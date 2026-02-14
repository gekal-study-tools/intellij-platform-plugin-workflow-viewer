import { useCallback, useEffect, useState } from 'react';
import ReactFlow, {
  useNodesState,
  useEdgesState,
  addEdge,
  type Connection,
  type Edge,
  Position,
  useReactFlow,
  ReactFlowProvider,
  Background,
  Controls,
  Panel,
} from 'reactflow';
import dagre from 'dagre';
import 'reactflow/dist/style.css';

const dagreGraph = new dagre.graphlib.Graph();
dagreGraph.setDefaultEdgeLabel(() => ({}));

const nodeWidth = 172;
const nodeHeight = 36;

const getLayoutedElements = (nodes: any[], edges: any[], direction = 'TB') => {
  const isHorizontal = direction === 'LR';
  dagreGraph.setGraph({ rankdir: direction });

  nodes.forEach((node) => {
    dagreGraph.setNode(node.id, { width: nodeWidth, height: nodeHeight });
  });

  edges.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target);
  });

  dagre.layout(dagreGraph);

  nodes.forEach((node) => {
    const nodeWithPosition = dagreGraph.node(node.id);
    node.targetPosition = isHorizontal ? Position.Left : Position.Top;
    node.sourcePosition = isHorizontal ? Position.Right : Position.Bottom;

    // Only set position if not already provided
    if (!node.position || (node.position.x === 0 && node.position.y === 0)) {
      // We are shifting the dagre node position (which is center-based) to top-left
      node.position = {
        x: nodeWithPosition.x - nodeWidth / 2,
        y: nodeWithPosition.y - nodeHeight / 2,
      };
    }

    return node;
  });

  return { nodes, edges };
};

function Flow() {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const { fitView } = useReactFlow();
  const [isFirstLoad, setIsFirstLoad] = useState(true);

  const onConnect = useCallback(
    (params: Connection | Edge) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  // Expose updateGraph function to window for Java JCEF communication
  useEffect(() => {
    (window as any).updateGraph = (newNodes: any[], newEdges: any[]) => {
      const { nodes: layoutedNodes, edges: layoutedEdges } = getLayoutedElements(
        newNodes,
        newEdges
      );
      setNodes([...layoutedNodes]);
      setEdges([...layoutedEdges]);
      
      if (isFirstLoad) {
        setTimeout(() => fitView({ padding: 0.2 }), 100);
        setIsFirstLoad(false);
      }
    };
  }, [setNodes, setEdges, fitView, isFirstLoad]);

  const onNodeDragStop = useCallback(
    (_: React.MouseEvent, node: any) => {
      if ((window as any).cefQuery) {
        (window as any).cefQuery({
          request: JSON.stringify({
            type: 'NODE_MOVED',
            nodeId: node.id,
            position: node.position,
          }),
          onSuccess: () => {},
          onFailure: () => {},
        });
      }
    },
    []
  );

  const onRefresh = useCallback(() => {
    if ((window as any).cefQuery) {
      (window as any).cefQuery({
        request: JSON.stringify({ type: 'REFRESH' }),
        onSuccess: () => {},
        onFailure: () => {},
      });
    }
  }, []);

  const onReset = useCallback(() => {
    if ((window as any).cefQuery) {
      (window as any).cefQuery({
        request: JSON.stringify({ type: 'RESET' }),
        onSuccess: () => {
          onRefresh();
          setTimeout(() => fitView({ padding: 0.2 }), 100);
        },
        onFailure: () => {},
      });
    }
  }, [onRefresh, fitView]);

  return (
    <div style={{ width: '100vw', height: '100vh' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onNodeDragStop={onNodeDragStop}
        fitView
      >
        <Background />
        <Controls />
        <Panel position="bottom-right" style={{ marginBottom: '20px' }}>
          <button onClick={onRefresh}>Refresh</button>
          <button onClick={onReset} style={{ marginLeft: '4px' }}>
            Reset Layout
          </button>
        </Panel>
      </ReactFlow>
    </div>
  );
}

function App() {
  return (
    <ReactFlowProvider>
      <Flow />
    </ReactFlowProvider>
  );
}

export default App;
