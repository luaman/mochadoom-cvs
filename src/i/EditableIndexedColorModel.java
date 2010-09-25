package i;
import java.awt.color.ColorSpace;
import java.math.BigInteger;
import java.util.Arrays;
import sun.awt.image.BufImgSurfaceData;

import java.awt.image.ColorModel;

public class EditableIndexedColorModel extends ColorModel {

	    private static native void initIDs();

	    public EditableIndexedColorModel(int i, int j, byte abyte0[], byte abyte1[], byte abyte2[])
	    {
	        super(i, opaqueBits, ColorSpace.getInstance(1000), false, false, 1, ColorModel.getDefaultTransferType(i));
	        transparent_index = -1;
	        lookupcache = new int[40];
	        if(i < 1 || i > 16)
	        {
	            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
	        } else
	        {
	            setRGBs(j, abyte0, abyte1, abyte2, null);
	            calculatePixelMask();
	            return;
	        }
	    }

	    public EditableIndexedColorModel(int i, int j, byte abyte0[], byte abyte1[], byte abyte2[], int k)
	    {
	        super(i, opaqueBits, ColorSpace.getInstance(1000), false, false, 1, ColorModel.getDefaultTransferType(i));
	        transparent_index = -1;
	        lookupcache = new int[40];
	        if(i < 1 || i > 16)
	        {
	            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
	        } else
	        {
	            setRGBs(j, abyte0, abyte1, abyte2, null);
	            setTransparentPixel(k);
	            calculatePixelMask();
	            return;
	        }
	    }

	    public EditableIndexedColorModel(int i, int j, byte abyte0[], byte abyte1[], byte abyte2[], byte abyte3[])
	    {
	        super(i, alphaBits, ColorSpace.getInstance(1000), true, false, 3, ColorModel.getDefaultTransferType(i));
	        transparent_index = -1;
	        lookupcache = new int[40];
	        if(i < 1 || i > 16)
	        {
	            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
	        } else
	        {
	            setRGBs(j, abyte0, abyte1, abyte2, abyte3);
	            calculatePixelMask();
	            return;
	        }
	    }

	    public EditableIndexedColorModel(int i, int j, byte abyte0[], int k, boolean flag)
	    {
	        this(i, j, abyte0, k, flag, -1);
	        if(i < 1 || i > 16)
	            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
	        else
	            return;
	    }

	    public EditableIndexedColorModel(int i, int j, byte abyte0[], int k, boolean flag, int l)
	    {
	        super(i, opaqueBits, ColorSpace.getInstance(1000), false, false, 1, ColorModel.getDefaultTransferType(i));
	        transparent_index = -1;
	        lookupcache = new int[40];
	        if(i < 1 || i > 16)
	            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
	        if(j < 1)
	            throw new IllegalArgumentException((new StringBuilder()).append("Map size (").append(j).append(") must be >= 1").toString());
	        map_size = j;
	        rgb = new int[calcRealMapSize(i, j)];
	        int i1 = k;
	        int j1 = 255;
	        boolean flag1 = true;
	        byte byte0 = 1;
	        for(int k1 = 0; k1 < j; k1++)
	        {
	            int l1 = abyte0[i1++] & 255;
	            int i2 = abyte0[i1++] & 255;
	            int j2 = abyte0[i1++] & 255;
	            flag1 = flag1 && l1 == i2 && i2 == j2;
	            if(flag)
	            {
	                j1 = abyte0[i1++] & 255;
	                if(j1 != 255)
	                {
	                    if(j1 == 0)
	                    {
	                        if(byte0 == 1)
	                            byte0 = 2;
	                        if(transparent_index < 0)
	                            transparent_index = k1;
	                    } else
	                    {
	                        byte0 = 3;
	                    }
	                    flag1 = false;
	                }
	            }
	            rgb[k1] = j1 << 24 | l1 << 16 | i2 << 8 | j2;
	        }

	        allgrayopaque = flag1;
	        setTransparency(byte0);
	        setTransparentPixel(l);
	        calculatePixelMask();
	    }

