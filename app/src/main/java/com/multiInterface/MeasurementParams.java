package com.multiInterface;

/**
 * Created by t-taman on 6/8/2016.
 */
public class MeasurementParams  implements java.io.Serializable {

    public long timeStamp;
    public String wifiID;
    public String cellID;
    public double wifiMedianRSSI;
    public double cellMedianRSSI;

    public String toString(){

        return (String.valueOf(timeStamp) + ' ' + wifiID + ' ' + cellID + ' ' +
                wifiMedianRSSI + ' ' + cellMedianRSSI);
    }

}
