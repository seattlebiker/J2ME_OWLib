

package com.unixwizardry.accessProvider;

/**
 * I2C methods for connecting to a Maxim/Dallas I2C device like the DS2482 1-wire
 * bridge.
 * <p>
 *
 * @version    0.00, 21 August 2000
 * @author     DS
 */

import com.unixwizardry.onewire.OneWireException;
import com.unixwizardry.onewire.utils.Convert;
import static com.unixwizardry.onewire.utils.Convert.bytesToHexLE;
import static com.unixwizardry.onewire.utils.Convert.toHexString;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdk.dio.DeviceManager;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;

public class I2C_Device {    
    protected boolean       TESTMODE = false;
    protected I2CDevice     i2c_device = null;       // I2C device
    private final byte      i2cBus;                       
    //private int serialClock = 100000;              // default clock 100KHz 
    private int         serialClock = -1;            // I have no idea why J2ME now needs the I2C clock set to -1
    private int         addressSizeBits = 7;         // default address size
    private final byte  address;                     // Common DS2482 I2C device address
    private final int   registerSize = 1;            //Register size in bytes
    private final int   bufferSize = 1;              // Register size in bytes
    public boolean      adapterPresent;
    boolean             statusOK = false;
    
    final ByteBuffer command;
    final ByteBuffer byteToRead;

    private String msg;
    public static int ERROR   = 1;
    public static int INFO    = 2;
    public static int DATA    = 3;
    
    public static boolean verbose = false;
    private int messageLevel = ERROR;
         
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
    }

    /**
     * Constructor for the I2C Dallas device.
     *
     * @param i2cBus Device bus. For Raspberry Pi usually it's 1
     * @param address Device address
     * @param addressSizeBits I2C normally uses 7 bits addresses
     * @param serialClock Clock speed
     */
    public I2C_Device(byte i2cBus, byte address, int addressSizeBits, int serialClock) {
        this.byteToRead = ByteBuffer.wrap(new byte[bufferSize]);
        this.command = ByteBuffer.wrap(new byte[bufferSize]);
        this.address = address;
        this.addressSizeBits = addressSizeBits;
        this.serialClock = serialClock;
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
     */
    public final boolean connectToDevice() {   
        boolean connectStatus = false;
        if (!TESTMODE) {
            try {                
                //System.out.println("[connectToDevice] Opening I2C device at: " + toHexString(address) + " on bus " + i2cBus);               
                I2CDeviceConfig config = new I2CDeviceConfig(i2cBus, address, addressSizeBits, serialClock);
                i2c_device = DeviceManager.open(config);               
                adapterPresent = true;
                connectStatus = true;
                printMessage("Connected to the I2C 1-wire device OK", "connectToDevice()", INFO);                   
            } catch (IOException e) {
                System.out.println("[I2C_Device][connectToDevice] Error connecting to device at address " + toHexString(address));
            }                
        } else {
            System.out.println("[I2C_Device][connectToDevice] In TESTMODE");
            System.out.println("[I2C_Device][connectToDevice] Opening I2C device at: " + toHexString(address));
        }
        return connectStatus;
    }

    /**
     * I2CwriteBlock() writes a sequence of bytes to the selected DS2482<p>
     * 
     * @param buffer is an array of bytes to be  written
     * @throws com.unixwizardry.onewire.OneWireException
     * @throws java.io.IOException
     */
    public void I2CwriteBlock(byte[] buffer) throws OneWireException, IOException, Error {
        //for ( byte b:buffer) {
        //    System.out.println("Writing: " + byteToHex(b));
        //}
        //System.out.println("[sendBlock] buffer is: " + bytesToHex(buffer));
        if (!TESTMODE) { 
            msg = "Sending " + Convert.toHexString(buffer);
            printMessage(msg, "I2CsendBlock()", INFO);
            i2c_device.write(ByteBuffer.wrap(buffer));           
        } else {
            System.out.println("[I2C_Device][I2CsendBlock] Sending " + bytesToHexLE(buffer) + " in TESTMODE ");
        }
    }
    
    /**
     * I2CSendByte() tries to write one single byte to the device
     *
     * @param byteToWrite is the single byte to be written
     * @throws java.io.IOException
     */
    
    public void I2CwriteByte(byte byteToWrite) {
        if (!TESTMODE) {
            try {
                command.clear();
                command.put(byteToWrite);
                command.rewind();   
                msg = "Sending " + Convert.byteToHex(byteToWrite);
                printMessage(msg, "I2CsendByte()", INFO);                
                i2c_device.write(command); 
            } catch (IOException e) {
                System.out.println("[I2C_Device][I2CsendByte] configure exception: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("[I2C_Device][I2CsendByte] Peripheral not available exception: " + e.getMessage());
            }
        }else {
            System.out.println("[I2C_Device][I2CsendByte] In TESTMODE");
        }
    }  
    
    /**
     * This method tries to write one single byte to particular registry
     *
     * @param registry Registry to write
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
        } catch (IOException e) {
            System.out.println("[I2C_Device][I2CsendByte]: writeOneByte: Error writing registry " + register);
        }

    }
    
    
    public void I2CwriteBytes(byte[] buffer) {
        //for ( byte b:buffer) {
        //    System.out.println("Writing: " + byteToHex(b));
        //}
        //System.out.println("[sendBlock] buffer is: " + bytesToHex(buffer));
        if (!TESTMODE) {
            try {           
                i2c_device.write(ByteBuffer.wrap(buffer));
            } catch (IOException e) {
                System.out.println("[I2C_Device][I2CsendBytes] configure exception: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("[I2C_Device][I2CsendBytes] Peripheral not available exception: " + e.getMessage());
            }
        } else {
            System.out.println("[I2C_Device][I2CsendBytes] Sending " + bytesToHexLE(buffer) + " in TESTMODE ");
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
        if (!TESTMODE) {
            try {
                result = i2c_device.read(byteToRead);
                if (result < 1) {
                    System.out.println("[I2C_Device][I2CreadByte] Byte could not be read");
                } else {
                    byteToRead.rewind();
                    return byteToRead.get();
                }
                return (byte) result;
            } catch (IOException ex) {
                Logger.getLogger(I2C_Device.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else {
            System.out.println("[I2C_Device][I2CreadByte] In TESTMODE");
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
        if (!TESTMODE) {
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
                Logger.getLogger(I2C_Device.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else {
            System.out.println("[I2C_Device][I2CreadByte] In TESTMODE");
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
