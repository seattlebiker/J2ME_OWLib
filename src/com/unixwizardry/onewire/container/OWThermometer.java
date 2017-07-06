/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unixwizardry.onewire.container;

import com.unixwizardry.onewire.adapter.OneWireIOException;
import com.unixwizardry.onewire.OneWireException;


/**
 *
 * @author Bruce Juntti <bjuntti at unixwizardry.com>
 */

public class OWThermometer implements TemperatureContainer {
   
    public OWThermometer(byte family) {
        
    }    
    
    public double ReadTemp() {
        double tempInCelsius = 0;
        byte[] data = null;
        int i;
        for (i = 0; i < 5; i++) {
            //data[i] = DS2482OWMaster.OWReadByte();
        }
        return tempInCelsius;
    }
    
    public double OWReadTemperature() {
        double tempInCelsius = 0;
        byte[] thermoData = null;
        for (int i = 0; i < 5; i++) {
            //thermoData[i] = OWReadByte();
        }
        return tempInCelsius;
    }  
    
    /*
    * C-source
    *
    function OWReadTemperature() {
        local data = [0,0,0,0, 0];
        local i;
        for(i=0; i<5; i++) { //we only need 5 of the bytes
            data[i] = OWReadByte();
            //server.log(format("read byte: %.2X", data[i]));
        }
     
        local raw = (data[1] << 8) | data[0];
        local SignBit = raw & 0x8000;  // test most significant bit
        if (SignBit) {raw = (raw ^ 0xffff) + 1;} // negative, 2's compliment
        local cfg = data[4] & 0x60;
        if (cfg == 0x60) {
            //server.log("12 bit resolution"); //750 ms conversion time
        } else if (cfg == 0x40) {
            //server.log("11 bit resolution"); //375 ms
            raw = raw << 1;
        } else if (cfg == 0x20) {
            //server.log("10 bit resolution"); //187.5 ms
            raw = raw << 2;
        } else { //if (cfg == 0x00)
            //server.log("9 bit resolution"); //93.75 ms
            raw = raw << 3;
        }
        //server.log(format("rawtemp= %.4X", raw));
     
        local celsius = raw / 16.0;
        if (SignBit) {celsius *= -1;}
        //server.log(format("Temperature = %.1f °C", celsius));
     
        local fahrenheit = celsius * 1.8 + 32.0;
        //server.log(format("Temperature = %.1f °F", fahrenheit));
        server.log(format("OneWire Device %.8X%.8X = %.1f °F", owDeviceAddress[0], owDeviceAddress[1], fahrenheit));
        server.show(format("%.1f °F", fahrenheit));
    }
       
    */

    @Override
    public boolean hasTemperatureAlarms() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasSelectableTemperatureResolution() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double[] getTemperatureResolutions() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getTemperatureAlarmResolution() throws OneWireException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getMaxTemperature() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getMinTemperature() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void doTemperatureConvert(byte[] state) throws OneWireIOException, OneWireException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getTemperature(byte[] state) throws OneWireIOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getTemperatureAlarm(int alarmType, byte[] state) throws OneWireException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getTemperatureResolution(byte[] state) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setTemperatureAlarm(int alarmType, double alarmValue, byte[] state) throws OneWireException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setTemperatureResolution(double resolution, byte[] state) throws OneWireException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public byte[] readDevice() throws OneWireIOException, OneWireException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeDevice(byte[] state) throws OneWireIOException, OneWireException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
