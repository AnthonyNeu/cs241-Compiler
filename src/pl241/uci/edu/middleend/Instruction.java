package pl241.uci.edu.middleend;

/*
Date:2015/03/03
This is used to store the instruction in pre-SSA form and SSA form. We use Result in pre-SSA form and SSAValue in SSA form.
 */

import pl241.uci.edu.cfg.ControlFlowGraph;
import pl241.uci.edu.frontend.Scanner;

import java.util.ArrayList;

public class Instruction {
    //there are three states of instructions
    //ELIMINATED for the CSE and copy propagation
    //PHI for the phi instruction
    //NORMAL for the other instructions
    public enum State{
        REPLACE,
        PHI,
        NORMAL
    }

    //store basic arithmetic instruction type
    private ArrayList<InstructionType> ArithOp;

    //store branch instruction type
    private ArrayList<InstructionType>BranchOp;

    //used to count the instruction
    private static int pc = 0;

    //the # of this instruction
    private int instructionPC;

    //operator of instruction
    private InstructionType op;

    //state of the instruction
    private State state;

    //operand of pre-SSA
    private Result result1;

    private Result result2;

    //operand of SSA
    private SSAValue s1;

    private SSAValue s2;

    //TODO:what is this for and why we need it?
    private String variableName;

    //TODO:what is this for and why we need it?
    private boolean leftLatestUpdated;

    //Instruction can be deleted by CSE,CP or DCE
    public boolean deleted = false;

    public boolean leftRepresentedByInstrId = false;

    public boolean rightRepresentedByInstrId = false;

    //the reference instruction's id
    public int referenceInstrId;

    private void initializedOpList()
    {
        ArithOp = new ArrayList<InstructionType>();
        ArithOp.add(InstructionType.ADD);
        ArithOp.add(InstructionType.DIV);
        ArithOp.add(InstructionType.MUL);
        ArithOp.add(InstructionType.SUB);
        ArithOp.add(InstructionType.CMP);

        BranchOp = new ArrayList<InstructionType>();
        BranchOp.add(InstructionType.BNE);
        BranchOp.add(InstructionType.BEQ);
        BranchOp.add(InstructionType.BLT);
        BranchOp.add(InstructionType.BLE);
        BranchOp.add(InstructionType.BLT);
        BranchOp.add(InstructionType.BLE);
    }

    public Instruction(InstructionType op,Result result1, Result result2)
    {
        initializedOpList();
        this.op = op;
        this.result1 = result1;
        this.result2 = result2;
        this.instructionPC = pc;
        pc++;
        this.state = State.NORMAL;

        //TODO:add instruction to control flow graph
        ControlFlowGraph.allInstructions.add(this);
    }

    public Instruction(InstructionType op,int varIdent, SSAValue s1, SSAValue s2)
    {
        initializedOpList();
        this.op = op;
        this.variableName= Scanner.ident.get(varIdent);
        this.s1 = s1;
        this.s2 = s2;
        this.instructionPC = pc;
        pc++;
        this.state = State.PHI;

        //TODO:add instruction to control flow graph
        ControlFlowGraph.allInstructions.add(this);
    }

    /**********************************get function**********************************/
    public String getVariableName()
    {
        return this.variableName;
    }

    public InstructionType getOp()
    {
        return this.op;
    }

    public static int getPc()
    {
        return pc;
    }

    public int getInstructionPC(){
        return this.instructionPC;
    }

    public int getReferenceInstrId(){
        return this.referenceInstrId;
    }

    public int getLeftAddress()
    {
        return this.result1.varIdent;
    }

    public int getRightAddress()
    {
        return this.result2.varIdent;
    }

    public Result getLeftResult()
    {
        return this.result1;
    }

    public Result getRightResult()
    {
        return this.result2;
    }

    public SSAValue getLeftSSA()
    {
        return this.s1;
    }

    public SSAValue getRightSSA(){return this.s2;}

    /**********************************set function**********************************/
    public void setVariableName(String variableName)
    {
        this.variableName = variableName;
    }

