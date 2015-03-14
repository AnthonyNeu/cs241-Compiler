package pl241.uci.edu.frontend;

import java.io.IOException;
import java.util.*;

import pl241.uci.edu.cfg.ControlFlowGraph;
import pl241.uci.edu.cfg.DelUseChain;
import pl241.uci.edu.backend.IRCodeGenerator;
import pl241.uci.edu.cfg.VariableTable;
import pl241.uci.edu.ir.BasicBlock;
import pl241.uci.edu.ir.BlockType;
import pl241.uci.edu.ir.FunctionDecl;
import pl241.uci.edu.middleend.Instruction;
import pl241.uci.edu.middleend.InstructionType;
import pl241.uci.edu.middleend.Result;
import pl241.uci.edu.optimizer.CP;
import pl241.uci.edu.optimizer.CSE;
import pl241.uci.edu.cfg.VCGGraphGenerator;
import pl241.uci.edu.cfg.DominatorTreeGenerator;
import pl241.uci.edu.optimizer.RegisterAllocation;
//import sun.org.mozilla.javascript.internal.Function;
import pl241.uci.edu.middleend.SSAValue;

/*
Date:2015/01/26
This class is used to do the syntax analysis of the program.
 */

/*
Implementation of parser based on the EBNF of PL241:
    letter = “a” | “b” | … | “z”.
    digit = “0” | “1” | … | “9”.
    relOp = “==“ | “!=“ | “<“ | “<=“ | “>“ | “>=“.
    ident = letter {letter | digit}.
    number = digit {digit}.
    designator = ident{ "[" expression "]" }.
    factor = designator | number | “(“ expression “)” | funcCall .
    term = factor { (“*” | “/”) factor}.
    expression = term {(“+” | “-”) term}.
    relation = expression relOp expression .
    assignment = “let” designator “<-” expression.
    funcCall = “call” ident [ “(“ [expression { “,” expression } ] “)” ].
    ifStatement = “if” relation “then” statSequence [ “else” statSequence ] “fi”.
    whileStatement = “while” relation “do” StatSequence “od”.
    returnStatement = “return” [ expression ] .
    statement = assignment | funcCall | ifStatement | whileStatement | returnStatement.
    statSequence = statement { “;” statement }.
    typeDecl = “var” | “array” “[“ number “]” { “[“ number “]” }.
    varDecl = typeDecl ident { “,” ident } “;” .
    funcDecl = (“function” | “procedure”) ident [formalParam] “;” funcBody “;” .
    formalParam = “(“ [ident { “,” ident }] “)” .
    funcBody = { varDecl } “{” [ statSequence ] “}”.
    computation = “main” { varDecl } { funcDecl } “{” statSequence “}” “.” .
 */

public class Parser {
    private Scanner scanner;

    private Token curToken;

    private IRCodeGenerator irCodeGenerator;

    private Result ref;



    public Parser (String path)throws IOException{
        scanner=new Scanner(path);
        irCodeGenerator=new IRCodeGenerator();
        ref = new Result();
        new ControlFlowGraph();
        new VariableTable();
    }

    public void parser() throws Throwable
    {
        scanner.startScanner();
        Next();
        computation();
        scanner.closeScanner();
    }

    //designator = ident{ "[" expression "]" }.
    private Result designator(BasicBlock curBlock,Stack<BasicBlock>joinBlocks,FunctionDecl function) throws IOException,Error{
        Result designator=new Result();
        ArrayList<Result> dimensions=new ArrayList<Result>();
        if(curToken==Token.IDENTIFIER){
            designator.buildResult(Result.ResultType.variable,scanner.getVarIdent());
            Next();
            while(curToken==Token.OPEN_BRACKET){
                Next();
                dimensions.add(expression(curBlock,joinBlocks,function));
                if(curToken==Token.CLOSE_BRACKET){
                    Next();
                }
                else{
                    Error("designator expect ']'");
                }
            }
        }
        else{
            Error("designator expect an identifier");
        }
        if(dimensions.isEmpty()){
            return designator;
        }
        else{
            designator.isArrayDesignator = true;
            designator.designatorDimension = dimensions;

            Result arr = null;
            if(function != null&&function.localArray.containsKey(designator.varIdent)) {
                arr = function.localArray.get(designator.varIdent);
            }
            else
                arr = VariableTable.ArrayDefinition.get(designator.varIdent);

            //calculate the instruction of array address
            Instruction ins = calculateArray(curBlock, arr, dimensions);

            //generate LOADADD
            this.ref.buildResult(Result.ResultType.instruction,ins.getInstructionPC());
            curBlock.generateInstruction(InstructionType.LOADADD,this.ref,null);
            return designator;
        }
    }

