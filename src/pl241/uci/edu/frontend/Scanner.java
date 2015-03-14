package pl241.uci.edu.frontend;


import java.util.ArrayList;
import java.io.IOException;

/*
Data:2015/03/02
This class is for the scanner of the code.
 */
public class Scanner {
    private FileReader fReader;
    private Character curChar;

    //if we get a number, store it in val
    private int val;

    //this is the id of variable
    private int id;

    //this is the list of identifiers
    public static ArrayList<String> ident;

    public Scanner(String fileName) throws IOException {
        fReader = new FileReader(fileName);
        curChar = null;
        id = -1;
        ident = new ArrayList<String>();
        ident.add("InputNum");
        ident.add("OutputNum");
        ident.add("OutputNewLine");
    }

    public void startScanner() throws IOException {
        fReader.openFile();
        curChar = fReader.getCurrentChar();
    }

    public void closeScanner() throws IOException {
        fReader.closeFile();
    }

    // Advance to the next character
    private void nextChar() throws IOException {
        curChar = fReader.getNextChar();
    }

    // Advance to the first character of next line
    private void nextLine() throws IOException {
        fReader.getNextLine();
        curChar = fReader.getCurrentChar();
    }

    // Advance to the next token
    public Token getNextToken() throws IOException {
        Token curToken = null;
        // Skip space and comment
        // we may get a divide token by this method
        if ((curToken = skipSpaceAndComment()) != null)
            return curToken;

        // check if eof
        if (curChar == '~')
            return Token.EOF;

        // Check if it is number token
        if ((curToken = getNumberToken()) != null)
            return curToken;

        // Check if it is a letter token(including ident and keyword token)
        if ((curToken = getIdentToken()) != null)
            return curToken;

        // otherwise, other tokens
        switch (curChar) {
            // if operand(+,-,*,/)
            case '+':
                nextChar();
                return Token.PLUS;
            case '-':
                nextChar();
                return Token.MINUS;
            case '*':
                nextChar();
                return Token.TIMES;
            // if comparison
            case '=':
                nextChar();
                if (curChar == '=') {
                    nextChar();
                    return Token.EQL;
                } else {
                    this.Error("\"=\" should be followed by \"=\"");
                    return Token.ERROR;
                }
            case '!':
                nextChar();
                if (curChar == '=') {
                    nextChar();
                    return Token.NEQ;
                } else {
                    this.Error("\"!\" should be followed by \"=\"");
                    return Token.ERROR;
                }
            case '>':
                nextChar();
                if (curChar == '=') {
                    nextChar();
                    return Token.GEQ;
                } else {
                    return Token.GRE;
                }
            case '<':
                nextChar();
                if (curChar == '=') {
                    nextChar();
                    return Token.LEQ;
                } else if(curChar == '-'){
                    nextChar();
                    return Token.BECOMETO;
                } else {
                    return Token.LSS;
                }
            // if punctuation(. , ; :)
            case '.':
                nextChar();
                return Token.PERIOD;
            case ',':
                nextChar();
                return Token.COMMA;
            case ';':
                nextChar();
                return Token.SEMICOMA;
            case ':':
                nextChar();
                return Token.COLON;
            // if block (, ), [, ], {, }
            case '(':
                nextChar();
                return Token.OPEN_PARENTHESIS;
            case ')':
                nextChar();
                return Token.CLOSE_PARENTHESIS;
            case '[':
                nextChar();
                return Token.OPEN_BRACKET;
            case ']':
                nextChar();
                return Token.CLOSE_BRACKET;
            case '{':
                nextChar();
                return Token.OPEN_BRACE;
            case '}':
                nextChar();
                return Token.CLOSE_BRACE;
        }

        return Token.ERROR;
    }

    /**
     * Skip space and comment
     * tab: \t;
     * carriage return: \r;
     * new line: \n;
     * space: \b or ' ';
     * in PL241, both # and / are used for comments
     */
    public Token skipSpaceAndComment() throws IOException {
        while (curChar == '\t' || curChar == '\r' || curChar == '\n' || curChar == ' ' || curChar == '#' || curChar == '/') {
            if (curChar == '\t' || curChar == '\r' || curChar == '\n' || curChar == ' ') {
                nextChar();
            } else if (curChar == '#') {
                nextLine();
            } else if (curChar == '/') {
                nextChar();
                if (curChar == '/') {
                    nextLine();
                } else {
                    return Token.DIVIDE;
                }
            }
        }
        return null;
    }

    public Token getNumberToken() throws IOException {
        boolean isNumber = false;
        this.val = 0;
        while (curChar >= '0' && curChar <= '9') { // If digit, number
            isNumber = true;
            // update val
            this.val = 10 * this.val + curChar - '0';
            nextChar();
        }
        return isNumber ? Token.NUMBER : null;
    }

    public Token getIdentToken() throws IOException{
        boolean isLetter = false;
        StringBuilder sb = null;
        // if letter or digit, ident or keyword
        while (Character.isLetterOrDigit(curChar)) {
            // The first letter should be a letter, actually digit is already filtered when
            // checking number token
            if (!isLetter && Character.isDigit(curChar)) {
                this.Error("Invalid identifier");
                return null;
            }
            //if it is not number,then an identifier
            isLetter = true;
            if (sb == null){
                sb = new StringBuilder("");
                sb.append(curChar);
            }
            else
                sb.append(curChar);
            nextChar();
        }
        if (!isLetter) // no letter Token
            return null;

        String tokenString = sb.toString();

        Token Keyword = buildKeyWord(tokenString);
        if (Keyword != null)
            return Keyword;

        // otherwise, it's a identifier
        // update the list
        if (!ident.contains(tokenString))
            ident.add(tokenString);
        this.id = ident.indexOf(tokenString);

        return Token.IDENTIFIER;
    }

    private Token buildKeyWord(String tokenString)
    {
        return Token.buildToken(tokenString);
    }

    public void Error(String errMsg) {
        System.err.println("Scanner Error: Syntax error at " + fReader.getNumOfLine() + ": " + errMsg);
    }

    /**********************************get function**********************************/
    public int getLineNumber(){
        return fReader.getNumOfLine();
    }

    public int getVarIdent(){
        return this.id;
    }

    public int getVal() {
        return this.val;
    }

    public int getID(){return this.id;}
}
