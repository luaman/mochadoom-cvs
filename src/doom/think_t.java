package doom;

//Historically, "think_t" is yet another
//function pointer to a routine to handle
//an actor.

//
//Experimental stuff.
//To compile this as "ANSI C with classes"
//we will need to handle the various
//action functions cleanly.
//
//typedef  void (*actionf_v)();
//typedef  void (*actionf_p1)( void* );
//typedef  void (*actionf_p2)( void*, void* );

/*typedef union
{
actionf_p1	acp1;
actionf_v	acv;
actionf_p2	acp2;

} actionf_t;

*/

public interface think_t {
    public actionf_t getType();
}
