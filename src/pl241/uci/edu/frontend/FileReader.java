package pl241.uci.edu.frontend;

/*
Date:2015/03/02
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileReader {
    private File file;
    private int numOfLine;
    private int charPosition;
    private BufferedReader buffer;
    private String line;

    public FileReader(String path) throws FileNotFoundException{
        this.file=new File(path);
        buffer=new BufferedReader(new java.io.FileReader(file));
    }

    public void openFile() throws IOException{
        line=buffer.readLine();
        numOfLine=1;
        charPosition=0;
    }

    public Character getCurrentChar(){
        if(charPosition>=line.length()){
            return '#';
        }
        if(line==null){
            return '~';
        }
        return line.charAt(charPosition);
    }

    public Character getNextChar(){
        charPosition++;
        if(charPosition>=line.length()){
            return '#';
        }
        return line.charAt(charPosition);
    }

    public void getNextLine() throws IOException{
        line=buffer.readLine();
        if(line!=null) {
            numOfLine++;
            charPosition = 0;
            while (line != null && line.trim().length() == 0) {
                line = buffer.readLine();
                numOfLine++;
            }
        }
    }

    public void closeFile() throws IOException{
        buffer.close();
    }

    public int getNumOfLine(){
        return numOfLine;
    }

    public int getCharPosition(){
        return charPosition;
    }
}
