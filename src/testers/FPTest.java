package testers;

import m.fixed_t;

class FPTest{         
public static final int PRECISION=16;

    public static void main(String argv[])
{
    fixed_t a=new fixed_t(0x00018000);
    fixed_t b=new fixed_t(0x00018000);
    
    a=F2F(32393.244141f);
    b=F2F(2.5f);
    
    System.out.println(Integer.toHexString(a.val));
    System.out.println(Integer.toHexString(b.val));
    
    b=fixed_t.FixedMul(a,b);    
    System.out.println(Integer.toHexString(b.get()));    

    a=F2F(2.5f);
    b=F2F(2.5f);
    
    
    a.set(a.val+b.val-(fixed_t.FixedMul(F2F(1.5f),a)).val);
    System.out.println(Integer.toHexString(a.get()));  
    
    a=F2F(10000.0f);
    b=F2F(0.5657f);
    
    System.out.println(Integer.toHexString(a.val));
    System.out.println(Integer.toHexString(b.val));
    
    a=fixed_t.FixedDiv(a,b);
    System.out.println(Integer.toHexString(a.val));

}

public static fixed_t F2F(float f){
    fixed_t tmp;
    int ing;
    float frac;


    ing=(int)Math.floor(f);
    System.out.println("Int: "+(int)(f));
    System.out.println("Hex: "+Integer.toHexString(ing));

    System.out.println("Frac: "+(f-Math.floor(f)));
    System.out.println("Frac hex: "+Integer.toHexString(FixedDecimal(f)));

    tmp= new fixed_t((ing<<16)|FixedDecimal(f));
    //System.out.println(Integer.toHexString(tmp.val));    
    return tmp;

}

public static char FixedDecimal(float f){
         char fixint_value=0;
         float decimal_part= (float) (f-Math.floor(f));
for ( int i = 1; i <= PRECISION; i++)
{
   if (decimal_part > 1.f/(float)(i + 1.0))
   {
      decimal_part -= 1.f/(float)(i + 1.0);
      fixint_value |= (1 << PRECISION - i);
   }
}
return fixint_value;
}

}

