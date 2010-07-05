package am;

public class mline_t
{
    
    
    public mline_t(mpoint_t a, mpoint_t b) {
        this.a = a;
        this.b = b;
    }

    public mline_t(int ax,int ay,int bx,int by) {
        this.a = new mpoint_t(ax,ay);
        this.a = new mpoint_t(bx,by);
    }
    
    mpoint_t a, b;
}
