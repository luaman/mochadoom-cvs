package rr;

import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;

import java.io.IOException;
import rr.drawfuns.ColVars;
import rr.drawfuns.R_DrawColumnBoom;
import rr.drawfuns.R_DrawColumnBoomLow;
import rr.drawfuns.R_DrawColumnBoomOpt;
import rr.drawfuns.R_DrawColumnBoomOptLow;
import rr.drawfuns.R_DrawFuzzColumn;
import rr.drawfuns.R_DrawFuzzColumnLow;
import rr.drawfuns.R_DrawSpanLow;
import rr.drawfuns.R_DrawSpanUnrolled;
import rr.drawfuns.R_DrawTLColumn;
import rr.drawfuns.R_DrawTranslatedColumn;
import rr.drawfuns.R_DrawTranslatedColumnLow;
import rr.drawfuns.SpanVars;

import v.DoomVideoRenderer;
import doom.DoomMain;
import doom.DoomStatus;

public abstract class UnifiedRenderer< V>
        extends RendererState<V> {

    public UnifiedRenderer(DoomStatus DS) {
        super(DS);
        this.MyThings = new Things();
        this.MySegs = new Segs();
        this.MyPlanes = new Planes();
    }

    private final class Segs
            extends SegDrawer {

        public Segs() {
            super();
        }

        /** For serial version, just complete the call */
        @Override
        protected final void CompleteColumn() {
            colfunc.invoke();
        }

    }

    // /////////////////////// The actual rendering calls
    // ///////////////////////

    /**
     * R_Init
     */

    public void Init()

    {
        // Any good reason for this to be here?
        // drawsegs=new drawseg_t[MAXDRAWSEGS];
        // C2JUtils.initArrayOfObjects(drawsegs);

        // DON'T FORGET ABOUT MEEEEEE!!!11!!!
        this.screen = V.getScreen(DoomVideoRenderer.SCREEN_FG);

        System.out.print("\nR_InitData");
        InitData();
        // InitPointToAngle ();
        System.out.print("\nR_InitPointToAngle");

        // ds.DM.viewwidth / ds.viewheight / detailLevel are set by the defaults
        System.out.print("\nR_InitTables");
        InitTables();

        SetViewSize(DM.M.getScreenBlocks(), DM.M.getDetailLevel());

        System.out.print("\nR_InitPlanes");
        MyPlanes.InitPlanes();

        System.out.print("\nR_InitLightTables");
        InitLightTables();

        System.out.print("\nR_InitSkyMap: " + TexMan.InitSkyMap());

        System.out.print("\nR_InitTranslationsTables");
        InitTranslationTables();

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
     * public void ExecuteSetViewSize () { int cosadj; int dy; int i,j,cheight;
     * int level, startmap; setsizeneeded = false; // 11 Blocks means
     * "full screen" if (setblocks == 11) { scaledviewwidth = SCREENWIDTH;
     * viewheight = SCREENHEIGHT; } else { scaledviewwidth =
     * setblocks*(SCREENWIDTH/10); // Height can only be a multiple of 8.
     * viewheight = (short) ((setblocks*(SCREENHEIGHT-
     * StatusBar.ST_HEIGHT)/10)&~7); } detailshift = setdetail; viewwidth =
     * scaledviewwidth>>detailshift; centery = viewheight/2; centerx =
     * viewwidth/2; centerxfrac=(centerx<<FRACBITS);
     * centeryfrac=(centery<<FRACBITS); projection=centerxfrac; if
     * (C2JUtils.flags(wide_ratio,4)) { wide_centerx = centerx; cheight =
     * SCREENHEIGHT * BaseRatioSizes[wide_ratio].multiplier / 48; } else {
     * wide_centerx = centerx * BaseRatioSizes[wide_ratio].multiplier / 48;
     * cheight = SCREENHEIGHT; } // e6y: wide-res projection =
     * wide_centerx<<FRACBITS; // proff 11/06/98: Added for high-res projectiony
     * = ((cheight * centerx * 320) / 200) / SCREENWIDTH * FRACUNIT; // e6y:
     * this is a precalculated value for more precise flats drawing (see
     * R_MapPlane) viewfocratio = (1.6f * centerx / wide_centerx) /
     * ((float)SCREENWIDTH / (float)cheight); // High detail if (detailshift==0)
     * { colfunc = basecolfunc =DrawColumn; fuzzcolfunc = DrawFuzzColumn;
     * transcolfunc = DrawTranslatedColumn; glasscolfunc=DrawTLColumn;
     * playercolfunc=DrawColumnPlayer; spanfunc = DrawSpan; } else { // Low
     * detail colfunc = basecolfunc = DrawColumnLow; fuzzcolfunc
     * =DrawFuzzColumn; transcolfunc = DrawTranslatedColumn;
     * glasscolfunc=DrawTLColumn; playercolfunc=DrawColumnPlayer; spanfunc =
     * DrawSpanLow; } InitBuffer (scaledviewwidth, viewheight);
     * InitTextureMapping (); // psprite scales //pspritescale =
     * FRACUNIT*viewwidth/SCREENWIDTH; //pspriteiscale =
     * FRACUNIT*SCREENWIDTH/viewwidth; //pspritescale=(int)
     * (FRACUNIT*((float)SCREEN_MUL*viewwidth)/SCREENWIDTH); //pspriteiscale =
     * (int) (FRACUNIT*(SCREENWIDTH/(viewwidth*(float)SCREEN_MUL))); // psprite
     * scales // proff 08/17/98: Changed for high-res // proff 11/06/98: Added
     * for high-res // e6y: wide-res TODO: they won't work correctly for now.
     * Fuck this. //pspritexscale = (wide_centerx << FRACBITS) / 160;
     * //pspriteyscale = (((cheight*viewwidth)/SCREENWIDTH) << FRACBITS) / 200;
     * //pspriteiscale = FixedDiv (FRACUNIT, pspritexscale); pspritescale=(int)
     * (FRACUNIT*((float)SCREEN_MUL*viewwidth)/SCREENWIDTH); pspriteiscale =
     * (int) (FRACUNIT*(SCREENWIDTH/(viewwidth*(float)SCREEN_MUL)));
     * skyscale=(int) (FRACUNIT*(SCREENWIDTH/(viewwidth*(float)SCREEN_MUL)));
     * BOBADJUST=(int)(Defines.SCREEN_MUL*65536.0); WEAPONADJUST=(int)
     * ((SCREENWIDTH/(2*Defines.SCREEN_MUL))*FRACUNIT); // thing clipping for
     * (i=0 ; i<viewwidth ; i++) screenheightarray[i] = (short) viewheight; //
     * planes for (i=0 ; i<viewheight ; i++) { // killough 5/2/98: reformatted
     * dy = Math.abs((i-viewheight/2)<<FRACBITS)+FRACUNIT/2; MyPlanes.Yslope[i]
     * = FixedDiv(projectiony, dy); // proff 08/17/98: Changed for high-res
     * MyPlanes.yslopef[i] = (projectiony //(viewwidth<<detailshift)/2) / dy; }
     * for (i=0 ; i<viewwidth ; i++) { // MAES: In this spot we must interpet it
     * as SIGNED, else it's pointless, right? // MAES: this spot caused the
     * "warped floor bug", now fixed. Don't forget xtoviewangle[i]! cosadj =
     * Math.abs(finecosine(xtoviewangle[i])); MyPlanes.distscale[i] = FixedDiv
     * (FRACUNIT,cosadj); } // Calculate the light levels to use // for each
     * level / scale combination. for (i=0 ; i< LIGHTLEVELS ; i++) { startmap =
     * ((LIGHTLEVELS-1-i)*2)*NUMCOLORMAPS/LIGHTLEVELS; for (j=0 ;
     * j<MAXLIGHTSCALE ; j++) { level = startmap - j //
     * *SCREENWIDTH/(viewwidth<<detailshift) /DISTMAP; if (level < 0) level = 0;
     * if (level >= NUMCOLORMAPS) level = NUMCOLORMAPS-1; scalelight[i][j] =
     * colormaps[level]; } } }
     */

    public static final class HiColor
            extends UnifiedRenderer<short[]> {

        public HiColor(DoomMain DM) {            
            super(DM);
            
            // Init any video-output dependant stuff            
            this.lights=new Lights<short[]>();
            this.colormap=new Colormaps<short[]>();
            this.VIS=new VisSprites.HiColor(this);
            
            // Init light levels
            lights.scalelight = new short[LIGHTLEVELS][MAXLIGHTSCALE][];
            lights.scalelightfixed = new short[MAXLIGHTSCALE][];
            lights.zlight = new short[LIGHTLEVELS][MAXLIGHTZ][];
            
            // Temporary vissprite
            avis=new vissprite_t<short[]>();
        }

        /**
         * R_InitColormaps This is VERY different for hicolor.
         * 
         * @throws IOException
         */
        protected void InitColormaps()
                throws IOException {

            /*
             * int lump, length; // For HiCOlor, load COLORS15 lump lump =
             * W.GetNumForName("COLORS15"); length = W.LumpLength(lump); //
             * Allow space for one extra colormap, to use as invuln. //colormaps
             * = new short[1+(length / 512)][256]; byte[] tmp = new
             * byte[length]; ByteBuffer bb=ByteBuffer.wrap(tmp);
             * bb.order(ByteOrder.LITTLE_ENDIAN); short[] tmp2=new
             * short[256+(length/2)]; W.ReadLump(lump,tmp); for (int
             * i=0;i<length/2;i++){ tmp2[i]=bb.getShort(); }
             * V.setColorMaps(tmp2, LIGHTLEVELS+2);
             */
            colormap.colormaps = V.getColorMaps();
            System.out.println("COLORS15 Colormaps: " + colormap.colormaps.length);

            /*
             * for (int i = 0; i < colormaps.length; i++) {
             * System.arraycopy(tmp2, i * 256, colormaps[i], 0, 256); }
             */

            /*
             * for (int i = 0; i < colormaps.length; i++) { for (int
             * j=0;j<256;j++) colormaps[i][j]=rgb4444To555(colormaps[i][j]); }
             */

            // MAES: blurry effect is hardcoded to this colormap.
            // Pointless, since we don't use indexes. Instead, a half-brite
            // processing works just fine.
            BLURRY_MAP = null;// colormaps[0];
            // colormaps = (byte *)( ((int)colormaps + 255)&~0xff);

        }
        
        /** Initializes the various drawing functions. They are all "pegged" to the
         *  same dcvars/dsvars object. Any initializations of e.g. parallel renderers
         *  and their supporting subsystems should occur here. 
         */
        
        protected void R_InitDrawingFunctions(){
            
            dcvars=new ColVars<byte[],short[]>();       
            maskedcvars=new ColVars<byte[],short[]>();
            //maskedcvars.dc_translation=translationtables[0];
            skydcvars=new ColVars<byte[],short[]>();
            
            dsvars=new SpanVars<byte[],short[]>();
            
            // Span functions. Common to all renderers unless overriden
            // or unused e.g. parallel renderers ignore them.
            DrawSpan=new R_DrawSpanUnrolled.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,dsvars,screen,I);
            DrawSpanLow=new R_DrawSpanLow.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,dsvars,screen,I);
            
            
            // Translated columns are usually sprites-only.
            DrawTranslatedColumn=new R_DrawTranslatedColumn.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);
            DrawTranslatedColumnLow=new R_DrawTranslatedColumnLow.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);
            DrawTLColumn=new R_DrawTLColumn(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);
            
            // Fuzzy columns. These are also masked.
            DrawFuzzColumn=new R_DrawFuzzColumn.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);
            DrawFuzzColumnLow=new R_DrawFuzzColumnLow.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);
            
            // Regular draw for solid columns/walls. Full optimizations.
            DrawColumn=new R_DrawColumnBoomOpt.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,dcvars,screen,I);
            DrawColumnLow=new R_DrawColumnBoomOptLow.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,dcvars,screen,I);
            
            // Non-optimized stuff for masked.
            DrawColumnMasked=new R_DrawColumnBoom.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);
            DrawColumnMaskedLow=new R_DrawColumnBoomLow.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);
            
            // Player uses masked
            DrawColumnPlayer=DrawColumnMasked; // Player normally uses masked.
            
            // Skies use their own. This is done in order not to stomp parallel threads.
            
            DrawColumnSkies=new R_DrawColumnBoomOpt.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,skydcvars,screen,I);
            DrawColumnSkiesLow=new R_DrawColumnBoomOptLow.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,skydcvars,screen,I);
        }

    }       
                       
          

}
