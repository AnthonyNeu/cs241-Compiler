package pl241.uci.edu.optimizer;

import pl241.uci.edu.cfg.ControlFlowGraph;
import pl241.uci.edu.ir.BasicBlock;
import pl241.uci.edu.ir.BlockType;
import pl241.uci.edu.ir.DominatorTreeNode;
import pl241.uci.edu.middleend.Instruction;
import pl241.uci.edu.middleend.Result;
import pl241.uci.edu.middleend.InstructionType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/*
Date:2015/03/05
This is used to allocate the register for all the value in SSA form.
 */
public class RegisterAllocation {
    private class Graph {
        private Map<Instruction, List<Instruction>> edgeList;

        public Graph() {
            edgeList = new HashMap<Instruction, List<Instruction>>();
        }

        public void addNode(Instruction a) {
            if (!edgeList.containsKey(a))
                edgeList.put(a, new ArrayList<Instruction>());
        }

        public void addEdge(Instruction a, Instruction b) {
            if (!edgeList.containsKey(a))
                edgeList.put(a, new ArrayList<Instruction>());
            if (!edgeList.containsKey(b))
                edgeList.put(b, new ArrayList<Instruction>());
            if (!isEdge(a, b)) {
                edgeList.get(a).add(b);
                edgeList.get(b).add(a);
            }
        }

        public List<Instruction> getNeighbors(Instruction a) {
            if (!edgeList.containsKey(a))
                return new ArrayList<Instruction>();
            else
                return edgeList.get(a);
        }

        public int getDegree(Instruction a) {
            return edgeList.get(a).size();
        }

        public boolean isEdge(Instruction a, Instruction b) {
            return edgeList.containsKey(a) && edgeList.get(a).contains(b);
        }

        public Set<Instruction> getNodes() {
            return edgeList.keySet();
        }
    }

    private class BasicBlockInfo {
        public int visited;
        public Set<Instruction> live;

        public BasicBlockInfo() {
            visited = 0;
            live = new HashSet<Instruction>();
        }
    }

    private Graph interferenceGraph;
    private Map<DominatorTreeNode, BasicBlockInfo> bbInfo;
    private Set<DominatorTreeNode> loopHeaders;
    private Map<Instruction, Integer> colors;

    public RegisterAllocation() {
    }

    public Map<Integer, Integer> allocate(DominatorTreeNode b) {
        // Reset allocator
        interferenceGraph = new Graph();
        bbInfo = new HashMap<DominatorTreeNode, BasicBlockInfo>();
        loopHeaders = new HashSet<DominatorTreeNode>();
        colors = new HashMap<Instruction, Integer>();

        calcLiveRange(b, null, 1);
        calcLiveRange(b, null, 2);
        colorGraph();
        saveVCGGraph("vcg/"+b.block.getId() + "interference.vcg"); // For debugging

        Map<Integer, Integer> coloredIDs = new HashMap<Integer, Integer>();
        for (Instruction i : colors.keySet()) {
            coloredIDs.put(i.getInstructionPC(), colors.get(i));
        }

        return coloredIDs;
    }

    private void colorGraph() {
        Instruction maxDegree = null;
        do {
            maxDegree = null;
            for (Instruction a : interferenceGraph.getNodes()) {
                if (!colors.containsKey(a) && (maxDegree == null || interferenceGraph.getDegree(a) > interferenceGraph.getDegree(maxDegree)))
                    maxDegree = a;
            }
            if (maxDegree == null)
                break;

            Set<Integer> taken = new HashSet<Integer>();
            for (Instruction b : interferenceGraph.getNeighbors(maxDegree)) {
                if (colors.containsKey(b))
                    taken.add(colors.get(b));
            }

            int reg = 1;
            while (taken.contains(reg))
                reg += 1;

            colors.put(maxDegree, reg);
        } while (maxDegree != null);
    }

