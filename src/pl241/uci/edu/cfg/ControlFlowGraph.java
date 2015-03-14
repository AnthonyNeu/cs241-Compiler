package pl241.uci.edu.cfg;

import pl241.uci.edu.ir.BasicBlock;
import pl241.uci.edu.ir.BlockType;
import pl241.uci.edu.ir.FunctionDecl;
import pl241.uci.edu.middleend.Instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*
Date:2015/03/04
This class is used to store control flow graph.
 */
public class ControlFlowGraph {
    //this is the first block in the control flow graph
    private static BasicBlock firstBlock;

    //this stores all the blocks in the control flow graph
    private static ArrayList<BasicBlock> blocks;

    //this stores all the functions in the control flow graph
    public static HashMap<Integer,FunctionDecl> allFunctions;

    //this stores all the instructions in the control flow graph
    public static ArrayList<Instruction> allInstructions;

    public static DelUseChain delUseChain;

    public ControlFlowGraph()
    {
        blocks = new ArrayList<BasicBlock>();
        firstBlock = new BasicBlock(BlockType.NORMAL);
        allFunctions = new HashMap<Integer,FunctionDecl>();
        allInstructions = new ArrayList<Instruction>();
        delUseChain = new DelUseChain();

        //add pre defined function
        allFunctions.put(0,new FunctionDecl(0));
        allFunctions.put(1,new FunctionDecl(1));
        allFunctions.put(2,new FunctionDecl(2));
    }

    public static BasicBlock getFirstBlock()
    {
        return firstBlock;
    }

    public static ArrayList<BasicBlock> getBlocks()
    {
        return blocks;
    }

    public static Instruction getInstruction(int pc)
    {
        for(BasicBlock block : blocks)
        {
            if(block.findInstruction(pc) != null)
                return block.findInstruction(pc);
        }
        Error("cannot find the instruction with pc = " + pc);
        return null;
    }

    public static void printInstruction()
    {
        for (BasicBlock block : blocks) {
            System.out.println("Block_" + block.getId() + "[");
            for (Map.Entry<Integer, Instruction> entry : block.getPhiFunctionGenerator().getPhiInstructionMap().entrySet())
                System.out.println(entry.toString());
            for (Instruction i : block.getInstructions())
                System.out.println(i.toString());
            System.out.println("]");
        }
    }

    private static void Error(String msg)
    {
        System.out.println("ControlFlowGraph Error! " + msg);
    }
}
