

package com.unixwizardry.onewire.utils;
        
public class PrintBits {
    
    private final static byte OWB = 0x01;
    private final static byte PPD = 0x02;
    private final static byte SD = 0x04;
    private final static byte LL = 0x08;
    private final static byte RST = 0x10;
    private final static byte SBR = 0x20;
    private final static byte TSB = 0x40;
    private final static byte DIR = (byte) 0x80;
    
    private final static byte APU = 0x01;
    private final static byte PPM = 0x02;
    private final static byte SPU = 0x04;
    private final static byte OWS = 0x08;
    private final static byte NAPU = 0x10;
    private final static byte NPPM = 0x20;
    private final static byte NSPU = 0x40;
    private final static byte NOWS = (byte) 0x80;
    private final static int STATUS = 0;
    private final static int CONFIG = 1;
    
    
    /**
    * Not to be instantiated
    */
   private PrintBits ()
   {}
   
    /**
     * Prints the bit names of the DS2482 status and configuration
     * registers.<p>
     * @author Bruce Juntti bjuntti at unixwizardry.com
     * @param reg 0 = status, 1 = config
     * @param bits hex value of register to print bit names
     * @return String representing bit names
     * <pre>
     * CONFIGURATON REGISTER
     * bit7 bit6 bit5 bit4 bit3 bit2 bit1 bit0
     * ========================================
     * ___  ___  ___  ___
     * 1WS  SPU  PPM  APU  1WS  SPU  PPM  APU           
     *  
     * STATUS REGISTER
     * bit7 bit6 bit5 bit4 bit3 bit2 bit1 bit0
     * ========================================
     * DIR  TSB  SBR  RST   LL   SD  PPD  1WB    
     * </pre>
     */
    public static String PrintBits(int reg, byte bits) {
        String rs = "";       
            if ( reg == 0 ) {
                if ((bits & DIR) == 0x80 ) {
                    rs = rs.concat("DIR ");
                } 
                if ((bits & TSB) == 0x40 ) {
                    rs = rs.concat("TSB ");
                }
                if ((bits & SBR) == 0x20) {
                    rs = rs.concat("SBR ");
                } 
                if ((bits & RST) == 0x10 ) {
                    rs = rs.concat("RST ");
                } 
                if ((bits & LL) == 0x08 ) {
                    rs = rs.concat("LL ");
                } 
                if ((bits & SD) == 0x04 ) {
                    rs = rs.concat("SD ");
                }
                if ((bits & PPD) == 0x02 ) {
                    rs = rs.concat("PPD ");
                }             
                if ((bits & OWB) == 0x01 ) {                    
                    rs = rs.concat("1WB ");
                } 
            }
            if ( reg == 1 ) {
                if ((bits & NOWS) == 0x80 ) {
                    rs = rs.concat("N1WS ");
                } 
                if ((bits & NSPU) == 0x40 ) {
                    rs = rs.concat("NSPU ");
                }
                if ((bits & NPPM) == 0x20 ) {
                    rs = rs.concat("NPPM ");
                } 
                if ((bits & NAPU) == 0x10 ) {
                    rs = rs.concat("NAPU ");
                } 
                if ((bits & OWS) == 0x08 ) {
                    rs = rs.concat("1WS ");
                } 
                if ((bits & SPU) == 0x04 ) {
                    rs = rs.concat("SPU ");
                }
                if ((bits & PPM) == 0x02 ) {
                    rs = rs.concat("PPM ");
                } 
                if ((bits & APU) == 0x01 ) {
                    rs = rs.concat("APU ");
                }
            } return rs;
    } 
    
    public static String printBinary(byte b) {
        String bString = Integer.toBinaryString((b & 0xFF) + 0x100).substring(1);
        return bString;
    }
}
