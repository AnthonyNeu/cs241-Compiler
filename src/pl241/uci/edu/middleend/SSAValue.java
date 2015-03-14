package pl241.uci.edu.middleend;

/*
Data:2015/03/02
This is used to store the reference ID of a value in SSA form.
 */
public class SSAValue {
    private int version;

    public SSAValue(int version)
    {
        this.version = version;
    }

    public int getVersion()
    {
        return this.version;
    }

    public void changeVersion(int id)
    {
        this.version = id;
    }

    public SSAValue clone()
    {
        return new SSAValue(this.version);
    }

    public String toString(){
        return Integer.toString(version);
    }
}
