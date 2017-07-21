/***********************************************************************
 * Adding copyright stuff and giving credit to Oracle for the original 
 * code for the great Java ME MOOC (Massive Online Oracle Class) that
 * gave me the impetus to get the Dallas Semiconductor 1-wire code to
 * work with Java Micro Edition.
 */


/* The below is for the Oracle code contained in the MOOC classes     */
/* Copyright 2014, Oracle and/or its affiliates. All rights reserved. */

package com.unixwizardry.accessProvider;

/**
 * I2C methods for connecting to a Maxim/Dallas I2C device like the DS2482 1-wire
 * bridge.
 * <p>
 *
 * @version    0.00, 21 August 2000
 * @author     DS
 */

import com.unixwizardry.onewire.utils.Convert;
import static com.unixwizardry.onewire.utils.Convert.toHexString;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdk.dio.DeviceManager;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;

public class I2C_Device {    
    protected I2CDevice     i2c_device = null;       // I2C device
    private byte            i2cBus = 1;              // Default I2C bus        
    private final int       serialClock = -1;        // I have no idea why J2ME 8.2 now needs the I2C clock set to -1
    private int             addressSizeBits = 7;     // Default address size
    private byte            address = 18;            // Default DS2482 I2C device address
    private final int       registerSize = 1;        //Register size in bytes
    private final int       bufferSize = 1;          // Register size in bytes
    public boolean          adapterPresent;
    boolean                 statusOK = false;
    
    final ByteBuffer command;
    final ByteBuffer byteToRead;

    private String msg;
    public static int ERROR   = 1;
    public static int INFO    = 2;
    public static int DATA    = 3;
    
    public static boolean verbose = false;
    private int messageLevel = ERROR;
      
    //private static final Logger LOGGER = Logger.getLogger(I2C_Device.class.getName());
    
    /**
     * Constructor for the I2C Dallas device. This method update the device address and
     * try to connect to the device
     *
     * @param address Device's address
     * @param i2cBus
     */
    public I2C_Device(byte address, byte i2cBus) {      
        this.byteToRead = ByteBuffer.wrap(new byte[bufferSize]);    // Initialize the ByteBuffers
        this.command    = ByteBuffer.wrap(new byte[bufferSize]);   
        this.address = address;
        this.i2cBus = i2cBus;    
        statusOK = connectToDevice();  
        //Logger.getLogger(I2C_Device.class.getName()).log(Level.SEVERE, null, ex);
    }

    /**
     * Constructor for the I2C Dallas device.
     *
     * @param i2cBus Device bus. For Raspberry Pi usually it's 1
     * @param address Device address
     * @param addressSizeBits I2C normally uses 7 bits addresses
     */
    public I2C_Device(byte i2cBus, byte address, int addressSizeBits) {
        this.byteToRead = ByteBuffer.wrap(new byte[bufferSize]);
        this.command = ByteBuffer.wrap(new byte[bufferSize]);
        this.address = address;
        this.addressSizeBits = addressSizeBits;
        this.i2cBus = i2cBus;
        statusOK = connectToDevice();       
    }

    /**
     * connectToDevice()
     * Constructs the shared ByteBuffers
     * Reusing the buffers rather than allocating new space each time is
     * good practice with embedded devices to reduce garbage collection.
     * command = ByteBuffer.allocateDirect(bufferSize);
     * byteToRead = ByteBuffer.allocateDirect(1);
     * 
     * @return Boolean: true if connected OK, otherwise false
     */
    public final boolean connectToDevice() {   
        boolean connectStatus = false;
        try {                
            //System.out.println("[connectToDevice] Opening I2C device at: " + toHexString(address) + " on bus " + i2cBus);               
            I2CDeviceConfig config = new I2CDeviceConfig(i2cBus, address, addressSizeBits, serialClock);
            i2c_device = DeviceManager.open(config);               
            adapterPresent = true;
            connectStatus = true;
            printMessage("Connected to the I2C 1-wire device OK", "connectToDevice()", INFO);                   
        } catch (IOException ex) {
            System.out.println("[I2C_Device][connectToDevice] Error connecting to device at address " + toHexString(address));
        }                        
        return connectStatus;
    }