    private void saveVCGGraph(String filename) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(new File(filename)));

            out.write("graph: {\n");
            out.write("title: \"Interference Graph\"\n");
            out.write("layoutalgorithm: dfs\n");
            out.write("manhattan_edges: yes\n");
            out.write("smanhattan_edges: yes\n");

            Set<Instruction> done = new HashSet<Instruction>();
            for (Instruction a : interferenceGraph.getNodes()) {
                done.add(a);

                out.write("node: {\n");
                out.write("title: \"" + a.getInstructionPC() + "\"\n");
                out.write("label: \"" + "REG: " + colors.get(a) + " ::: " + a.toString() + "\"\n");
                out.write("}\n");

                for (Instruction b : interferenceGraph.getNodes()) {
                    if (interferenceGraph.isEdge(a, b) && !done.contains(b)) {
                        out.write("edge: {\n");
                        out.write("sourcename: \"" + a.getInstructionPC() + "\"\n");
                        out.write("targetname: \"" + b.getInstructionPC() + "\"\n");
                        out.write("color: blue\n");
                        out.write("}\n");
                    }
                }
            }
            out.write("}\n");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<Instruction> calcLiveRange(DominatorTreeNode b, DominatorTreeNode last, int pass) {
        Set<Instruction> live = new HashSet<Instruction>();

        if (!bbInfo.containsKey(b))
            bbInfo.put(b, new BasicBlockInfo());

        if (b != null) {
            if (bbInfo.get(b).visited >= pass) {
                live.addAll(bbInfo.get(b).live);
            } else {
                bbInfo.get(b).visited += 1;
                if (bbInfo.get(b).visited == 2) {
                    for (DominatorTreeNode h : loopHeaders)
                        bbInfo.get(b).live.addAll(bbInfo.get(h).live);
                }

                int index = 0;
                for (DominatorTreeNode child : b.getChildren()) {
                    if (b.block.getType() == BlockType.WHILE_JOIN && index == 0)
                        loopHeaders.add(b);

                    live.addAll(calcLiveRange(child, b, pass));
                    index += 1;

                    if (b.block.getType() == BlockType.WHILE_JOIN && index == 0)
                        loopHeaders.remove(b);
                }
                List<Instruction> reverse = new ArrayList<Instruction>();
                reverse.addAll(b.block.getInstructions());
                Collections.reverse(reverse);
                for (Instruction ins : reverse) {
                    if (ins.getOp() != InstructionType.PHI && (!ins.deleted) && ins.getOp()!=InstructionType.END) {
                        live.remove(ins);
                        interferenceGraph.addNode(ins);
                        for (Instruction other : live) {
                            interferenceGraph.addEdge(ins, other);
                        }
                        if(ins.getLeftResult()!= null && ins.getLeftResult().type == Result.ResultType.instruction)
                        {
                            if(last != null && last.block.findInstruction(ins.getLeftResult().instrRef) != null)
                                live.add(last.block.findInstruction(ins.getLeftResult().instrRef));
                            else if(b.block.findInstruction(ins.getLeftResult().instrRef)!=null)
                                live.add(b.block.findInstruction(ins.getLeftResult().instrRef));
                        }
                        if(ins.getRightResult()!= null && ins.getRightResult().type == Result.ResultType.instruction)
                        {
                            if(last != null && last.block.findInstruction(ins.getRightResult().instrRef) != null)
                                live.add(last.block.findInstruction(ins.getRightResult().instrRef));
                            else if(b.block.findInstruction(ins.getRightResult().instrRef)!=null)
                                live.add(b.block.findInstruction(ins.getRightResult().instrRef));
                        }
                    }
                }

                bbInfo.get(b).live = new HashSet<Instruction>();
                bbInfo.get(b).live.addAll(live);
            }
            List<Instruction> reverse = new ArrayList<Instruction>();
            reverse.addAll(b.block.getInstructions());
            Collections.reverse(reverse);
            for (Instruction ins : reverse) {
                if (ins.getOp() == InstructionType.PHI) {
                    live.remove(ins);
                    interferenceGraph.addNode(ins);
                    for (Instruction other : live)
                        interferenceGraph.addEdge(ins, other);
                    if(ins.getLeftResult()!= null && ins.getLeftResult().type == Result.ResultType.instruction)
                    {
                        if(last != null && last.block.findInstruction(ins.getLeftResult().instrRef) != null)
                            live.add(last.block.findInstruction(ins.getLeftResult().instrRef));
                        else if(b.block.findInstruction(ins.getLeftResult().instrRef)!=null)
                            live.add(b.block.findInstruction(ins.getLeftResult().instrRef));
                    }
                    if(ins.getRightResult()!= null && ins.getRightResult().type == Result.ResultType.instruction)
                    {
                        if(last != null && last.block.findInstruction(ins.getRightResult().instrRef) != null)
                            live.add(last.block.findInstruction(ins.getRightResult().instrRef));
                        else if(b.block.findInstruction(ins.getRightResult().instrRef)!=null)
                            live.add(b.block.findInstruction(ins.getRightResult().instrRef));
                    }
                }
            }
        }
        return live;
    }
}