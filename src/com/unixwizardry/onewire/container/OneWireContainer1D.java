

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

package com.unixwizardry.onewire.container;

// imports
import com.unixwizardry.onewire.OneWireException;
import com.unixwizardry.onewire.adapter.OneWireIOException;
import com.unixwizardry.onewire.utils.CRC16;
import com.unixwizardry.onewire.adapter.*;
import com.unixwizardry.onewire.utils.Convert;
import static com.unixwizardry.onewire.utils.Convert.byteToHex;
import static com.unixwizardry.onewire.utils.Convert.bytesToHexLE;
import java.util.ArrayList;
import java.util.ListIterator;


/**
 * <P> 1-Wire container for 512 byte memory with external counters, DS2423.  This container
 * encapsulates the functionality of the 1-Wire family 
 * type <B>1D</B> (hex)</P>
 *
 * <P> This 1-Wire device is primarily used as a counter with memory. </P>
 *
 * <P> Each counter is associated with a memory page.  The counters for pages 
 * 12 and 13 are incremented with a write to the memory on that page.  The counters
 * for pages 14 and 15 are externally triggered. See the method
 * {@link #readCounter(int) readCounter} to read a counter directly.  Note that the
 * the counters may also be read with the <CODE> PagedMemoryBank </CODE> interface 
 * as 'extra' information on a page read. </P>
 * 
 * <H3> Features </H3> 
 * <UL>
 *   <LI> 4096 bits (512 bytes) of read/write nonvolatile memory
 *   <LI> 256-bit (32-byte) scratchpad ensures integrity of data
 *        transfer
 *   <LI> Memory partitioned into 256-bit (32-byte) pages for
 *        packetizing data
 *   <LI> Data integrity assured with strict read/write
 *        protocols
 *   <LI> Overdrive mode boosts communication to
 *        142 kbits per second
 *   <LI> Four 32-bit read-only non rolling-over page
 *        write cycle counters
 *   <LI> Active-low external trigger inputs for two of
 *        the counters with on-chip debouncing
 *        compatible with reed and Wiegand switches
 *   <LI> 32 factory-preset tamper-detect bits to
 *        indicate physical intrusion
 *   <LI> On-chip 16-bit CRC generator for
 *        safeguarding data transfers
 *   <LI> Operating temperature range from -40&#176C to
 *        +70&#176C
 *   <LI> Over 10 years of data retention
 * </UL>
 * 
 * <H3> Memory </H3> 
 *  
 * <P> The memory can be accessed through the objects that are returned
 * from the {@link #getMemoryBanks() getMemoryBanks} method. </P>
 * 
 * The following is a list of the MemoryBank instances that are returned: 
 *
 * <UL>
 *   <LI> <B> Scratchpad Ex </B> 
 *      <UL> 
 *         <LI> <I> Implements </I> {@link com.unixwizardry.onewire.container.MemoryBank MemoryBank}, 
 *                   {@link com.unixwizardry.onewire.container.PagedMemoryBank PagedMemoryBank}
 *         <LI> <I> Size </I> 32 starting at physical address 0
 *         <LI> <I> Features</I> Read/Write not-general-purpose volatile
 *         <LI> <I> Pages</I> 1 pages of length 32 bytes 
 *         <LI> <I> Extra information for each page</I>  Target address, offset, length 3
 *      </UL> 
 *   <LI> <B> Main Memory </B>
 *      <UL> 
 *         <LI> <I> Implements </I> {@link com.unixwizardry.onewire.container.MemoryBank MemoryBank}, 
 *                  {@link com.unixwizardry.onewire.container.PagedMemoryBank PagedMemoryBank}
 *         <LI> <I> Size </I> 384 starting at physical address 0
 *         <LI> <I> Features</I> Read/Write general-purpose non-volatile
 *         <LI> <I> Pages</I> 12 pages of length 32 bytes giving 29 bytes Packet data payload
 *         <LI> <I> Page Features </I> page-device-CRC 
 *      </UL> 
 *   <LI> <B> Memory with write cycle counter </B>
 *      <UL> 
 *         <LI> <I> Implements </I> {@link com.unixwizardry.onewire.container.MemoryBank MemoryBank}, 
 *                  {@link com.unixwizardry.onewire.container.PagedMemoryBank PagedMemoryBank}
 *         <LI> <I> Size </I> 64 starting at physical address 384
 *         <LI> <I> Features</I> Read/Write general-purpose non-volatile
 *         <LI> <I> Pages</I> 2 pages of length 32 bytes giving 29 bytes Packet data payload
 *         <LI> <I> Page Features </I> page-device-CRC 
 *         <LI> <I> Extra information for each page</I>  Write cycle counter, length 8
 *      </UL> 
 *   <LI> <B> Memory with externally triggered counter </B>
 *      <UL> 
 *         <LI> <I> Implements </I> {@link com.unixwizardry.onewire.container.MemoryBank MemoryBank}, 
 *                  {@link com.unixwizardry.onewire.container.PagedMemoryBank PagedMemoryBank}
 *         <LI> <I> Size </I> 64 starting at physical address 448
 *         <LI> <I> Features</I> Read/Write general-purpose non-volatile
 *         <LI> <I> Pages</I> 2 pages of length 32 bytes giving 29 bytes Packet data payload
 *         <LI> <I> Page Features </I> page-device-CRC 
 *         <LI> <I> Extra information for each page</I>  Externally triggered counter, length 8
 *      </UL> 
 * </UL>
 * 
 * <H3> Usage </H3> 
 * 
 * <DL> 
 * <DD> <H4> Example</H4> 
 * Read the two external counters of this containers instance 'owd': 
 * <PRE> <CODE>
 *  System.out.print("Counter on page 14: " + owd.readCounter(14));
 *  System.out.print("Counter on page 15: " + owd.readCounter(15));
 * </CODE> </PRE>
 * <DD> See the usage example in 
 * {@link com.unixwizardry.onewire.container.OneWireContainer OneWireContainer}
 * to enumerate the MemoryBanks.
 * <DD> See the usage examples in 
 * {@link com.unixwizardry.onewire.container.MemoryBank MemoryBank} and
 * {@link com.unixwizardry.onewire.container.PagedMemoryBank PagedMemoryBank}
 * for bank specific operations.
 * </DL>
 *
 * <H3> DataSheet </H3> 
 * <DL>
 * <DD><A HREF="http://pdfserv.maxim-ic.com/arpdf/DS2422-DS2423.pdf"> http://pdfserv.maxim-ic.com/arpdf/DS2422-DS2423.pdf</A>
 * </DL>
 * 
 * @see com.unixwizardry.onewire.container.MemoryBank
 * @see com.unixwizardry.onewire.container.PagedMemoryBank
 * 
 * @version     0.00, 28 Aug 2000
 * @author      DS
 * @author      Bruce Juntti (some updating)
 */
