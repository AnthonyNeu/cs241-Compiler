package pl241.uci.edu.middleend;

import pl241.uci.edu.frontend.Scanner;
import pl241.uci.edu.ir.BasicBlock;
import pl241.uci.edu.frontend.Token;
import java.util.ArrayList;

/*
Data:2015/03/02
This is for the intermediate representation and store the variable in pre-SSA form.
 */
public class Result implements Comparable<Result>{
    public enum ResultType{
        constant, variable, register, condition, branch, instruction;
    }
    public ResultType type;//result type
    public int value;//value if constant
    public int varIdent;//address if variable
    public SSAValue ssaVersion;//ssa version if variable
    public int regno;//register number if register
    public int fixuplocation;
    public Token relOp;
    public BasicBlock branchBlock;
    public int instrRef;
    public boolean isMove = true;

    //use in array
    public boolean isArray = false;
    public boolean isArrayDesignator = false;
    public static int arrayAddressCounter = 0;
    public int arrayAddress;


    //for the define of a array
    public ArrayList<Result> arrayDimension = new ArrayList<Result>();

    //for the designator which is a array
    public ArrayList<Result> designatorDimension = new ArrayList<Result>();

    public Result(){

    }

    public Result(Result result){
        this.type=result.type;
        this.value=result.value;
        this.varIdent=result.varIdent;
        this.ssaVersion=result.ssaVersion;
        this.regno=result.regno;
        this.fixuplocation=result.fixuplocation;
        this.relOp=result.relOp;
        this.branchBlock=result.branchBlock;
        this.instrRef=result.instrRef;
        this.isMove = result.isMove;
    }

    public Result(int varIdent,Instruction ins,boolean isLeft)
    {
        this.type = ResultType.variable;
        if(isLeft)
        {
            this.varIdent = varIdent;
            this.ssaVersion = ins.getLeftSSA();
        }
        else
        {
            this.varIdent = varIdent;
            this.ssaVersion = ins.getRightSSA();
        }
    }

    public void buildResult(ResultType type,int inputValue){
        switch(type) {
            case constant:
                this.type = type;
                this.value = inputValue;
                break;
            case variable:
                this.type = type;
                this.varIdent = inputValue;
                break;
            case register:
                this.type = type;
                this.regno = inputValue;
                break;
            case instruction:
                this.type = type;
                this.instrRef = inputValue;
                break;
            default:
                break;
        }
    }

    public void setSSAVersion(int inputSSAVersion){
        this.ssaVersion=new SSAValue(inputSSAVersion);
    }

    public static Result buildBranch(BasicBlock branchBlock){
        Result result=new Result();
        result.type=ResultType.branch;
        result.branchBlock=branchBlock;
        return result;
    }

    public static Result buildConstant(int value){
        Result result=new Result();
        result.type=ResultType.constant;
        result.value=value;
        return result;
    }

    public Result deepClone(Result r)
    {
        return new Result(r);
    }

    @Override
    public int compareTo(Result other) {
        if(this.type != ResultType.variable || other.type != ResultType.variable)
            try {
                throw new Exception("Only can compare var result!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        if(this.varIdent== other.varIdent && this.ssaVersion == other.ssaVersion)
            return 0;
        return -1;
    }

    @Override
    public int hashCode() {
        return this.varIdent * 17 + this.ssaVersion.hashCode() * 31;
    }

    public boolean equals(Object other){
        Result other2 = (Result)other;
        if(this.type != ResultType.variable || other2.type != ResultType.variable)
            try {
                throw new Exception("Only can compare var result!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        if(this.varIdent == other2.varIdent && this.ssaVersion == other2.ssaVersion)
            return true;
        return false;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder("");
        switch(type){
            case constant:
                sb.append(value);
                break;
            case variable:
                sb.append(Scanner.ident.get(varIdent) + "_" + ssaVersion.getVersion());
                break;
            case register:
                sb.append("r" + regno);
                break;
            case condition:
                sb.append(fixuplocation);
                break;
            case branch:
                sb.append( branchBlock!= null ? "[" + branchBlock.getId() + "]": "-1");
                break;
            case instruction:
                sb.append("(" + instrRef + ")");
                break;
            default:
                return "";
        }
        return sb.toString();
    }

    public void setArrayDimension(ArrayList<Result> r)
    {
        this.arrayDimension = r;
    }

    public static void updateArrayAddressCounter(int length)
    {
        arrayAddressCounter  = arrayAddressCounter + length;
    }

    public void setArrayAddress(int address)
    {
        this.arrayAddress = address;
    }

    public boolean isIdent(int ident, int oldSSA){
        return this.type == ResultType.variable && this.varIdent == ident && this.ssaVersion.getVersion() == oldSSA;
    }
}
