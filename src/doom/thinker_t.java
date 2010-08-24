package doom;

public interface thinker_t {
   public thinker_t   getPrev();
   public thinker_t   getNext();
   public void   setPrev(thinker_t t);
   public void   setNext(thinker_t t);

   public think_t     getFunction();
   public void     setFunction(think_t acv);
}
