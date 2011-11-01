package rr;

import static data.Defines.*;
import static data.Limits.*;
import static data.Tables.*;
import static m.fixed_t.*;
import java.io.IOException;
import utils.C2JUtils;
import v.DoomVideoRenderer;
import doom.DoomMain;
import doom.player_t;

public class UnifiedRenderer extends RendererState{
    
    public UnifiedRenderer(DoomMain DM) {
      super(DM);
      this.MySegs=new Segs();
      this.MyPlanes=new Planes();
      }
  
  private final class Segs extends SegDrawer{


      public Segs(){
          super();
      }

      
      /** For serial version, just complete the call */
      @Override
      protected final void CompleteColumn(){
          colfunc.invoke();
      }
      
      }
  
  protected final class Planes extends PlaneDrawer{
     
      /**
       * R_DrawPlanes
       * At the end of each frame.
       * 
       * This also means that visplanes must have been set BEFORE we called this
       * function. Therefore, look for errors behind.
       * 
       * @throws IOException 
       */
       
      @Override
      
      public final void DrawPlanes () 
      {
    	  if(DEBUG) System.out.println(" >>>>>>>>>>>>>>>>>>>>>   DrawPlanes: "+ lastvisplane);
          visplane_t      pln=null; //visplane_t
          int         light;
          int         x;
          int         stop;
          int         angle;
                      
      if (RANGECHECK){
          if (ds_p > MAXDRAWSEGS)
          I.Error("R_DrawPlanes: drawsegs overflow (%i)",
              ds_p );
          
          if (lastvisplane > MAXVISPLANES)
              I.Error(" R_DrawPlanes: visplane overflow (%i)",
               lastvisplane);
          
          if (lastopening  > MAXOPENINGS)
              I.Error( "R_DrawPlanes: opening overflow (%i)",
               lastopening );
      }

          for (int pl = 0 ; pl < lastvisplane ;  pl++)
          {
              pln=visplanes[pl];
             if (DEBUG2) System.out.println(pln);
              
          if (pln.minx > pln.maxx)
              continue;
          // sky flat
          if (pln.picnum == TexMan.getSkyFlatNum())
          {
              // Cache skytexture stuff here. They aren't going to change while
              // being drawn, after all, are they?
              int skytexture=TexMan.getSkyTexture();
              skydcvars.dc_texheight=TexMan.getTextureheight(skytexture)>>FRACBITS;              
              skydcvars.dc_iscale = pspriteiscale>>detailshift;
              
              /* Sky is allways drawn full bright,
               * i.e. colormaps[0] is used.
               * Because of this hack, sky is not affected
               * by INVUL inverse mapping.
               */    
              skydcvars.dc_colormap = colormaps[0];
              skydcvars.dc_texturemid = TexMan.getSkyTextureMid();
              for (x=pln.minx ; x <= pln.maxx ; x++)
              {
            
                  skydcvars.dc_yl = pln.getTop(x);
                  skydcvars.dc_yh = pln.getBottom(x);
              
              if (skydcvars.dc_yl <= skydcvars.dc_yh)
              {
                  angle = (int) (addAngles(viewangle, xtoviewangle[x])>>>ANGLETOSKYSHIFT);
                  skydcvars.dc_x = x;
                  // Optimized: texheight is going to be the same during normal skies drawing...right?
                  skydcvars.dc_source = GetCachedColumn(skytexture,angle);
                  skycolfunc.invoke();
              }
              }
              continue;
          }
          
          // regular flat
          dsvars.ds_source = ((flat_t)W.CacheLumpNum(TexMan.getFlatTranslation(pln.picnum),
                         PU_STATIC,flat_t.class)).data;
          
          
          if (dsvars.ds_source.length==0){
              System.err.printf("YOU READ SHIT %s %d %d %d\n ", W.GetNameForNum(TexMan.getFlatTranslation(pln.picnum)),TexMan.getFlatTranslation(pln.picnum),pln.picnum, dsvars.ds_source.length);
          }
          
          planeheight = Math.abs(pln.height-viewz);
          light = (pln.lightlevel >> LIGHTSEGSHIFT)+extralight;

          if (light >= LIGHTLEVELS)
              light = LIGHTLEVELS-1;

          if (light < 0)
              light = 0;

          planezlight = zlight[light];

          // We set those values at the border of a plane's top to a "sentinel" value...ok.
          pln.setTop(pln.maxx+1,visplane_t.SENTINEL);
          pln.setTop(pln.minx-1, visplane_t.SENTINEL);
          
          stop = pln.maxx + 1;

          
          for (x=pln.minx ; x<= stop ; x++) {
        	  MakeSpans(x,pln.getTop(x-1),
              pln.getBottom(x-1),
              pln.getTop(x),
              pln.getBottom(x));
          	}
          
          //Z_ChangeTag (ds_source, PU_CACHE);
          }
      }

      
      
  } // End Plane class
      

  
 
 


///////////////////////// The actual rendering calls ///////////////////////
 
