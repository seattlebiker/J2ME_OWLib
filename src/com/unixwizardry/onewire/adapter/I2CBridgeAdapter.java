/*
 * Portions of this code are from Oracle so I include this disclaimer
 */
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

package com.unixwizardry.onewire.adapter;

import static com.unixwizardry.onewire.adapter.DS2482.OWMatchROMCmd;
import static com.unixwizardry.onewire.adapter.DS2482.OWSearchCmd;
import com.unixwizardry.accessProvider.I2C_Device;
import com.unixwizardry.onewire.OneWireException;
import com.unixwizardry.onewire.container.OneWireContainer;
import com.unixwizardry.onewire.utils.Address;
import com.unixwizardry.onewire.utils.Convert;
import static com.unixwizardry.onewire.utils.Convert.byteToHex;
import static com.unixwizardry.onewire.utils.Convert.bytesToHexLE;
import static com.unixwizardry.onewire.utils.Convert.toHexString;
import static com.unixwizardry.onewire.utils.PrintBits.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.lang.Thread.sleep;


public class I2CBridgeAdapter extends I2C_Device implements DS2482 {    
        
    //private final int bufferSize = 1;           // Register size in bytes
    //private static byte dscrc_table [];         // Empty CRC table to be filled by createCrcTable()   
    private static final int POLL_LIMIT = 16;   // Number of times totest status 
    private boolean BRIDGE_BUSY = false;
    private final int REG_SIZE = 2;
    int LastDiscrepancy = 0;
    boolean LastDeviceFlag = false;
    int LastFamilyDiscrepancy = 0;
    byte[] device_serial_no;
    private static final int ONEWIRE_ROM_BYTE_LENGTH = 8;
    int crc8;
    /* current device */
    private byte[] CurrentDevice = new byte[8];
    private String msg;
    
    // 
    // The DS2482 understands eight function commands, which fall into four categories: device control, I²C
    // communication, 1-Wire set-up and 1-Wire communication. The feedback path to the host is controlled by a read
    // pointer, which is set automatically by each function command for the host to efficiently access relevant information.
    // The host processor sends these commands and applicable parameters as strings of one or two bytes using the I²C
    // interface. The I²C protocol requires that each byte be acknowledged by the receiving party to confirm acceptance
    // or not be acknowledged to indicate an error condition (invalid code or parameter) or to end the communication.
    // Details of the I²C protocol including acknowledge are found in the I²C interface description of this document.
    
    //DEVICE REGISTERS
    // The DS2482 has three registers that the I²C host can read: Configuration, Status, and Read Data. These registers
    // are addressed by a read pointer. The position of the read pointer, i.e., the register that the host reads in a
    // subsequent read access, is defined by the instruction that the has DS2482 executed last. The host has read and
    // write access to the Configuration register to enable certain 1-Wire features.
    // Configuration Register
    // The DS2482 supports allows four 1-Wire features that are enabled or selected through the Configuration register.
    // These features are:
    //     Active Pullup (APU)
    //     Presence Pulse Masking (PPM)
    //     Strong Pullup (SPU)
    //     1-Wire Speed (1WS)
    // These features can be selected in any combination. While APU, PPM, and 1WS maintain their state, SPU returns
    // to its inactive state as soon as the strong pullup has ended.
    //   
    // DS2482 Commands
    //
    
    //--------
    //-------- Static Variables
    //--------

    private final static byte   DS2482_DEFAULT_ADDRESS   = 0x18;   // DS2482-100 default address
    private final static int    DEFAULT_I2C_BUS          = 1;    
    private I2CBridgeAdapter    ds2482_bridge;
    
    /** Version string for this adapter class */
    private static final String CLASS_VERSION = "0.00";  
    
    private static final byte DS2482SetReadPointer = (byte) 0xE1;         
    //private static final byte OWSkipROMCmd = (byte) 0xCC;
    //private static final byte OWConvertTemp = 0x44;
    //private static final byte OWReadScratchPadCmd = (byte) 0xBE;                   
    //private static final byte DS2482StatusRegister = (byte) 0xF0;
    
        // The read-only Status register is the general means for the DS2482 to report 
        // bit-type data from the 1-Wire side, 1-Wire busy status and its own reset status 
        // to the host processor.
        // All 1-Wire communication commands and the Device Reset command position 
        // the read pointer at the Status register for the host processor to read with minimal
        // protocol overhead. Status information is updated during the execution of certain 
        // commands only. Details are given in the description of the various status bits below.
        
        // bit7 bit6 bit5 bit4 bit3 bit2 bit1 bit0
        //========================================
        //  DIR  TSB  SBR  RST   LL   SD  PPD  1WB
        //
    private static final byte STATUS_1WB = 0x01;    // 1-wire busy
    private static final byte STATUS_PPD = 0x02;    // Presence Pulse Detected
    private static final byte STATUS_SD  = 0x04;    // Short Detected
    private static final byte STATUS_LL  = 0x08;    // Logic Level of the selected 1-wire line withour initiating any 1-wire communication
    private static final byte STATUS_RST = 0x10;    // If '1', the DS2482 has performed a reset
    private static final byte STATUS_SBR = 0x20;    // Single-Bit Result - logic state of 1-wire line sampled at tMSR of a 
                                                    // 1-wire Single Bit command or the 1st bit of a 1-wire Triplet command   
    private static final byte STATUS_TSB = 0x40;    // Triplet Send Bit - reports state of the active 1-wire line sampled at tMSR of the 2nd bit
                                                    // of a 1-wire Triplet command.  Power-on value = 0
    private static final byte STATUS_DIRECTION_TAKEN = (byte) 0x80; // Branch Direction taken - search direction chosen by the 3rd bit of the triplet
    
   /** Flag to indicate next search will look only for alarming devices */
   private final boolean doAlarmSearch  = false;

   /** Flag to indicate next search will be a 'first' */
   private final boolean resetSearch    = true;

   /** Flag to indicate next search will not be preceeded by a 1-Wire reset */
   private final boolean skipResetOnSearch    = false;
    
   /** Speed modes for 1-Wire Network, regular                    */
   public static final int SPEED_REGULAR = 0;

   /** Speed modes for 1-Wire Network, flexible for long lines    */
   /** (Pretty sure not used for DS2482 devices                   */
   public static final int SPEED_FLEX = 1;

   /** Speed modes for 1-Wire Network, overdrive                  */
   public static final int SPEED_OVERDRIVE = 2;

   /** Speed modes for 1-Wire Network, hyperdrive                 */
   /** (Pretty sure not used for DS2482 devices                   */
   public static final int SPEED_HYPERDRIVE = 3;

   /** 1-Wire Network level, normal (weak 5Volt pullup)                            */
   public static final char LEVEL_NORMAL = 0;

   /** 1-Wire Network level, (strong 5Volt pullup, used for power delivery) */
   public static final char LEVEL_POWER_DELIVERY = 1;

   /** 1-Wire Network level, (strong pulldown to 0Volts, reset 1-Wire)      */
   public static final char LEVEL_BREAK = 2;

   /** 1-Wire Network level, (strong 12Volt pullup, used to program eprom ) */
   public static final char LEVEL_PROGRAM = 3;

   /** 1-Wire Network reset result = no presence */
   public static final int RESET_NOPRESENCE = 0x00;

   /** 1-Wire Network reset result = presence    */
   public static final int RESET_PRESENCE = 0x01;

   /** 1-Wire Network reset result = alarm       */
   public static final int RESET_ALARM = 0x02;

   /** 1-Wire Network reset result = shorted     */
   public static final int RESET_SHORT = 0x03;

   /** Condition for power state change, immediate                      */
   public static final int CONDITION_NOW = 0;

   /** Condition for power state change, after next bit communication   */
   public static final int CONDITION_AFTER_BIT = 1;

   /** Condition for power state change, after next byte communication  */
   public static final int CONDITION_AFTER_BYTE = 2;

   /** Duration used in delivering power to the 1-Wire, 1/2 second         */
   public static final int DELIVERY_HALF_SECOND = 0;

   /** Duration used in delivering power to the 1-Wire, 1 second           */
   public static final int DELIVERY_ONE_SECOND = 1;

   /** Duration used in delivering power to the 1-Wire, 2 seconds          */
   public static final int DELIVERY_TWO_SECONDS = 2;

   /** Duration used in delivering power to the 1-Wire, 4 second           */
   public static final int DELIVERY_FOUR_SECONDS = 3;

   /** Duration used in delivering power to the 1-Wire, smart complete     */
   public static final int DELIVERY_SMART_DONE = 4;

   /** Duration used in delivering power to the 1-Wire, infinite           */
   public static final int DELIVERY_INFINITE = 5;

   /** Duration used in delivering power to the 1-Wire, current detect     */
   public static final int DELIVERY_CURRENT_DETECT = 6;

   /** Duration used in delivering power to the 1-Wire, 480 us             */
   public static final int DELIVERY_EPROM = 7;

