package automap;

public class fline_t
{
    public fline_t(){
        a=new fpoint_t();
        b=new fpoint_t();
    }
    
    public fline_t(fpoint_t a, fpoint_t b){
        this.a=a;
        this.b=b;
    }
    
    public fpoint_t a, b;
}
