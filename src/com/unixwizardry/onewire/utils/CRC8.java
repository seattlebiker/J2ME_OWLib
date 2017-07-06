

/*---------------------------------------------------------------------------
 * Copyright (C) 1999,2000 Dallas Semiconductor Corporation, All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY,  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL DALLAS SEMICONDUCTOR BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Dallas Semiconductor
 * shall not be used except as stated in the Dallas Semiconductor
 * Branding Policy.
 *---------------------------------------------------------------------------
 */

/**
 * CRC8 is a class to contain an implementation of the
 * Cyclic-Redundancy-Check CRC8 for the iButton.  The CRC8 is used
 * in the 1-Wire Network address of all iButtons and 1-Wire
 * devices.
 * <p>
 * CRC8 is based on the polynomial = X^8 + X^5 + X^4 + 1.
 *
 * @version    0.00, 28 Aug 2000
 * @author     DS
 *
 */

package com.unixwizardry.onewire.utils;

import static com.unixwizardry.onewire.utils.Convert.byteToHex;
import static com.unixwizardry.onewire.utils.Convert.bytesToHexLE;

public class CRC8
{

   //--------
   //-------- Variables
   //--------

   /**
    * CRC 8 lookup table
    */
   private static final byte dscrc_table [];

   /*
    * Create the lookup table
    */
   static {

      //Translated from the assembly code in iButton Standards, page 129.
      dscrc_table = new byte [256];

      int acc;
      int crc;

      for (int i = 0; i < 256; i++)
      {
         acc = i;
         crc = 0;

         for (int j = 0; j < 8; j++)
         {
            if (((acc ^ crc) & 0x01) == 0x01)
            {
               crc = ((crc ^ 0x18) >> 1) | 0x80;
            }
            else
               crc = crc >> 1;

            acc = acc >> 1;
         }
         dscrc_table [i] = ( byte ) crc;
      }
   }

   //--------
   //-------- Constructor
   //--------

   /**
    * Private constructor to prevent instantiation.
    */
   private CRC8 ()
   {
   }

   //--------
   //-------- Methods
   //--------

   /**
    * Perform the CRC8 on the data element based on the provided seed.
    * <p>
    * CRC8 is based on the polynomial = X^8 + X^5 + X^4 + 1.
    *
    * @param    dataToCRC data element on which to perform the CRC8
    * @param    seed        seed the CRC8 with this value
    * @return   CRC8 value
    */
   public static int compute(int dataToCRC, int seed)
   {
      return (dscrc_table [(seed ^ dataToCRC) & 0x0FF] & 0x0FF);
   }

   /**
    * Perform the CRC8 on the data element based on a zero seed.
    * <p>
    * CRC8 is based on the polynomial = X^8 + X^5 + X^4 + 1.
    *
    * @param dataToCRC  data element on which to perform the CRC8
    * @return  CRC8 value
    */
   public static int compute(int dataToCRC)
   {
      return (dscrc_table [dataToCRC & 0x0FF] & 0x0FF);
   }

   /**
    * Perform the CRC8 on an array of data elements based on a
    * zero seed.
    * <p>
    * CRC8 is based on the polynomial = X^8 + X^5 + X^4 + 1.
    *
    * @param   dataToCrc   array of data elements on which to perform the CRC8
    * @return  CRC8 value
    */
   public static int compute(byte dataToCrc [])
   {
      return compute(dataToCrc, 0, dataToCrc.length);
   }

   /**
    * Perform the CRC8 on an array of data elements based on a
    * zero seed.
    * <p>
    * CRC8 is based on the polynomial = X^8 + X^5 + X^4 + 1.
    *
    * @param   dataToCrc   array of data elements on which to perform the CRC8
    * @param   off         offset into array
    * @param   len         length of data to crc
    * @return  CRC8 value
    */
   public static int compute(byte dataToCrc [], int off, int len)
   {
      return compute(dataToCrc, off, len, 0);
   }

   /**
    * Perform the CRC8 on an array of data elements based on the
    * provided seed.
    * <p>
    * CRC8 is based on the polynomial = X^8 + X^5 + X^4 + 1.
    *
    * @param   dataToCrc   array of data elements on which to perform the CRC8
    * @param   off         offset into array
    * @param   len         length of data to crc
    * @param   seed        seed to use for CRC8
    * @return  CRC8 value
    */
   public static int compute(byte dataToCrc [], int off, int len, int seed)
   {

      // loop to do the crc on each data element
      int CRC8 = seed;

      //System.out.println("[CRC8] getting CRC8 on " + bytesToHex(dataToCrc) + " offset = " + off + " length = " + len );
      for (int i = 0; i < len; i++) {
         CRC8 = dscrc_table [(CRC8 ^ dataToCrc [i + off]) & 0x0FF];
         //System.out.println("CRC8 is: " + CRC8);
      }
      
      //System.out.println("[CRC8][compute] returning crc: " + Integer.toHexString((CRC8 & 0xFF)));   
      //System.out.println("[CRC8][compute] returning crc: " + byteToHex((byte) (CRC8 & 0xFF)));
      return (CRC8 & 0x0FF);
   }