    public void setOp(InstructionType op)
    {
        this.op = op;
    }

    public void setPc(int newpc)
    {
        pc = newpc;
    }

    public void setInstructionPC(int instructionPC)
    {
        this.instructionPC = instructionPC;
    }

    public void setReferenceInstrId(int id)
    {
        this.referenceInstrId = id;
    }

    public void setLeftResult(Result left)
    {
        this.result1 = left;
    }

    public void setRightResult(Result right)
    {
        this.result2 = right;
    }

    public void setLeftLatestUpdated(boolean bool)
    {
        this.leftLatestUpdated = bool;
    }

    public void setRightSSA(SSAValue s2){this.s2 = s2;}

    public void setLeftSSA(SSAValue s1){this.s1 = s1;}

    public void setState(Instruction.State state) {this.state = state;}


    /**********************************boolean function**********************************/
    public boolean isLeftLatestUpdated()
    {
        return this.leftLatestUpdated;
    }

    public boolean isReadInstruction()
    {
        return this.op == InstructionType.READ;
    }

    public boolean isWriteInstruction()
    {
        return this.op == InstructionType.WRITE || this.op == InstructionType.WLN;
    }

    public boolean isMoveConstant()
    {
        return this.op == InstructionType.MOVE && this.result1.type == Result.ResultType.constant && this.result2.type == Result.ResultType.variable;
    }

    public boolean isMoveInstruction()
    {
        return this.op == InstructionType.MOVE && this.result1.type == Result.ResultType.instruction && this.result2.type == Result.ResultType.variable;
    }

    public boolean isMoveVar()
    {
        return this.op == InstructionType.MOVE && this.result1.type == Result.ResultType.variable && this.result2.type == Result.ResultType.variable;
    }

    public boolean isArithOrBranch()
    {
        return (!(result1 == null || result2 == null)) && ArithOp.contains(op) || BranchOp.contains(op) && result1.type == Result.ResultType.variable && result2.type == Result.ResultType.variable;
    }

    public boolean isExpressionOp(){
        return (op==InstructionType.NEG||op==InstructionType.ADD||op==InstructionType.SUB||op==InstructionType.MUL||op==InstructionType.DIV||
                op==InstructionType.ADDA||op==InstructionType.LOADADD||op==InstructionType.STOREADD);
    }

    public boolean isLoadInstruction()
    {
        return this.op == InstructionType.LOAD;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("");
        sb.append(instructionPC + ": ");

        if(this.state == State.REPLACE){
            sb.append(" (" + referenceInstrId + ")");
            return sb.toString();
        }

        sb.append(InstructionType.getInstructionName(this.op) + " ");
        if (this.state == State.NORMAL) {
            if (BranchOp.contains(op)) {
                sb.append(result2.toString());
            } else {
                sb.append(result1 != null ? result1.toString() + " " : "");
                sb.append(result2 != null ? result2.toString() : "");
            }
        } else {
            String var1="";
            String var2="";
            if (leftRepresentedByInstrId) {
                var1 = "(" + this.getLeftResult().instrRef + ")";
            } else if(result1!=null&&result1.type==Result.ResultType.constant){
                var1 = Integer.toString(result1.value);
            } else if(result1!=null&&result1.type==Result.ResultType.instruction){
                var1 = "(" + result1.instrRef + ")";
            } else{
                var1 = this.variableName + "_" + s1.toString();
            }
            if(rightRepresentedByInstrId){
                var2 = "(" + this.getRightResult().instrRef + ")";
            } else if(result2!=null&&result2.type==Result.ResultType.constant){
                var2 = Integer.toString(result2.value);
            } else if(result2!=null&&result2.type==Result.ResultType.instruction){
                var2 = "(" + result2.instrRef + ")";
            } else{
                var2 = this.variableName + "_" + s2.toString();
            }

            sb.append(variableName + "_" + instructionPC + " " + var1 + " " + var2);
        }
        if (deleted) {
            sb.append("(deleted)");
        }
        return sb.toString();
    }
}