public class OneWireContainer1D extends OneWireContainer implements CounterContainer
{

   //--------
   //-------- Static Final Variables
   //--------

   /**
    * DS2423 read commands
    */
   private static final byte READ_MEMORY_COMMAND = ( byte ) 0xA5;
   private static final byte READ_SCRATCHPAD_COMMAND = ( byte ) 0xAA;
   
   /**
    * DS2423 write scratchpad command
    */
   private static final byte WRITE_SCRATCHPAD_COMMAND = ( byte ) 0xF0;
   
   /**
    * DS2423 copy scratchpad command
    */
   private static final byte COPY_SCRATCHPAD_COMMAND = ( byte ) 0x5A;
   
   //--------
   //-------- Variables
   //--------

   /**
    * Internal buffer
    */
   private final byte[] buffer = new byte [14];

   //--------
   //-------- Constructors
   //--------

   /**
    * Create an empty container that is not complete until after a call 
    * to <code>setupContainer</code>. <p>
    *
    * This is one of the methods to construct a container.  The others are
    * through creating a OneWireContainer with parameters.
    *
    * @see #setupContainer(com.unixwizardry.onewire.adapter.I2CBridgeAdapter,byte[]) super.setupContainer()
    */
   public OneWireContainer1D ()
   {
      super();
   }

