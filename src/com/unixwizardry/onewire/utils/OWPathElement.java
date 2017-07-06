
package com.unixwizardry.onewire.utils;

import com.unixwizardry.onewire.container.OneWireContainer;


/**
 * 1-Wire Network path element.  Instances of this class are
 * used to represent a single branch of a complex 1-Wire network.
 *
 * <H3> Usage </H3> 
 * 
 * <DL> 
 * <DD> <H4> Example</H4> 
 * Enumerate through the 1-wire path elements in the 1-Wire path 'path' and print information:
 * <PRE> <CODE>
 *   OWPathElement path_element;
 *
 *   // enumerature through the path elements
 *   for (Enumeration path_enum = path.getAllOWPathElements(); 
 *           path_enum.hasMoreElements(); )
 *   {
 *
 *      // cast the enum as a OWPathElement
 *      path_element = (OWPathElement)path_enum.nextElement();
 *   
 *      // print info
 *      System.out.println("Address: " + path_element.getContainer().getAddressAsString());
 *      System.out.println("Channel number: " + path_element.getChannel()); 
 *   }
 * </CODE> </PRE>
 * </DL>
 *
 * @see com.unixwizardry.onewire.utils.OWPath
 * @see com.unixwizardry.onewire.container.OneWireContainer
 *
 * @version    0.00, 18 September 2000
 * @author     DS
 */
public class OWPathElement
{

   //--------
   //-------- Variables 
   //--------

   /** OneWireContainer of the path element */
   private OneWireContainer owc;

   /** Channel the path is on */
   private int channel;

   //--------
   //-------- Constructors
   //--------

   /**
    * Don't allow without OneWireContainer and channel.
    */
   private OWPathElement ()
   {
   }

   /**
    * Create a new 1-Wire path element.
    *
    * @param  owcInstance device that is the path element. Must implement
    *         SwitchContainer.
    * @param  channelNumber channel number of the 1-Wire path 
    */
   public OWPathElement (OneWireContainer owcInstance, int channelNumber)
   {
      owc     = owcInstance;
      channel = channelNumber;
   }

   /**
    * Get the 1-Wire container for this 1-Wire path element.
    *
    * @return OneWireContainer of this 1-Wire path element
    *
    * @see com.unixxwizardry.onewire.container.OneWireContainer
    */
   public OneWireContainer getContainer ()
   {
      return owc;
   }

   /**
    * Get the channel number of this 1-Wire path element.
    *
    * @return channel number of this element
    */
   public int getChannel ()
   {
      return channel;
   }
}