    //factor = designator | number | “(“ expression “)” | funcCall .
    private Result factor(BasicBlock curBlock,Stack<BasicBlock>joinBlocks,FunctionDecl function) throws IOException,Error{
        Result factor=new Result();
        if(curToken==Token.IDENTIFIER){
            factor=designator(curBlock,joinBlocks,function);
            if(!factor.isArrayDesignator)
                factor.ssaVersion= VariableTable.getLatestVersion(factor.varIdent);
            else
            {
                Result ref1 = new Result();
                ref1.buildResult(Result.ResultType.instruction,this.ref.instrRef);
                return ref1;
            }
        }
        else if(curToken==Token.NUMBER){
            factor.buildResult(Result.ResultType.constant,scanner.getVal());
            Next();
        }
        else if(curToken==Token.OPEN_PARENTHESIS){
            Next();
            factor=expression(curBlock,joinBlocks,function);
            if(curToken==Token.CLOSE_PARENTHESIS){
                Next();
            }
            else{
                Error("factor expect ')'");
            }
        }
        else if(curToken==Token.CALL){
            factor=funcCall(curBlock,joinBlocks);
        }
        else{
            Error("invalid factor");
        }
        return factor;
    }

    //term = factor { (“*” | “/”) factor}.
    private Result term(BasicBlock curBlock,Stack<BasicBlock>joinBlocks,FunctionDecl function) throws IOException,Error{
        Result x=factor(curBlock,joinBlocks,function);
        while(curToken==Token.TIMES||curToken==Token.DIVIDE){
            Token operator=curToken;
            Next();
            x.isMove = false;
            Result y=factor(curBlock,joinBlocks,function);
            x.instrRef= Instruction.getPc();

            irCodeGenerator.generateArithmeticIC(curBlock,operator,x,y);
            ControlFlowGraph.delUseChain.updateDefUseChain(x,y);
        }
        return x;
    }

    //expression = term {(“+” | “-”) term}.
    private Result expression(BasicBlock curBlock,Stack<BasicBlock>joinBlocks,FunctionDecl function) throws IOException,Error{
        Result x=term(curBlock,joinBlocks,function);
        while(curToken==Token.PLUS||curToken==Token.MINUS){
            //we are not doing move,wo assign a expression
            x.isMove = false;
            Token operator=curToken;
            Next();
            Result y=term(curBlock,joinBlocks,function);
            x.instrRef=Instruction.getPc();
            //generate the instruction
            //irCodeGenerator.generateArithmeticIC(curBlock,operator,x,y);
            if(!y.isMove)
                y.type = Result.ResultType.instruction;
            irCodeGenerator.generateArithmeticIC(curBlock, operator, x, y);
            ControlFlowGraph.delUseChain.updateDefUseChain(x,y);
            x.instrRef = Instruction.getPc()-1;
        }
        if(!x.isMove)
            x.type = Result.ResultType.instruction;
        return x;
    }

    //relation = expression relOp expression .
    private Result relation(BasicBlock curBlock,Stack<BasicBlock>joinBlocks,FunctionDecl function) throws IOException,Error{
        Result relation=null;
        Result x=expression(curBlock,joinBlocks,function);
        if(curToken==Token.EQL||curToken==Token.NEQ||curToken==Token.LEQ||curToken==Token.LSS||curToken==Token.GEQ||curToken==Token.GRE){
            Token relOp=curToken;
            Next();
            Result y=expression(curBlock,joinBlocks,function);
            irCodeGenerator.generateCMPIC(curBlock,x,y);
            relation=new Result();
            relation.relOp=relOp;
            relation.type= Result.ResultType.condition;
            relation.fixuplocation=0;
        }
        else{
            Error("invalid relation expect a relation operator");
        }
        return relation;
    }

    //assignment = “let” designator “<-” expression.
    private void assignment(BasicBlock curBlock,Stack<BasicBlock> joinBlocks,FunctionDecl function) throws IOException,Error{
        if(curToken==Token.LET){
            Next();
            Result variable=designator(curBlock,joinBlocks,function);
            if(joinBlocks!=null&&joinBlocks.size()>0){
                if(!variable.isArrayDesignator)
                    joinBlocks.peek().createPhiFunction(variable.varIdent);
            }
            if(curToken==Token.BECOMETO){
                Next();
                Result value=expression(curBlock,joinBlocks,function);
                if(!variable.isArrayDesignator) {
                    if (joinBlocks != null) {
                        irCodeGenerator.assignmentIC(curBlock, joinBlocks.peek(), variable, value);
                    } else {
                        irCodeGenerator.assignmentIC(curBlock, null, variable, value);
                    }
                }
                else
                {
                    //generate SOTREADD
                    curBlock.generateInstruction(InstructionType.STOREADD,value,this.ref);
                }
            }
            else{
                Error("wrong assignment, expect '<-'!");
            }
        }
        else{
            Error("wrong assignment, expect 'let'!");
        }
    }