	    public EditableIndexedColorModel(int i, int j, int ai[], int k, boolean flag, int l, int i1)
	    {
	        super(i, opaqueBits, ColorSpace.getInstance(1000), false, false, 1, i1);
	        transparent_index = -1;
	        lookupcache = new int[40];
	        if(i < 1 || i > 16)
	            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
	        if(j < 1)
	            throw new IllegalArgumentException((new StringBuilder()).append("Map size (").append(j).append(") must be >= 1").toString());
	        if(i1 != 0 && i1 != 1)
	        {
	            throw new IllegalArgumentException("transferType must be eitherDataBuffer.TYPE_BYTE or DataBuffer.TYPE_USHORT");
	        } else
	        {
	            setRGBs(j, ai, k, flag);
	            setTransparentPixel(l);
	            calculatePixelMask();
	            return;
	        }
	    }

	    public EditableIndexedColorModel(int i, int j, int ai[], int k, int l, BigInteger biginteger)
	    {
	        super(i, alphaBits, ColorSpace.getInstance(1000), true, false, 3, l);
	        transparent_index = -1;
	        lookupcache = new int[40];
	        if(i < 1 || i > 16)
	            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
	        if(j < 1)
	            throw new IllegalArgumentException((new StringBuilder()).append("Map size (").append(j).append(") must be >= 1").toString());
	        if(l != 0 && l != 1)
	            throw new IllegalArgumentException("transferType must be eitherDataBuffer.TYPE_BYTE or DataBuffer.TYPE_USHORT");
	        if(biginteger != null)
	        {
	            int i1 = 0;
	            do
	            {
	                if(i1 >= j)
	                    break;
	                if(!biginteger.testBit(i1))
	                {
	                    validBits = biginteger;
	                    break;
	                }
	                i1++;
	            } while(true);
	        }
	        setRGBs(j, ai, k, true);
	        calculatePixelMask();
	    }

	    private void setRGBs(int i, byte abyte0[], byte abyte1[], byte abyte2[], byte abyte3[])
	    {
	        if(i < 1)
	            throw new IllegalArgumentException((new StringBuilder()).append("Map size (").append(i).append(") must be >= 1").toString());
	        map_size = i;
	        rgb = new int[calcRealMapSize(pixel_bits, i)];
	        int j = 255;
	        byte byte0 = 1;
	        boolean flag = true;
	        for(int k = 0; k < i; k++)
	        {
	            int l = abyte0[k] & 255;
	            int i1 = abyte1[k] & 255;
	            int j1 = abyte2[k] & 255;
	            flag = flag && l == i1 && i1 == j1;
	            if(abyte3 != null)
	            {
	                j = abyte3[k] & 255;
	                if(j != 255)
	                {
	                    if(j == 0)
	                    {
	                        if(byte0 == 1)
	                            byte0 = 2;
	                        if(transparent_index < 0)
	                            transparent_index = k;
	                    } else
	                    {
	                        byte0 = 3;
	                    }
	                    flag = false;
	                }
	            }
	            rgb[k] = j << 24 | l << 16 | i1 << 8 | j1;
	        }

	        allgrayopaque = flag;
	        setTransparency(byte0);
	    }

	    private void setRGBs(int i, int ai[], int j, boolean flag)
	    {
	        map_size = i;
	        rgb = new int[calcRealMapSize(pixel_bits, i)];
	        int k = j;
	        byte byte0 = 1;
	        boolean flag1 = true;
	        BigInteger biginteger = validBits;
	        for(int l = 0; l < i;)
	        {
	            if(biginteger == null || biginteger.testBit(l))
	            {
	                int i1 = ai[k];
	                int j1 = i1 >> 16 & 255;
	                int k1 = i1 >> 8 & 255;
	                int l1 = i1 & 255;
	                flag1 = flag1 && j1 == k1 && k1 == l1;
	                if(flag)
	                {
	                    int i2 = i1 >>> 24;
	                    if(i2 != 255)
	                    {
	                        if(i2 == 0)
	                        {
	                            if(byte0 == 1)
	                                byte0 = 2;
	                            if(transparent_index < 0)
	                                transparent_index = l;
	                        } else
	                        {
	                            byte0 = 3;
	                        }
	                        flag1 = false;
	                    }
	                } else
	                {
	                    i1 |= -16777216;
	                }
	                rgb[l] = i1;
	            }
	            l++;
	            k++;
	        }

	        allgrayopaque = flag1;
	        setTransparency(byte0);
	    }