   /**
    * Create a container with the provided adapter instance
    * and the address of the iButton or 1-Wire device.<p>
    *
    * This is one of the methods to construct a container.  The other is
    * through creating a OneWireContainer with NO parameters.
    *
    * @param  sourceAdapter     adapter instance used to communicate with
    * this iButton
    * @param  newAddress        {@link com.unixwizardry.onewire.utils.Address Address}  
    *                           of this 1-Wire device
    *
    * @see #OneWireContainer1D() OneWireContainer1D 
    * @see com.unixwizardry.onewire.utils.Address utils.Address
    */
   public OneWireContainer1D (I2CBridgeAdapter sourceAdapter, byte[] newAddress)
   {
      super(sourceAdapter, newAddress);
   }

   /**
    * Create a container with the provided adapter instance
    * and the address of the iButton or 1-Wire device.<p>
    *
    * This is one of the methods to construct a container.  The other is
    * through creating a OneWireContainer with NO parameters.
    *
    * @param  sourceAdapter     adapter instance used to communicate with
    * this 1-Wire device
    * @param  newAddress        {@link com.unixwizardry.onewire.utils.Address Address}
    *                            of this 1-Wire device
    *
    * @see #OneWireContainer1D() OneWireContainer1D 
    * @see com.unixwizardry.onewire.utils.Address utils.Address
    */
   
   public OneWireContainer1D (I2CBridgeAdapter sourceAdapter, long newAddress)
   {
      super(sourceAdapter, newAddress);
   }
   
   /**
    * Create a container with the provided adapter instance
    * and the address of the iButton or 1-Wire device.<p>
    *
    * This is one of the methods to construct a container.  The other is
    * through creating a OneWireContainer with NO parameters.
    *
    * @param  sourceAdapter     adapter instance used to communicate with
    * this 1-Wire device
    * @param  newAddress        {@link com.unixwizardry.onewire.utils.Address Address}
    *                            of this 1-Wire device
    *
    * @see #OneWireContainer1D() OneWireContainer1D 
    * @see com.unixwizardry.onewire.utils.Address utils.Address
    */
   
   public OneWireContainer1D (I2CBridgeAdapter sourceAdapter, String newAddress)
   {
      super(sourceAdapter, newAddress);
   }
   

   //--------
   //-------- Information methods
   //--------
  
    
   /**
    * Get the Dallas Semiconductor part number of the iButton
    * or 1-Wire Device as a string.  For example 'DS1992'.
    *
    * @return iButton or 1-Wire device name
    */
   public String getName ()
   {
      return "DS2423";
   }

   /**
    * Get a short description of the function of this iButton 
    * or 1-Wire Device type.
    *
    * @return device description
    */
   public String getDescription ()
   {
      return "1-Wire counter with 4096 bits of read/write, nonvolatile "
             + "memory.  Memory is partitioned into sixteen pages of 256 bits each.  "
             + "256 bit scratchpad ensures data transfer integrity.  "
             + "Has overdrive mode.  Last four pages each have 32 bit "
             + "read-only non rolling-over counter.  The first two counters "
             + "increment on a page write cycle and the second two have "
             + "active-low external triggers.";
   }