   final int c1WS = 0x00;
   final int cSPU = 0x00;
   final int cPPM = 0x00;
   //int cAPU = CONFIG_APU;  
      
   final byte CONFIG_APU = 0x01;    // Set Active-Pullup
       
   public byte DS2482Config = (byte) (c1WS | cSPU | cPPM | CONFIG_APU);
   //--------
   //-------- Variables
   //--------
     
    /**
     * Constructor
     * 
     * @param address
     * @param bus
     */       
    public I2CBridgeAdapter(byte address, byte bus) {
        super(address, bus);
        DS2482WrtCfg(DS2482Config);
        this.CurrentDevice = null;
        this.CurrentDevice = new byte[8];
        this.device_serial_no = new byte[7];
        setVerbose(true);
        // Initialize Dallas Semiconductor CRC table
        //byte[] dscrc_table = new byte[256];
    }   
    

   /**
    *  Retrieve the name of the port adapter as a string.  The 'Adapter'
    *  is a device that connects to a 'port' that allows one to
    *  communicate with an iButton or other 1-Wire device.  As example
    *  of this is 'DS9097E'.
    *
    *  @return  <code>String</code> representation of the port adapter.
    */
   public String getAdapterName()
   {
      return "DS2482Family";
   }

   /**
    * Retrieves the version of the adapter.
    *
    * @return  <code>String</code> of the adapter version.  It will return
    * "<na>" if the adapter version is not or cannot be known.
    *
    * @throws OneWireIOException on a 1-Wire communication error such as
    *         no device present.  This could be
    *         caused by a physical interruption in the 1-Wire Network due to
    *         shorts or a newly arriving 1-Wire device issuing a 'presence pulse'.
    * @throws OneWireException on a communication or setup error with the 1-Wire
    *         adapter
    */
   public String getAdapterVersion ()
      throws OneWireIOException, OneWireException
   {
      return "<na>";
   }
   
   /**
    * Returns whether adapter can physically support overdrive mode.
    *
    * @return  <code>true</code> if this port adapter can do OverDrive,
    * <code>false</code> otherwise.
    *
    * @throws OneWireIOException on a 1-Wire communication error with the adapter
    * @throws OneWireException on a setup error with the 1-Wire
    *         adapter
    */
   public boolean canOverdrive ()
      throws OneWireIOException, OneWireException
   {
      return false;
   }

   /**
    * Returns whether the adapter can physically support hyperdrive mode.
    *
    * @return  <code>true</code> if this port adapter can do HyperDrive,
    * <code>false</code> otherwise.
    *
    * @throws OneWireIOException on a 1-Wire communication error with the adapter
    * @throws OneWireException on a setup error with the 1-Wire
    *         adapter
    */
   public boolean canHyperdrive ()
      throws OneWireIOException, OneWireException
   {
      return false;
   }

   /**
    * Returns whether the adapter can physically support flex speed mode.
    *
    * @return  <code>true</code> if this port adapter can do flex speed,
    * <code>false</code> otherwise.
    *
    * @throws OneWireIOException on a 1-Wire communication error with the adapter
    * @throws OneWireException on a setup error with the 1-Wire
    *         adapter
    */
   public boolean canFlex ()
      throws OneWireIOException, OneWireException
   {
      return false;
   }

   /**
    * Returns whether adapter can physically support 12 volt power mode.
    *
    * @return  <code>true</code> if this port adapter can do Program voltage,
    * <code>false</code> otherwise.
    *
    * @throws OneWireIOException on a 1-Wire communication error with the adapter
    * @throws OneWireException on a setup error with the 1-Wire
    *         adapter
    */
   public boolean canProgram ()
      throws OneWireIOException, OneWireException
   {
      return false;
   }

   /**
    * Gets exclusive use of the 1-Wire to communicate with an iButton or
    * 1-Wire Device.
    * This method should be used for critical sections of code where a
    * sequence of commands must not be interrupted by communication of
    * threads with other iButtons, and it is permissible to sustain
    * a delay in the special case that another thread has already been
    * granted exclusive access and this access has not yet been
    * relinquished. <p>
    *
    * It can be called through the OneWireContainer
    * class by the end application if they want to ensure exclusive
    * use.  If it is not called around several methods then it
    * will be called inside each method.
    *
    * @param blocking <code>true</code> if want to block waiting
    *                 for an excluse access to the adapter
    * @return <code>true</code> if blocking was false and a
    *         exclusive session with the adapter was aquired
    *
    */
   public void beginExclusive (boolean blocking) {
       //System.out.println("beginExclusive");
       BRIDGE_BUSY = blocking;
   }

   
   /**
    * Relinquishes exclusive control of the 1-Wire Network.
    * This command dynamically marks the end of a critical section and
    * should be used when exclusive control is no longer needed.
    */
   public void endExclusive () {
       BRIDGE_BUSY = false;
       //System.out.println("endExclusive");
   };

   
     
   /**
    *  Retrieve a description of the port required by this port adapter.
    *  An example of a 'Port' would 'serial communication port'.
    *
    *  @return  <code>String</code> description of the port type required.
    */
   public String getPortTypeDescription()
   {
      return "I2C 1-wire bridge";
   }

   /**
    *  Retrieve a version string for this class.
    *
    *  @return  version string
    */
   public String getClassVersion()
   {
      return CLASS_VERSION;
   }
   
   /**
    *  Retrieve a list of the platform appropriate port names for this
    *  adapter.  A port must be selected with the method 'selectPort'
    *  before any other communication methods can be used.  Using
    *  a communication method before 'selectPort' will result in
    *  a <code>OneWireException</code> exception.
    *
    *  @return  enumeration of type <code>String</code> that contains the port
    *  names
    */
   public Enumeration getPortNames()
   {
      return null;
   }
   
   /**
    *  Retrieve the name of the selected port as a <code>String</code>.
    *
    *  @return  <code>String</code> of selected port
    *
    *  @throws OneWireException if valid port not yet selected
    */
   public String getPortName() throws OneWireException
   {
      return "I2C port";
   }
   
    /**
     *
     * @return 
     */
    public byte[] getReadableCurrentDeviceAddress() {
        byte[] right_sized = new byte[8];
        int i, j;
        for (i = 7, j = 0; i >= 0; i--, j++ ) {         
                right_sized[j] = CurrentDevice[i];
        }
        return right_sized;
    }
    
    /**
    *  Detect adapter presence on the selected port.
    *
    *  @return  <code>true</code> if the adapter is confirmed to be connected to
    *  the selected port, <code>false</code> if the adapter is not connected.
    *
    *  @throws OneWireIOException
    *  @throws OneWireException
    */
   public boolean adapterDetected() throws OneWireIOException, OneWireException {     
      return adapterPresent;
   }
   
   public void freePort() {
       
   }
   
