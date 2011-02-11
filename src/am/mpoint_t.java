package am;

import m.fixed_t;

public class mpoint_t
{
  public mpoint_t(fixed_t x, fixed_t y) {
        this.x = x;
        this.y = y;
    }

  public mpoint_t(int x, int y) {
      this.x = new fixed_t(x);
      this.y = new fixed_t(x);
  }
  
  public fixed_t x,y;
};
