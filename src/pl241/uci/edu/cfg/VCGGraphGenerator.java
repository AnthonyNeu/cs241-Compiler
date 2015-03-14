package pl241.uci.edu.cfg;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import pl241.uci.edu.middleend.*;
import pl241.uci.edu.frontend.*;
import pl241.uci.edu.ir.*;

//TODO:this is not tested with VCG.

/*
Date:2015/03/04
This class is used to generate the .vcg file which can be visualized by VCG.
 */
public class VCGGraphGenerator {
    private PrintWriter writer;

    public VCGGraphGenerator(String outputName){
        try{
            writer = new PrintWriter(new FileWriter("vcg/" + outputName + ".vcg"));
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void printCFG() {
        writer.println("graph: { title: \"Control Flow Graph\"");
        writer.println("layoutalgorithm: dfs");
        writer.println("manhattan_edges: yes");
        writer.println("smanhattan_edges: yes");
        for(BasicBlock block : ControlFlowGraph.getBlocks()) {
            printCFGNode(block);
        }
        writer.println("}");
        writer.close();
    }

    public void printDominantTree(){
        writer.println("graph: { title: \"Dominant Tree\"");
        writer.println("layoutalgorithm: dfs");
        writer.println("manhattan_edges: yes");
        writer.println("smanhattan_edges: yes");

        printDominantTreeUtil(DominatorTreeGenerator.root);

        writer.println("}");
        writer.close();
    }

    private void printDominantTreeUtil(DominatorTreeNode root){
        if(root == null)
            return;
        printDTNode(root);
        for(DominatorTreeNode child : root.children)
            printDominantTreeUtil(child);
    }

    private void printCFGNode(BasicBlock block) {
        writer.println("node: {");
        writer.println("title: \"" + block.getId() + "\"");
        writer.println("label: \"" + block.getId() + "[");
        for(Map.Entry<Integer, Instruction> entry : block.getPhiFunctionGenerator().getPhiInstructionMap().entrySet()){
            String var = Scanner.ident.get(entry.getKey());
            Instruction instr = entry.getValue();
            instr.setVariableName(var);
            this.printInstruction(entry.getValue());
        }
        for(Instruction inst : block.getInstructions()) {
            this.printInstruction(inst);
        }
        writer.println("]\"");
        writer.println("}");

        if(block.getFollowBlock() != null) {
            printEdge(block.getId(), block.getFollowBlock().getId());
        }

        if(block.getElseBlock() != null) {
            printEdge(block.getId(), block.getElseBlock().getId());
        }

        if(block.getBackBlock() != null) {
            printEdge(block.getId(), block.getBackBlock().getId());
        }

        if(block.getJoinBlock() != null && block.getFollowBlock() == null) {
            printEdge(block.getId(), block.getJoinBlock().getId());
        }
    }

    private void printDTNode(DominatorTreeNode node) {
        writer.println("node: {");
        writer.println("title: \"" + node.block.getId() + "\"");
        writer.println("label: \"" + node.block.getId() + "[");
        for(Map.Entry<Integer, Instruction> entry : node.block.getPhiFunctionGenerator().getPhiInstructionMap().entrySet()){
            String var = Scanner.ident.get(entry.getKey());
            Instruction instr = entry.getValue();
            instr.setVariableName(var);
            this.printInstruction(entry.getValue());
        }
        for(Instruction inst : node.block.getInstructions()) {
            this.printInstruction(inst);
        }
        writer.println("]\"");
        writer.println("}");

        for(DominatorTreeNode child : node.children){
            printEdge(node.block.getId(), child.block.getId());
        }
    }

    public void printEdge(int sourceId, int targetId){
        writer.println("edge: { sourcename: \"" + sourceId + "\"");
        writer.println("targetname: \"" + targetId + "\"");
        writer.println("color: blue");
        writer.println("}");
    }

    public void printInstruction(Instruction instruction){
        writer.println(instruction);
    }
}