    //funcCall = “call” ident [ “(“ [expression { “,” expression } ] “)” ].
    private Result funcCall(BasicBlock curBlock,Stack<BasicBlock>joinBlocks)throws IOException{
        Result x = new Result();
        FunctionDecl func = null;
        if(curToken == Token.CALL)
        {
            Next();
            if(curToken == Token.IDENTIFIER) {
                //x is a function variable in funcCall
                x.buildResult(Result.ResultType.variable, scanner.getID());

                func = ControlFlowGraph.allFunctions.get(x.varIdent);
                Next();

                //index for the number of expression
                int index = 0;

                if (curToken == Token.OPEN_PARENTHESIS) {
                    Next();
                    Result y = new Result();
                    //find expression
                    if (isExpression(curToken)) {
                        y = expression(curBlock, joinBlocks,func);

                        //get the original definition of y
                        //don't need to do this for the definition of function
                        if (x.varIdent >= 3)
                            irCodeGenerator.generateASSIGNMENTIC(curBlock, y, func.getParameters().get(index++));

                        while (curToken == Token.COMMA) {
                            Next();
                            y = expression(curBlock, joinBlocks,func);
                            if (x.varIdent >= 3)
                                irCodeGenerator.generateASSIGNMENTIC(curBlock, y, func.getParameters().get(index++));
                        }
                    }

                    if (curToken == Token.CLOSE_PARENTHESIS)
                        Next();
                    else
                        Error("Expect ) in funcCall!");

                    //branch to function
                    if (x.varIdent == 0) {
                        //need to read
                        Instruction ins = irCodeGenerator.generateIOIC(curBlock, x.varIdent, null);
                        Result returnRE = new Result();
                        returnRE.buildResult(Result.ResultType.instruction, ins.getInstructionPC());
                        return returnRE;
                    } else if (x.varIdent < 3) {
                        irCodeGenerator.generateIOIC(curBlock, x.varIdent, x.varIdent == 0? null:y);
                    } else {
                        Result branch = Result.buildBranch(ControlFlowGraph.allFunctions.get(x.varIdent).getFirstFuncBlock());
                    }
                }
                else
                {
                    //for function without parameters
                    //branch to function
                    if (x.varIdent == 0) {
                        //need to read
                        Instruction ins = irCodeGenerator.generateIOIC(curBlock, x.varIdent, null);
                        Result returnRE = new Result();
                        returnRE.buildResult(Result.ResultType.instruction, ins.getInstructionPC());
                        return returnRE;
                    } else if (x.varIdent < 3) {
                        irCodeGenerator.generateIOIC(curBlock, x.varIdent, null);
                    } else {
                        Result branch = Result.buildBranch(ControlFlowGraph.allFunctions.get(x.varIdent).getFirstFuncBlock());
                    }
                }
            }
            else
                Error("Expect IDENT in funcCall");
        }
        else
            Error("Expect CALL token in funcCall!");
        return x.varIdent<3 ? null:func.getReturnInstr();
    }

    //ifStatement = “if” relation “then” statSequence [ “else” statSequence ] “fi”.
    private BasicBlock ifStatement(BasicBlock curBlock,Stack<BasicBlock> joinBlocks,FunctionDecl function) throws IOException,Error{
        HashMap<Integer,ArrayList<SSAValue>> ssaUseChain=VariableTable.cloneSSAUseChain();
        if(curToken==Token.IF){
            Next();
            Result follow=new Result();
            follow.fixuplocation=0;
            Result relation=relation(curBlock,null,function);
            BasicBlock joinBlock=new BasicBlock(BlockType.IF_JOIN);
            curBlock.setJoinBlock(joinBlock);
            irCodeGenerator.condNegBraFwd(curBlock,relation);
            BasicBlock thenEndBlock=null;
            BasicBlock elseEndBlock=null;
            if(curToken==Token.THEN){
                Next();
                if(joinBlocks==null){
                    joinBlocks=new Stack<BasicBlock>();
                }
                joinBlocks.push(joinBlock);
                thenEndBlock=stateSequence(curBlock.createIfBlock(),joinBlocks,null);
                joinBlocks.pop();
                if(curToken==Token.ELSE){
                    VariableTable.setSSAUseChain(ssaUseChain);
                    irCodeGenerator.unCondBraFwd(thenEndBlock,follow);
                    Next();
                    BasicBlock elseBlock=curBlock.createElseBlock();
                    irCodeGenerator.fix(relation.fixuplocation,elseBlock);
                    joinBlocks.push(joinBlock);
                    elseEndBlock=stateSequence(elseBlock,joinBlocks,null);
                    joinBlocks.pop();
                }
                else{
                    irCodeGenerator.fix(relation.fixuplocation,joinBlock);
                }
                if(curToken==Token.FI){
                    Next();
                    irCodeGenerator.fixAll(follow.fixuplocation,joinBlock);
                    thenEndBlock.setJoinBlock(joinBlock);

                    if (elseEndBlock != null) {
                        elseEndBlock.setJoinBlock(joinBlock);
                    } else {
                        //curBlock.setElseBlock(joinBlock);
                    }
                    //updatePhiFuncsInJoinBlocks(curBlock, thenEndBlock, elseEndBlock, joinBlock, ssaUseChain);
                    //createPhiInIfJoinBlocks(curBlock, thenEndBlock, elseEndBlock, joinBlock, ssaUseChain);
                    VariableTable.setSSAUseChain(ssaUseChain);
                    updateReferenceForPhiVarInJoinBlock(joinBlock);
                    return joinBlock;
                }
                else{
                    Error("Expect fi in if statement!");
                }
            }
            else{
                Error("Expect then in if statement!");
            }
        }
        else{
            Error("Expect if in if statement!");
        }
        return null;
    }

