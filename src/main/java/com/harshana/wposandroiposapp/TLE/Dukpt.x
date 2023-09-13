package indeevari.wposandroiposapp.TLE;



import indeevari.wposandroiposapp.Base.Keys;
import indeevari.wposandroiposapp.Crypto.DESCrypto;
import indeevari.wposandroiposapp.Utilities.Utility;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;



public class Dukpt {

    String szVariant="";
    String szdiriveKey;//32
    byte[] unksn;//8
    byte[] unShift;//8
    byte[] unCReg1;//szCReg1_64
    byte[] unCReg2;
    byte[] undiriveKey_L;// String szdiriveKey_L;//16
    byte[] undiriveKey_R;//String szdiriveKey_R;//16
    int inintkey = 0;
    long incount = 0;
    int inshiftBitIndex;

    private static Dukpt instance = null;

    public static Dukpt getInstance()
    {
        if (instance == null)
            instance =  new Dukpt();

        return instance;
    }

    public void inInitKeyDukpt(String hexInitTmkKey)
    {
        byte[] undiriveKey;

        undiriveKey = new byte[50];
        unksn = new byte[8];
        unShift = new byte[8];
        unCReg1 = new byte[8];
        unCReg2 = new byte[8];
        undiriveKey_L = new byte[8];
        undiriveKey_R = new byte[8];

        inintkey = 0;

        byte[] key = Utility.hexStr2Byte(hexInitTmkKey);
        undiriveKey_L = Arrays.copyOf(key,8);
        undiriveKey_R = Arrays.copyOfRange(key,8,16);


        //inKsnCreate(unksn);
        unksn = TLEKeyGeneration.unKsnCreate();
        unksn[7] = 0x01;

        unShift[5] = 0x10;
        unShift[6] = 0x00;
        unShift[7] = 0x00;

        inshiftBitIndex = 0;
        inNewKey3();

    }

    int inNewKey3()
    {
        int keyindex =0,inhost=0;
        String szShift;//[17]
        String szShift64;
        String szksn16;//[17]
        String szksn64;


        byte[] unZero = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
        byte[] unCOCO = { (byte)0xC0, (byte)0xC0, (byte)0xC0, (byte)0xC0, 0x00, 0x00, 0x00, 0x00 };

        unShift[0] = 0x00;
        unShift[1] = 0x00;
        unShift[2] = 0x00;
        unShift[3] = 0x00;
        unShift[4] = 0x00;

        inhost = TLEKeyGeneration.inGetHostGroupRef();
        szShift = Utility.byte2HexStr(unShift,0,8);
        szShift64 = Utility.convertHexToBinary(szShift);

        szksn16 = Utility.byte2HexStr(unksn,0,8);
        szksn64 = Utility.convertHexToBinary(szksn16);

        unCReg1 = Utility.xorArrays(unksn,unZero);
        unCReg2 = Utility.xorArrays(unCReg1,undiriveKey_R);

        byte[] unTemp = new byte[8];

        try {
            String left = Utility.byte2HexStr(undiriveKey_L);
            unTemp =  TLEKeyGeneration.unDesEncpt(unCReg2,undiriveKey_L);
        }catch (Exception e)
        {
        }

        Arrays.fill(unCReg2,(byte)0);
        unCReg2 = Arrays.copyOf(unTemp,8);

        Arrays.fill(unTemp,(byte)0);
        unTemp = Utility.xorArrays(unCReg2,undiriveKey_R);
        String right = Utility.byte2HexStr(undiriveKey_R);

        Arrays.fill(unCReg2,(byte)0);
        unCReg2 = Arrays.copyOf(unTemp,8);

        Arrays.fill(unTemp,(byte)0);
        unTemp = Utility.xorArrays(unCOCO,undiriveKey_L);

        byte L[] = new byte[8];
        byte R[] = new byte[8];

        //Arrays.fill(undiriveKey_L,(byte)0);
        //undiriveKey_L = Arrays.copyOf(unTemp,8);
        L = Arrays.copyOf(unTemp,8);

        Arrays.fill(unTemp,(byte)0);
        unTemp = Utility.xorArrays(unCOCO,undiriveKey_R);

        //Arrays.fill(undiriveKey_R,(byte)0);
        //undiriveKey_R = Arrays.copyOf(unTemp,8);
        R = Arrays.copyOf(unTemp,8);

        Arrays.fill(unTemp,(byte)0);
        unTemp = Utility.xorArrays(unCReg1,R);

        Arrays.fill(unCReg1,(byte)0);
        unCReg1 = Arrays.copyOf(unTemp,8);

        Arrays.fill(unTemp,(byte)0);

        try {
            unTemp =  TLEKeyGeneration.unDesEncpt(unCReg1,L);
        }catch (Exception e)
        {
        }

        Arrays.fill(unCReg1,(byte)0);
        unCReg1 = Arrays.copyOf(unTemp,8);

        Arrays.fill(unTemp,(byte)0);
        unTemp = Utility.xorArrays(unCReg1,R);

        Arrays.fill(unCReg1,(byte)0);
        unCReg1 = Arrays.copyOf(unTemp,8);


        byte[] unDukptFK = new byte[16];
        if(inintkey == 0)
        {

            System.arraycopy(unCReg1, 0, unDukptFK, 0, unCReg1.length);
            System.arraycopy(unCReg2, 0, unDukptFK, unCReg1.length, unCReg2.length);

            keyindex = TLEKeyGeneration.inGETFUTUREKEYSLOTINDEX(inhost)+((20-inshiftBitIndex));
            TLEKeyGeneration.inStoreSecureAria(unDukptFK,keyindex);

            int x = 0 ;
            if (keyindex == 60)
            {
                x = 10;
            }
        }
        else
        {
            System.arraycopy(unCReg1, 0, unDukptFK, 0, unCReg1.length);
            System.arraycopy(unCReg2, 0, unDukptFK, unCReg1.length, unCReg2.length);

            keyindex = TLEKeyGeneration.inGETFUTUREKEYSLOTINDEX(inhost)+(inshiftBitIndex);
            TLEKeyGeneration.inStoreSecureAria(unDukptFK,keyindex);
        }

        inNewKey1();
        return 0;
    }

