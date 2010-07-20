package automap;

import m.fixed_t;

public class mpoint_t
{
  public mpoint_t(fixed_t x, fixed_t y) {
        this.x = x.val;
        this.y = y.val;
    }

  public mpoint_t(int x, int y) {
      this.x = x;
      this.y = y;
  }

  public mpoint_t(double x, double y) {
      this.x = (int) x;
      this.y = (int) y;
  }
  
  /** fixed_t */
  public int x,y;
};