   /**
    * Retrieves the alternate Dallas Semiconductor part numbers or names.
    * A 'family' of 1-Wire Network devices may have more than one part number
    * depending on packaging.  There can also be nicknames such as
    * 'Crypto iButton'.
    *
    * @return 1-Wire device alternate names
    */
   public String getAlternateNames ()
   {
      return "1-kbit/4-kbit 1-Wire RAM with four 32-bit, read-only counters";
   }
   
   
   /**
    * Get the maximum speed this iButton or 1-Wire device can
    * communicate at.
    * Override this method if derived iButton type can go faster then
    * SPEED_REGULAR(0).
    *
    * @return maximum speed
    * @see com.unixwizardry.onewire.container.OneWireContainer#setSpeed super.setSpeed
    * @see com.unixwizardry.onewire.adapter.I2CBridgeAdapter#SPEED_REGULAR I2CBridgeAdapter.SPEED_REGULAR
    * @see com.unixwizardry.onewire.adapter.I2CBridgeAdapter#SPEED_OVERDRIVE I2CBridgeAdapter.SPEED_OVERDRIVE
    * @see com.unixwizardry.onewire.adapter.I2CBridgeAdapter#SPEED_FLEX I2CBridgeAdapter.SPEED_FLEX
    */
   public int getMaxSpeed ()
   {
      return I2CBridgeAdapter.SPEED_REGULAR;
   }

   /**
    * Get a ListIterator object of memory bank instances that implement one or more
    * of the following interfaces:
    * {@link com.unixwizardry.onewire.container.MemoryBank MemoryBank}, 
    * {@link com.unixwizardry.onewire.container.PagedMemoryBank PagedMemoryBank}, 
    * and {@link com.unixwizardry.onewire.container.OTPMemoryBank OTPMemoryBank}. 
    * @return <CODE>Enumeration</CODE> of memory banks 
    */
   @Override
   public ArrayList getMemoryBanks (){
      ArrayList<MemoryBank> bank_list = new ArrayList<MemoryBank>();

      // scratchpad
      MemoryBankScratchEx scratch = new MemoryBankScratchEx(this);

      bank_list.add(scratch);

      // NVRAM 
      MemoryBankNVCRC nv = new MemoryBankNVCRC(this, scratch);

      nv.numberPages          = 12;
      nv.size                 = 384;
      nv.extraInfoLength      = 8;
      nv.readContinuePossible = false;
      nv.numVerifyBytes       = 8;

      bank_list.add(nv);

      // NVRAM (with write cycle counters)
      nv                      = new MemoryBankNVCRC(this, scratch);
      nv.numberPages          = 2;
      nv.size                 = 64;
      nv.bankDescription      = "Memory with write cycle counter";
      nv.startPhysicalAddress = 384;
      nv.extraInfo            = true;
      nv.extraInfoDescription = "Write cycle counter";
      nv.extraInfoLength      = 8;
      nv.readContinuePossible = false;
      nv.numVerifyBytes       = 8;

      bank_list.add(nv);

      // NVRAM (with external counters)
      nv                      = new MemoryBankNVCRC(this, scratch);
      nv.numberPages          = 2;
      nv.size                 = 64;
      nv.bankDescription      = "Memory with externally triggered counter";
      nv.startPhysicalAddress = 448;
      nv.extraInfo            = true;
      nv.extraInfoDescription = "Externally triggered counter";
      nv.extraInfoLength      = 8;

      bank_list.add(nv);   
      return bank_list;
   }

   //--------
   //-------- Custom Methods for this 1-Wire Device Type  
   //--------