 /**
  * R_RenderView
  * 
  * As you can guess, this renders the player view of a particular player object.
  * In practice, it could render the view of any mobj too, provided you adapt the
  * SetupFrame method (where the viewing variables are set).
  * 
  */
 
public void RenderPlayerView (player_t player)
{   
    
  // Viewing variables are set according to the player's mobj. Interesting hacks like
  // free cameras or monster views can be done.
  SetupFrame (player);

  // Clear buffers. 
  MyBSP.ClearClipSegs ();
  MyBSP.ClearDrawSegs ();
  MyPlanes.ClearPlanes ();
  MyThings.ClearSprites ();
  
  // Check for new console commands.
  DGN.NetUpdate ();

  // The head node is the last node output.
  MyBSP.RenderBSPNode (LL.numnodes-1);
  
  // Check for new console commands.
  DGN.NetUpdate ();
  
  // FIXME: "Warped floor" fixed, now to fix same-height visplane bleeding.
  MyPlanes.DrawPlanes ();
  
  // Check for new console commands.
  DGN.NetUpdate ();
  
  MyThings.DrawMasked ();
  
  colfunc=basecolfunc;

  // Check for new console commands.
  DGN.NetUpdate ();             
}



/**
 * R_Init
 */
  
public void Init ()

{
	// Any good reason for this to be here?
    //drawsegs=new drawseg_t[MAXDRAWSEGS];
	//C2JUtils.initArrayOfObjects(drawsegs);
	
    // DON'T FORGET ABOUT MEEEEEE!!!11!!!
    this.screen=V.getScreen(DoomVideoRenderer.SCREEN_FG);
    
   System.out.print("\nR_InitData");
   InitData ();
   //InitPointToAngle ();
   System.out.print("\nR_InitPointToAngle");
   
   // ds.DM.viewwidth / ds.viewheight / detailLevel are set by the defaults
   System.out.print ("\nR_InitTables");
   InitTables ();
   
   SetViewSize (DM.M.getScreenBlocks(), DM.M.getDetailLevel());
   
   System.out.print ("\nR_InitPlanes");
   MyPlanes.InitPlanes ();
   
   System.out.print("\nR_InitLightTables");
   InitLightTables ();
   
   System.out.print("\nR_InitSkyMap: "+TexMan.InitSkyMap ());
   
   System.out.print("\nR_InitTranslationsTables");
   InitTranslationTables ();
   
   System.out.print("\nR_InitTranMap: ");
   R_InitTranMap(0);
   
   System.out.print("\nR_InitDrawingFunctions: ");
   R_InitDrawingFunctions();
   
   framecount = 0;
}

/**
 * R_ExecuteSetViewSize
 */

/*
public void ExecuteSetViewSize ()
{
    int cosadj;
    int dy;
    int     i,j,cheight;
    int     level, startmap;   
    
    setsizeneeded = false;

    // 11 Blocks means "full screen"
    
    if (setblocks == 11)
    {
    scaledviewwidth = SCREENWIDTH;
    viewheight = SCREENHEIGHT;
    }
    else
    {
        scaledviewwidth = setblocks*(SCREENWIDTH/10);
        // Height can only be a multiple of 8.
        viewheight = (short) ((setblocks*(SCREENHEIGHT- StatusBar.ST_HEIGHT)/10)&~7);
    }
    
    detailshift = setdetail;
    viewwidth = scaledviewwidth>>detailshift;
    
    centery = viewheight/2;
    centerx = viewwidth/2;
    centerxfrac=(centerx<<FRACBITS);
    centeryfrac=(centery<<FRACBITS);
    projection=centerxfrac;
    
    if (C2JUtils.flags(wide_ratio,4))
    {
      wide_centerx = centerx;
      cheight = SCREENHEIGHT * BaseRatioSizes[wide_ratio].multiplier / 48;
    }
    else
    {
      wide_centerx = centerx * BaseRatioSizes[wide_ratio].multiplier / 48;
      cheight = SCREENHEIGHT;
    }
    
    // e6y: wide-res
    projection = wide_centerx<<FRACBITS;

  // proff 11/06/98: Added for high-res
    projectiony = ((cheight * centerx * 320) / 200) / SCREENWIDTH * FRACUNIT;
    // e6y: this is a precalculated value for more precise flats drawing (see R_MapPlane)
    viewfocratio = (1.6f * centerx / wide_centerx) / ((float)SCREENWIDTH / (float)cheight);
    

    // High detail
    if (detailshift==0)
    {
        
    colfunc = basecolfunc =DrawColumn;
    fuzzcolfunc = DrawFuzzColumn;
    transcolfunc = DrawTranslatedColumn;
    glasscolfunc=DrawTLColumn;
    playercolfunc=DrawColumnPlayer;
    spanfunc = DrawSpan;
    }
    else {
    // Low detail
    colfunc = basecolfunc = DrawColumnLow;
    fuzzcolfunc =DrawFuzzColumn;
    transcolfunc = DrawTranslatedColumn;
    glasscolfunc=DrawTLColumn;
    playercolfunc=DrawColumnPlayer;
    spanfunc = DrawSpanLow;
    
    }

    InitBuffer (scaledviewwidth, viewheight);
    
    InitTextureMapping ();
    
    // psprite scales
    //pspritescale = FRACUNIT*viewwidth/SCREENWIDTH;
    //pspriteiscale = FRACUNIT*SCREENWIDTH/viewwidth;
    
    
    //pspritescale=(int) (FRACUNIT*((float)SCREEN_MUL*viewwidth)/SCREENWIDTH);
    //pspriteiscale = (int) (FRACUNIT*(SCREENWIDTH/(viewwidth*(float)SCREEN_MUL)));
    
    // psprite scales
    // proff 08/17/98: Changed for high-res
    // proff 11/06/98: Added for high-res
    // e6y: wide-res TODO: they won't work correctly for now. Fuck this.
    //pspritexscale = (wide_centerx << FRACBITS) / 160;
    //pspriteyscale = (((cheight*viewwidth)/SCREENWIDTH) << FRACBITS) / 200;
    //pspriteiscale = FixedDiv (FRACUNIT, pspritexscale);
    pspritescale=(int) (FRACUNIT*((float)SCREEN_MUL*viewwidth)/SCREENWIDTH);
    pspriteiscale = (int) (FRACUNIT*(SCREENWIDTH/(viewwidth*(float)SCREEN_MUL)));
    
    skyscale=(int) (FRACUNIT*(SCREENWIDTH/(viewwidth*(float)SCREEN_MUL)));

    BOBADJUST=(int)(Defines.SCREEN_MUL*65536.0);
    WEAPONADJUST=(int) ((SCREENWIDTH/(2*Defines.SCREEN_MUL))*FRACUNIT);
    
    // thing clipping
    for (i=0 ; i<viewwidth ; i++)
    screenheightarray[i] = (short) viewheight;
    
    // planes
    for (i=0 ; i<viewheight ; i++)
    {  // killough 5/2/98: reformatted
    dy = Math.abs((i-viewheight/2)<<FRACBITS)+FRACUNIT/2;    
    MyPlanes.Yslope[i] = FixedDiv(projectiony, dy);
 // proff 08/17/98: Changed for high-res
    MyPlanes.yslopef[i] = (projectiony
    //(viewwidth<<detailshift)/2)
     / dy;
     
    }
    
    for (i=0 ; i<viewwidth ; i++)
    {
    // MAES: In this spot we must interpet it as SIGNED, else it's pointless, right?
    // MAES: this spot caused the "warped floor bug", now fixed. Don't forget xtoviewangle[i]!    
    cosadj = Math.abs(finecosine(xtoviewangle[i]));
    MyPlanes.distscale[i] = FixedDiv (FRACUNIT,cosadj);
    }
    
    // Calculate the light levels to use
    //  for each level / scale combination.
    for (i=0 ; i< LIGHTLEVELS ; i++)
    {
    startmap = ((LIGHTLEVELS-1-i)*2)*NUMCOLORMAPS/LIGHTLEVELS;
    for (j=0 ; j<MAXLIGHTSCALE ; j++)
    {
        level = startmap - j
        // *SCREENWIDTH/(viewwidth<<detailshift)
        /DISTMAP;
         
        
        if (level < 0)
        level = 0;

        if (level >= NUMCOLORMAPS)
        level = NUMCOLORMAPS-1;

        scalelight[i][j] = colormaps[level];
    }
    }
}*/
  
}