    /*private void createPhiInIfJoinBlocks(BasicBlock curBlock, BasicBlock ifEndBlock, BasicBlock elseEndBlock, BasicBlock joinBlock, HashMap<Integer, ArrayList<SSAValue>> ssaUseChain)  throws Error {
        HashSet<Integer> phiVars = new HashSet<Integer>();
        HashSet<Integer> ifPhiVars = ifEndBlock.getPhiVars(curBlock);
        phiVars.addAll(ifPhiVars);
        HashSet<Integer> elsePhiVars = null;
        if (elseEndBlock != null) {
            elsePhiVars = elseEndBlock.getPhiVars(curBlock);
            phiVars.addAll(elsePhiVars);
        }
        HashSet<Integer> curPhiVars = joinBlock.getPhiVars();
        for (Integer phiVar : phiVars) {
            if (!curPhiVars.contains(phiVar)) {
                joinBlock.createPhiFunction(phiVar);
                if (ifPhiVars.contains(phiVar))
                    joinBlock.updatePhiFunction(phiVar, ifEndBlock.findLastSSA(phiVar, curBlock), ifEndBlock.getType());
                if (elseEndBlock != null && elsePhiVars.contains(phiVar))
                    joinBlock.updatePhiFunction(phiVar, elseEndBlock.findLastSSA(phiVar, curBlock), elseEndBlock.getType());
                else
                    joinBlock.updatePhiFunction(phiVar, ssaUseChain.get(phiVar).get(ssaUseChain.get(phiVar).size() - 1), elseEndBlock.getType());
            }
        }
    }

    private void updatePhiFuncsInJoinBlocks(BasicBlock curBlock, BasicBlock ifLastBlock, BasicBlock elseLastBlock, BasicBlock joinBlock, HashMap<Integer, ArrayList<SSAValue>> ssaMap) {
        HashMap<Integer, Instruction> leftPhiFuncs = ifLastBlock.getPhiFuncsFromStartBlock(curBlock);
        updateValuesInOuterPhiFunc(leftPhiFuncs, joinBlock, true);
        if (elseLastBlock != null) {
            HashMap<Integer, Instruction> rightPhiFuncs = elseLastBlock.getPhiFuncsFromStartBlock(curBlock);
            updateValuesInOuterPhiFunc(rightPhiFuncs, joinBlock, false);
        }
        else {
            for (Integer phiVar : joinBlock.getPhiFuncs().keySet()) {
                joinBlock.updatePhiFunction(phiVar, ssaMap.get(phiVar).get(ssaMap.get(phiVar).size() - 1), elseLastBlock.getType());
            }
        }
    }*/

    private void updateReferenceForPhiVarInJoinBlock(BasicBlock joinBlock) {
        for (Map.Entry<Integer, Instruction> entry : joinBlock.getPhiFuncs().entrySet()) {
            VariableTable.addSSAUseChain(entry.getKey(), entry.getValue().getInstructionPC());
            for (Instruction instr : joinBlock.getInstructions()) {
                if (instr.getLeftResult() != null && instr.getLeftResult().varIdent == entry.getKey()) {
                    instr.getLeftResult().setSSAVersion(entry.getValue().getInstructionPC());
                }
                if (instr.getRightResult() != null && instr.getRightResult().varIdent == entry.getKey()) {
                    instr.getRightResult().setSSAVersion(entry.getValue().getInstructionPC());
                }
            }
        }
    }

    private void updateValuesInOuterPhiFunc(HashMap<Integer, Instruction> phifuncs, BasicBlock outJoinBlock, boolean Left) {
        if (Left) {
            for (Map.Entry<Integer, Instruction> entry1 : phifuncs.entrySet()) {
                for (Map.Entry<Integer, Instruction> entry2 : outJoinBlock.getPhiFuncs().entrySet()) {
                    if (entry1.getKey() == entry2.getKey()) {
                        entry2.getValue().setLeftSSA(new SSAValue(entry1.getValue().getInstructionPC()));
                    }
                }
            }
        }
        else {
            for (Map.Entry<Integer, Instruction> entry1 : phifuncs.entrySet()) {
                for (Map.Entry<Integer, Instruction> entry2 : outJoinBlock.getPhiFuncs().entrySet()) {
                    if (entry1.getKey() == entry2.getKey()) {
                        entry2.getValue().setRightSSA(new SSAValue(entry1.getValue().getInstructionPC()));
                    }
                }
            }
        }
    }