    int inNewKey1()
    {
        inshiftBitIndex++;
        //New key-1 1 step
        if (inshiftBitIndex < 21 && inshiftBitIndex > 12)
        {
            unShift[7] = (byte)inPower(2,( 20 - inshiftBitIndex));
            unShift[6] = 0x00;
        }

        if (inshiftBitIndex < 13 && inshiftBitIndex > 4)
        {
            unShift[6] = (byte)inPower(2,( 20 - inshiftBitIndex - 8));
            unShift[5] = 0x00;
        }

        if (0 < inshiftBitIndex && inshiftBitIndex < 5)
        {
            unShift[5] = (byte)inPower(2,4 - inshiftBitIndex);
            //unShift[6]= 0x00;
        }
        if(inshiftBitIndex > 20)
        {
            unShift[5] = 0x00;
            unShift[6] = 0x00;
            unShift[7] = 0x00;
        }

        inincCount();
        //New key-1 2 step
        if((unShift[5] == 0x00) && (unShift[6] == 0x00) &&  (unShift[7] == 0x00))
            inNewKey4();
        else
            inNewKey3();

        return 0;
    }

    int inNewKey4()
    {

        TLEKeyGeneration.inIncTxnCount();
        return 0;
    }


    int inincCount()
    {
        long intemp  = 0L;

        if(inintkey == 0)
        {
            incount= (long)inPower(2,inshiftBitIndex);
            inCreateKSN();
        }
        else
        {
            intemp  = incount;
            incount=  incount + (long)inPower(2,20-inshiftBitIndex);//senuli
            inCreateKSN();
            incount = intemp;
        }
        return 0;

    }

    int inPower(int x,int n){
        int thePower=0;
        if (n < 1)
            thePower = 1;
        else
            thePower = inPower(x,n-1)*x;
        return(thePower);
    }

    public int inTxnDukptInit()
    {
        int inhost=0;
        inintkey = 1;

        inhost = TLEKeyGeneration.inGetHostGroupRef();
        while(true)
        {
            incount = TLEKeyGeneration.lnGetHostTxnCount();
            if(inSkipTenBits()==0)
                break;
        }

        inDiraveKeyMake();
        return 0;
    }