	    private int calcRealMapSize(int i, int j)
	    {
	        int k = Math.max(1 << i, j);
	        return Math.max(k, 256);
	    }

	    private BigInteger getAllValid()
	    {
	        int i = (map_size + 7) / 8;
	        byte abyte0[] = new byte[i];
	        Arrays.fill(abyte0, (byte)-1);
	        abyte0[0] = (byte)(255 >>> i * 8 - map_size);
	        return new BigInteger(1, abyte0);
	    }

	    public int getTransparency()
	    {
	        return transparency;
	    }

	    public int[] getComponentSize()
	    {
	        if(nBits == null)
	        {
	            if(supportsAlpha)
	            {
	                nBits = new int[4];
	                nBits[3] = 8;
	            } else
	            {
	                nBits = new int[3];
	            }
	            nBits[0] = nBits[1] = nBits[2] = 8;
	        }
	        return nBits;
	    }

	    public final int getMapSize()
	    {
	        return map_size;
	    }

	    public final int getTransparentPixel()
	    {
	        return transparent_index;
	    }

	    public final void getReds(byte abyte0[])
	    {
	        for(int i = 0; i < map_size; i++)
	            abyte0[i] = (byte)(rgb[i] >> 16);

	    }

	    public final void getGreens(byte abyte0[])
	    {
	        for(int i = 0; i < map_size; i++)
	            abyte0[i] = (byte)(rgb[i] >> 8);

	    }

	    public final void getBlues(byte abyte0[])
	    {
	        for(int i = 0; i < map_size; i++)
	            abyte0[i] = (byte)rgb[i];

	    }

	    public final void getAlphas(byte abyte0[])
	    {
	        for(int i = 0; i < map_size; i++)
	            abyte0[i] = (byte)(rgb[i] >> 24);

	    }

	    public final void getRGBs(int ai[])
	    {
	        System.arraycopy(rgb, 0, ai, 0, map_size);
	    }

	    private void setTransparentPixel(int i)
	    {
	        if(i >= 0 && i < map_size)
	        {
	            rgb[i] &= 16777215;
	            transparent_index = i;
	            allgrayopaque = false;
	            if(transparency == 1)
	                setTransparency(2);
	        }
	    }

	    private void setTransparency(int i)
	    {
	        if(transparency != i)
	        {
	            transparency = i;
	            if(i == 1)
	            {
	                supportsAlpha = false;
	                numComponents = 3;
	                nBits = opaqueBits;
	            } else
	            {
	                supportsAlpha = true;
	                numComponents = 4;
	                nBits = alphaBits;
	            }
	        }
	    }

	    private final void calculatePixelMask()
	    {
	        int i = pixel_bits;
	        if(i == 3)
	            i = 4;
	        else
	        if(i > 4 && i < 8)
	            i = 8;
	        pixel_mask = (1 << i) - 1;
	    }

	    public final int getRed(int i)
	    {
	        return rgb[i & pixel_mask] >> 16 & 255;
	    }

	    public final int getGreen(int i)
	    {
	        return rgb[i & pixel_mask] >> 8 & 255;
	    }

	    public final int getBlue(int i)
	    {
	        return rgb[i & pixel_mask] & 255;
	    }

	    public final int getAlpha(int i)
	    {
	        return rgb[i & pixel_mask] >> 24 & 255;
	    }

	    public final int getRGB(int i)
	    {
	        return rgb[i & pixel_mask];
	    }

