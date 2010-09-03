package doom;

public interface thinker_t {
   public thinker_t   getSPrev();
   public thinker_t   getSNext();
   public thinker_t   getBPrev();
   public thinker_t   getBNext();

   public void   setSPrev(thinker_t t);
   public void   setSNext(thinker_t t);
   public void   setBPrev(thinker_t t);
   public void   setBNext(thinker_t t);

   
   public think_t     getFunction();
   public void     setFunction(think_t acv);
}