   /**
    * Perform the CRC8 on an array of data elements based on the
    * provided seed.
    * <p>
    * CRC8 is based on the polynomial = X^8 + X^5 + X^4 + 1.
    *
    * @param   dataToCrc   array of data elements on which to perform the CRC8
    * @param   seed        seed to use for CRC8
    * @return  CRC8 value
    */
   public static int compute(byte dataToCrc [], int seed)
   {
      return compute(dataToCrc, 0, dataToCrc.length, seed);
   }
   
    /**
     * Calculate the CRC8 of the byte value provided with the current global
     *'crc8' value. Returns current global crc8 value
     * @param crc New byte to add to the accumulated CRC
     * @param data_U is the current global CRC value
     * @return CRC8 value of data_U
     */
    
   /*
   public static byte compute(byte crc, byte data_U) {
        int i;
        byte crc8_U = (byte) (crc & 0xFF);

        // See Application Note 27
        crc8_U = (byte) (Byte.toUnsignedInt(crc8_U) ^ Byte.toUnsignedInt(data_U));
        for (i = 0; i < 8; ++i) {
            if ((Byte.toUnsignedInt(crc8_U) & 1) != 0) {
                crc8_U = (byte) ((Byte.toUnsignedInt(crc8_U) >> 1) ^ 0x8c);
            } else {
                crc8_U = (byte) (Byte.toUnsignedInt(crc8_U) >> 1);
            }
        }
        return (byte) (crc8_U & 0xFF);
    }
    */
    /*
    public static byte compute(byte in, byte crc) {
       
        int[] array = {
            0, 94, 188, 226, 97, 63, 221, 131, 194, 156, 126, 32, 163, 253, 31, 65,
            157, 195, 33, 127, 252, 162, 64, 30, 95, 1, 227, 189, 62, 96, 130, 220,
            35, 125, 159, 193, 66, 28, 254, 160, 225, 191, 93, 3, 128, 222, 60, 98,
            190, 224, 2, 92, 223, 129, 99, 61, 124, 34, 192, 158, 29, 67, 161, 255,
            70, 24, 250, 164, 39, 121, 155, 197, 132, 218, 56, 102, 229, 187, 89, 7,
            219, 133, 103, 57, 186, 228, 6, 88, 25, 71, 165, 251, 120, 38, 196, 154,
            101, 59, 217, 135, 4, 90, 184, 230, 167, 249, 27, 69, 198, 152, 122, 36,
            248, 166, 68, 26, 153, 199, 37, 123, 58, 100, 134, 216, 91, 5, 231, 185,
            140, 210, 48, 110, 237, 179, 81, 15, 78, 16, 242, 172, 47, 113, 147, 205,
            17, 79, 173, 243, 112, 46, 204, 146, 211, 141, 111, 49, 178, 236, 14, 80,
            175, 241, 19, 77, 206, 144, 114, 44, 109, 51, 209, 143, 12, 82, 176, 238,
            50, 108, 142, 208, 83, 13, 239, 177, 240, 174, 76, 18, 145, 207, 45, 115,
            202, 148, 118, 40, 171, 245, 23, 73, 8, 86, 180, 234, 105, 55, 213, 139,
            87, 9, 235, 181, 54, 104, 138, 212, 149, 203, 41, 119, 244, 170, 72, 22,
            233, 183, 85, 11, 136, 214, 52, 106, 43, 117, 151, 201, 74, 20, 246, 168,
            116, 42, 200, 150, 21, 75, 169, 247, 182, 232, 10, 84, 215, 137, 107, 53};
                  
        return 0;         
    }
 
   
   byte calc_crc8(byte data) {
   int i; 
   byte crc8;

   // See Application Note 27
   crc8 = (byte) (crc8 ^ data);
   for (i = 0; i < 8; ++i)
   {
      if (crc8 & 0x01) {
         crc8 = (byte) ((crc8 >> 1) ^ 0x8c);
      }
      else
         crc8 = (byte) (crc8 >> 1);
   }

   return crc8;
}
   */
}