    //whileStatement = “while” relation “do” StatSequence “od”.
    private BasicBlock whileStatement(BasicBlock curBlock, Stack<BasicBlock> joinBlocks,FunctionDecl function) throws IOException,Error{
        HashMap<Integer,ArrayList<SSAValue>> ssaUseChain=VariableTable.cloneSSAUseChain();
        if (curToken==Token.WHILE) {
            Next();
            BasicBlock joinBlock = new BasicBlock(BlockType.WHILE_JOIN);
            curBlock.setFollowBlock(joinBlock);
            curBlock = joinBlock;
            Result relation = relation(curBlock, null,function);
            irCodeGenerator.condNegBraFwd(curBlock, relation);
            BasicBlock doEndBlock = null;
            if (curToken== Token.DO) {
                Next();
                BasicBlock startBlock = curBlock.createDoBlock();
                if (joinBlocks == null) {
                    joinBlocks = new Stack<BasicBlock>();
                }
                joinBlocks.push(joinBlock);
                doEndBlock = stateSequence(startBlock, joinBlocks, null);
                doEndBlock.generateInstruction(InstructionType.BRA, null, Result.buildBranch(curBlock));
                updateReferenceForPhiVarInLoopBody(joinBlock, startBlock, doEndBlock);
                BasicBlock followBlock = curBlock.createElseBlock();
                irCodeGenerator.fix(relation.fixuplocation, followBlock);
                doEndBlock.setBackBlock(curBlock);
                joinBlocks.pop();
                for (BasicBlock jB : joinBlocks) {
                    updateValuesInOuterPhiFunc(joinBlock.getPhiFuncs(), jB, false);
                }
                createPhiInWhileJoinBlocks(curBlock, doEndBlock, joinBlock, ssaUseChain);
                VariableTable.setSSAUseChain(ssaUseChain);
                updateReferenceForPhiVarInJoinBlock(joinBlock);
                if (curToken == Token.OD) {
                    Next();
                    return followBlock;
                } else
                    Error("whileStatement expect 'od'");
            } else
                Error("whileStatement expect 'do'");
        } else {
            Error("whileStatement expect 'while'");
        }
        return null;
    }

    private void createPhiInWhileJoinBlocks(BasicBlock curBlock,BasicBlock doEndBlock, BasicBlock joinBlock,HashMap<Integer,ArrayList<SSAValue>> ssaUseChain){
        HashSet<Integer> phiVars = new HashSet<Integer>();
        phiVars.addAll(doEndBlock.getPhiVars(curBlock));
        HashSet<Integer> curPhiVars = joinBlock.getPhiVars();
        for (Integer phiVar : phiVars) {
            if (!curPhiVars.contains(phiVar)) {
                Instruction curInstr = joinBlock.createPhiFunction(phiVar);
                joinBlock.updatePhiFunction(phiVar, doEndBlock.findLastSSA(phiVar, curBlock), doEndBlock.getType());
                joinBlock.updatePhiFunction(phiVar, ssaUseChain.get(phiVar).get(ssaUseChain.get(phiVar).size() - 1),BlockType.IF );
                if (joinBlock.getType() == BlockType.WHILE_JOIN) {
                    doEndBlock.assignNewSSA(phiVar, ssaUseChain.get(phiVar).get(ssaUseChain.get(phiVar).size() - 1), new SSAValue(curInstr.getInstructionPC()), curBlock);
                }
            }
        }
    }

    public void updateReferenceForPhiVarInLoopBody(BasicBlock innerJoinBlock, BasicBlock startBlock, BasicBlock doLastBlock)  {
        for (Map.Entry<Integer, Instruction> entry : innerJoinBlock.getPhiFuncs().entrySet())
            innerJoinBlock.updateVarReferenceToPhi(entry.getKey(), entry.getValue().getLeftSSA().getVersion(), entry.getValue().getInstructionPC(), startBlock, doLastBlock);
    }

    //returnStatement = “return” [ expression ] .
    private Result returnStatement(BasicBlock curBlock,Stack<BasicBlock>joinBlocks,FunctionDecl function) throws IOException{
        Result x = new Result();
        if(curToken == Token.RETURN)
        {
            Next();
            if(isExpression(curToken))
                x = expression(curBlock,joinBlocks,function);
        }
        else
            Error("Expect RETURN in returnStatement!");
        return x;
    }

    //statement = assignment | funcCall | ifStatement | whileStatement | returnStatement.
    private BasicBlock statement(BasicBlock curBlock,Stack<BasicBlock> joinBlocks,FunctionDecl function) throws IOException,Error{
        if(curToken==Token.LET){
            assignment(curBlock,joinBlocks,function);
            return curBlock;
        }
        else if(curToken == Token.CALL)
        {
            funcCall(curBlock,joinBlocks);
            return curBlock;
        }
        else if(curToken == Token.IF)
        {
            return ifStatement(curBlock,joinBlocks,function);
        }
        else if(curToken == Token.WHILE)
        {
            return whileStatement(curBlock,joinBlocks,function);
        }
        else if(curToken == Token.RETURN)
        {
            Result x = returnStatement(curBlock,joinBlocks,function);
            irCodeGenerator.generateReturnIC(curBlock, x, function);
            return curBlock;
        }
        else{
            Error("invalid statement!");
            return null;
        }
    }