	    public synchronized Object getDataElements(int i, Object obj)
	    {
	        int j = i >> 16 & 255;
	        int k = i >> 8 & 255;
	        int l = i & 255;
	        int i1 = i >>> 24;
	        int j1 = 0;
	        for(int k1 = 38; k1 >= 0 && (j1 = lookupcache[k1]) != 0; k1 -= 2)
	            if(i == lookupcache[k1 + 1])
	                return installpixel(obj, ~j1);

	        if(allgrayopaque)
	        {
	            int l1 = 256;
	            int l2 = (j * 77 + k * 150 + l * 29 + 128) / 256;
	            int l3 = 0;
	            do
	            {
	                if(l3 >= map_size)
	                    break;
	                if(rgb[l3] != 0)
	                {
	                    int k2 = (rgb[l3] & 255) - l2;
	                    if(k2 < 0)
	                        k2 = -k2;
	                    if(k2 < l1)
	                    {
	                        j1 = l3;
	                        if(k2 == 0)
	                            break;
	                        l1 = k2;
	                    }
	                }
	                l3++;
	            } while(true);
	        } else
	        if(transparency == 1)
	        {
	            int i2 = 2147483647;
	            int ai[] = rgb;
	            int i4 = 0;
	            do
	            {
	                if(i4 >= map_size)
	                    break;
	                int i3 = ai[i4];
	                if(i3 == i && i3 != 0)
	                {
	                    j1 = i4;
	                    i2 = 0;
	                    break;
	                }
	                i4++;
	            } while(true);
	            if(i2 != 0)
	            {
	                for(int j4 = 0; j4 < map_size; j4++)
	                {
	                    int j3 = ai[j4];
	                    if(j3 != 0)
	                    {
	                        int l4 = (j3 >> 16 & 255) - j;
	                        int l5 = l4 * l4;
	                        if(l5 < i2)
	                        {
	                            int i5 = (j3 >> 8 & 255) - k;
	                            l5 += i5 * i5;
	                            if(l5 < i2)
	                            {
	                                int j5 = (j3 & 255) - l;
	                                l5 += j5 * j5;
	                                if(l5 < i2)
	                                {
	                                    j1 = j4;
	                                    i2 = l5;
	                                }
	                            }
	                        }
	                    }
	                }

	            }
	        } else
	        if(i1 == 0 && transparent_index >= 0)
	        {
	            j1 = transparent_index;
	        } else
	        {
	            int j2 = 2147483647;
	            int ai1[] = rgb;
	            for(int k3 = 0; k3 < map_size; k3++)
	            {
	                int k4 = ai1[k3];
	                if(k4 == i)
	                {
	                    if(validBits != null && !validBits.testBit(k3))
	                        continue;
	                    j1 = k3;
	                    break;
	                }
	                int k5 = (k4 >> 16 & 255) - j;
	                int i6 = k5 * k5;
	                if(i6 >= j2)
	                    continue;
	                k5 = (k4 >> 8 & 255) - k;
	                i6 += k5 * k5;
	                if(i6 >= j2)
	                    continue;
	                k5 = (k4 & 255) - l;
	                i6 += k5 * k5;
	                if(i6 >= j2)
	                    continue;
	                k5 = (k4 >>> 24) - i1;
	                i6 += k5 * k5;
	                if(i6 < j2 && (validBits == null || validBits.testBit(k3)))
	                {
	                    j1 = k3;
	                    j2 = i6;
	                }
	            }

	        }
	        System.arraycopy(lookupcache, 2, lookupcache, 0, 38);
	        lookupcache[39] = i;
	        lookupcache[38] = ~j1;
	        return installpixel(obj, j1);
	    }

