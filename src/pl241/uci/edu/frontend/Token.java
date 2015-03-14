package pl241.uci.edu.frontend;

public enum Token {

    /** keyword **/
    LET, CALL, IF, THEN, ELSE, FI,WHILE, DO, OD, RETURN,
    VAR, ARRAY, FUNCTION, PROCEDURE, MAIN,

    /** designator **/
    BECOMETO, // <-

    /** Operator **/
    PLUS, MINUS, TIMES, DIVIDE, // +,-,*,/

    /** Comparison **/
    EQL, NEQ, LSS, GRE, LEQ, GEQ, // ==, !=, <, >, <=, >=

    /** Punctuation **/
    PERIOD, COMMA, SEMICOMA, COLON, // . , ; :

    /** Block **/
    OPEN_PARENTHESIS, CLOSE_PARENTHESIS, // (, )
    OPEN_BRACKET, CLOSE_BRACKET, // [, ]
    OPEN_BRACE, CLOSE_BRACE, // {, }

    /** Others **/
    NUMBER, // 0 ~ 9
    IDENTIFIER, // identifier
    EOF, // End of file
    ERROR;

    public static Token buildToken(String s){
        switch(s){
            case "let":
                return LET;
            case "call":
                return CALL;
            case "if":
                return IF;
            case "then":
                return THEN;
            case "else":
                return ELSE;
            case "fi":
                return FI;
            case "while":
                return WHILE;
            case "do":
                return DO;
            case "od":
                return OD;
            case "return":
                return RETURN;
            case "var":
                return VAR;
            case "array":
                return ARRAY;
            case "function":
                return FUNCTION;
            case "procedure":
                return PROCEDURE;
            case "main":
                return MAIN;
            default:
                return null;
        }
    }

    public static String getTokenName(Token token)
    {
        switch (token){
            case LET:
                return "let";
            case CALL:
                return "call";
            case IF:
                return "if";
            case THEN:
                return "then";
            case ELSE:
                return "else";
            case FI:
                return "fi";
            case WHILE:
                return "while";
            case DO:
                return "do";
            case OD:
                return "od";
            case RETURN:
                return "return";
            case VAR:
                return "var";
            case ARRAY:
                return "array";
            case FUNCTION:
                return "function";
            case PROCEDURE:
                return "procedure";
            case MAIN:
                return "main";
            default:
                return null;
        }
    }
}