    //statSequence = statement { “;” statement }.
    private BasicBlock stateSequence(BasicBlock curBlock,Stack<BasicBlock> joinBlocks,FunctionDecl function) throws IOException,Error{
        BasicBlock nextBlock=statement(curBlock,joinBlocks,function);
        while(curToken==Token.SEMICOMA){
            Next();
            nextBlock=statement(nextBlock,joinBlocks,function);
        }
        return nextBlock;
    }

    //typeDecl = “var” | “array” “[“ number “]” { “[“ number “]” }.
    private ArrayList<Result> typeDecl() throws IOException{
        if(curToken == Token.VAR)
        {
            Next();
            return null;
        }
        else if(curToken == Token.ARRAY)
        {
            ArrayList<Result> returnRes = new ArrayList<>();
            Next();
            boolean hasDimension = false;
            while(curToken == Token.OPEN_BRACKET)
            {
                hasDimension = true;
                Next();
                Result x = new Result();
                if(curToken == Token.NUMBER)
                {
                    x.buildResult(Result.ResultType.constant,scanner.getVal());
                    returnRes.add(x);
                    Next();
                }
                else
                {
                    Error("Expect number in the dimension of ARRAY declaration!");
                }

                if(curToken == Token.CLOSE_BRACKET)
                {
                    Next();
                }
                else
                    Error("Expect ] in ARRAY declaration!");
            }

            if(!hasDimension)
                Error("Expect [] in ARRAY declaration!");
            else
                return returnRes;
        }
        else
        {
            Error("Expect VAR or ARRAY in typeDecl!");
        }
        return null;
    }

    //varDecl = typeDecl ident { “,” ident } “;” .
    private void varDecl(BasicBlock curBlock,FunctionDecl function) throws IOException{
        Result x = null;

        //call the typedel for the variable and array
        ArrayList <Result> r = typeDecl();

        if(this.curToken == Token.IDENTIFIER)
        {
            x = new Result();
            x.buildResult(Result.ResultType.variable,scanner.getID());

            //if x is a array, set the dimension
            if(r != null)
            {
                x.setArrayDimension(r);
                x.isArray = true;
                x.setArrayAddress(Result.arrayAddressCounter);

                int length = 1;
                //calculate the length of array
                for(int i = r.size() -1  ; i >=0;i--)
                    length = length  * r.get(i).value;
                Result.updateArrayAddressCounter(length);
            }

            //declare the variable
            declareVariable(curBlock,x,function);
            Next();
            while(curToken == Token.COMMA)
            {
                Next();
                if(this.curToken == Token.IDENTIFIER) {
                    x = new Result();
                    x.buildResult(Result.ResultType.variable, scanner.getID());

                    //if x is a array, set the dimension
                    if(r != null)
                    {
                        x.setArrayDimension(r);
                        x.isArray = true;
                        x.setArrayAddress(Result.arrayAddressCounter);

                        int length = 1;
                        //calculate the length of array
                        for(int i = r.size() -1  ; i >=0;i--)
                            length = length  * r.get(i).value;
                        Result.updateArrayAddressCounter(length);
                    }

                    //declare the variable
                    declareVariable(curBlock, x, function);
                    Next();
                }
                else
                {
                    Error("expect IDENT in variable declaration!");
                }
            }
            if(curToken == Token.SEMICOMA)
            {
                Next();
            }
            else
            {
                Error("Expect ;, in the end of variable declaration!");
            }
        }
        else
        {
            Error("expect IDENT in variable declaration!");
        }
    }

    private void declareVariable(BasicBlock curBlock,Result x, FunctionDecl function)
    {
        if(x.type != Result.ResultType.variable)
            this.Error("Variable declaration error! type should be variable! in " + scanner.getLineNumber());
        else if(!x.isArray)
        {
            int varIdent = x.varIdent;
            x.setSSAVersion(Instruction.getPc());
            VariableTable.addSSAUseChain(x.varIdent,x.ssaVersion);

            if(function!=null)
            {
                function.getLocalVariables().add(varIdent);
            }
            else
            {
                VariableTable.addGlobalVariable(varIdent);
            }
            //TODO:DO WE NEED TO DO THIS? WHERE TO PLACE LOAD INSTRUCTION?
            //curBlock.generateInstruction(InstructionType.LOAD,x,null);
            //we add a move at the end of every variable declaration
            curBlock.generateInstruction(InstructionType.MOVE, Result.buildConstant(0), x);
        }
        else
        {
            int varIdent = x.varIdent;
            x.setSSAVersion(Instruction.getPc());
            VariableTable.addSSAUseChain(x.varIdent,x.ssaVersion);

            if(function != null)
                function.localArray.put(x.varIdent,x);
            else
                VariableTable.ArrayDefinition.put(x.varIdent,x);
        }
    }

