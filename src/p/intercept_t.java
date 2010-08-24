package p;

import rr.line_t;
import m.fixed_t;

public class intercept_t {

        fixed_t frac;       // along trace line
        boolean isaline;
        // MAES: this was an union of a mobj_t and a line_t.
        mobj_t thing;
        line_t line;
        
        public Interceptable d(){
            return (isaline)? line: thing;
        }
        
    }
