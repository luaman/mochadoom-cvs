package automap;

public class mline_t
{
    public mline_t(){
        this(0,0,0,0);
    }
    
    public mline_t(mpoint_t a, mpoint_t b) {
        this.a = a;
        this.b = b;
    }

    public mline_t(int ax,int ay,int bx,int by) {
        this.a = new mpoint_t(ax,ay);
        this.a = new mpoint_t(bx,by);
    }
        
    public mline_t(double ax,double ay,double bx,double by) {
        this.a = new mpoint_t(ax,ay);
        this.a = new mpoint_t(bx,by);
    }
    
    public mpoint_t a, b;
}