    //funcDecl = (“function” | “procedure”) ident [formalParam] “;” funcBody “;” .
    private void funcDecl() throws IOException{
        if(curToken == Token.FUNCTION || curToken == Token.PROCEDURE) {
            Next();
            if(curToken == Token.IDENTIFIER)
            {
                Result x = new Result();
                x.buildResult(Result.ResultType.variable,scanner.getID());
                //declare function
                FunctionDecl function = declareFunction(x);
                Next();
                if(curToken == Token.OPEN_PARENTHESIS)
                    formalParam(function);
                if(curToken == Token.SEMICOMA)
                {
                    Next();
                    funcBody(function);
                    if (curToken == Token.SEMICOMA)
                        Next();
                    else
                        Error("Expect ; after funcBody in funcDecl");
                }
                else
                    Error("Expect ; after IDENT in funcDecl");
            }
            else
                Error("Expect IDENT in funcDecl!");
        }
        else
            Error("Expect function or procedure name in funcDecl!");
    }

    private FunctionDecl declareFunction(Result x)
    {
        if(x.type != Result.ResultType.variable)
            Error("declareFunction error! type of x should be variable");
        else
        {
            int functionIdent = x.varIdent;

            if(!ControlFlowGraph.allFunctions.containsKey(functionIdent))
            {
                FunctionDecl func = new FunctionDecl(functionIdent);
                ControlFlowGraph.allFunctions.put(functionIdent,func);
                return func;
            }
            else
            {
                Error("Function name redefined!");
                return null;
            }
        }
        return null;
    }

    //formalParam = “(“ [ident { “,” ident }] “)” .
    private void formalParam(FunctionDecl function) throws IOException{
        Result x = null;
        if(curToken == Token.OPEN_PARENTHESIS)
        {
            Next();
            if(curToken == Token.IDENTIFIER)
            {
                x = new Result();
                x.buildResult(Result.ResultType.variable,scanner.getID());
                //declare variable
                declareVariable(function.getFirstFuncBlock(),x,function);
                //add variable to parameters
                function.getParameters().add(x);
                Next();
                while(curToken == Token.COMMA)
                {
                    Next();
                    if(curToken == Token.IDENTIFIER) {
                        x = new Result();
                        x.buildResult(Result.ResultType.variable, scanner.getID());
                        //declare variable
                        declareVariable(function.getFirstFuncBlock(), x, function);
                        //add variable to parameters
                        function.getParameters().add(x);
                        Next();
                    }
                    else
                        Error("Expect ident after ,in formalParam!");
                }
            }
            if(curToken == Token.CLOSE_PARENTHESIS)
                Next();
            else
                Error("Expect ) in formalParam!");
        }
        else
            Error("Expect ( in formalParam!");
    }

    //funcBody = { varDecl } “{” [ statSequence ] “}”.
    private void funcBody(FunctionDecl function) throws IOException {
        while(curToken == Token.VAR || curToken == Token.ARRAY)
            varDecl(function.getFirstFuncBlock(),function);

        if(curToken == Token.OPEN_BRACE)
        {
            Next();

            if(isStatement(curToken))
                stateSequence(function.getFirstFuncBlock(),null,function);

            if (curToken == Token.CLOSE_BRACE)
                Next();
            else
                Error("Expect } in funcBody!");
        }
        else
            Error("Expect { in funcBody!");
    }

    //computation = “main” { varDecl } { funcDecl } “{” statSequence “}” “.” .
    private void computation() throws IOException{
        if(curToken == Token.MAIN)
        {
            Next();

            while(curToken == Token.VAR || curToken == Token.ARRAY)
                varDecl(ControlFlowGraph.getFirstBlock(),null);

            while(curToken == Token.FUNCTION || curToken == Token.PROCEDURE)
                funcDecl();
            if(curToken == Token.OPEN_BRACE){
                Next();
                BasicBlock lastBlock = stateSequence(ControlFlowGraph.getFirstBlock(),null,null);
                if(curToken == Token.CLOSE_BRACE)
                {
                    Next();
                    if(curToken == Token.PERIOD)
                    {
                        //Next();
                        lastBlock.generateInstruction(InstructionType.END,null,null);
                    }
                    else
                        Error("Expect . in the end!");
                }
                else
                    Error("Expect } in computation!");
            }
            else
                Error("Expect { in computation!");
        }
        else
            Error("Expect MAIN in computation!");
    }

    private void Next() throws IOException{
        curToken=scanner.getNextToken();
    }

    private void Error(String msg)
    {
        System.out.println("Parser Error! " + msg);
    }

    private boolean isStatement(Token token)
    {
        return (token == Token.IF || token == Token.WHILE || token == Token.CALL || token == Token.RETURN || token == Token.LET);
    }

    private boolean isExpression(Token token)
    {
        return (token == Token.IDENTIFIER || token == Token.NUMBER || token == Token.OPEN_PARENTHESIS || token == Token.CALL);
    }