	    private Object installpixel(Object obj, int i)
	    {
	        switch(transferType)
	        {
	        case 3: // '\003'
	            int ai[];
	            if(obj == null)
	                obj = ai = new int[1];
	            else
	                ai = (int[])(int[])obj;
	            ai[0] = i;
	            break;

	        case 0: // '\0'
	            byte abyte0[];
	            if(obj == null)
	                obj = abyte0 = new byte[1];
	            else
	                abyte0 = (byte[])(byte[])obj;
	            abyte0[0] = (byte)i;
	            break;

	        case 1: // '\001'
	            short aword0[];
	            if(obj == null)
	                obj = aword0 = new short[1];
	            else
	                aword0 = (short[])(short[])obj;
	            aword0[0] = (short)i;
	            break;

	        case 2: // '\002'
	        default:
	            throw new UnsupportedOperationException((new StringBuilder()).append("This method has not been implemented for transferType ").append(transferType).toString());
	        }
	        return obj;
	    }

	    public int[] getComponents(int i, int ai[], int j)
	    {
	        if(ai == null)
	            ai = new int[j + numComponents];
	        ai[j + 0] = getRed(i);
	        ai[j + 1] = getGreen(i);
	        ai[j + 2] = getBlue(i);
	        if(supportsAlpha && ai.length - j > 3)
	            ai[j + 3] = getAlpha(i);
	        return ai;
	    }

	    public int[] getComponents(Object obj, int ai[], int i)
	    {
	        int j;
	        switch(transferType)
	        {
	        case 0: // '\0'
	            byte abyte0[] = (byte[])(byte[])obj;
	            j = abyte0[0] & 255;
	            break;

	        case 1: // '\001'
	            short aword0[] = (short[])(short[])obj;
	            j = aword0[0] & 65535;
	            break;

	        case 3: // '\003'
	            int ai1[] = (int[])(int[])obj;
	            j = ai1[0];
	            break;

	        case 2: // '\002'
	        default:
	            throw new UnsupportedOperationException((new StringBuilder()).append("This method has not been implemented for transferType ").append(transferType).toString());
	        }
	        return getComponents(j, ai, i);
	    }

	    public int getDataElement(int ai[], int i)
	    {
	        int j = ai[i + 0] << 16 | ai[i + 1] << 8 | ai[i + 2];
	        if(supportsAlpha)
	            j |= ai[i + 3] << 24;
	        else
	            j |= -16777216;
	        Object obj = getDataElements(j, null);
	        int k;
	        switch(transferType)
	        {
	        case 0: // '\0'
	            byte abyte0[] = (byte[])(byte[])obj;
	            k = abyte0[0] & 255;
	            break;

	        case 1: // '\001'
	            short aword0[] = (short[])(short[])obj;
	            k = aword0[0];
	            break;

	        case 3: // '\003'
	            int ai1[] = (int[])(int[])obj;
	            k = ai1[0];
	            break;

	        case 2: // '\002'
	        default:
	            throw new UnsupportedOperationException((new StringBuilder()).append("This method has not been implemented for transferType ").append(transferType).toString());
	        }
	        return k;
	    }

	    public Object getDataElements(int ai[], int i, Object obj)
	    {
	        int j = ai[i + 0] << 16 | ai[i + 1] << 8 | ai[i + 2];
	        if(supportsAlpha)
	            j |= ai[i + 3] << 24;
	        else
	            j &= -16777216;
	        return getDataElements(j, obj);
	    }

	    public WritableRaster createCompatibleWritableRaster(int i, int j)
	    {
	        WritableRaster writableraster;
	        if(pixel_bits == 1 || pixel_bits == 2 || pixel_bits == 4)
	            writableraster = Raster.createPackedRaster(0, i, j, 1, pixel_bits, null);
	        else
	        if(pixel_bits <= 8)
	            writableraster = Raster.createInterleavedRaster(0, i, j, 1, null);
	        else
	        if(pixel_bits <= 16)
	            writableraster = Raster.createInterleavedRaster(1, i, j, 1, null);
	        else
	            throw new UnsupportedOperationException("This method is not supported  for pixel bits > 16.");
	        return writableraster;
	    }

	    public boolean isCompatibleRaster(Raster raster)
	    {
	        int i = raster.getSampleModel().getSampleSize(0);
	        return raster.getTransferType() == transferType && raster.getNumBands() == 1 && 1 << i >= map_size;
	    }

