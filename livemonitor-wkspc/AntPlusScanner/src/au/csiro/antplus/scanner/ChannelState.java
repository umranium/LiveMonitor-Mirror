package au.csiro.antplus.scanner;

public enum ChannelState {
    /** Channel is open, but searching for device to connect to */
    SEARCHING,
    
    /** Channel is open and connected to a device */
    CONNECTED,
    
    /** Channel is closed */
    CLOSED,
    
}