    private Instruction calculateArray(BasicBlock curBlock,Result array,ArrayList<Result> dimension)
    {
        if(dimension.size() > 1) {
            Instruction prev = null;
            //generate instruction for the load of array
            for (int i = 0; i < dimension.size() - 1; i++) {
                Result d = dimension.get(i);
                Instruction ins;
                if (d.type == Result.ResultType.constant) {
                    //generate MUL d.value array.dimension
                    ins = curBlock.generateInstruction(InstructionType.MUL, Result.buildConstant(d.value), Result.buildConstant(array.arrayDimension.get(i).value));
                }else if(d.type == Result.ResultType.variable)
                {
                    //generate MUL instruction.referenceID array.dimension
                    Result ref1 = new Result();
                    ref1.buildResult(Result.ResultType.instruction, d.ssaVersion.getVersion());
                    ins = curBlock.generateInstruction(InstructionType.ADD, ref1, Result.buildConstant(array.arrayDimension.get(i).value));
                }
                else {
                    //generate MUL instruction.referenceID array.dimension
                    Result ref = new Result();
                    ref.buildResult(Result.ResultType.instruction, d.instrRef);
                    ins = curBlock.generateInstruction(InstructionType.MUL, ref, Result.buildConstant(array.arrayDimension.get(i).value));
                }

                if (prev != null) {
                    //generate ADD ins.reference prev.reference
                    Result ref1 = new Result();
                    ref1.buildResult(Result.ResultType.instruction, ins.getInstructionPC());
                    Result ref2 = new Result();
                    ref2.buildResult(Result.ResultType.instruction, prev.getInstructionPC());
                    prev = curBlock.generateInstruction(InstructionType.ADD, ref1, ref2);
                }
                else
                    prev = ins;
            }

            //for the last dimension, a little different
            Result d = dimension.get(dimension.size()-1);

            Result ref = new Result();
            ref.buildResult(Result.ResultType.instruction, prev.getInstructionPC());

            Instruction ins;
            if (d.type == Result.ResultType.constant) {
                //generate ADD prev.reference constant
                ins = curBlock.generateInstruction(InstructionType.ADD, Result.buildConstant(d.value), ref);
            }else if(d.type == Result.ResultType.variable)
            {
                                 //generate ADD instruction.referenceID array.dimension
                Result ref1 = new Result();
                ref1.buildResult(Result.ResultType.instruction, d.ssaVersion.getVersion());
                ins = curBlock.generateInstruction(InstructionType.ADD, ref1, ref);
            } else
            {
                //generate ADD instruction.referenceID array.dimension
                Result ref1 = new Result();
                ref1.buildResult(Result.ResultType.instruction, d.instrRef);
                ins = curBlock.generateInstruction(InstructionType.ADD, ref1, ref);
            }

            ref.buildResult(Result.ResultType.instruction, ins.getInstructionPC());

            //generate ADDA ins.reference array.address
            return curBlock.generateInstruction(InstructionType.ADDA, ref, Result.buildConstant(array.arrayAddress));
        }
        else
        {
            Result d = dimension.get(0);

            Instruction adda;
            if (d.type == Result.ResultType.constant) {
                //generate ADDA prev.reference constant
                adda = curBlock.generateInstruction(InstructionType.ADDA, Result.buildConstant(d.value), Result.buildConstant(array.arrayAddress));
            }
            else if(d.type == Result.ResultType.variable) {
                                //generate ADDA instruction.referenceID array.dimension
                Result ref = new Result();
                ref.buildResult(Result.ResultType.instruction, d.ssaVersion.getVersion());
                adda = curBlock.generateInstruction(InstructionType.ADDA, ref, Result.buildConstant(array.arrayAddress));
            }else {
                //generate ADDA instruction.referenceID array.dimension
                Result ref = new Result();
                ref.buildResult(Result.ResultType.instruction, d.instrRef);
                adda = curBlock.generateInstruction(InstructionType.ADDA, ref, Result.buildConstant(array.arrayAddress));
            }
            return adda;
        }
    }

    public static void main(String []args) throws Throwable{
        String testname = "test031";
        Parser p = new Parser("src/test/"+testname +".txt");
        p.parser();
        ControlFlowGraph.printInstruction();

//        System.out.println(ControlFlowGraph.delUseChain.xDefUseChains);
//        System.out.println(ControlFlowGraph.delUseChain.yDefUseChains);

        VCGGraphGenerator vcg = new VCGGraphGenerator(testname);
        vcg.printCFG();

        DominatorTreeGenerator dt = new DominatorTreeGenerator();
        dt.buildDominatorTree(DominatorTreeGenerator.root);

        //vcg.printDominantTree();

        VCGGraphGenerator vcg_cp = new VCGGraphGenerator(testname + "_CP");
        CP cp = new CP();
        cp.CPoptimize(DominatorTreeGenerator.root);
        vcg_cp.printDominantTree();

        VCGGraphGenerator vcg_cse = new VCGGraphGenerator(testname + "_CSE");
        CSE cse = new CSE();
        cse.CSEoptimize(DominatorTreeGenerator.root);
        vcg_cse.printDominantTree();

        RegisterAllocation ra = new RegisterAllocation();
        //ra.allocate(DominatorTreeGenerator.root);
    }
}
