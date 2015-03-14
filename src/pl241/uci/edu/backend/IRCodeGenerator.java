package pl241.uci.edu.backend;

import pl241.uci.edu.cfg.ControlFlowGraph;
import pl241.uci.edu.cfg.VariableTable;
import pl241.uci.edu.frontend.Token;
import pl241.uci.edu.ir.BasicBlock;
import pl241.uci.edu.ir.FunctionDecl;
import pl241.uci.edu.middleend.Instruction;
import pl241.uci.edu.middleend.InstructionType;
import pl241.uci.edu.middleend.Result;
import pl241.uci.edu.optimizer.RegisterAllocation;

import java.util.HashMap;

/*
Date:2015/03/05
This class is used to generate the intermediate representation code, which is in the form of Instruction with pre-SSA result as operand.
 */
public class IRCodeGenerator {

    private CodeTable codeTable;

    public IRCodeGenerator() {
        this.codeTable = new CodeTable();
    }

    public void generateArithmeticIC(BasicBlock curBlock,Token curToken,Result x, Result y){
        if(x.type== Result.ResultType.constant&&y.type== Result.ResultType.constant){
            switch(codeTable.arithmeticCode.get(curToken)){
                case ADD:
                    x.value=x.value+y.value;
                    break;
                case SUB:
                    x.value=x.value-y.value;
                    break;
                case MUL:
                    x.value=x.value*y.value;
                    break;
                case DIV:
                    x.value=x.value/y.value;
                    break;
            }
        }
        else{
            //if y is not constant, deallocate y from registers.
            if(y.type==Result.ResultType.constant){
                curBlock.generateInstruction(codeTable.arithmeticCode.get(curToken),x,y);
            }
            else{
                curBlock.generateInstruction(codeTable.arithmeticCode.get(curToken),x,y);
            }
        }
    }

    public void generateCMPIC(BasicBlock curBlock,Result x,Result y){
        curBlock.generateInstruction(InstructionType.CMP,x,y);
    }

    public Instruction generateIOIC(BasicBlock curBlock,int ioType,Result x){
        return curBlock.generateInstruction(codeTable.predifinedFuncCode.get(ioType),x,null);
    }

    public void generateVarDeclIC(BasicBlock curBlock,Result x,FunctionDecl function){
        int varIdent=x.varIdent;
        x.setSSAVersion(Instruction.getPc());
        VariableTable.addSSAUseChain(x.varIdent, x.ssaVersion);
        if(function!=null){
            function.addLocalVariable(x.varIdent);
        }
        else{
            VariableTable.addGlobalVariable(x.varIdent);
        }
        Result zeroConstant=Result.buildConstant(0);
        curBlock.generateInstruction(InstructionType.MOVE,zeroConstant,x);
    }

    public void generateReturnIC(BasicBlock curBlock,Result x,FunctionDecl function)
    {
        Result curIns = new Result();
        curIns.buildResult(Result.ResultType.instruction,Instruction.getPc());
        curBlock.generateInstruction(InstructionType.MOVE,x,curIns);
        //set the function's return result
        function.setReturnInstr(curIns);
    }

    public void generateASSIGNMENTIC(BasicBlock curBlock,Result x,Result parameter)
    {
        curBlock.generateInstruction(InstructionType.MOVE,x,parameter);
    }

    public void assignmentIC(BasicBlock curBlock,BasicBlock joinBlock,Result variable,Result value){
        variable.setSSAVersion(Instruction.getPc());
        VariableTable.addSSAUseChain(variable.varIdent, variable.ssaVersion);
        curBlock.generateInstruction(InstructionType.MOVE, value, variable);
        if (joinBlock != null) {
            joinBlock.updatePhiFunction(variable.varIdent, variable.ssaVersion, curBlock.getType());
        }
    }

    public void returnStateIC(BasicBlock curBlock,Result variable,FunctionDecl function){
        Result returnInstr=new Result();
        returnInstr.buildResult(Result.ResultType.instruction,Instruction.getPc());
        curBlock.generateInstruction(InstructionType.MOVE,variable,returnInstr);
        function.setReturnInstr(returnInstr);
    }

    public void condNegBraFwd(BasicBlock curBlock,Result relation){
        relation.fixuplocation=Instruction.getPc();
        curBlock.generateInstruction(codeTable.branchCode.get(relation.relOp),relation,Result.buildBranch(null));
    }

    public void unCondBraFwd(BasicBlock curBlock, Result follow) {
        Result branch = Result.buildBranch(null);
        branch.fixuplocation = follow.fixuplocation;
        curBlock.generateInstruction(InstructionType.BRA, null, branch);
        follow.fixuplocation = Instruction.getPc() - 1;
    }


    /**
     * This is used to fix the branch block of a instruction.
     * @param pc, the pc of a instruction.
     * @param referenceBlock, new branch block of a instruction.
     *                        As only the second operand of the instruction can indicate the branch block,
     *                        we only fix the right result.
     */
    public void fix(int pc,BasicBlock referenceBlock)
    {
        ControlFlowGraph.getInstruction(pc).getRightResult().branchBlock = referenceBlock;
    }

    public void fixAll(int pc, BasicBlock referenceBlock) {
        while (pc != 0) {
            int next = ControlFlowGraph.getInstruction(pc).getRightResult().fixuplocation;
            fix(pc, referenceBlock);
            pc = next;
        }
    }

    private void Error(String msg)
    {
        System.out.println("IRCodeGenerator Error! " + msg);
    }
}
