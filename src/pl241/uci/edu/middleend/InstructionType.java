package pl241.uci.edu.middleend;

/*
The operations encoded in instruction nodes consist of an operator and up to two operands.
neg x              unary minus
add x y            addition
sub x y            subtraction
mul x y            multiplication
div x y            division
cmp x y            comparison
adda xy            add two addresses x and y
load y             load from memory(address y)
store y x          store y to memory(address x)
move y x           assign x :=y
phi x x1 x2 ...    x:=Phi(x1,x2,x3,...)
end                end of program
bra y              branch to y
bne x y            branch to y on x not equal
beq x y            branch to y on x equal
ble x y            branch to y on x less or equal
blt x y            branch to y on x less
bge x y            branch to y on x greater or equal
bgt x y            branch to y on x greater
read
write x
writeNewLine
 */

public enum InstructionType {
    NEG,
    ADD,
    SUB,
    MUL,
    DIV,
    CMP,
    ADDA,
    LOAD,
    STORE,
    MOVE,
    PHI,
    END,
    BRA,
    BNE,
    BEQ,
    BLE,
    BLT,
    BGE,
    BGT,
    RETURN,
    LOADADD, //For arrays only
    STOREADD, //For arrays only
    READ,
    WRITE,
    WLN;


    public static String getInstructionName(InstructionType type)
    {
        switch (type)
        {
            case NEG:
                return "NEG";
            case ADD:
                return "ADD";
            case SUB:
                return "SUB";
            case MUL:
                return "MUL";
            case DIV:
                return "DIV";
            case CMP:
                return "CMP";
            case ADDA:
                return "ADDA";
            case LOAD:
                return "LOAD";
            case STORE:
                return "STORE";
            case MOVE:
                return "MOVE";
            case PHI:
                return "PHI";
            case END:
                return "END";
            case BRA:
                return "BRA";
            case BNE:
                return "BNE";
            case BEQ:
                return "BEQ";
            case BLE:
                return "BLE";
            case BLT:
                return "BLT";
            case BGE:
                return "BGE";
            case BGT:
                return "BGT";
            case RETURN:
                return "RETURN";
            case LOADADD:
                return "LOADADD";
            case STOREADD:
                return "STOREADD";
            case READ:
                return "READ";
            case WRITE:
                return "WRITE";
            case WLN:
                return "WLN";
            default:
                return null;
        }
    }
}