   /**
    * Read the counter value associated with a page on this 
    * 1-Wire Device.
    *
    * @param  counterPage    page number of the counter to read
    *
    * @return  4 byte value counter stored in a long integer
    *
    * @throws OneWireIOException on a 1-Wire communication error such as 
    *         no 1-Wire device present.  This could be
    *         caused by a physical interruption in the 1-Wire Network due to 
    *         shorts or a newly arriving 1-Wire device issuing a 'presence pulse'.
    * @throws OneWireException on a communication or setup error with the 1-Wire 
    *         adapter
    */
   @Override
   public long readCounter (int counterPage) throws OneWireIOException, OneWireException
   {
      // check if counter page provided is valid
      if ((counterPage < 12) || (counterPage > 15))
         throw new OneWireException(
            "OneWireContainer1D-invalid counter page");

      // select the device 
      if (adapter.OWSelect(address)) {
         int crc16;

         // read memory command
         buffer [0] = READ_MEMORY_COMMAND;
         crc16      = CRC16.compute(READ_MEMORY_COMMAND);

         // address of last data byte before counter
         int address = (counterPage << 5) + 31;
         
         debugMsg = "address = " + byteToHex( (byte) address);
         printMessage(debugMsg, "[readCounter]", OneWireContainer.INFO);
         
         // append the address
         buffer [1] = ( byte ) address;
         crc16      = CRC16.compute(buffer [1], crc16);
         buffer [2] = ( byte ) (address >>> 8);
         crc16      = CRC16.compute(buffer [2], crc16);
         debugMsg = "buffer = " + bytesToHexLE(buffer);
         printMessage(debugMsg, "[readCounter]", OneWireContainer.INFO);
         
         // now add the read bytes for data byte,counter,zero bits, crc16
         for (int i = 3; i < 14; i++)
            buffer [i] = ( byte ) 0xFF;

         // send the block
         adapter.dataBlock(buffer, 0, 14);

         // calculate the CRC16 on the result and check if correct
         if (CRC16.compute(buffer, 3, 11, crc16) == 0xB001)
         {

            // extract the counter out of this verified packet
            long return_count = 0;

            for (int i = 4; i >= 1; i--)
            {
               return_count <<= 8;
               return_count |= (buffer [i + 3] & 0xFF);
            }

            // return the result count
            return return_count;
         }
      }

      // device must not have been present
      throw new OneWireIOException("OneWireContainer1D-device not present");
   }
   
    public void doSimpleWrite() throws OneWireIOException, OneWireException {
        byte[] cmdBuf = new byte[14];
        byte[] readBuf = new byte[512];
        int i, j;
        
        // select the device
        if (adapter.OWSelect(address)) {
            // read memory command
            cmdBuf [0] = WRITE_SCRATCHPAD_COMMAND;
            cmdBuf [1] = 0x26;
            cmdBuf [2] = 0x00;
            
            cmdBuf [3] = (byte) 0xAA;
            cmdBuf [3] = (byte) 0xBB;
            adapter.dataBlock(cmdBuf, 0, 5);
            
            /*  After issuing the Read Scratchpad command, the master begins 
                reading. The first 2 bytes will be the target address. The next 
                byte will be the ending offset/data status byte (E/S) followed 
                by the scratchpad data beginning at the byte offset (T4: T0).
                The master may read data until the end of the scratchpad after 
                which the data read will be all logic 1s.
            */
            
            // Reset and select device again for scratchpad read
            adapter.OWSelect(address);
            adapter.OWWriteByte(READ_SCRATCHPAD_COMMAND);
            // verify scratchpad data and target address
            for(i = 0; i < 3; i++) {
                readBuf[i] = adapter.OWReadByte();
            }
            
            //for(i = 0; i < 3; i++) {
            //    System.out.println("readBuf[" + i + "] = " + byteToHex(readBuf[i]));
            //}
            
            // Reset and select device again for copy scratchpad to memory
            adapter.OWSelect(address);
            cmdBuf[0] = COPY_SCRATCHPAD_COMMAND;
            for(i = 1, j = 0; i < 4; i++, j++) {
                cmdBuf[i] = readBuf[j];
            }
            adapter.dataBlock(cmdBuf, 0, 4);
            
            // Read entire memory
            adapter.OWSelect(address);
            adapter.OWWriteByte(READ_MEMORY_COMMAND);
            for(i = 0; i < 512; i++) {
                readBuf[i] = adapter.OWReadByte();
            }
            
            // print everything
            //System.out.println(Convert.toHexString(readBuf));
        } else      
           throw new OneWireIOException("OneWireContainer1D-device not present");
       
   }
}

