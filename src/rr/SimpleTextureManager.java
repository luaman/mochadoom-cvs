package rr;

import static data.Defines.PU_CACHE;
import static data.Defines.PU_STATIC;
import static data.Defines.SKYFLATNAME;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;

import i.IDoomSystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import p.LevelLoader;
import doom.DoomStatus;
import w.DoomBuffer;
import w.IWadLoader;
import w.WadLoader;

/** An attempt to separate texture mapping functionality from
 *  the rest of the rendering. Seems to work like a charm, and
 *  it makes it clearer what needs and what doesn't need to be 
 *  exposed.
 * 
 * @author Maes
 *
 */

public class SimpleTextureManager
        implements TextureManager {
    
    IWadLoader W;
    IDoomSystem I;
    LevelLoader LL;
    DoomStatus DM;
    
    //
    // Graphics.
    // DOOM graphics for walls and sprites
    // is stored in vertical runs of opaque pixels (posts).
    // A column is composed of zero or more posts,
    // a patch or sprite is composed of zero or more columns.
    // 

    protected int     firstflat;
    protected int     lastflat;
    protected int     numflats;
    /** HACK */
    protected flat_t[] flats;

    
    protected int     firstpatch;
    protected int     lastpatch;
    protected int     numpatches;


    protected int     numtextures;
    
    /** The unchached textures themselves, stored just as patch lists and various properties */
    protected texture_t[] textures;

    /** Width per texture? */
    protected int[]            texturewidthmask;
    /** fixed_t[] needed for texture pegging */
    
    /** How tall each composite texture is supposed to be */
    protected int[]        textureheight;    
    
    /** How large each composite texture is supposed to be */
    protected int[]            texturecompositesize;
    /** Tells us which patch lump covers which column of which texture */
    protected short[][]         texturecolumnlump;
    
    /** This is supposed to store indexes into a patch_t lump which point to the columns themselves 
     *  Instead, we're going to return indexes to columns inside a particular patch.
     *  In the case of patches inside a non-cached multi-patch texture (e.g. those made of non-overlapping
     *  patches), we're storing indexes INSIDE A PARTICULAR PATCH. E.g. for STARTAN1, which is made of two
     *  32-px wide patches, it should go something like 0, 1,2 ,3...31, 0,1,2,....31.
     *  
     * */
    protected char[][]    texturecolumnofs;
    
    /** couple with texturecomposite */
    protected char texturecoloffset;
    //short[][]    texturecolumnindexes;
    /** Stores [textures][columns][data]. */
    protected byte[][][] texturecomposite;

    /** for global animation. Storage stores actual lumps, translation is a relative -> relative map */
    protected int[]        flattranslation, flatstorage,texturetranslation;
    
    // This is also in DM, but one is enough, really.
    protected int skytexture,skytexturemid,skyflatnum;
    
    public SimpleTextureManager(DoomStatus DC) {
        this.DM=DC;
        this.W=DM.W;
        this.I=DM.I;
        this.LL=DM.LL;
    }
  
    /** Hash table used for matching flat <i>lump</i> to flat <i>num</i> */

    Hashtable<Integer, Integer> FlatCache;
    

      /**
       * R_CheckTextureNumForName Check whether texture is available. Filter out
       * NoTexture indicator. Can be sped up with a hash table, but it's pointless.
       */
    
        @Override
       public int CheckTextureNumForName(String name) {
          Integer i;
          // "NoTexture" marker.
          if (name.charAt(0) == '-')  return 0;
          
          i=TextureCache.get(name);
          if (i==null) return -1;
          else return i;

          /* for (i = 0; i < numtextures; i++)
              if (textures[i].name.compareToIgnoreCase(name) == 0)
                  return i;

          return -1; */
      }
    
        /** Hash table used for fast texture lookup */

        Hashtable<String, Integer> TextureCache;
        
        
        /**
         * R_TextureNumForName
         * Calls R_CheckTextureNumForName,
         * aborts with error message.
         */
      
        public int TextureNumForName(String name) {
            int i;

            i = CheckTextureNumForName(name);

            if (i == -1) {
                I.Error("R_TextureNumForName: %s not found", name);
            }
            return i;
        }

    /**
     * R_InitTextures
     * Initializes the texture list
     *  with the textures from the world map.
     */
    
    public void InitTextures () throws IOException
    {
        maptexture_t   mtexture=new maptexture_t();
        texture_t      texture;
        mappatch_t[]     mpatch;
        texpatch_t[]    patch;

        ByteBuffer        maptex, maptex2, maptex1;
        
        String        name;
        ByteBuffer       names;
        int       name_p;
        
        int[]        patchlookup;
        
        int         totalwidth;
        int         nummappatches;
        int         offset;
        int         maxoff;
        int         maxoff2;
        int         numtextures1;
        int         numtextures2;

        int        directory;
        
        int         temp1;
        int         temp2;
        int         temp3;

        
        // Load the patch names from pnames.lmp.
        //name[8] = 0;    
        names = W.CacheLumpName ("PNAMES", PU_STATIC).getBuffer();
        names.order(ByteOrder.LITTLE_ENDIAN);
        
        // Number of patches.
        names.rewind();
        nummappatches = names.getInt();
        
        patchlookup = new int[nummappatches];
        
        for (int i=0 ; i<nummappatches ; i++)
        {
        // Get a size limited string;
        name=DoomBuffer.getNullTerminatedString(names, 8).toUpperCase();
        patchlookup[i] = W.CheckNumForName (name);
        }
        
        names=null;
        
        // Load the map texture definitions from textures.lmp.
        // The data is contained in one or two lumps,
        //  TEXTURE1 for shareware, plus TEXTURE2 for commercial.
        maptex = maptex1 = W.CacheLumpName ("TEXTURE1", PU_STATIC).getBuffer();
        maptex.rewind();
        maptex.order(ByteOrder.LITTLE_ENDIAN);
        numtextures1 = maptex.getInt();
        maxoff = W.LumpLength (W.GetNumForName ("TEXTURE1"));
        directory = 1;
        
        if (W.CheckNumForName ("TEXTURE2") != -1)
        {
        maptex2 = W.CacheLumpName ("TEXTURE2", PU_STATIC).getBuffer();
        maptex2.order(ByteOrder.LITTLE_ENDIAN);
        maptex2.rewind();
        numtextures2 = maptex2.getInt();
        maxoff2 = W.LumpLength (W.GetNumForName ("TEXTURE2"));
        }
        else
        {
        maptex2 = null;
        numtextures2 = 0;
        maxoff2 = 0;
        }
        
        numtextures = numtextures1 + numtextures2;
        
        textures = new texture_t[numtextures];
        // MAES: Texture hashtable.          
        TextureCache=new Hashtable<String, Integer>(numtextures);
        
        texturecolumnlump = new short[numtextures][];
        texturecolumnofs = new char[numtextures][];
        //texturecolumnindexes = new short[numtextures][];
        texturecomposite = new byte[numtextures][][];
        texturecompositesize = new int[numtextures];
        texturewidthmask = new int[numtextures];
        textureheight = new int[numtextures];

        totalwidth = 0;
        
        //  Really complex printing shit...
        temp1 = W.GetNumForName ("S_START");  // P_???????
        temp2 = W.GetNumForName ("S_END") - 1;
        temp3 = ((temp2-temp1+63)/64) + ((numtextures+63)/64);
        System.out.print("[");
        for (int i=0 ; i<numtextures ; i++,directory++)
        {
        
        if ((i&63)==0)
            System.out.print ('.');

        if (i == numtextures1)
        {
            // Start looking in second texture file.
            maptex = maptex2;
            maxoff = maxoff2;
            directory = 1; // offset "1" inside maptex buffer
            //System.err.print("Starting looking into TEXTURE2\n");
        }
        //System.out.print("Directory "+directory);
        offset = maptex.getInt(directory*4);
        if (offset > maxoff)
            I.Error("R_InitTextures: bad texture directory");
       // System.err.printf("offset %d\n",offset);
        
        maptex.position(offset);
        // Read "maptexture", which is the on-disk form.
        mtexture.unpack(maptex);
        //System.err.println(mtexture.name+ " @"+offset);

        // MAES: the HashTable only needs to know the correct names.
        TextureCache.put(mtexture.name.toUpperCase(), new Integer(i));
        
        // We don't need to manually copy trivial fields.
        textures[i]=new texture_t();
        textures[i].copyFromMapTexture(mtexture);
        texture = textures[i];
        
        
        //System.out.println("Patches: "+textures[i].patchcount);
        
        // However we do need to correct the "patch.patch" field through the patchlookup
        mpatch = mtexture.patches;
        patch = texture.patches;

        for (int j=0 ; j<texture.patchcount ; j++)
        {
            patch[j].patch = patchlookup[mpatch[j].patch];
            if (patch[j].patch == -1)
            {
            I.Error ("R_InitTextures: Missing patch in texture %s",
                 texture.name);
            }
        }       
        
        // Columns and offsets of taxture = textures[i]
        texturecolumnlump[i] = new short[texture.width];
        //C2JUtils.initArrayOfObjects( texturecolumnlump[i], column_t.class);
        texturecolumnofs[i] = new char[texture.width];
        
        int j = 1;
        while (j*2 <= texture.width)
            j<<=1;

        texturewidthmask[i] = j-1;
        textureheight[i] = texture.height<<FRACBITS;
            
        totalwidth += texture.width;
        }

        maptex1=null;
        if (maptex2!=null)
        maptex2=null;
        
        // Precalculate whatever possible.  
        for (int i=0 ; i<numtextures ; i++)
        GenerateLookup (i);
        
        // Create translation table for global animation.
        texturetranslation = new int[numtextures];
        
        for (int i=0 ; i<numtextures ; i++)
            texturetranslation[i] = i;
    }
    
    /**
     * R_GenerateLookup
     * 
     * Creates the lookup tables for a given texture (aka, where inside the texture cache
     * is the offset for particular column... I think.
     * 
     * @throws IOException 
     */
    @Override
    public void GenerateLookup (int texnum) throws IOException
    {
        texture_t      texture;
        short[]       patchcount; //Keeps track of how many patches overlap a column.
        texpatch_t[]     patch;  
        patch_t        realpatch;
        int         x;
        int         x1;
        int         x2;

        short[]      collump;
         char[] colofs;
        
        texture = textures[texnum];

        // Composited texture not created yet.
        texturecomposite[texnum] = null;
        
        // We don't know ho large the texture will be, yet, but it will be a multiple of its height.
        texturecompositesize[texnum] = 0;

        // This is the only place where those can be actually modified.
        // They are still null at this point.
        collump = texturecolumnlump[texnum];
        colofs = texturecolumnofs[texnum];
        
        /* Now count the number of columns  that are covered by more 
         * than one patch. Fill in the lump / offset, so columns
         * with only a single patch are all done.
         */

        patchcount = new short[texture.width];
        patch = texture.patches;
            
        // for each patch in a texture...
        for (int i=0; i<texture.patchcount;i++)
        {
        realpatch = (patch_t) W.CacheLumpNum (patch[i].patch, PU_CACHE,patch_t.class);
        x1 = patch[i].originx;
        x2 = x1 + realpatch.width;
        
        // Where does the patch start, inside the compositetexture?
        if (x1 < 0)
            x = 0;
        else
            x = x1;

        // Correct, starts at originx. Where does it end?
        
        if (x2 > texture.width)
            x2 = texture.width;
        for ( ; x<x2 ; x++)
        {
            /* Obviously, if a patch starts at x it does cover the x-th column
             *  of a texture, even if transparent. 
             */
            patchcount[x]++;
            // Column "x" of composite texture "texnum" is covered by this patch.
            collump[x] = (short) patch[i].patch;
            
            /* This is supposed to be a raw pointer to the beginning of the column
             * data, as it appears inside the PATCH.
             * 
             * Instead, we can return the actual column index (x-x1)
             * As an example, the second patch of STARTAN1 (width 64) starts
             * at column 32. Therefore colofs should be something like
             * 0,1,2,...,31,0,1,....31, indicating that the 32-th column of
             * STARTAN1 is the 0-th column of the patch that is assigned to that column
             * (the latter can be looked up in texturecolumnlump[texnum].
             * 
             * Any questions?
             * 
             */
            colofs[x] = (char) (x-x1);
            // This implies that colofs[x] is 0 for a void column?
                
        } // end column of patch.
        } // end patch
        
        // Now check all columns again.
        for ( x=0 ; x<texture.width ; x++)
        {
        // Can only occur if a column isn't covered by a patch at all, not even a transparent one.
        if (patchcount[x]==0)
        {
            System.err.print ("R_GenerateLookup: column without a patch ("+texture.name+")\n");
            return;
        }
        // I_Error ("R_GenerateLookup: column without a patch");
        
        
        // Columns where more than one patch overlaps.
        if (patchcount[x] > 1)
        {
            // Use the cached block. This column won't be read from the wad system.
            collump[x] = -1;    
            colofs[x] = (char) texturecompositesize[texnum];
            
            if (texturecompositesize[texnum] > 0x10000-texture.height)
            {
            I.Error ("R_GenerateLookup: texture %i is >64k",
                 texnum);
            }
            
            texturecompositesize[texnum] += texture.height;
        }
        }   
    }

    /**
     * R_GenerateComposite
     * Using the texture definition,
     *  the composite texture is created from the patches,
     *  and each column is cached.
     * @throws IOException 
     */
    
    public void GenerateComposite (int texnum) 
    {
        byte[][]       block;
        texture_t      texture;
        texpatch_t[]     patch;  
        patch_t        realpatch=null;
        int         x;
        int         x1;
        int         x2;
        column_t       patchcol;
        short[]      collump;
        char[] colofs; // unsigned short
       // short[] colidxs; // unsigned short
        
        texture = textures[texnum];

        // Allocate both the composite and assign it to block.
        // texturecompositesize indicates a size in BYTES. We need a number of columns, though.
        // Z_Malloc (texturecompositesize[texnum], PU_STATIC, &texturecomposite[texnum]);
        block = texturecomposite[texnum]=new byte[texture.width][];
     
        // Now block is divided into columns. We need to allocate enough data for each column
        for (int i=0;i<texture.width;i++)
        	// NASTY hack: apparently the renderer is supposed to overflow.
        	// I'd rather waste some extra memory than try to fix 20 years worth
        	// of rendering functions.
            block[i]=new byte[texture.height];
        

        collump = texturecolumnlump[texnum];
        colofs = texturecolumnofs[texnum];
        
       // colidxs = texturecolumnindexes[texnum];
        
        // Composite the columns together.
        patch = texture.patches;
        
        // For each patch in the texture...
        for (int i=0 ;i<texture.patchcount; i++)
        {

        realpatch = W.CachePatchNum(patch[i].patch, PU_CACHE);
       //     if (realpatch.name=="SKY1"){
       //         System.out.println("Sky found!");
       //     }
        x1 = patch[i].originx;
        x2 = x1 + realpatch.width;

        if (x1<0)
            x = 0;
        else
            x = x1;
        
        if (x2 > texture.width)
            x2 = texture.width;

        for ( ; x<x2 ; x++)
        {
            // Column does not have multiple patches?
            if (collump[x] >= 0)
            continue;
            
           // patchcol = (column_t *)((byte *)realpatch
            //            + LONG(realpatch.columnofs[x-x1]));
            
            
            // We can look this up cleanly in Java. Ha!
            patchcol=realpatch.columns[x-x1];
            DrawColumnInCache (patchcol,
                     block[x], colofs[x],
                     patch[i].originy,
                     texture.height);
        }
                            
        }
    }
    
    /**
     *  R_DrawColumnInCache
     *  Clip and draw a column from a patch into a cached post.
     *  
     *  This means that columns are effectively "uncompressed" into cache, here,
     *  and that composite textures are generally uncompressed...right?
     *  
     *  Actually: "compressed" or "masked" textures are retrieved in the same way.
     *  There are both "masked" and "unmasked" drawing methods. If a masked
     *  column is passed to a method that expects a full, dense column...well,
     *  it will look fugly/overflow/crash. Vanilla Doom tolerated this, 
     *  we're probably going to have more problems.
     *  
     *  @param patch Actually it's a single column to be drawn. May overdraw existing ones or void space.
     *  @param cache the column cache itself. Actually it's the third level [texture][column]->data.
     *  @param offset an offset inside the column cache.
     *  @param originy vertical offset. Caution with masked stuff!
     *  @param cacheheight the maximum height it's supposed to reach when drawing?
     *  
     */
    
    public void
    DrawColumnInCache
    ( column_t patch,
      byte[]     cache,
      int offset,
      int       originy,
      int       cacheheight )
    {
        int     count;
        int     position;
        int  source=0; // treat as pointers
        int   dest=3; // Inside raw data cache
        
         /* Iterate inside column. This is starkly different from the C code,
          * because posts positions are already precomputed at load time
          */
        
        for (int i=0;i<patch.posts;i++){

            source += 3; // Relative to patch's data.
            count = patch.postlen[i]; // length of this particular post
            position = originy + patch.postdeltas[i]; // Position to draw inside cache.

        // Post would be drawn outside of screen. Fuck it.
        // FIXME: this could very well end up "between" posts.
        // Probably the cause of fucked up tiling in vanilla.
            
        if (position < 0)
        {
            count += position; // Consider that we have a "drawing debt".
            position = 0;
        }

        // Post will go too far outside.
        if (position + count > cacheheight)
            count = cacheheight - position;

        if (count > 0) // Draw this post. Won't draw posts that start "outside"

            // Will start at post's start, but will only draw enough pixels not to overdraw.
            System.arraycopy( patch.data, source, cache, position,count);
            
            // This should position us at the beginning of the next post
            source=patch.postofs[i]+patch.postlen[i]+1;
        }
    }

    /**
     * R_InitFlats
     *
     * Scans WADs for F_START/F_END lumps, and also any additional
     * F1_ and F2_ pairs.
     * 
     * Correct behavior would be to detect F_START/F_END lumps, and skip any marker lumps
     * sandwiched in between. If F_START and F_END are external, use external override.
     * Also, in the presence of external FF_START lumps, merge their contents with those previously read.
     * 
     *
     */
    
    @Override
    public final void InitFlats ()
    {
        numflats=0;
        int extendedflatstart=-1;
        firstflat=W.GetNumForName(LUMPSTART); // This is the start of normal lumps.
        if (FlatCache==null) FlatCache=new Hashtable<Integer,Integer>(); else FlatCache.clear();
        Hashtable<String,Integer> FlatNames=new Hashtable<String,Integer> (); // Store names here.
        
        // Normally, if we don't use Boom features, we could look for F_END and that's it.
        // However we want to avoid using flat translation and instead store absolute lump numbers.
        // So we need to do a clean parse.
        
        // The rule is: we scan from the very first F_START to the very first F_END.
        // We discard markers, and only assign sequential numbers to valid lumps.
        // These are the vanilla flats, and will work with fully merged PWADs too.
        
        // Normally, this marks the end of regular lumps. However, if DEUTEX extension
        // are present, it will actually mark the end of the extensions due to lump
        // priority, so its usefulness as an absolute end-index for regular flats
        // is dodgy at best. Gotta love the inconsistent mundo hacks!
        
        int lastflatlump=W.GetNumForName(LUMPEND);
        
		// 
        int lump=firstflat;
        int seq=0;
        String name;
        while (!(name=W.GetNameForNum(lump)).equalsIgnoreCase(LUMPEND)){
            if (!W.isLumpMarker(lump)){
                // Not a marker. Put in cache.
                FlatCache.put(lump, seq);
                // Save its name too.
                FlatNames.put(name, lump);
                seq++; // Advance sequence
                numflats++; // Total flats do increase
            }     
            lump++; // Advance lump.
        }

        
        extendedflatstart=W.CheckNumForName(DEUTEX); // This is the start of DEUTEX flats.
        if (extendedflatstart>-1){
       	// If extended ones are present, then Advance slowly.
        lump=extendedflatstart;

        // The end of those extended flats is also marked by F_END, as noted above.
        while (!(name=W.GetNameForNum(lump)).equalsIgnoreCase(LUMPEND)){
            if (!W.isLumpMarker(lump)){
                // Not a marker. Check if it's supposed to replace something.
                if (FlatNames.containsKey(name)){
                    // Well, it is. Off with its name, save the lump number though.
                    int removed=FlatNames.remove(name);
                    // Put new name in list
                    FlatNames.put(name, lump);
                    // Remove old lump, but keep sequence.
                    int oldseq=FlatCache.remove(removed);
                    // Put new lump number with old sequence. 
                    FlatCache.put(lump, oldseq);
                    } else {  // Add normally
                        FlatCache.put(lump, seq);
                        // Save its name too.
                        FlatNames.put(name, lump);
                        seq++; // Advance sequence
                        numflats++; // Total flats do increase
                    }
            }
            lump++; // Advance lump.
        }
        }
        // So now we have a lump -> sequence number mapping.

            // Create translation table for global animation.
            flattranslation = new int[numflats];
            flatstorage = new int[numflats];
            
            // MAJOR CHANGE: flattranslation stores absolute lump numbers. Adding
            // firstlump is not necessary anymore.      
            // Now, we're pretty sure that we have a progressive value mapping.
   
            Enumeration<Integer> stuff= FlatCache.keys();
            while (stuff.hasMoreElements()){
                int nextlump=stuff.nextElement();
                flatstorage[FlatCache.get(nextlump)]=nextlump;
                // Lump is used as the key, while the relative lump number is the value.
                //FlatCache.put(j, k-1);
                
                }
            
              for (int i=0;i<numflats;i++){
                  flattranslation[i]=i;
                //  System.out.printf("Verification: flat[%d] is %s in lump %d\n",i,W.GetNameForNum(flattranslation[i]),flatstorage[i]);  
              }
            }
    
    private final static String LUMPSTART="F_START";
    private final static String LUMPEND="F_END";
    private final static String DEUTEX="FF_START";
    
    /**
     * R_PrecacheLevel
     * Preloads all relevant graphics for the level.
     * 
     * MAES: Everything except sprites.
     * A Texturemanager != sprite manager.
     * So the relevant functionality has been split into
     * PrecacheThinkers (found in common rendering code).
     * 
     * 
     */
    
    
    int     flatmemory;
    int     texturememory;    

    public void PrecacheLevel () throws IOException
    {
        
        this.preCacheFlats();
        this.preCacheTextures();
        
        // recache sprites.
        
        /* MAES: this code into PrecacheThinkers
        spritepresent = new boolean[numsprites];
        
        
        for (th = P.thinkercap.next ; th != P.thinkercap ; th=th.next)
        {
        if (th.function==think_t.P_MobjThinker)
            spritepresent[((mobj_t )th).sprite.ordinal()] = true;
        }
        
        spritememory = 0;
        for (i=0 ; i<numsprites ; i++)
        {
        if (!spritepresent[i])
            continue;

        for (j=0 ; j<sprites[i].numframes ; j++)
        {
            sf = sprites[i].spriteframes[j];
            for (k=0 ; k<8 ; k++)
            {
            lump = firstspritelump + sf.lump[k];
            spritememory += W.lumpinfo[lump].size;
            W.CacheLumpNum(lump , PU_CACHE,patch_t.class);
            }
        }
        }
        */
    }
    
    protected final void preCacheFlats(){
    	boolean[]       flatpresent;
        int         lump;
        
        
        if (DM.demoplayback)
        return;
        
        // Precache flats.
        flatpresent = new boolean[numflats];
        flats=new flat_t[numflats];
        
        for (int i=0 ; i<LL.numsectors ; i++)
        {
        flatpresent[LL.sectors[i].floorpic] = true;
        flatpresent[LL.sectors[i].ceilingpic] = true;
        }
        
        flatmemory = 0;

        for (int i=0 ; i<numflats ; i++)
        {
        if (flatpresent[i])
        {
            lump = firstflat + i;
            flatmemory += W.GetLumpInfo(lump).size;
            flats[i]=(flat_t) W.CacheLumpNum(lump, PU_CACHE,flat_t.class);
        }
        }
    }
    
    protected final void preCacheTextures(){
    	boolean []      texturepresent;
        texture_t      texture;
        int lump;

    	
    	// Precache textures.
        texturepresent = new boolean[numtextures];
        
        for (int i=0 ; i<LL.numsides ; i++)
        {
        texturepresent[LL.sides[i].toptexture] = true;
        texturepresent[LL.sides[i].midtexture] = true;
        texturepresent[LL.sides[i].bottomtexture] = true;
        }

        // Sky texture is always present.
        // Note that F_SKY1 is the name used to
        //  indicate a sky floor/ceiling as a flat,
        //  while the sky texture is stored like
        //  a wall texture, with an episode dependend
        //  name.
        texturepresent[skytexture] = true;
        
        texturememory = 0;
        for (int i=0 ; i<numtextures ; i++)
        {
        if (!texturepresent[i])
            continue;

        texture = textures[i];
        
        for (int j=0 ; j<texture.patchcount ; j++)
        {
            lump = texture.patches[j].patch;
            texturememory += W.GetLumpInfo(lump).size;
            W.CacheLumpNum(lump , PU_CACHE,patch_t.class);
        }
        }
    }
    
    /**
     * R_FlatNumForName
     * Retrieval, get a flat number for a flat name.
     * 
     * Unlike the texture one, this one is not used frequently. Go figure.
     */
     
    @Override
    public final int FlatNumForName(String name) {
        int i;
        
        //System.out.println("Checking for "+name);

        i = W.CheckNumForName(name);

        //System.out.printf("R_FlatNumForName retrieved lump %d for name %s picnum %d\n",i,name,FlatCache.get(i));
        if (i == -1) {
            I.Error("R_FlatNumForName: %s not found", name);
        }
        
        return FlatCache.get(i);
    }

    @Override
    public final int getTextureColumnLump(int tex, int col) {
        return texturecolumnlump[tex][col];
    }


    @Override
    public final char getTextureColumnOfs(int tex, int col) {
        return texturecolumnofs[tex][col];
    }

    @Override
    public final int getTexturewidthmask(int tex) {
        return texturewidthmask[tex];
    }

    @Override
    public final byte[][] getTextureComposite(int tex) {       
        return texturecomposite[tex];
    }

    @Override
    public final byte[] getTextureComposite(int tex, int col) {
        return texturecomposite[tex][col];
    }
    
    @Override
    public final int getTextureheight(int texnum) {
            return textureheight[texnum];
    }

    @Override
    public final int getTextureTranslation(int texnum) {
        return texturetranslation[texnum];
    }
    
    /** Returns a flat after it has been modified by the translation table e.g. by animations */
    @Override
    public int getFlatTranslation(int flatnum) {
        return flatstorage[flattranslation[flatnum]];
    }

    @Override
    public final void setTextureTranslation(int texnum, int amount) {
        texturetranslation[texnum]=amount;
    }
    
    /** This affects ONLY THE TRANSLATION TABLE, not the lump storage.
     * 
     */
    
    @Override
    public final void setFlatTranslation(int flatnum, int amount) {
        flattranslation[flatnum]=amount;
    }
    

    
    //////////////////////////////////From r_sky.c /////////////////////////////////////

//////////////////////////////////From r_sky.c /////////////////////////////////////

    /**
     * R_InitSkyMap
     * Called whenever the view size changes.
     */

    public int InitSkyMap ()
    {
        skyflatnum = FlatNumForName ( SKYFLATNAME );
        skytexturemid = 100*FRACUNIT;
        return skyflatnum;
    }

    @Override
    public int getSkyFlatNum() {
        return skyflatnum;
    }

    @Override
    public void setSkyFlatNum(int skyflatnum) {
        this.skyflatnum = skyflatnum;
    }



    @Override
    public int getSkyTexture() {
        return skytexture;
    }

    @Override
    public void setSkyTexture(int skytexture) {
        this.skytexture = skytexture;
    }

    /*@Override
    public int getFirstFlat() {
        return firstflat;
    } */

    @Override
    public int getSkyTextureMid() {
        return skytexturemid;
    }

    @Override
    public String CheckTextureNameForNum(int texnum) {       
        return textures[texnum].name;
    }

    @Override
    public int getFlatLumpNum(int flatnum) {
        // TODO Auto-generated method stub
        return 0;
    }
    
}
