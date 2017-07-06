


package com.unixwizardry.onewire.adapter;

public interface DS2482 {
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
    
        // All 1-Wire communication commands and the Device Reset command position 
        // the read pointer at the Status register for the host processor to read with minimal
        // protocol overhead. Status information is updated during the execution of certain 
        // commands only. Details are given in the description of the various status bits below.
        
        // bit7 bit6 bit5 bit4 bit3 bit2 bit1 bit0
        //========================================
        //  DIR  TSB  SBR  RST   LL   SD  PPD  1WB
        //
    
    
    static final byte STATUS_1WB = 0x01;    // 1-wire busy
    static final byte STATUS_PPD = 0x02;    // Presence Pulse Detected
    static final byte STATUS_SHORT = 0x04;     // Short Detected
    static final byte STATUS_LL = 0x08;     // Logic Level of the selected 1-wire line withour initiating any 1-wire communication
    static final byte STATUS_RST = 0x10;    // If '1', the DS2482 has performed a reset
    static final byte STATUS_SBR = 0x20;    // Single-Bit Result - logic state of 1-wire line sampled at tMSR of a 
                                                    // 1-wire Single Bit command or the 1st bit of a 1-wire Triplet command   
    static final byte STATUS_TSB = 0x40;    // Triplet Send Bit - reports state of the active 1-wire line sampled at tMSR of the 2nd bit
                                                    // of a 1-wire Triplet command.  Power-on value = 0
    static final byte STATUS_DIRECTION_TAKEN = (byte) 0x80; // Branch Direction taken - search direction chosen by the 3rd bit of the triplet
        
    static final byte DS2482ResetCmd            = (byte) 0xF0;    
    static final byte DS2482SelectReadPointer   = (byte) 0xE1; 
    // Using the below selects the register to read with the above commad
    static final byte DS2482ReadDataRegister    = (byte) 0xE1;
    static final byte DS2482ReadStatusRegister  = (byte) 0xF0;
    static final byte DS2482ReadConfigRegister  = (byte) 0xC3;

    final byte        DS2482_SEL_CHANNEL        = (byte) 0xC3;         // Selects 1-wire channel in a DS2482-800
    final byte        DS2482_WriteConfigRegCmd  = (byte) 0xD2;
    static final byte DS2482_1WireResetCmd      = (byte) 0xB4;
    static final byte DS2482_1WireSingleBitCmd  = (byte) 0x87;
    static final byte DS2482_1WireWriteByteCmd  = (byte) 0xA5;
    static final byte DS2482_1WireReadByteCmd   = (byte) 0x96;
    static final byte DS2482_1WireTripletCmd    = (byte) 0x78;
    
    static final byte OWResetCmd                = (byte) 0xB4;
    static final byte OWMatchROMCmd             = 0x55;
    static final byte OWSkipRomCmd              = (byte) 0xCC;
    static final byte OWSearchCmd               = (byte) 0xF0;
    static final byte OWSReadROMCmd             = (byte) 0x33;   
    static final byte OWAlarmSearchCmd          = (byte) 0xEC;
    static final byte OWConvertTempCmd          = (byte) 0x44;
    static final byte OWReadScratchpadCmd       = (byte) 0xBE;
    static final byte OWSkipROMCmd              = (byte) 0xCC;
    static final byte OWReadScratchPadCmd       = (byte) 0xBE;
                 
    static final byte DS2482StatusRegister      = (byte) 0xF0;   
           
    boolean DS2482Reset();
    boolean DS2482Detect();
    byte DS2482WrtCfg(byte config);
    
    
    public boolean DS2482ChannelSelect(int channel);
    public byte DS2482OWTriplet(byte search_direction);
}
