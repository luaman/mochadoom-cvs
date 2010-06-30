package doom;


//
// INTERMISSION
// Structure passed e.g. to WI_Start(wb)
//

public class wbplayerstruct_t {
     public boolean in; // whether the player is in game
     
     // Player stats, kills, collected items etc.
     public int     skills;
     public int     sitems;
     public int     ssecret;
     public int     stime; 
     public int[]   frags=new int[4];
     public int     score;  // current score on entry, modified on return
   
 }
