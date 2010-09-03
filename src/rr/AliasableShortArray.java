package rr;

public class AliasableShortArray {
    short[] array;
    int alias;
    
    public short getElement(int i){
        return this.array[i+alias];
    }
    
    public short setElement(int i, short val){
        return this.array[i+alias]=val;
    }
    
    public short[] getArray() {
        return array;
    }

    public void setArray(short[] array) {
        this.array = array;
    }

    public int getAlias() {
        return alias;
    }

    public void setAlias(int alias) {
        this.alias = alias;
    }

    public AliasableShortArray(short[] array, int alias) {
        this.array = array;
        this.alias = alias;
    }   
    
    
}
