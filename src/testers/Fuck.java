package testers;

public class Fuck {

    
    class Ass implements Fuckable{

        @Override
        public void fuck() {
            System.out.println(messages[0]);            
        }
        
    }
    
    class Pussy implements Fuckable{

        @Override
        public void fuck() {
            System.out.println(messages[1]);            
        }
        
    }
    
    public Fuckable[] holes;
    public String[] messages;
    
    public Fuck(){
        holes=new Fuckable[2];
        holes[0]=new Ass();
        holes[1]=new Pussy();
        messages=new String[]{"You fucked the ass!","You fucked the pussy!"};
        
    }
    
    
    
}
