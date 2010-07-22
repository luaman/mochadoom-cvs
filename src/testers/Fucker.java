package testers;

import testers.Fuck.Ass;

public class Fucker {

    Fuckable ff;
    
    public Fucker(Fuckable f){
        ff=f;
    }
    
    public Fucker(Class f) throws InstantiationException, IllegalAccessException{
        if (f.isInstance(Fuckable.class)){
            ff=(Fuckable) f.newInstance();
        }
    }
    
    
    public void fuck(){
        ff.fuck();
    }
    
    public void setFucker(Fucker f) throws InstantiationException, IllegalAccessException{
        f=new Fucker(Ass.class);
        f.fuck();
    }
    
}
