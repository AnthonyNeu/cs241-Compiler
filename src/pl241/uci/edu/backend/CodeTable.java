package pl241.uci.edu.backend;

import pl241.uci.edu.middleend.InstructionType;
import pl241.uci.edu.frontend.Token;
import java.util.HashMap;

/*
This is used to store the hash map from the IR code to DLX code.
 */
public class CodeTable {
    public final int InputNum = 0;

    public final int OutputNum = 1;

    public final int OutputNewLine = 2;

    public HashMap<Token,InstructionType> arithmeticCode;

    public HashMap<Token,InstructionType> branchCode;

    public HashMap<Integer,InstructionType> predifinedFuncCode;

    public CodeTable()
    {
        arithmeticCode = new HashMap<Token,InstructionType>();
        branchCode = new HashMap<Token,InstructionType>();
        predifinedFuncCode = new HashMap<Integer,InstructionType>();

        arithmeticCode.put(Token.PLUS, InstructionType.ADD);
        arithmeticCode.put(Token.MINUS, InstructionType.SUB);
        arithmeticCode.put(Token.TIMES, InstructionType.MUL);
        arithmeticCode.put(Token.DIVIDE, InstructionType.DIV);

        branchCode.put(Token.EQL, InstructionType.BNE);
        branchCode.put(Token.NEQ, InstructionType.BEQ);
        branchCode.put(Token.LSS, InstructionType.BGE);
        branchCode.put(Token.LEQ, InstructionType.BGT);
        branchCode.put(Token.GRE, InstructionType.BLE);
        branchCode.put(Token.GEQ, InstructionType.BLT);

        predifinedFuncCode.put(InputNum, InstructionType.READ);
        predifinedFuncCode.put(OutputNum, InstructionType.WRITE);
        predifinedFuncCode.put(OutputNewLine, InstructionType.WLN);
    }
}
