package doom;

public abstract class thinker_t {
   
   /*
   public thinker_t   getSPrev(); 
   public thinker_t   getSNext();
   public thinker_t   getBPrev();
   public thinker_t   getBNext();
   */

   /* Sets previous in sector */
   //public void   setSPrev(thinker_t t);
   //public void   setSNext(thinker_t t);
   /* Sets previous in blockmap */
   //public void   setBPrev(thinker_t t);
   //public void   setBNext(thinker_t t);

    public thinker_t prev;
    public thinker_t next;
   public think_t     function;

}
