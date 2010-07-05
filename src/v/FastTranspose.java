package v;

public class FastTranspose {

    /*
    The functions here are for a very fast matrix transpose, needed
    for Bailey's four-step FFT. Some assumptions: the matrix must
    be square, must be a power of two on a side, and must be stored
    as a 1-D array of longs. It uses block transposition; divide the
    matrix into blocks of a certain optimal size; then transpose the
    diagonal blocks and transpose/switch the off-diagonal blocks. BL
    below is the power of two defining block size.

    On a 90MHz Pentium, blocktranspose() can transpose a 1024x1024
    matrix in .097 seconds, more than six times faster than a naive
    transpose routine. There's plenty of room for assembly optimization,
    but right now the time is dominated by cache misses, and tweaking
    code here and there won't help too much.
 */
    
    public static int BL=5;


 /*----------------------------------------------------------------------*/
 private static void copyblock( byte[] src, byte[] dest, int startpt, int startlength,
                 int destpt, int destlength ) {
    int i,j;
    int pt1=startpt;
    int pt2=destpt;
    for(i=0; i<(1<<BL); i++) {
       for(j=0; j<(1<<BL); j+=16) {
           pt1=startpt+j;
           pt2=destpt+j;
       /* Move a 2^BL x 2^BL block from matrix "startpt" (of size
          1<<startlength on a side) to matrix "destpt" (of 
          size 1<<destlength on a side). The inline assembly
          below preloads the rows of destptr so they're in
          cache when data movement takes place. Then the block
          gets loaded, row by row. */

           /*
          asm(
          "movl (%0,%1,4), %%eax":
           : "r"(destpt), "r"(j)
           : "%eax");*/

          dest[pt2]=src[pt1];                    /* unrolled 8 times, or */
          dest[pt2+1]=src[pt1+1];                /* one cache line worth */
          dest[pt2+2]=src[pt1+2];
          dest[pt2+3]=src[pt1+3];          
          dest[pt2+4]=src[pt1+4];
          dest[pt2+5]=src[pt1+5];
          dest[pt2+6]=src[pt1+6];
          dest[pt2+7]=src[pt1+7];
          dest[pt2+8]=src[pt1+8];
          dest[pt2+9]=src[pt1+9];          
          dest[pt2+10]=src[pt1+10];
          dest[pt2+11]=src[pt1+11];
          dest[pt2+12]=src[pt1+12];
          dest[pt2+13]=src[pt1+13];
          dest[pt2+14]=src[pt1+14];
          dest[pt2+15]=src[pt1+15];/*
          dest[pt2+16]=src[pt1+16];          
          dest[pt2+17]=src[pt1+17];
          dest[pt2+18]=src[pt1+18];
          dest[pt2+19]=src[pt1+19];
          dest[pt2+20]=src[pt1+20];
          dest[pt2+21]=src[pt1+21];
          dest[pt2+22]=src[pt1+22];
          dest[pt2+23]=src[pt1+23];
          dest[pt2+24]=src[pt1+24];
          dest[pt2+25]=src[pt1+25];
          dest[pt2+26]=src[pt1+26];
          dest[pt2+27]=src[pt1+27];
          dest[pt2+28]=src[pt1+28];
          dest[pt2+29]=src[pt1+29];
          dest[pt2+30]=src[pt1+30];          
          dest[pt2+31]=src[pt1+31];*/
       }      startpt += startlength;                      /* next row of startpt */
       destpt += destlength;                        /* next row of destpt  */
    }                     
 }

 /*-------------------------------------------------------------------*/
 private static void copytranspose( byte[] src, byte[] dest, int startpt, int destpt, long destlength ) {

    int i, j;
    byte temp;
    int row;
    int blen=(1<<BL);
    /* this function switches the columns in the 2^BL x 2^BL block
       "startpt" with the rows of the 2^BL x 2^BL block in "destpt".
       This also has the effect of transposing the block in
       destpt, which need only then be copied into its appropriate
       place. The code below pairs nicely, and performs the
       copying while all the data involved is in cache. */
    
    
    int pt1,pt2;
    int blen2=2*blen;
    int blen3=3*blen;
    int blen4=4*blen;
    int blen5=5*blen;
    int blen6=6*blen;
    int blen7=7*blen;
    for(i=0; i<blen; i++) {
        row=startpt;   
       for(j=0; j<blen; j+=8, row+=8*blen) {
           pt2=destpt+j;
           //pt1=startpt+j;
           
          temp = dest[pt2];
          dest[pt2] = src[row];
          src[row] = temp;
                                           /* gcc turns the BL stuff here  */
          temp = dest[pt2+1];              /* into numbers, saving lots of */
          dest[pt2+1] = src[row+blen];        /* pointer addition and AGIs    */
          src[row+blen] = temp;

          temp = dest[pt2+2];
          dest[pt2+2] = src[row+blen2];           
          src[row+2*blen] = temp;

          temp = dest[pt2+3];
          dest[pt2+3] = src[row+blen3];           
          src[row+3*blen] = temp;

          temp = dest[pt2+4];
          dest[pt2+4] = src[row+blen4];           
          src[row+4*blen] = temp;
                                     
          temp = dest[pt2+5];
          dest[pt2+5] = src[row+blen5];           
          src[row+5*blen] = temp;

          temp = dest[pt2+6];
          dest[pt2+6] = src[row+blen6];           
          src[row+6*blen] = temp;

          temp = dest[pt2+7];
          dest[pt2+7] = src[row+blen7];           
          src[row+7*blen] = temp;
       }
       startpt++;                                 /* move to next column */
       destpt+=destlength;                        /* move to next row    */
    }                     
 }


 /*-------------------------------------------------------------------*/
 public static void blocktranspose( byte[] x , byte[] scratch, int side ) {
    int corner = 0;
    int col, row;
    int blocks = side-BL;

    /* transpose an array "x". "scratch" must be the size 
       of one full block, and "size" is the power of two x
       is on a side (e.g. 512x512 matrix has side=9). The loop
       below transposes block (1,1) and then moves down the first
       row and first column of blocks, switching their transposes.
       Then the process repeats for the second row and column, etc */

    for(col=1; col<=1<<blocks; col++) {

       copyblock( x,scratch,corner, 1<<side, 0, 1<<BL );     /* diagonals */
       copytranspose( scratch,x,0, corner, 1<<side );
   
       for( row=1; row< (1<<blocks)-col+1; row++) {

          copyblock( x,scratch,corner+(row<<BL), 1<<side,
                     0, 1<<BL );                   /* off-diagonals */
                                                        
          copytranspose( scratch,x,0, corner+(row<<(side+BL)),
                     1<<side );

          copyblock( scratch,x,0, 1<<BL,
                     corner+(row<<BL), 1<<side);

       }
       corner += (1<<(side+BL)) + (1<<BL);   
    }
                                             /* move down and to the right */
 }
 
}