    @Override
    public void close() {
        if(!TESTMODE) {
            if (i2c_device.isOpen()) {
                try {
                    i2c_device.close();                    
                } catch (IOException ex) {
                    Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            System.out.println("[close] In TESTMODE");
        }
    }
    
    /**
     * Returns the "family" byte of the 1-wire device
     * @return
     */
    public int getOWFamily() {
        return CurrentDevice[0];
    }
    
    /**
     *
     * @return
     */
    public byte getDeviceCRC() {
        return CurrentDevice[7];
    }
    
   /**
    * Copies the 'current' iButton address being used by the adapter into
    * the array.  This address is the last iButton or 1-Wire device found
    * in a search (findNextDevice()...).
    * This method copies into a user generated array to allow the
    * reuse of the buffer.  When searching many iButtons on the one
    * wire network, this will reduce the memory burn rate.
    *
    * @param  address An array to be filled with the current iButton address.
    * @see    com.dalsemi.onewire.utils.Address
    */
   public void getAddress(byte[] address)
   {
      System.arraycopy(CurrentDevice, 0, address, 0, 8);
   }
    
   /**
    * Gets the 'current' 1-Wire device address being used by the adapter as a long.
    * This address is the last iButton or 1-Wire device found
    * in a search (findNextDevice()...).
    *
    * @return <code>long</code> representation of the iButton address
    * @see   com.dalsemi.onewire.utils.Address
    */
   public long getAddressAsLong ()
   {
      byte[] address = new byte [8];

      getAddress(address);

      return Address.toLong(address);
   }

   /**
    * Gets the 'current' 1-Wire device address being used by the adapter as a String.
    * This address is the last iButton or 1-Wire device found
    * in a search (findNextDevice()...).
    *
    * @return <code>String</code> representation of the iButton address
    * @see   com.dalsemi.onewire.utils.Address
    */
   public String getAddressAsString ()
   {
      byte[] address = new byte [8];

      getAddress(address);

      return Address.toString(address);
   }

   /**
    * Verifies that the iButton or 1-Wire device specified is present
    * on the 1-Wire Network and in an alarm state. This does not
    * affect the 'current' device state information used in searches
    * (findNextDevice...).
    *
    * @param  address  device address to verify is present and alarming
    *
    * @return  <code>true</code> if device is present and alarming, else
    * <code>false</code>.
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    *
    * @see   com.dalsemi.onewire.utils.Address
    */
   public boolean isAlarming (byte[] address)
      throws OneWireIOException, OneWireException
   {
      OWReset();
      OWWriteByte((byte) 0xEC);   // Conditional search commands

      //return strongAccess(address);
      return true;
   }

    /**
     * <p>Find the "first" device on the 1-wire network</p>
     * @author Bruce Juntti bjuntti at unixwizardry.com
     * @return boolean TRUE if device found, ROM number in CurrentDevice buffer
     */   
    public boolean findFirstDevice() {
        // reset the search state
        LastDiscrepancy = 0;
        LastDeviceFlag = false;
        LastFamilyDiscrepancy = 0;
        return OWSearch();
    }
    
    /**
     *<p>Finds the next device on the 1-wire bus.  Does not change the search state</p>
     * @return TRUE if another device was found, otherwise false 
     */
    public boolean findNextDevice() {
        // Leave the search state alone
        return OWSearch();
    }
	
    
    public OneWireContainer getDeviceContainer () {
      // Mask off the upper bit.
      byte[] address = new byte [8];
      getAddress(address);

      return getDeviceContainer(CurrentDevice);
   }
    
    /**
    * Constructs a <code>OneWireContainer</code> object with a user supplied 
    * 1-Wire network address.
    *
    * @param  address  device address with which to create a new container
    *
    * @return  The <code>OneWireContainer</code> object
    * @see   com.dalsemi.onewire.utils.Address
    */
    public OneWireContainer getDeviceContainer (byte[] address) {
      int              family_code   = address [0] & 0x7F;
      String           family_string =
         ((family_code) < 16)
         ? ("0" + Integer.toHexString(family_code)).toUpperCase()
         : (Integer.toHexString(family_code)).toUpperCase();
      Class            ibutton_class = null;
      OneWireContainer new_ibutton;
      //System.out.println("[getDeviceContainer] getDeviceContainer(" + bytesToHex(address));

      // If we don't get one, do the normal lookup method.
      if (ibutton_class == null)
      {

         // try to load the ibutton container class
         try
         {
            ibutton_class =
               Class.forName("com.unixwizardry.onewire.container.OneWireContainer"
                             + family_string);
         }
         catch (Exception e)
         {
            ibutton_class = null;
         }

         // if did not get specific container try the general one
         if (ibutton_class == null)
         {

            // try to load the ibutton container class
            try
            {
               ibutton_class = Class.forName(
                  "com.unixwizardry.onewire.container.OneWireContainer");
            }
            catch (Exception e)
            {
               System.out.println("EXCEPTION: Unable to load OneWireContainer"
                                  + e);
               return null;
            }
         }
      }

      // try to load the ibutton container class
      try
      {
         // create the iButton container with a reference to this adapter
         new_ibutton = ( OneWireContainer ) ibutton_class.newInstance();

         new_ibutton.setupContainer(this, address);
      }
      catch (Exception e)
      {
         System.out.println(
            "EXCEPTION: Unable to instantiate OneWireContainer "
            + ibutton_class + ": " + e);
         e.printStackTrace();

         return null;
      }

      // return this new container
      return new_ibutton;
   }

	
   /**
    * Returns a <code>OneWireContainer</code> object corresponding to the first iButton
    * or 1-Wire device found on the 1-Wire Network. If no devices are found,
    * then a <code>null</code> reference will be returned. In most cases, all further
    * communication with the device is done through the <code>OneWireContainer</code>.
    *
    * @return  The first <code>OneWireContainer</code> object found on the
    * 1-Wire Network, or <code>null</code> if no devices found.
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    */
   public OneWireContainer getFirstDeviceContainer () throws OneWireIOException, OneWireException
   {
        if (findFirstDevice() == true) {
            return getDeviceContainer();
        }
        else
            return null;
   }
   
   
   /**
    * Returns a <code>OneWireContainer</code> object corresponding to the next iButton
    * or 1-Wire device found. The previous 1-Wire device found is used
    * as a starting point in the search.  If no devices are found,
    * then a <code>null</code> reference will be returned. In most cases, all further
    * communication with the device is done through the <code>OneWireContainer</code>.
    *
    * @return  The next <code>OneWireContainer</code> object found on the
    * 1-Wire Network, or <code>null</code> if no iButtons found.
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    */
   public OneWireContainer getNextDeviceContainer () throws OneWireIOException, OneWireException
   {
      if (findNextDevice() == true)
      {
         return getDeviceContainer();
      }
      else
         return null;
   }
   
   
   /**
    * Returns an enumeration of <code>OneWireContainer</code> objects corresponding
    * to all of the iButtons or 1-Wire devices found on the 1-Wire Network.
    * If no devices are found, then an empty enumeration will be returned.
    * In most cases, all further communication with the device is done
    * through the OneWireContainer.
    *
    * @return  <code>Enumeration</code> of <code>OneWireContainer</code> objects
    * found on the 1-Wire Network.
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    */
   public Enumeration getAllDeviceContainers() throws OneWireIOException, OneWireException
   {      
        ArrayList ibutton_list = new ArrayList();
        OneWireContainer ibutton;

        ibutton = getFirstDeviceContainer();
        if (ibutton != null) {
            msg = "Adding device container to list: " + ibutton;
            printMessage(msg, "getAllDeviceContainers()", INFO);
            ibutton_list.add(ibutton);

         // loop to get all of the ibuttons
         do {
            ibutton = getNextDeviceContainer();           
            if (ibutton != null) {
                msg = "Adding device container to list: " + ibutton;
                printMessage(msg, "getAllDeviceContainers()", INFO);               
                ibutton_list.add(ibutton);
            }
         }
         while (ibutton != null);
      }
      return Collections.enumeration(ibutton_list);
   }

	
            
    /*
     * OWFirst
     * <p>
     * Find the "first" devices on the 1-wire network
     * @version    0.00, 6 June 2015
     * @author Bruce Juntti bjuntti at unixwizardry.com
     * @return boolean TRUE if device found, ROM number in CurrentDeviceESS buffer;
          FALSE if no device present
     */

    public boolean OWFirst() {
        // reset the search state
        LastDiscrepancy = 0;
        LastDeviceFlag = false;
        LastFamilyDiscrepancy = 0;
        return OWSearch();
    }
    
  
    public boolean OWNext() {
        // Leave the search state alone
        return OWSearch();
    }
    
    /**
     * Resets the DS2482 1-wire bridge which does a global reset of the device 
     * state-machine logic and terminates any ongoing 1-Wire communication.
     * The command code for the device reset is 0xF0.
     * <p>
     * @version    0.00, 6 June 2015
     * @author Bruce Juntti <bjuntti at unixwizardry.com>
     * @return Status byte
     * <p>
     *  bit7 bit6 bit5 bit4 bit3 bit2 bit1 bit0
     *  ========================================
     *  DIR  TSB  SBR  RST   LL   SD  PPD  1WB
     * 
     */
    public boolean DS2482Reset() {
        int poll_count = 0;       
        byte status = 0;
        byte cmd = DS2482ResetCmd;
        
        //try {         
            I2CwriteByte(cmd);
        //} catch (OneWireException ex) {
        //    Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
        //} catch (IOException ex) {
        //    Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
        //} catch (Error ex) {
        //    Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
        //}
            do {            
                status = I2CreadByte();
                msg = " status = " + byteToHex(status);
                printMessage(msg, "DS2482Reset()", INFO);
                poll_count++;
            } while (0x01 == (status & STATUS_1WB) && poll_count < POLL_LIMIT);
        
        
        if ((status & STATUS_SD) == STATUS_SD) {
            System.out.println("[DS2482Reset] Short detected");
            return false;
        }
        //System.out.println("[DS2482Reset] Status: " + PrintBits(0, status));
        return (((status & STATUS_RST) == STATUS_RST) ? true: false); 
    }   
        

    /**
     * Does a RESET on the DS2482 device
     * @return TRUE if it went OK, otherwise FALSE
     */
    @Override
    public boolean DS2482Detect() {       
        if ( !DS2482Reset() ) {
            msg = "Detect returned false";
            printMessage(msg, "DS2482Detect()", ERROR);
            return false;
        }
        //
        // Putting config register back, setting APU
        //
        byte cfg = DS2482WrtCfg(DS2482Config);
        msg = "cfg is now = " + PrintBits(1, cfg);
        printMessage(msg, "DS2482Detect()", INFO );      
        return true;  
    }   

    /**
     * reset()
     * (AKA OWreset)
     * Does a reset of the 1-wire bus via the DS2482 I2C 1-wire bridge
     * @return status byte from DS2482
     * <p>
     *  bit7 bit6 bit5 bit4 bit3 bit2 bit1 bit0
     *  ========================================
     *  DIR  TSB  SBR  RST   LL   SD  PPD  1WB
     * 
     */
    public int OWReset() {     
        byte reset_cmd = DS2482_1WireResetCmd;
        int poll_count = 0;
        byte status_reg = 0;
        byte retval;
        //I2CwriteBlock(tmp);
        I2CwriteByte(reset_cmd);
        do {
        //    try {
        //        Thread.sleep(200);
        //    } catch (InterruptedException ex) {
        //       Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
        //    }
        //    
            status_reg = I2CreadByte();
            poll_count++;
        } while (0x01 == (status_reg & STATUS_1WB) && poll_count < POLL_LIMIT);
        msg = "status register: " + PrintBits(0, status_reg);
        printMessage(msg, "OWReset()", INFO);
        if((status_reg & STATUS_PPD) == STATUS_PPD)  {
            msg = "returning RESET_PRESENCE";
            printMessage(msg, "OWReset()", INFO);
            return RESET_PRESENCE;
        } else if ((status_reg & STATUS_SD) == STATUS_SD ) {
            msg = "returning RESET_SHORT";
            printMessage(msg, "OWReset()", INFO);
            return RESET_SHORT;
        } else {
            msg = "returning RESET_NOPRESENCE";
            printMessage(msg, "OWReset()", INFO);
            return RESET_NOPRESENCE;
        }
    }
    
    /**
     * <p>
     * After a device reset (power-up cycle or initiated by the Device Reset command) 
     * the Configuration register reads 00h. When writing to the Configuration register, 
     * the new data is accepted only if the upper nibble (bits 7 to 4) is the one's complement 
     * of the lower nibble (bits 3 to 0). When read, the upper nibble is always 0h.
     * <pre>
     * CONFIGURATON REGISTER
     * bit t7 bit6 bit5 bit4 bit3 bit2 bit1 bit0
     * ========================================
     * ___  ___  ___  ___
     * 1WS  SPU  PPM  APU  1WS  SPU  PPM  APU
     * </pre>
     * @param config Lower nibble only used - the upper 1's complement is generated in this method
     * @return
     */
    @Override
    public byte DS2482WrtCfg(byte config) {
        int bitmask = 0x00FF;
        byte cfgreg;
        byte cfg = (byte) ((config | ~config << 4) & bitmask) ;
        byte result;
        byte[] temp = {DS2482_WriteConfigRegCmd, cfg};    
        msg = "Writing config " + byteToHex(cfg);
        printMessage(msg, "DS2482WrtCfg()", INFO);
        try {
            try {
                I2CwriteBlock(temp);
            } catch (IOException | Error ex) {
                Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (OneWireException ex) {
            Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }
        cfgreg = I2CreadByte();
        msg = "returned: " + PrintBits(1, cfgreg);
        printMessage(msg, "DS2482WrtCfg()", INFO);   
        return cfgreg;
    }
    

   /**
    * <p>
    * Send 1 bit of communication to the 1-Wire Net and return the
    * result 1 bit read from the 1-Wire Net. The parameter 'sendbit'
    * least significant bit is used and the least significant bit
    * of the result is the return bit.
    * 
    * @param sendbit the least significant bit is the bit to send
    * @return True or False
    */
    public boolean OWTouchBit(byte sendbit) {
        byte status_reg = 0;
        int poll_count = 0;
        byte[] tempp = {DS2482_1WireSingleBitCmd, sendbit};
        try {
            try {
                I2CwriteBlock(tempp);
            } catch (IOException | Error ex) {
                Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (OneWireException ex) {
            Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }
        do {
            status_reg = I2CreadByte();
        } while (0x01 == (status_reg & STATUS_1WB) && poll_count++ < POLL_LIMIT);
        if (poll_count == POLL_LIMIT) {            
            DS2482Reset();
            System.out.println("[OWTouchBit] Poll count exceeded; DS2482 was reset: result was " + PrintBits(0, status_reg));
            return false;
        }
        if ((status_reg & STATUS_SBR) == STATUS_SBR) {
            return true;
        } else {
            return false;
        }
    }
       

    /**
     * Send 8 bits of communication to the 1-Wire Net and return the
     * result 8 bits read from the 1-Wire Net. The parameter 'sendbyte'
     * least significant 8 bits are used and the least significant 8 bits
     * of the result are the return byte.
     * <p>
     * @param sendbyte - 8 bits to send (least significant byte)
     * @return - 8 bits to send (least significant byte)
     */
        public byte OWTouchByte(byte sendbyte) {
        if ( sendbyte == 0xFF) {
            return OWReadByte();
        } else {
            OWWriteByte(sendbyte);
            return sendbyte;
        }
    }
 
    
   /**
    * OWSearch does a 1-wire search using the 1-wire Triplet command.
    * 
    * resetSearch - Reset the search (1), or not (0)
    * lastdevice - If the last device has been found (1), or not (0)
    * deviceAddress - the returned serial number
    *   
    * continues from the previous search state. The search state
    * can be reset by using the 'OWFirst' function.
    * This function contains one parameter 'alarm_only'.
    * When 'alarm_only' is TRUE (1) the find alarm command
    * 0xEC is sent instead of the normal search command 0xF0.
    * Using the find alarm command 0xEC will limit the search to only
    * 1-Wire devices that are in an 'alarm' state.
    * 
    * @version    0.00, 6 June 2015
    * @author Bruce Juntti <bjuntti at unixwizardry.com>
    * @return 
    * <p>
    * TRUE (1) if 1-wire device was found and its Serial Number
    * placed in the global ROM
    * <p>
    * FALSE (0): when no new device was found.  Either the are no devices
    * on the 1-Wire Net.
    * 
    */   
    public boolean OWSearch() {
        int id_bit_number = 1;
        int last_zero = 0, rom_byte_number = 0;
        boolean search_result = false;
        int presence;
        byte id_bit, cmp_id_bit;
        byte rom_byte_mask = 0x01;
        byte search_direction;
        byte status;
        
        if (LastDeviceFlag) {
            //System.out.println("1-wire search completed");
            LastDiscrepancy = 0;
        }
        
        // if the last call was not the last one
        if (!LastDeviceFlag) {
            presence = OWReset();        
            if ( presence != RESET_PRESENCE ) {
                // Then reset the search
                LastDiscrepancy = 0;
                LastDeviceFlag = false;
                LastFamilyDiscrepancy = 0;
                return false;
            } 
                       
            OWWriteByte( (byte)OWSearchCmd);            
            
            // Loop to do the search
            do {
                // if this discrepancy is before the Last Discrepancy
                // on a previous next then pick the same as last time                
                if (id_bit_number < LastDiscrepancy) {
                    if ( (CurrentDevice[rom_byte_number] & rom_byte_mask) > 0)                         
                        search_direction = 1;
                    else 
                        search_direction = 0;                                                                                                    
                } else {
                    // if equal to last pick 1, if not then pick 0
                    if (id_bit_number == LastDiscrepancy) 
                        search_direction = 1;
                     else 
                        search_direction = 0;                    
                }
                
                // Perform a 1-wire triplet operation on the DS2482 which will perform
                // 2 read bits and 1 write bit               
                status = DS2482OWTriplet(search_direction);                
                                
                id_bit = (byte) (status & STATUS_SBR);
                cmp_id_bit = (byte) (status & STATUS_TSB);
                int IDbit = 0, cmpIDbit = 0;
                
                if (id_bit > 0)
                    IDbit = 1;
                if (cmp_id_bit > 0)
                    cmpIDbit = 1;
                
                if (IDbit == 1) {
                    if (cmpIDbit == 1) {
                        break;
                    }
                }
                
                search_direction = (byte) (((status & STATUS_DIRECTION_TAKEN) == STATUS_DIRECTION_TAKEN) ? 1 : 0);
                               
                if (id_bit == 0x20 && cmp_id_bit == 0x40) {         // If both id_bit and its complement are 1, 
                    LastDiscrepancy = LastFamilyDiscrepancy = 0;    // then no 1-wire devices were found
                    LastDeviceFlag = false;
                    break;
                } else {
                    if (id_bit == 0 && cmp_id_bit == 0 && (search_direction == 0)) {
                        last_zero = id_bit_number;
                        // Check for last discrepancy in family
                        if (last_zero < 9)
                            LastFamilyDiscrepancy = last_zero;                        
                    }
                    // set or clear the bit in the ROM byte rom_byte_number
                    // with mask rom_byte_mask
                    if (search_direction == 1) {
                        CurrentDevice[rom_byte_number] |= rom_byte_mask; 
                    } else {
                        CurrentDevice[rom_byte_number] &= ~rom_byte_mask;
                    }
                
                    
                    // increment the byte counter id_bit_number
                    // and shift the mask rom_byte_mask
                    id_bit_number++;
                    rom_byte_mask <<= 1;
                    
                    // if the mask is 0 then go to new SerialNum byte rom_byte_number
                    // and reset mask
                    if (rom_byte_mask == 0) {                       
                        rom_byte_number++;
                        rom_byte_mask = 1;                        
                    }                     
                }
                if (rom_byte_number < 7)
                    device_serial_no[rom_byte_number] = CurrentDevice[rom_byte_number];
                
            } while (rom_byte_number < 8);  // Loop through all ROM bytes 0-7
            
            // if the search was successful then
            if (!((id_bit_number < 65) || (crc8 != 0))) {
                // search successful so set LastDiscrepancy, LastDeviceFlag, search_result
                //System.out.println("Found device at " + bytesToHex(CurrentDevice));
                // Getting the CRC8 value of the device serial needs work - it does the CRC of the FIRST device, then bails
                //crc8 = computeCRC8(device_serial_no);  // Calculate the CRC               
                //System.out.println("Calculated CRC of device serial " + bytesToHex(device_serial_no) + " is " + Integer.toHexString(crc8));
                LastDiscrepancy = last_zero;                
                // check for last device
                if (LastDiscrepancy == 0) {
                   LastDeviceFlag = true; 
                }
                search_result = true;
            }
        }
                
        // if no device found then reset counters so next
        // 'search' will be like a first
        if (!search_result || (CurrentDevice[0] == 0)) {
            LastDiscrepancy = 0;
            LastDeviceFlag = false;
            LastFamilyDiscrepancy = 0;
            search_result = false;
        }
        return search_result;
    }
    
    //--------------------------------------------------------------------------
    // Use the DS2482 help command '1-Wire triplet' to perform one bit of a
    // 1-Wire search.
    // This command does two read bits and one write bit. The write bit
    // is either the default direction (all device have same bit) or in case of
    // a discrepancy, the 'search_direction' parameter is used.
    //
    // Returns – The DS2482 status byte result from the triplet command
    //
    @Override
    public byte DS2482OWTriplet(byte search_direction) {
        byte direction;
        int poll_count = 0;
        byte received = 0;
        direction = search_direction > 0 ? (byte) 0xFF : 0x0; 
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        byte[] temp = {DS2482_1WireTripletCmd, direction};
        try {
            try {
                I2CwriteBlock(temp);
            } catch (IOException | Error ex) {
                Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (OneWireException ex) {
            Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }
        do {
            received = I2CreadByte();
        } while (0x01 == STATUS_1WB && poll_count++ < POLL_LIMIT);

        if (poll_count == POLL_LIMIT) {
            DS2482Reset();
            System.out.println("[DS2482_OWtriplet] Poll count exceeded; DS2482 was reset: result was " + PrintBits(0, received));
            return 0x0;
        }
        return received;
    }
      
        
    //--------------------------------------------------------------------------
    // Select the 1-Wire channel on a DS2482-800.
    //
    // Returns: TRUE if channel selected
    //          FALSE device not detected or failure to perform select
    //
    @Override
    public boolean DS2482ChannelSelect(int channel) {
        byte ch, ch_read, check;      
       
        switch (channel) {
            default:
            case 0:
                ch = (byte) 0xF0;
                ch_read = (byte) 0xB8;
                break;
            case 1:
                ch = (byte) 0xE1;
                ch_read = (byte) 0xB1;
                break;
            case 2:
                ch = (byte) 0xD2;
                ch_read = (byte) 0xAA;
                break;
            case 3:
                ch = (byte) 0xC3;
                ch_read = (byte) 0xA3;
                break;
            case 4:
                ch = (byte) 0xB4;
                ch_read = (byte) 0x9C;
                break;
            case 5:
                ch = (byte) 0xA5;
                ch_read = (byte) 0x95;
                break;
            case 6:
                ch = (byte) 0x96;
                ch_read = (byte) 0x8E;
                break;
            case 7:
                ch = (byte) 0x87;
                ch_read = (byte) 0x87;
                break;
        }

        byte[] buffer = {DS2482_SEL_CHANNEL, (byte) ch}; 
        msg = "sending = " + toHexString(buffer);
        printMessage(msg, "DS2482ChannelSelect()", INFO);
        try {
            I2CwriteBlock(buffer);           
        } catch (IOException | OneWireException | Error ex) {
            return false;
        }
        check = I2CreadByte();
        msg = "check = " + byteToHex(check);
        printMessage(msg, "DS2482ChannelSelect()", INFO);
        // check for failure due to incorrect read back of channel
         
        if (check == ch_read)
            return true;
        else
            return false;
    }
         
    /**
     * OWWriteBit - Writes a single bit to the device
     * @param sendbyte
     * @return True for success or False for failure
     */
    public boolean OWWriteBit(byte sendbyte) {
        byte byteToSend = (byte) (sendbyte & 0xFF);
        byte received;
        int poll_count = 0;
        byte[] tempp = {DS2482_1WireWriteByteCmd};
        try {
            try {
                I2CwriteBlock(tempp);
            } catch (IOException | Error ex) {
                Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (OneWireException ex) {
            Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }
        do {
            received = I2CreadByte();
        } while (0x01 == (received & STATUS_1WB) && poll_count++ < POLL_LIMIT);
        if (poll_count == POLL_LIMIT) {
            DS2482Reset();
            System.out.println("[OWWriteByte] Poll count exceeded; DS2482 was reset: result was " + PrintBits(0, received));
            return false;
        }
        return true;
    }        

    /**
     * Send 8 bits of communication to the 1-Wire Net 
     * <p>
     * The parameter 'sendbyte' least significant 8 bits are used.
     * @param sendbyte - 8 bits to send (least significant byte)
     */
    public void OWWriteByte(byte sendbyte) {
        byte byteToSend = (byte) (sendbyte & 0xFF);
        byte received_status;
        int poll_count = 0;
        byte[] tempp = {DS2482_1WireWriteByteCmd, byteToSend};
        try {
            try {
                I2CwriteBlock(tempp);
            } catch (IOException | Error ex) {
                Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (OneWireException ex) {
            Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }
        do {
            received_status = I2CreadByte();
        } while (0x01 == (received_status & STATUS_1WB) && poll_count++ < POLL_LIMIT);
        if (poll_count == POLL_LIMIT) {
            DS2482Reset();
            System.out.println("[OWWriteByte] Poll count exceeded; DS2482 was reset: result was " + PrintBits(0, received_status));
        }
    }
    
   /**
    *  Sends a block of data and returns the data received in the same array.
    *  This method is used when sending a block that contains reads and writes.
    *  The 'read' portions of the data block need to be pre-loaded with 0xFF's.
    *  It starts sending data from the index at offset 'off' for length 'len'.
    *
    *  @param  dataBlock  array of data to transfer to and from the 1-Wire Network.
    *  @param  off        offset into the array of data to start
    *  @param  len        length of data to send / receive starting at 'off'
    *
    *  For family 10 device first incoming block looks like:
    * 
    *  read scratchpad cmd
    *     /-- 9 bytes of all 1's --\
    *  BE FF FF FF FF FF FF FF FF FF
    *  BE 1F 00 1C 17 FF FF 06 4D 8D  // Real output
    *  returns:
    *  BE 01 02 03 04 05 06 07 08 09
    *    |------ CRC8 ------------|
    *
    *  For family 26 device incoming first block looks like:
    * 
    *  Recall memory cmd
    *  /
    * B8 01<--- page number
    *
    * Second incoming block looks like
    * read scratchpad cmd
    *    /--- 10 bytes of all 1's ---\
    * BE FF FF FF FF FF FF FF FF FF FF
    * returns:
    * BE FF 01 02 03 04 05 06 07 08 09
    *       |------ CRC8 ------------|
    */
                                     
    public void dataBlock(byte dataBlock[], int off, int len) {
        int t_off, t_len;
        t_off = off;
        t_len = len;               
        byte cmd1 = dataBlock[0];   
        byte cmd2 = dataBlock[1];       
        msg = "[I2CBridgeAdapter][dataBlock] dataBlock = " + bytesToHexLE(dataBlock) + " ,off = " 
                + off + " ,len = " + len;
        printMessage(msg, "dataBlock()", INFO);
        
        for (int i = 0; i < t_len; i++) {           
            if (( dataBlock[i] & 0xFF) != 0xFF ) {                 
                t_off++;
                OWWriteByte(dataBlock[i]);
            }
        }
                           
        byte recv[] = new byte[t_len];    // allocate space for read from device 
        int j = t_off;
        for ( int i = 0; j < recv.length; i++, j++ ) {           
            recv[i] = OWReadByte();           
        }          
        System.arraycopy(recv, 0, dataBlock, t_off, t_len - t_off);                          
    }
          
    /**
    *  Sends a block of data and returns the data received in the same array.
    *  This method is used when sending a block that contains reads and writes.
    *  The 'read' portions of the data block need to be pre-loaded with 0xFF's.
    *  It starts sending data from the index at offset 'off' for length 'len'.
    *
    *  @param  dataBlock  array of data to transfer to and from the 1-Wire Network.
    *  @param  off        offset into the array of data to start
    *  @param  len        length of data to send / receive starting at 'off'
    */
    public void dataBlockORIG(byte dataBlock[], int off, int len) {
        int t_off, t_len;
        t_off = off;
        t_len = len;
        byte cmd1 = dataBlock[0];
        byte cmd2 = dataBlock[1];
        byte ignore = 1;
        msg = "[I2CBridgeAdapter][dataBlockORIG] dataBlock = " + Convert.toHexString(dataBlock) + ", off = " 
                + off + ", len = " + len;
        //System.out.println(msg);
        printMessage(msg, "dataBlockORIG()", INFO);
                        
        OWWriteByte(cmd1);
        if ( cmd2 != (byte) 0xff ) {
            System.out.println("[Writing " + byteToHex(cmd2) + " to 1-wire bus");
            OWWriteByte(cmd2);
            ignore++;
        }       
        
        byte recv[] = new byte[t_len];    // allocate space for read from device 
        int j = t_off;
        for ( int i = 0; j < t_len - ignore; i++, j++ ) {           
            recv[i] = OWReadByte();           
        }          
        System.arraycopy(recv, 0, dataBlock, ignore, t_len - ignore);       
    }
    
    
    //--------------------------------------------------------------------------
    // Send 8 bits of read communication to the 1-Wire Net and return the
    // result 8 bits read from the 1-Wire Net.
    //
    // Returns:  8 bits read from 1-Wire Net
    //
    // NOTE: To read the data byte received from the 1-Wire line, issue the Set
    //       Read Pointer command and select the Read Data register. Then access
    //       the DS2482 in read mode.
    //

    /**
     *
     * @return
     */
    public byte OWReadByte() {
        byte received;
        int poll_count = 0;
        byte temp = DS2482_1WireReadByteCmd;
        I2CwriteByte(temp);
        
        // loop checking 1WB bit for completion of 1-Wire operation
        // abort if poll limit reached       
        do {
            received = I2CreadByte();
        } while (0x01 == (received & STATUS_1WB) && poll_count++ < POLL_LIMIT);
        if (poll_count == POLL_LIMIT) {
            DS2482Reset();
            System.out.println("[OWReadByte] Poll count exceeded; DS2482 was reset: result was " + PrintBits(0, received));
        }    
        byte [] toSend = {DS2482SetReadPointer, DS2482ReadDataRegister };
        try {
            try {
                I2CwriteBlock(toSend);
            } catch (IOException | Error ex) {
                Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (OneWireException ex) {
            Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }
        do {
            received = I2CreadByte();
        } while (0x01 == (received & STATUS_1WB) && poll_count++ < POLL_LIMIT);
        if (poll_count == POLL_LIMIT) {
            DS2482Reset();
            System.out.println("[OWReadByte] Poll count exceeded; DS2482 was reset: result was " + PrintBits(0, received));
        }    
        return received;
    }
   
    
    /**
     *
     * @return
     */
    public byte[] getCurrentDevice() {
        return CurrentDevice;
    }
    
   /**
    * Returns the current data transfer speed on the 1-Wire Network. <p>
    *
    * @return <code>int</code> representing the current 1-Wire speed
    * <ul>
    * <li>     0 (SPEED_REGULAR) set to normal communication speed
    * <li>     1 (SPEED_FLEX) set to flexible communication speed used
    *            for long lines
    * <li>     2 (SPEED_OVERDRIVE) set to normal communication speed to
    *            overdrive
    * <li>     3 (SPEED_HYPERDRIVE) set to normal communication speed to
    *            hyperdrive
    * <li>     3 future speeds
    * </ul>
    */
   public int getSpeed ()
   {
      return SPEED_REGULAR;
   }  
   
     /**
    * Sets the new speed of data
    * transfer on the 1-Wire Network.
    * 
    * DS2482 1-Wire Speed (1WS) Bit:
    * ==============================
    * The 1WS bit determines the timing of any 1-Wire communication generated by the 
    * DS2482. All 1-Wire slave devices support standard speed (1WS = 0), where the transfer 
    * of a single bit (tSLOT in Figure 4) is completed within 65μs. Many 1-Wire device 
    * can also communicate at a higher data rate, called Overdrive speed. To change from
    * standard to Overdrive speed, a 1-Wire device needs to receive an Overdrive Skip ROM 
    * or Overdrive Match ROM command, as explained in the device data sheets. The change 
    * in speed occurs immediately after the 1-Wire device has received the speed-changing 
    * command code. The DS2482 must take part in this speed change to stay synchronized. 
    * This is accomplished by writing to the Configuration register with the 1WS bit being 1 
    * immediately after the 1-Wire Byte command that changes the speed of a 1-Wire device. 
    * Writing to the Configuration register with the 1WS bit being 0 followed by a 1-Wire 
    * Reset command changes the DS2482 and any 1-Wire devices on the active 1-Wire line back
    * to standard speed.
    * 
    * @param speed
    * <ul>
    * <li>     0 (SPEED_REGULAR) set to normal communcation speed
    * <li>     1 (SPEED_FLEX) set to flexible communcation speed used
    *            for long lines
    * <li>     2 (SPEED_OVERDRIVE) set to normal communcation speed to
    *            overdrive
    * <li>     3 (SPEED_HYPERDRIVE) set to normal communcation speed to
    *            hyperdrive
    * <li>     greater than 3 future speeds
    * </ul>
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    *         or the adapter does not support this operation
    */
   public void setSpeed (int speed) throws OneWireIOException, OneWireException
   {
      if (speed != SPEED_REGULAR)
         throw new OneWireException(
            "Non-regular 1-Wire speed not supported by this adapter type");
   }
   
   /**
    * Verifies that the iButton or 1-Wire device specified is present on
    * the 1-Wire Network. This does not affect the 'current' device
    * state information used in searches (findNextDevice...).
    *
    * @param  address  device address to verify is present
    *
    * @return  <code>true</code> if device is present, else
    *         <code>false</code>.
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    *
    * @see   com.unixwizardry.onewire.utils.Address
    */
   public boolean isPresent (byte[] address) throws OneWireIOException, OneWireException
   {
      OWReset();
      I2CwriteByte((byte) 0xF0);   // Search ROM command
      return true;
   }
     
     
   /**
    * Verifies that the iButton or 1-Wire device specified is present on
    * the 1-Wire Network. This does not affect the 'current' device
    * state information used in searches (findNextDevice...).
    *
    * @param  address  device address to verify is present
    *
    * @return  <code>true</code> if device is present, else
    *         <code>false</code>.
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    *
    * @see   com.dalsemi.onewire.utils.Address
    */
   public boolean isPresent (long address)
      throws OneWireIOException, OneWireException
   {
      return isPresent(Address.toByteArray(address));
   }

   /**
    * Verifies that the iButton or 1-Wire device specified is present on
    * the 1-Wire Network. This does not affect the 'current' device
    * state information used in searches (findNextDevice...).
    *
    * @param  address  device address to verify is present
    *
    * @return  <code>true</code> if device is present, else
    *         <code>false</code>.
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    *
    * @see   com.dalsemi.onewire.utils.Address
    */
   public boolean isPresent (String address)
      throws OneWireIOException, OneWireException
   {
      return isPresent(Address.toByteArray(address));
   }
   
   
   /**
    * Selects the specified iButton or 1-Wire device by broadcasting its
    * address.  This operation is referred to a 'MATCH ROM' operation
    * in the iButton and 1-Wire device data sheets.  This does not
    * affect the 'current' device state information used in searches
    * (findNextDevice...).
    *
    * Warning, this does not verify that the device is currently present
    * on the 1-Wire Network (See isPresent).
    *
    * @param  address    address of iButton or 1-Wire device to select
    *
    * @return  <code>true</code> if device address was sent, <code>false</code>
    * otherwise.
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    *
    * @see com.unixwizardry.onewire.adapter.DSPortAdapter#isPresent(byte[])
    * @see com.unixwizardry.onewire.utils.Address
    */
    public boolean OWSelect(byte[] address) throws OneWireIOException, OneWireException {
        // send 1-Wire Reset
        int rslt = OWReset();
        if (rslt != RESET_PRESENCE) {
            System.out.println("No presence pulse, rslt = " + rslt);
            return false;
        }

        // broadcast the MATCH ROM command and address
        byte[] send_packet = new byte[9];
     
        send_packet[0] = OWMatchROMCmd;   // MATCH ROM command
        System.arraycopy(address, 0, send_packet, 1, 8);                 
        /*
       try {
           sleep(500);
       } catch (InterruptedException ex) {
           Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
       }
        */
        
        for(int i = 0; i < 9; i++) {
            OWWriteByte(send_packet[i]);
        }
                      
        return ((rslt == RESET_PRESENCE) || (rslt == RESET_ALARM));
    }
    
       /**
    * Selects the specified iButton or 1-Wire device by broadcasting its
    * address.  This operation is referred to a 'MATCH ROM' operation
    * in the iButton and 1-Wire device data sheets.  This does not
    * affect the 'current' device state information used in searches
    * (findNextDevice...).
    *
    * Warning, this does not verify that the device is currently present
    * on the 1-Wire Network (See isPresent).
    *
    * @param address    address of iButton or 1-Wire device to select
    * @param channel
    *
    * @return  <code>true</code> if device address was sent, <code>false</code>
    * otherwise.
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    *
    * @see com.unixwizardry.onewire.adapter.DSPortAdapter#isPresent(byte[])
    * @see com.unixwizardry.onewire.utils.Address
    */
    public boolean OWSelect(byte[] address, int channel) throws OneWireIOException, OneWireException {
        // send 1-Wire Reset
        //System.out.println("[I2CBridgeAdapter][select] address is " + bytesToHex(address) + " Doing reset()...");
        int rslt = OWReset();   // Do a reset and after reset to the correct channel on the DS2482-800
        DS2482ChannelSelect(channel);
        if (rslt != RESET_PRESENCE) {
            System.out.println("No presence pulse, rslt = " + rslt);
            return false;
        }

        // broadcast the MATCH ROM command and address
        byte[] send_packet = new byte[9];
        
        send_packet[0] = OWMatchROMCmd;   // MATCH ROM command
        System.arraycopy(address, 0, send_packet, 1, 8);
        //System.out.println("[I2CBridgeAdapter][select] sending OWMatchROMCmd + " + bytesToHex(address));
        
       try {
           sleep(500);
       } catch (InterruptedException ex) {
           Logger.getLogger(I2CBridgeAdapter.class.getName()).log(Level.SEVERE, null, ex);
       }
        
        for(int i = 0; i < 9; i++) {
            OWWriteByte(send_packet[i]);
        }
                      
        return ((rslt == RESET_PRESENCE) || (rslt == RESET_ALARM));
    }

    /**
    * Selects the specified iButton or 1-Wire device by broadcasting its
    * address.  This operation is refered to a 'MATCH ROM' operation
    * in the iButton and 1-Wire device data sheets.  This does not
    * affect the 'current' device state information used in searches
    * (findNextDevice...).
    *
    * Warning, this does not verify that the device is currently present
    * on the 1-Wire Network (See isPresent).
    *
    * @param  address    address of iButton or 1-Wire device to select
    *
    * @return  <code>true</code> if device address was sent,<code>false</code>
    * otherwise.
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    *
    * @see com.unixwizardry.onewire.adapter.DSPortAdapter#isPresent(byte[])
    * @see   com.unixwizardry.onewire.utils.Address
    */
   public boolean OWSelect (long address)
      throws OneWireIOException, OneWireException
   {
      return OWSelect(Address.toByteArray(address));
   }

   /**
    * Selects the specified iButton or 1-Wire device by broadcasting its
    * address.  This operation is refered to a 'MATCH ROM' operation
    * in the iButton and 1-Wire device data sheets.  This does not
    * affect the 'current' device state information used in searches
    * (findNextDevice...).
    *
    * Warning, this does not verify that the device is currently present
    * on the 1-Wire Network (See isPresent).
    *
    * @param  address    address of iButton or 1-Wire device to select
    *
    * @return  <code>true</code> if device address was sent,<code>false</code>
    * otherwise.
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    *
    * @see com.unixwizardry.onewire.adapter.DSPortAdapter#isPresent(byte[])
    * @see   com.unixwizardry.onewire.utils.Address
    */
   public boolean OWSelect (String address)
      throws OneWireIOException, OneWireException
   {
      return OWSelect(Address.toByteArray(address));
   }

   /**
    * Selects the specified iButton or 1-Wire device by broadcasting its
    * address.  This operation is refered to a 'MATCH ROM' operation
    * in the iButton and 1-Wire device data sheets.  This does not
    * affect the 'current' device state information used in searches
    * (findNextDevice...).
    *
    * Warning, this does not verify that the device is currently present
    * on the 1-Wire Network (See isPresent).
    *
    * @param  address    address of iButton or 1-Wire device to select
    *
    * @return  <code>true</code> if device address was sent, <code>false</code>
    * otherwise.
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    *
    * @see com.unixwizardry.onewire.adapter.DSPortAdapter#isPresent(byte[])
    * @see   com.unixwizardry.onewire.utils.Address
    */
   public boolean OLDselect (byte[] address)
      throws OneWireIOException, OneWireException
   {
      // send 1-Wire Reset
      int rslt = OWReset();

      // broadcast the MATCH ROM command and address
      byte[] send_packet = new byte [9];

      send_packet [0] = 0x55;   // MATCH ROM command

      System.arraycopy(address, 0, send_packet, 1, 8);
      dataBlock(send_packet, 0, 9);

      // success if any device present on 1-Wire Network
      return ((rslt == RESET_PRESENCE) || (rslt == RESET_ALARM));
   }

   /**
    * Writes the bit state in a byte array.
    *
    * @param state new state of the bit 1, 0
    * @param index bit index into byte array
    * @param buf byte array to manipulate
    */
    private void arrayWriteBit (int state, int index, byte[] buf) {
      int nbyt = (index >>> 3);
      int nbit = index - (nbyt << 3);

      if (state == 1)
         buf [nbyt] |= (0x01 << nbit);
      else
         buf [nbyt] &= ~(0x01 << nbit);
    }
   
   /**
    * Reads a bit state in a byte array.
    *
    * @param index bit index into byte array
    * @param buf byte array to read from
    *
    * @return bit state 1 or 0
    */
    private int arrayReadBit (int index, byte[] buf) {
      int nbyt = (index >>> 3);
      int nbit = index - (nbyt << 3);

      return ((buf [nbyt] >>> nbit) & 0x01);
    }

   /**
    * Selects the specified iButton or 1-Wire device by broadcasting its
    * address.  This operation is refered to a 'MATCH ROM' operation
    * in the iButton and 1-Wire device data sheets.  This does not
    * affect the 'current' device state information used in searches
    * (findNextDevice...).
    *
    * In addition, this method asserts that the select did find some
    * devices on the 1-Wire net.  If no devices were found, a OneWireException
    * is thrown.
    *
    * Warning, this does not verify that the device is currently present
    * on the 1-Wire Network (See isPresent).
    *
    * @param  address    address of iButton or 1-Wire device to select
    *
    * @throws OneWireIOException on a 1-Wire communication error, or if their
    *         are no devices on the 1-Wire net.
    * @throws OneWireException on a setup error with the 1-Wire adapter
    *
    * @see com.unixwizardry.onewire.adapter.DSPortAdapter#isPresent(byte[])
    * @see   com.unixwizardry.onewire.utils.Address
    */
   public void assertSelect(byte[] address)
      throws OneWireIOException, OneWireException
   {
      if(!OWSelect(address))
         throw new OneWireIOException("Device " + Address.toString(address)
             + " not present.");
   }

   /**
    * Selects the specified iButton or 1-Wire device by broadcasting its
    * address.  This operation is refered to a 'MATCH ROM' operation
    * in the iButton and 1-Wire device data sheets.  This does not
    * affect the 'current' device state information used in searches
    * (findNextDevice...).
    *
    * In addition, this method asserts that the select did find some
    * devices on the 1-Wire net.  If no devices were found, a OneWireException
    * is thrown.
    *
    * Warning, this does not verify that the device is currently present
    * on the 1-Wire Network (See isPresent).
    *
    * @param  address    address of iButton or 1-Wire device to select
    *
    * @return  <code>true</code> if device address was sent,<code>false</code>
    * otherwise.
    *
    * @throws OneWireIOException on a 1-Wire communication error, or if their
    *         are no devices on the 1-Wire net.
    * @throws OneWireException on a setup error with the 1-Wire adapter
    *
    * @see com.unixwizardry.onewire.adapter.DSPortAdapter#isPresent(byte[])
    * @see   com.unixwizardry.onewire.utils.Address
    */
   public void assertSelect(long address)
      throws OneWireIOException, OneWireException
   {
      if(!OWSelect(Address.toByteArray(address)))
         throw new OneWireIOException("Device " + Address.toString(address)
             + " not present.");
   }

   /**
    * Selects the specified iButton or 1-Wire device by broadcasting its
    * address.  This operation is refered to a 'MATCH ROM' operation
    * in the iButton and 1-Wire device data sheets.  This does not
    * affect the 'current' device state information used in searches
    * (findNextDevice...).
    *
    * In addition, this method asserts that the select did find some
    * devices on the 1-Wire net.  If no devices were found, a OneWireException
    * is thrown.
    *
    * Warning, this does not verify that the device is currently present
    * on the 1-Wire Network (See isPresent).
    *
    * @param  address    address of iButton or 1-Wire device to select
    *
    * @throws OneWireIOException on a 1-Wire communication error, or if their
    *         are no devices on the 1-Wire net.
    * @throws OneWireException on a setup error with the 1-Wire adapter
    *
    * @see com.unixwizardry.onewire.adapter.DSPortAdapter#isPresent(byte[])
    * @see   com.unixwizardry.onewire.utils.Address
    */
   public void assertSelect(String address)
      throws OneWireIOException, OneWireException
   {
      if(!OWSelect(Address.toByteArray(address)))
         throw new OneWireIOException("Device " + address
             + " not present.");
   }
       
    
    /**
    * Returns whether the adapter can physically support strong 5 volt power
    * mode.
    * <p>
    * Active Pullup (APU) (From DS2482-100 Specifications PDF)
    * The APU bit controls whether an active pullup (controlled slew-rate transistor) 
    * or a passive pullup (RWPU resistor) is used to drive a 1-Wire line from low to 
    * high. When APU = 0, active pullup is disabled (resistor mode). Active Pullup
    * should be selected if the 1-Wire line has a substantial length (several 10m) or 
    * if there is a large number (~20 or more) of devices connected to a 1-Wire line. 
    * The active pullup does not apply to the rising edge of a presence pulse or a 
    * recovery after a short on the 1-Wire line. The circuit that controls rising edges 
    * (Figure 2) operates as follows: At t1 the pulldown (from DS2482 or 1-Wire
    * slave) ends. From this point on the 1-Wire bus is pulled high through RWPU internal 
    * to the DS2482. VCC and the capacitive load of the 1-Wire line determine the slope. 
    * In case that active pullup is disabled (APU = 0), the resistive pullup continues, 
    * as represented by the solid line. With active pullup enabled (APU = 1), when at t2 
    * the voltage has reached a level between VIL1max and VIH1min, the DS2482 actively 
    * pulls the 1-Wire line high applying a controlled slew rate, as represented by the 
    * dashed line. The active pullup continues until tAPUOT is expired at t3. From that time 
    * on the resistive pullup will continue.
    * </p>
    * @return  <code>true</code> if this port adapter can do strong 5 volt
    * mode, <code>false</code> otherwise.
    *
    * @throws OneWireIOException on a 1-Wire communication error with the adapter
    * @throws OneWireException on a setup error with the 1-Wire
    *         adapter
    */
    public boolean canDeliverPower () throws OneWireIOException, OneWireException
    {
        return true;
    }

   /**
    * Returns whether the adapter can physically support "smart" strong 5
    * volt power mode.  "smart" power delivery is the ability to deliver
    * power until it is no longer needed.  The current drop it detected
    * and power delivery is stopped.
    *
    * <p>
    * Strong Pullup (SPU) (From DS2482-100 Specifications PDF)
    * The SPU bit controls whether the DS2482 will apply a low impedance pullup 
    * to VCC on the 1-Wire line after the last bit of either a 1-Wire Write Byte 
    * command or after a 1-Wire Single Bit command has completed. The strong
    * pullup feature is commonly used with 1-Wire EEPROM devices when copying 
    * scratchpad data to the main memory or when performing a SHA-1 computation, 
    * and with parasitically powered temperature sensors or A-to-D converters. 
    * The respective device data sheets specify the location in the communications 
    * protocol after which the strong pullup should be applied. The SPU bit in the 
    * Configuration register of the DS2482 must be set immediately
    * prior to issuing the command that puts the 1-Wire device into the state 
    * where it needs the extra power.  If SPU is 1, the DS2482 applies active 
    * pullup to the rising edge of the time slot in which the strong pullup starts,
    * regardless of the APU bit setting. However, in contrast to setting APU = 1 for 
    * active pullup, the low-impedance pullup does not end after tAPUOT is expired. 
    * Instead, as shown in Figure 4, the low-impedance pullup remains active 
    * until either the next 1-Wire communication command is issued (the typical case), 
    * the Configuration register is written to with the SPU bit being 0, or the Device 
    * Reset command is issued. The PCTLZ control output is active low for the entire 
    * duration of the low-impedance pullup, enabling an external p-channel MOSFET to 
    * supply additional  power to the 1-Wire line. PCTLZ remains inactive (high) 
    * at all other time slots that do not use the strong pullup feature. Additionally, 
    * when the pullup ends, the SPU bit is automatically reset to 0. Using the strong 
    * pullup does not change the state of the APU bit in the Configuration register.
    * </p>
    * @return  <code>true</code> if this port adapter can do "smart" strong
    * 5 volt mode, <code>false</code> otherwise.
    *
    * @throws OneWireIOException on a 1-Wire communication error with the adapter
    * @throws OneWireException on a setup error with the 1-Wire
    *         adapter
    */
    public boolean canDeliverSmartPower () throws OneWireIOException, OneWireException
    {
      return true;
    }
   
   
   /**
    * Sets the duration to supply power to the 1-Wire Network.
    * This method takes a time parameter that indicates the program
    * pulse length when the method startPowerDelivery().<p>
    *
    * Note: to avoid getting an exception,
    * use the canDeliverPower() and canDeliverSmartPower()
    * method to check it's availability.
    *
    * @param timeFactor
    * <ul>
    * <li>   0 (DELIVERY_HALF_SECOND) provide power for 1/2 second.
    * <li>   1 (DELIVERY_ONE_SECOND) provide power for 1 second.
    * <li>   2 (DELIVERY_TWO_SECONDS) provide power for 2 seconds.
    * <li>   3 (DELIVERY_FOUR_SECONDS) provide power for 4 seconds.
    * <li>   4 (DELIVERY_SMART_DONE) provide power until the
    *          the device is no longer drawing significant power.
    * <li>   5 (DELIVERY_INFINITE) provide power until the
    *          setPowerNormal() method is called.
    * </ul>
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    */
   public void setPowerDuration (int timeFactor) throws OneWireIOException, OneWireException
   {
      throw new OneWireException(
         "Power delivery not supported by this adapter type");
   }
   
   /**
    * Sets the 1-Wire Network voltage to supply power to a 1-Wire device.
    * This method takes a time parameter that indicates whether the
    * power delivery should be done immediately, or after certain
    * conditions have been met. <p>
    *
    * Note: to avoid getting an exception,
    * use the canDeliverPower() and canDeliverSmartPower()
    * method to check it's availability.
    *
    * @param changeCondition
    * <ul>
    * <li>   0 (CONDITION_NOW) operation should occur immediately.
    * <li>   1 (CONDITION_AFTER_BIT) operation should be pending
    *           execution immediately after the next bit is sent.
    * <li>   2 (CONDITION_AFTER_BYTE) operation should be pending
    *           execution immediately after next byte is sent.
    * </ul>
    *
    * @return <code>true</code> if the voltage change was successful,
    * <code>false</code> otherwise.
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    */
   public boolean startPowerDelivery (int changeCondition) throws OneWireIOException, OneWireException
   {
      throw new OneWireException(
         "Power delivery not supported by this adapter type");
   }
   
   /**
    * Sets the 1-Wire Network voltage to normal level.  This method is used
    * to disable 1-Wire conditions created by startPowerDelivery and
    * startProgramPulse.  This method will automatically be called if
    * a communication method is called while an outstanding power
    * command is taking place.
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    *         or the adapter does not support this operation
    */
   public void setPowerNormal () throws OneWireIOException, OneWireException
   {
     
   } 
    
   /**
    * Sets the 1-Wire Network voltage to eprom programming level.
    * This method takes a time parameter that indicates whether the
    * power delivery should be done immediately, or after certain
    * conditions have been met. <p>
    *
    * Note: to avoid getting an exception,
    * use the canProgram() method to check it's
    * availability. <p>
    *
    * @param changeCondition
    * <ul>
    * <li>   0 (CONDITION_NOW) operation should occur immediately.
    * <li>   1 (CONDITION_AFTER_BIT) operation should be pending
    *           execution immediately after the next bit is sent.
    * <li>   2 (CONDITION_AFTER_BYTE) operation should be pending
    *           execution immediately after next byte is sent.
    * </ul>
    *
    * @return <code>true</code> if the voltage change was successful,
    * <code>false</code> otherwise.
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    *         or the adapter does not support this operation
    */
   public boolean startProgramPulse (int changeCondition)
      throws OneWireIOException, OneWireException
   {
      throw new OneWireException(
         "Program pulse delivery not supported by this adapter type");
   }
   
   /**
    * Sets the duration for providing a program pulse on the
    * 1-Wire Network.
    * This method takes a time parameter that indicates the program
    * pulse length when the method startProgramPulse().<p>
    *
    * Note: to avoid getting an exception,
    * use the canDeliverPower() method to check it's
    * availability. <p>
    *
    * @param timeFactor
    * <ul>
    * <li>   7 (DELIVERY_EPROM) provide program pulse for 480 microseconds
    * <li>   5 (DELIVERY_INFINITE) provide power until the
    *          setPowerNormal() method is called.
    * </ul>
    *
    * @throws OneWireIOException on a 1-Wire communication error
    * @throws OneWireException on a setup error with the 1-Wire adapter
    */
   public void setProgramPulseDuration (int timeFactor)
      throws OneWireIOException, OneWireException
   {
      throw new OneWireException(
         "Program pulse delivery not supported by this adapter type");
   }      
      
}
