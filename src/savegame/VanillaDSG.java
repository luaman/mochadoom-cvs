package savegame;
import static data.Limits.MAXPLAYERS;
import i.DoomStatusAware;

import java.io.IOException;
import doom.DoomContext;
import doom.DoomStatus;

import p.ThinkerList;
import w.DoomFile;
import w.IReadableDoomObject;

public class VanillaDSG implements IDoomSaveGame, IReadableDoomObject, DoomStatusAware {
    
    VanillaDSGHeader header;
    DoomStatus DS;
    
    public VanillaDSG(){
        
    }
    

    @Override
    public void setThinkerList(ThinkerList li) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doSave() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doLoad() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public IDoomSaveGameHeader getHeader() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setHeader(IDoomSaveGameHeader header) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void read(DoomFile f) throws IOException
            {
        this.header=new VanillaDSGHeader();
        header.read(f);
        P_UnArchivePlayers(f);
        
    }
    
    
 /**
  * P_UnArchivePlayers
 * @throws IOException 
  */
 protected void P_UnArchivePlayers (DoomFile f) throws IOException
 {
     int     i;
     int     j;
     
     for (i=0 ; i<MAXPLAYERS ; i++)
     {
     // Multiplayer savegames are different!
     if (!DS.playeringame[i])
         continue;
     PADSAVEP(f);
     DS.players[i].read(f);
     
     //memcpy (&players[i],save_p, sizeof(player_t));
     //save_p += sizeof(player_t);
     
     // will be set when unarc thinker
     //players[i].mo = NULL;   
     //players[i].message = NULL;
     //players[i].attacker = NULL;

     /* for (j=0 ; j<NUMPSPRITES ; j++)
     {
         if (C2JUtils.eval(DM.players[i].psprites[j].state))
         {
             DM.players[i]. psprites[j].state 
             =info.states[ (int)DM.players[i].psprites[j].state];
         }
     } */
     }
 }
    
    /**
     * Pads save_p to a 4-byte boundary
     * so that the load/save works on SGI&Gecko.
     * 
     * @param save_p
     */
    
    protected final int PADSAVEP(int save_p){
        return (save_p += (4 - ((int) save_p & 3)) & 3);
    }
    protected final void PADSAVEP(DoomFile f) throws IOException{
        long save_p=f.getFilePointer();
        f.seek(save_p+(4 - ((int) save_p & 3)) & 3);
    }


    @Override
    public void updateStatus(DoomStatus DC) {
        this.DS=DC;
        
    }

}