    int inDiraveKeyMake()
    {
        String szCount = "",szEFgyn="",szCount2="0000000000",szCounBit64="";
        int i=63,inhost=0,inIndex = 0;
        int inFKIndex = 0;
        long intemp = 0;
        byte[] undiriveKey = new byte[16];


        szCount = String.format("%06X",incount);
        szEFgyn = TLEKeyGeneration.szTleEFGyn();
        String str1 = szEFgyn.substring(0,1);
        szCount = szCount.substring(1, szCount.length());
        szCount = str1 + szCount;


        szCount2 = szCount2 + szCount;
        szCounBit64 = Utility.convertHexToBinary(szCount2);

        while (szCounBit64.charAt(i)!='1')
        {
            i--;
            if(i==43)
                break;
        }
        inFKIndex = (63 - i);
        inshiftBitIndex = 20 - inFKIndex;

        inhost = TLEKeyGeneration.inGetHostGroupRef();
        inIndex = TLEKeyGeneration.inGETFUTUREKEYSLOTINDEX(inhost) + (inshiftBitIndex);

        undiriveKey = TLEKeyGeneration.unDataFromSecureAria(inIndex);

        undiriveKey_L = Arrays.copyOf(undiriveKey,8);
        undiriveKey_R = Arrays.copyOfRange(undiriveKey,8,16);

        inshiftBitIndex++;
        intemp = incount;
        incount = incount + (long)inPower(2,20 - inshiftBitIndex);
        inCreateKSN();
        incount = intemp;
        inCreatShift();
        return 0;
    }

    int inCreatShift()
    {
        unShift = new byte[8];

        unShift[0] = 0x00;
        unShift[1] = 0x00;
        unShift[2] = 0x00;
        unShift[3] = 0x00;
        unShift[4] = 0x00;
        unShift[5] = 0x00;
        unShift[6] = 0x00;
        unShift[7] = 0x00;

        if (inshiftBitIndex < 21 && inshiftBitIndex > 12)
        {
            unShift[7]= (byte) inPower(2,(20-inshiftBitIndex));
            unShift[6]= 0x00;
        }

        if (inshiftBitIndex < 13 && inshiftBitIndex > 4)
        {
            unShift[6]= (byte)inPower(2,(20-inshiftBitIndex - 8));
            unShift[5]= 0x00;
        }

        if (0 < inshiftBitIndex && inshiftBitIndex < 5)
        {
            unShift[5]= (byte)inPower(2,4 - inshiftBitIndex);
            //unShift[6]= 0x00;
        }

        if(inshiftBitIndex>20)
            unShift[7]= 0x00;

        if((unShift[5] == 0x00) && (unShift[6] == 0x00) &&  (unShift[7] == 0x00))
            inNewKey4();

        else
            inNewKey3();

        return 0;
    }

    int inCreateKSN()
    {
        String szCount="",szEFgyn ="";
        byte[] unCount = new byte[3];


        szCount = String.format("%06X",incount);
        szEFgyn = TLEKeyGeneration.szTleEFGyn();
        String str1 = szEFgyn.substring(0,1);
        szCount = szCount.substring(1, szCount.length());
        szCount = str1 + szCount;

        unksn = TLEKeyGeneration.unKsnCreate();
        // SVC_DSP_2_HEX(szCount,(char*)unksn+5,3); //please check here 3 by gayan
        unCount = Utility.hexStr2Byte(szCount,0,6);
        unksn[5]=unCount[0];
        unksn[6]=unCount[1];
        unksn[7]=unCount[2];

        String test = Utility.byte2HexStr(unksn);
        return 0;
    }


    int inSkipTenBits()
    {
        String szCount= "",szEFgyn = "",szCount2="0000000000",szCounBit64="";
        int i=63,r=0;
        int inFKIndex = 0;

        szCount = String.format("%06X", incount);
        szEFgyn = TLEKeyGeneration.szTleEFGyn();

        String str1 = szEFgyn.substring(0,1);
        szCount = szCount.substring(1, szCount.length());
        szCount = str1 + szCount;


        if((inBitCout(szCount)>10))
        {
            szCount2 = szCount2 + szCount;
            szCounBit64 = Utility.convertHexToBinary(szCount2);

            while (szCounBit64.charAt(i)!='1')// szCounBit64[i]!='1'
            {
                i--;
                if(i==43)
                    break;
            }
            inFKIndex = (63 - i);
            inshiftBitIndex = 20 - inFKIndex;
            r = inshiftBitIndex+1;
            incount = incount + (long)inPower(2,21 - r);
            TLEKeyGeneration.vdSetHostCountWhenBitCoutTen(incount);

            return -1;

        }
        else
        {
            return 0;
        }

    }