    /**
     * I2CwriteBlock() writes a sequence of bytes to the selected DS2482<p>
     * 
     * @param buffer is an array of bytes to be written   
     */
    public void I2CwriteBlock(byte[] buffer) {       

        msg = "Sending " + Convert.toHexString(buffer);
        printMessage(msg, "I2CsendBlock()", INFO);
        try {           
            i2c_device.write(ByteBuffer.wrap(buffer));
        } catch (IOException ex) {
            System.out.println("[I2C_Device][I2CwriteBlock] Error encountered: " + ex.getMessage());            
        }      
    }
    
    /**
     * I2CSendByte() tries to write one single byte to the device
     *
     * @param byteToWrite is the single byte to be written
     */
    
    public void I2CwriteByte(byte byteToWrite) {
        try {
            command.clear();
            command.put(byteToWrite);
            command.rewind();   
            msg = "Sending " + Convert.byteToHex(byteToWrite);
            printMessage(msg, "I2CsendByte()", INFO);                
            i2c_device.write(command); 
        } catch (IOException ex) {
            System.out.println("[I2C_Device][I2CwriteByte] Error encountered: " + ex.getMessage());
        }        
    }  
    
    /**
     * This method tries to write one single byte to particular registry
     *
     * @param register Register to write
     * @param byteToWrite Byte to be written    
     */
    public void I2CwriteByte(int register, byte byteToWrite) {
        command.clear();
        command.put(byteToWrite);
        command.rewind();
        msg = "Sending " + Convert.byteToHex(byteToWrite) + " to register " + register;
                printMessage(msg, "I2CsendByte()", INFO);         
        try {
            i2c_device.write(register, registerSize, command);
        } catch (IOException ex) {
            System.out.println("[I2C_Device][I2CwriteByte]: I2CwriteByte: Error writing register " + 
                    register + " " + ex.getMessage());
        }

    }
    
    
    public void I2CwriteBytes(byte[] buffer) {       
        try {           
            i2c_device.write(ByteBuffer.wrap(buffer));
        } catch (IOException ex) {
            System.out.println("[I2C_Device][I2CwriteBytes] Error encountered: " + ex.getMessage());
        }        
    }
    
    
    
    /**
     * This method reads one byte from the I2C device. The method
     * checks that the byte is actually read, otherwise it'll show some messages
     * in the output
     *
     * @return Byte read from the register
     */
    public byte I2CreadByte() {
        byteToRead.clear();
        int result; 
        try {
            result = i2c_device.read(byteToRead);
            if (result < 1) {
                System.out.println("[I2C_Device][I2CreadByte] source could not be read");
            } else {
                byteToRead.rewind();
                return byteToRead.get();
            }
            //return (byte) result;
            
            } catch (IOException ex) {
                System.out.println("[I2C_Device][I2CwriteBytes] Error encountered: " + ex.getMessage());               
            }       
        return 2;
    }

    
    /**
     * This method reads one byte from the I2C device. The method
     * checks that the byte is actually read, otherwise it'll show some messages
     * in the output
     *
     * @param register
     * @return Byte read from the register
     */
    public byte I2CreadByte(byte register) {
        byteToRead.clear();
        int result; 
        try {
            result = i2c_device.read(register, 1, byteToRead);
            if (result < 1) {
                System.out.println("[I2C_Device][I2CreadByte] Byte could not be read");
            } else {
                byteToRead.rewind();
                return byteToRead.get();
            }
            return (byte) result;
        } catch (IOException ex) {
            System.out.println("[I2C_Device][I2CreadByte] Error encountered: " + ex.getMessage());
        }
        return 2;
    }
    
    
    /**
     * This method closes the open I2CDevice resource
     *
     *
     * @throws java.io.IOException
     */
    public void close() throws IOException {
        i2c_device.close();
    }
   
    /**
     * Print a message if verbose messaging is turned on
     *
     * @param message The message to print
     * @param method The method being called
     * @param level
     */
    public final void printMessage(String message, String method, int level) {
        if (verbose && level <= messageLevel) {
            System.out.println("DEBUG: " + " " + method + " " + message);
        }
    }
   
    /**
     * Turn on or off verbose messaging
     *
     * @param verbose Whether to enable verbose messages
     */
    public final void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Set the level of messages to display, 1 = ERROR, 2 = INFO
     *
     * @param level The level for messages
     */
    public final void setMessageLevel(int level) {
        messageLevel = level;
    }

}
