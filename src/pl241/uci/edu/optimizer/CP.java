package pl241.uci.edu.optimizer;

import pl241.uci.edu.ir.BasicBlock;
import pl241.uci.edu.ir.BlockType;
import pl241.uci.edu.middleend.InstructionType;
import pl241.uci.edu.middleend.Result;
import pl241.uci.edu.middleend.Instruction;
import pl241.uci.edu.ir.DominatorTreeNode;
import pl241.uci.edu.frontend.Scanner;
import pl241.uci.edu.middleend.SSAValue;

import java.util.HashMap;
import java.util.Map;

/*
Date:2015:03/08
This is used to do copy propagation.
 */
public class CP {
    public CP(){

    }

    public void CPoptimize(DominatorTreeNode root)
    {
        CPoptimizeRecursion(root,new HashMap<Result, Integer>(),new HashMap<Result, Integer>());
    }

    /**
     * Do copy propagation.
     * @param root, root node of dominator tree.
     * @param ResultTOInstruction, contain all the result can be replace by a constant
     * @param ResultTOConstant, constain all the result can be replace by a instruction
     */
    public void CPoptimizeRecursion(DominatorTreeNode root,HashMap<Result,Integer>ResultTOInstruction,HashMap<Result,Integer>ResultTOConstant)
    {
        if(root == null)
            return;

        if(root.block.getPhiFuncs()!=null){
            for(Map.Entry<Integer, Instruction> entry : root.block.getPhiFuncs().entrySet()){
                Result x=new Result();
                x.buildResult(Result.ResultType.variable,entry.getKey());
                x.setSSAVersion(entry.getValue().getInstructionPC());
                ResultTOInstruction.put(x,entry.getValue().getInstructionPC());
            }
        }
        for(Instruction ins : root.block.getInstructions())
        {
            if(ins.isReadInstruction())
            {
                int pc = ins.getInstructionPC();

                //next instruction is move instruction
                Instruction next = root.block.getNextIntruction(ins);
                if(next.getOp() == InstructionType.MOVE) {
                    Result readY = new Result();
                    ins.setLeftResult(readY);
                    readY.type = next.getRightResult().type;
                    readY.varIdent = next.getRightResult().varIdent;
                    readY.ssaVersion = new SSAValue(next.getRightResult().ssaVersion.getVersion() -1 );
                    Result y = next.getRightResult();
                    ResultTOInstruction.put(y, pc);

                    // mark next instr as deleted
                    next.deleted = true;
                }
            }
            else if(ins.isMoveConstant())
            {
                int constant = ins.getLeftResult().value;
                ResultTOConstant.put(ins.getRightResult(),constant);

                //mark instr as deleted
                ins.deleted = true;
            }
            else if(ins.isMoveInstruction())
            {
                int pc = ins.getLeftResult().instrRef;
                ResultTOInstruction.put(ins.getRightResult(),pc);

                //mark instr as deleted
                ins.deleted = true;
            }
            //the variable is in the right result
            else if(ins.isMoveVar())
            {
                Result left = ins.getLeftResult();
                Result right = ins.getRightResult();
                if(ResultTOConstant.containsKey(left)){
                    int constant = ResultTOConstant.get(left);
                    ResultTOConstant.put(right, constant);
                    //mark instr as deleted
                    ins.deleted = true;
                }else{
                    int pc = ResultTOInstruction.get(left);
                    ResultTOInstruction.put(right, pc);
                    // mark instr as deleted
                    ins.deleted = true;
                    ins.referenceInstrId = pc;
                }
            }
            else
            {
                Result left = ins.getLeftResult();
                Result right = ins.getRightResult();

                if(left != null && left.type == Result.ResultType.variable)
                {
                    if(ResultTOConstant.containsKey(left))
                    {
                        int constant = ResultTOConstant.get(left);
                        left.type = Result.ResultType.constant;
                        left.value = constant;
                    }
                    else {
                        for(Map.Entry<Result, Integer> entry : ResultTOInstruction.entrySet())
                        {
                            if(entry.getKey().varIdent==left.varIdent&&entry.getKey().ssaVersion.getVersion()==left.ssaVersion.getVersion()) {
                                int pc = entry.getValue();
                                left.type = Result.ResultType.instruction;
                                left.instrRef = pc;
                            }
                        }
                    }
                }else if(left != null && left.type == Result.ResultType.instruction) {
                        for(Map.Entry<Result, Integer> entry : ResultTOInstruction.entrySet())
                        {
                            if(entry.getKey().ssaVersion.getVersion()==left.instrRef) {
                                int pc = entry.getValue();
                                left.type = Result.ResultType.instruction;
                                left.instrRef = pc;
                            }
                        }
                }

                if(right != null && right.type == Result.ResultType.variable)
                {
                    if(ResultTOConstant.containsKey(right))
                    {
                        int constant = ResultTOConstant.get(right);
                        right.type = Result.ResultType.constant;
                        right.value = constant;
                    }
                    else if(ResultTOInstruction.containsKey(right))
                    {
                        int pc = ResultTOInstruction.get(right);
                        right.type = Result.ResultType.instruction;
                        right.instrRef = pc;
                    }
                }else if(right != null && right.type == Result.ResultType.instruction) {
                        for(Map.Entry<Result, Integer> entry : ResultTOInstruction.entrySet())
                        {
                            if(entry.getKey().ssaVersion.getVersion()==right.instrRef) {
                                int pc = entry.getValue();
                                right.type = Result.ResultType.instruction;
                                right.instrRef = pc;
                            }
                        }
                }
            }
        }

        if(root.block.getType() == BlockType.IF && root.block.getPreBlock().getElseBlock() != null)
        {
            for(Map.Entry<Integer, Instruction> entry : root.block.getJoinBlock().getPhiFunctionGenerator().getPhiInstructionMap().entrySet()){
                Instruction oldIns = entry.getValue();
                Result left = new Result(entry.getKey(),oldIns,true);

                int instrId;
                if(ResultTOConstant.containsKey(left)){
                    int constant = ResultTOConstant.get(left);
                    // put constant in result
                    left.type = Result.ResultType.constant;
                    left.value = constant;
                    oldIns.setLeftResult(left);
                }else if(ResultTOInstruction.containsKey(left)){
                    instrId = ResultTOInstruction.get(left);
                    // change result type to instr and assign instr#
                    left.type = Result.ResultType.instruction;
                    left.instrRef = instrId;
                    oldIns.leftRepresentedByInstrId = true;
                    oldIns.setLeftResult(left);
                }
            }
        }
        else if(root.block.getType() == BlockType.ELSE && !(root.block.getPreBlock().getType() == BlockType.WHILE_JOIN))
        {
            for(Map.Entry<Integer, Instruction> entry : root.block.getJoinBlock().getPhiFunctionGenerator().getPhiInstructionMap().entrySet()){
                Instruction oldIns = entry.getValue();
                Result right = new Result(entry.getKey(),oldIns,false);

                int instrId;
                if(ResultTOConstant.containsKey(right)){
                    int constant = ResultTOConstant.get(right);
                    // put constant in result
                    right.type = Result.ResultType.constant;
                    right.value = constant;
                    oldIns.setRightResult(right);
                }else if(ResultTOInstruction.containsKey(right)){
                    instrId = ResultTOInstruction.get(right);
                    // change result type to instr and assign instr#
                    right.type = Result.ResultType.instruction;
                    right.instrRef = instrId;
                    oldIns.rightRepresentedByInstrId = true;
                    oldIns.setRightResult(right);
                }
            }
        }
        else if(root.block.getBackBlock()!= null)
        {
            for(Map.Entry<Integer, Instruction> entry : root.block.getBackBlock().getPhiFunctionGenerator().getPhiInstructionMap().entrySet()){
                Instruction oldIns = entry.getValue();
                Result right = new Result(entry.getKey(),oldIns,false);

                int instrId;
                if(ResultTOConstant.containsKey(right)){
                    int constant = ResultTOConstant.get(right);
                    // put constant in result
                    right.type = Result.ResultType.constant;
                    right.value = constant;
                    oldIns.setRightResult(right);
                }else if(ResultTOInstruction.containsKey(right)){
                    instrId = ResultTOInstruction.get(right);
                    // change result type to instr and assign instr#
                    right.type = Result.ResultType.instruction;
                    right.instrRef = instrId;
                    oldIns.rightRepresentedByInstrId = true;
                    oldIns.setRightResult(right);
                }
            }
        }else if(root.block.getFollowBlock()!=null&&root.block.getFollowBlock().getType()==BlockType.WHILE_JOIN){
            for(Map.Entry<Integer, Instruction> entry : root.block.getFollowBlock().getPhiFunctionGenerator().getPhiInstructionMap().entrySet()){
                Instruction oldIns = entry.getValue();
                Result left = new Result(entry.getKey(),oldIns,true);

                int instrId;
                if(ResultTOConstant.containsKey(left)){
                    int constant = ResultTOConstant.get(left);
                    // put constant in result
                    left.type = Result.ResultType.constant;
                    left.value = constant;
                    oldIns.setLeftResult(left);
                }else if(ResultTOInstruction.containsKey(left)){
                    instrId = ResultTOInstruction.get(left);
                    // change result type to instr and assign instr#
                    left.type = Result.ResultType.instruction;
                    left.instrRef = instrId;
                    oldIns.leftRepresentedByInstrId = true;
                    oldIns.setLeftResult(left);
                }
            }
        }
        else if(root.block.getJoinBlock() != null && root.block.getJoinBlock().getType() == BlockType.IF_JOIN && root.block.getElseBlock() == null) {
            for (Map.Entry<Integer, Instruction> entry : root.block.getJoinBlock().getPhiFunctionGenerator().getPhiInstructionMap().entrySet()) {
                Instruction oldIns = entry.getValue();
                Result left = new Result(entry.getKey(),oldIns,true);

                int instrId;
                if(ResultTOConstant.containsKey(left)){
                    int constant = ResultTOConstant.get(left);
                    // put constant in result
                    left.type = Result.ResultType.constant;
                    left.value = constant;
                    oldIns.setLeftResult(left);
                }else if(ResultTOInstruction.containsKey(left)){
                    instrId = ResultTOInstruction.get(left);
                    // change result type to instr and assign instr#
                    left.type = Result.ResultType.instruction;
                    left.instrRef = instrId;
                    oldIns.leftRepresentedByInstrId = true;
                    oldIns.setLeftResult(left);
                }

                Result right = new Result(entry.getKey(), oldIns, false);
                if (ResultTOConstant.containsKey(right)) {
                    int constant = ResultTOConstant.get(right);
                    // put constant in result
                    right.type = Result.ResultType.constant;
                    right.value = constant;
                    oldIns.setRightResult(right);
                } else if (ResultTOInstruction.containsKey(right)) {
                    instrId = ResultTOInstruction.get(right);
                    // change result type to instr and assign instr#
                    right.type = Result.ResultType.instruction;
                    right.instrRef = instrId;
                    oldIns.rightRepresentedByInstrId = true;
                    oldIns.setRightResult(right);
                }
            }
        }

        for(DominatorTreeNode child : root.children){
            CPoptimizeRecursion(child, new HashMap<Result, Integer>(ResultTOInstruction),
                    new HashMap<Result, Integer>(ResultTOConstant));
        }
    }
}