	    public SampleModel createCompatibleSampleModel(int i, int j)
	    {
	        int ai[] = new int[1];
	        ai[0] = 0;
	        if(pixel_bits == 1 || pixel_bits == 2 || pixel_bits == 4)
	            return new MultiPixelPackedSampleModel(transferType, i, j, pixel_bits);
	        else
	            return new ComponentSampleModel(transferType, i, j, 1, i, ai);
	    }

	    public boolean isCompatibleSampleModel(SampleModel samplemodel)
	    {
	        if(!(samplemodel instanceof ComponentSampleModel) && !(samplemodel instanceof MultiPixelPackedSampleModel))
	            return false;
	        if(samplemodel.getTransferType() != transferType)
	            return false;
	        return samplemodel.getNumBands() == 1;
	    }

	    public BufferedImage convertToIntDiscrete(Raster raster, boolean flag)
	    {
	        if(!isCompatibleRaster(raster))
	            throw new IllegalArgumentException("This raster is not compatiblewith this IndexColorModel.");
	        Object obj;
	        if(flag || transparency == 3)
	            obj = ColorModel.getRGBdefault();
	        else
	        if(transparency == 2)
	            obj = new DirectColorModel(25, 16711680, 65280, 255, 16777216);
	        else
	            obj = new DirectColorModel(24, 16711680, 65280, 255);
	        int i = raster.getWidth();
	        int j = raster.getHeight();
	        WritableRaster writableraster = ((ColorModel) (obj)).createCompatibleWritableRaster(i, j);
	        Object obj1 = null;
	        Object obj2 = null;
	        int k = raster.getMinX();
	        int l = raster.getMinY();
	        for(int i1 = 0; i1 < j;)
	        {
	            obj1 = raster.getDataElements(k, l, i, 1, obj1);
	            int ai[];
	            if(obj1 instanceof int[])
	                ai = (int[])(int[])obj1;
	            else
	                ai = DataBuffer.toIntArray(obj1);
	            for(int j1 = 0; j1 < i; j1++)
	                ai[j1] = rgb[ai[j1] & pixel_mask];

	            writableraster.setDataElements(0, i1, i, 1, ai);
	            i1++;
	            l++;
	        }

	        return new BufferedImage(((ColorModel) (obj)), writableraster, false, null);
	    }

	    public boolean isValid(int i)
	    {
	        return i >= 0 && i < map_size && (validBits == null || validBits.testBit(i));
	    }

	    public boolean isValid()
	    {
	        return validBits == null;
	    }

	    public BigInteger getValidPixels()
	    {
	        if(validBits == null)
	            return getAllValid();
	        else
	            return validBits;
	    }

	    public void finalize()
	    {
	        BufImgSurfaceData.freeNativeICMData(this);
	    }

	    public String toString()
	    {
	        return new String((new StringBuilder()).append("IndexColorModel: #pixelBits = ").append(pixel_bits).append(" numComponents = ").append(numComponents).append(" color space = ").append(colorSpace).append(" transparency = ").append(transparency).append(" transIndex   = ").append(transparent_index).append(" has alpha = ").append(supportsAlpha).append(" isAlphaPre = ").append(isAlphaPremultiplied).toString());
	    }

	    private int rgb[];
	    private int map_size;
	    private int pixel_mask;
	    private int transparent_index;
	    private boolean allgrayopaque;
	    private BigInteger validBits;
	    private static int opaqueBits[] = {
	        8, 8, 8
	    };
	    private static int alphaBits[] = {
	        8, 8, 8, 8
	    };
	    private static final int CACHESIZE = 40;
	    private int lookupcache[];

	    static 
	    {
	        ColorModel.loadLibraries();
	        initIDs();
	    }
	}


	/*
		DECOMPILATION REPORT

		Decompiled from: C:\Program Files\Java\jre6\lib\rt.jar
		Total time: 32 ms
		Jad reported messages/errors:
		Exit status: 0
		Caught exceptions:
	*/
	 
}
