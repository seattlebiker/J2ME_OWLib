/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unixwizardry.onewire.container;

import com.unixwizardry.onewire.OneWireException;
import com.unixwizardry.onewire.adapter.OneWireIOException;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 *
 * @author BJ1832
 */
public interface CounterContainer {
    
    /**
    * Get the Dallas Semiconductor part number of the iButton
    * or 1-Wire Device as a string.  For example 'DS1992'.
    *
    * @return iButton or 1-Wire device name
    */
   public String getName ();
    
    /**
    * Get a short description of the function of this iButton 
    * or 1-Wire Device type.
    *
    * @return device description
    */
   public String getDescription ();
   
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
   public int getMaxSpeed ();
   
   /**
    * Get a ListIterator of memory bank instances that implement one or more
    * of the following interfaces:
    * {@link com.unixwizardry.onewire.container.MemoryBank MemoryBank}, 
    * {@link com.unixwizardry.onewire.container.PagedMemoryBank PagedMemoryBank}, 
    * and {@link com.unixwizardry.onewire.container.OTPMemoryBank OTPMemoryBank}. 
    * @return <CODE>Enumeration</CODE> of memory banks 
    */
   public ArrayList getMemoryBanks ();
   
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
   public long readCounter (int counterPage) throws OneWireIOException, OneWireException;
   
   public void doSimpleWrite () throws OneWireIOException, OneWireException;
   
   
}
