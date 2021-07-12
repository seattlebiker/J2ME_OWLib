# J2ME_OWlib

Java ME 1-wire library
=======================

- I am submitting this OLD Netbeans project Java MicroEdition code with the hope that it might help others with the DS2482 1-wire master
This is working code... well, most of it.  It did work with a few of the Dallas/Maxim devices at one time,
but it's been so long since I've looked at it.  I mostly shelved it since apparently the entire JavaME
team left Sun Microsystems shorlly after being acquired by Oracle, and it was basically impossible to
get questions answered since apparently nobody uses Java MicroEdition now?
- Caveat: I'm only submitting this repository with the hopes it might help others use the DS2482.
- Please don't laugh at my code; in a prior life I was a UNIX Sysadmin and engineer.  Now retired, and doing
fun stuff with embedded devices and mostly using VSCode with the PlatformIO extension.

Adapted from Maxim's 1-wire Java library owapi_1_10

I wanted to use some of the nifty functions in the library since they were already
figured out - why reinvent the wheel?  I did have to "invent" one of the spokes of the
wheel - there was no Java "adapter" class for the DS2482 I2C 1-wire bridge chip so I came 
up with one.  I took part in the Oracle Java ME MOOC using the Raspberry Pi, and being 
pretty much of a Java newbie, I learned a lot from this course.  There was a lesson in 
the MOOC for accessing I2C devices using the Pi and Java ME.  Using the I2C_Device code 
in the course I came up with a "I2CBridgeAdapter" class.  

Each of the Container classes needed (or needs) to be changed; the readDevice() method
needs to call OWreadByte/OWwriteByte instead of what they are doing now.
Some more changes I made to adapt the library to I2C:

I2C_Device:
===========
I renamed the read and write functions to be more clear: I2CsendByte(), I2CwriteByte(), and 
I2CwriteBytes().


I2CBridgeAdapter:
=================
Since the 1-wire API code from Maxim was
pretty much based on the serial DS2480 device which uses the DSPortAdapter as a parent class,
I made the I2CBridgeAdapter a subclass of the I2C_Device.

OneWireContainer10:
===================
readDevice():  For the life of me I can't figure out why the developers at Maxim chose to 
do this:

	// read scratchpad command
	buffer [0] = ( byte ) READ_SCRATCHPAD_COMMAND;

	// now add the read bytes for data bytes and crc8
	for (int i = 1; i < 10; i++)
    	     buffer [i] = ( byte ) 0x0FF;

	// send the block
	adapter.dataBlock(buffer, 0, buffer.length);

Have no idea why they're sending a READ_SCRATCHPAD_COMMAND and 9 bytes of 0xFF, so
I took that out figuring it must have something to do with the serial port adapter, like
the DS9490.

Since the DS2482 reads one byte at a time from the 1-wire bus, the Container classes
needed to be modified to do this using a for-loop.

