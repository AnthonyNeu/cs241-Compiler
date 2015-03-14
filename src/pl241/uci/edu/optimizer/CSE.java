package pl241.uci.edu.optimizer;

import pl241.uci.edu.ir.BlockType;
import pl241.uci.edu.ir.DominatorTreeNode;
import pl241.uci.edu.ir.ExpressionNode;
import pl241.uci.edu.middleend.Instruction;
import pl241.uci.edu.middleend.InstructionType;
import pl241.uci.edu.middleend.Result;

import java.util.Map;
import java.util.HashMap;

/*
Date:2015/03/08
This class is used to do the common subsequence elimination.
 */
public class CSE {
    //for all the instruction from NEG to DIV, we build a hash table to hold all the instructions
    //HashMap<ExpressionNode,Integer> is a hashmap from the expression node to the instruction pc code
    private HashMap<InstructionType,HashMap<ExpressionNode,Integer>> cse;


    public CSE()
    {
        cse = new HashMap<InstructionType,HashMap<ExpressionNode,Integer>>();

        cse.put(InstructionType.NEG, new HashMap<ExpressionNode, Integer>());
        cse.put(InstructionType.ADD, new HashMap<ExpressionNode, Integer>());
        cse.put(InstructionType.MUL, new HashMap<ExpressionNode, Integer>());
        cse.put(InstructionType.DIV, new HashMap<ExpressionNode, Integer>());
        cse.put(InstructionType.SUB, new HashMap<ExpressionNode, Integer>());
        cse.put(InstructionType.ADDA, new HashMap<ExpressionNode, Integer>());
        cse.put(InstructionType.LOADADD, new HashMap<ExpressionNode, Integer>());
        cse.put(InstructionType.STOREADD, new HashMap<ExpressionNode, Integer>());
    }

    public void CSEoptimize(DominatorTreeNode root)
    {
        CSEoptimizaRecursion(root,new HashMap<Integer, Integer>());
    }

    /**
     * This method is used to do CSE recursively.
     * @param root, root of dominator tree.
     * @param replaceInstruction, Hashmap from the instruction ID to the reference instruction ID.
     */
    private void CSEoptimizaRecursion(DominatorTreeNode root,HashMap<Integer,Integer> replaceInstruction)
    {
        if(root == null)
            return;

        for(Instruction ins : root.block.getInstructions()) {
            if (ins.deleted) {
                continue;
            }

            Result left = ins.getLeftResult();
            Result right = ins.getRightResult();

            if (left != null && left.type == Result.ResultType.instruction && replaceInstruction.containsKey(left.instrRef))
                left.instrRef = replaceInstruction.get(left.instrRef);

            if (right != null && right.type == Result.ResultType.instruction && replaceInstruction.containsKey(right.instrRef))
                right.instrRef = replaceInstruction.get(right.instrRef);


            if (ins.isExpressionOp()) {
                HashMap<ExpressionNode, Integer> tempExp = cse.get(ins.getOp());
                ExpressionNode curExp = new ExpressionNode(left, right);
                if (!tempExp.containsKey(curExp)) {
                    tempExp.put(curExp, ins.getInstructionPC());

                    //mark next instruction as replace
                    Instruction next = root.block.getNextIntruction(ins);
                    if (next != null && !next.deleted && next.getOp() == InstructionType.MOVE) {
                        next.setState(Instruction.State.REPLACE);
                        next.referenceInstrId = ins.getInstructionPC();
                    }
                } else {
                    //mark this instruction as deleted
                    ins.deleted = true;

                    //mark next intruction as replce
                    Instruction next = root.block.getNextIntruction(ins);
                    if (next != null&&!next.deleted&&next.getOp()==InstructionType.MOVE) {
                        next.setState(Instruction.State.REPLACE);
                        next.referenceInstrId = tempExp.get(curExp);
                    }
                    replaceInstruction.put(ins.getInstructionPC(), tempExp.get(curExp));
                }
            }
        }

            //update the result in phi function
        for(Map.Entry<Integer, Instruction> entry : root.block.getPhiFunctionGenerator().getPhiInstructionMap().entrySet()){
            Instruction instr = entry.getValue();
            Result left = instr.getLeftResult();
            Result right = instr.getRightResult();
            if(left != null && left.type == Result.ResultType.instruction && replaceInstruction.containsKey(left.instrRef)){
                left.instrRef = replaceInstruction.get(left.instrRef);
            }
            if(right != null && right.type == Result.ResultType.instruction && replaceInstruction.containsKey(right.instrRef)){
                right.instrRef = replaceInstruction.get(right.instrRef);
            }
        }
        if(root.block.getBackBlock()!=null){
            for(Map.Entry<Integer, Instruction> entry : root.block.getBackBlock().getPhiFunctionGenerator().getPhiInstructionMap().entrySet()){
                Instruction instr = entry.getValue();
                Result left = instr.getLeftResult();
                Result right = instr.getRightResult();
                if(left != null && left.type == Result.ResultType.instruction && replaceInstruction.containsKey(left.instrRef)){
                    left.instrRef = replaceInstruction.get(left.instrRef);
                }
                if(right != null && right.type == Result.ResultType.instruction && replaceInstruction.containsKey(right.instrRef)){
                    right.instrRef = replaceInstruction.get(right.instrRef);
                }
            }
        }

            //do CSE to all the child nodes
        for(DominatorTreeNode child:root.getChildren())
        {
            CSEoptimizaRecursion(child,new HashMap<Integer, Integer>(replaceInstruction));
        }
    }
}
