package pl241.uci.edu.ir;

import java.util.ArrayList;
import java.util.HashMap;

import pl241.uci.edu.middleend.Result;
import pl241.uci.edu.ir.BlockType;

public class FunctionDecl {
    //this is the id of the function name, use to look up the function name in the hash table in Scanner
    private int funcID;

    private BasicBlock firstFuncBlock;

    //id of the local variable
    private ArrayList<Integer> localVariables;

    //id of the global variable
    private ArrayList<Integer> globalVariables;

    //store the definition of array definition
    public HashMap<Integer,Result> localArray;

    //function parameters
    private ArrayList<Result> parameters;

    //return result of the function
    private Result returnInstr;

    public FunctionDecl(int ID){
        this.funcID= ID;
        firstFuncBlock = new BasicBlock(BlockType.NORMAL);
        localVariables = new ArrayList<Integer>();
        globalVariables = new ArrayList<Integer>();
        parameters = new ArrayList<Result>();
        returnInstr = new Result();
        localArray = new HashMap<>();
    }

    public void addLocalVariable(int localVariable){
        getLocalVariables().add(localVariable);
    }

    public void addGlobalVariable(int globalVariable){
        getGlobalVariables().add(globalVariable);
    }

    public int getFuncID() {
        return this.funcID;
    }

    public void setFuncID(int ID) {
        this.funcID = ID;
    }

    public BasicBlock getFirstFuncBlock() {
        return firstFuncBlock;
    }

    public void setFirstFuncBlock(BasicBlock firstFuncBlock) {
        this.firstFuncBlock = firstFuncBlock;
    }

    public Integer getLocalVariable(int index)
    {
        if(index < this.localVariables.size())
            return this.localVariables.get(index);
        else
        {
            this.Error("Cannot find local variable! Index out of bound!");
            return null;
        }
    }

    public ArrayList<Integer> getLocalVariables() {
        return localVariables;
    }

    public void setLocalVariables(ArrayList<Integer> localVariables) {
        this.localVariables = localVariables;
    }

    public Integer getGlobalVariable(int index)
    {
        if(index < this.globalVariables.size())
            return this.globalVariables.get(index);
        else
        {
            this.Error("Cannot find global variable! Index out of bound!");
            return null;
        }
    }
    
    public ArrayList<Integer> getGlobalVariables() {
        return globalVariables;
    }

    public void setGlobalVariables(ArrayList<Integer> globalVariables) {
        this.globalVariables = globalVariables;
    }

    public ArrayList<Result> getParameters() {
        return parameters;
    }

    public void setParameters(ArrayList<Result> parameters) {
        this.parameters = parameters;
    }

    public Result getReturnInstr() {
        return returnInstr;
    }

    public void setReturnInstr(Result returnInstr) {
        this.returnInstr = returnInstr;
    }

    private void Error(String msg)
    {
        System.out.println("FunctionDecl Error! "+msg);
    }
}