    int inBitCout(String szCount)
    {
        int i=0;
        int bitCount=0;

        for(i=1;i<6;i++)
        {
            switch(szCount.charAt(i))
            {
                case '0':
                    bitCount+=0;
                    break;

                case '1':
                    bitCount+=1;
                    break;

                case '2':
                    bitCount+=1;
                    break;

                case '3':
                    bitCount+=2;
                    break;

                case '4':
                    bitCount+=1;
                    break;

                case '5':
                    bitCount+=2;
                    break;

                case '6':
                    bitCount+=2;
                    break;

                case '7':
                    bitCount+=3;
                    break;

                case '8':
                    bitCount+=1;
                    break;

                case '9':
                    bitCount+=2;
                    break;

                case 'A':
                    bitCount+=2;
                    break;

                case 'B':
                    bitCount+=3;
                    break;

                case 'C':
                    bitCount+=2;
                    break;

                case 'D':
                    bitCount+=3;
                    break;

                case 'E':
                    bitCount+=3;
                    break;

                case 'F':
                    bitCount+=4;
                    break;
            }

        }

        return bitCount;

    }

    public int inUniqueKeyPerTxn()
    {
        int inhost = 0;
        inhost = TLEKeyGeneration.inGetHostGroupRef();

        if(TLEKeyGeneration.szGetTHUKID() == "03" || TLEKeyGeneration.szGetTHUKID()=="04")
        {
            inTxnDukptInit();
            szVariant = "0000000000FF0000";
            inDataEncriptionDecriptionKey();

            //for kcv
            TLEKeyGeneration.kcvForUniqueKey = szCurrentKvc(29);

            return 0;
        }


        return 0;
    }

    String szCurrentKvc(int inIndex)
    {
        String dummy = "0000000000000000";
        String key  = Keys.getKeyFromSlot(inIndex);
        byte[] encrypted = null;

        //encrypt the data using the key
        try
        {
            encrypted = DESCrypto.encrypt3Des(dummy,key);
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }

        String keyCheck = Utility.byte2HexStr(encrypted);
        return keyCheck;
    }


    int inDataEncriptionDecriptionKey()
    {
        byte[] unVariant = new byte[8];
        byte[] unVariant_L = new byte[8];
        byte[] unVariant_R = new byte[8];
        byte[] unVariant_LR = new byte[16];
        byte[] unVariant_EL = new byte[8];
        byte[] unVariant_ER = new byte[8];
        byte[] unVariant_ELR = new byte[16];
        int  i = 0;


        unVariant = Utility.hexStr2Byte(szVariant,0,16);

        unVariant_L = Utility.xorArrays(undiriveKey_L,unVariant);
        unVariant_R = Utility.xorArrays(undiriveKey_R,unVariant);

        ByteArrayOutputStream bos = null;
        try
        {
            bos = new ByteArrayOutputStream();
            bos.write(unVariant_L);
            bos.write(unVariant_R);

        }catch (Exception ex)
        {
            ex.printStackTrace();
            return -1;
        }

        unVariant_LR = bos.toByteArray();

        // unVariant_LR = Arrays.copyOf(unVariant_L,8);
        // for(i=0;i<8;i++)
        //    unVariant_LR[8+i] = unVariant_R[i];

        try {
            unVariant_EL = TLEKeyGeneration.unTripleDesEncpt(unVariant_L,unVariant_LR);
            unVariant_ER = TLEKeyGeneration.unTripleDesEncpt(unVariant_R,unVariant_LR);

        }
        catch (Exception ex)
        {}

        try
        {
            bos.reset();
            bos.write(unVariant_EL);
            bos.write(unVariant_ER);

        }catch (Exception ex)
        {
            ex.printStackTrace();
            return -1;
        }

        unVariant_ELR = bos.toByteArray();
        //unVariant_ELR = Arrays.copyOf(unVariant_EL,8);
        //  for(i=0;i<8;i++)
        //       unVariant_ELR[8+i] = unVariant_ER[i];

        TLEKeyGeneration.inStoreSecureAria(unVariant_ELR,29);

        return 0;
    }




}

