package au.csiro.umran.test.readblue.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.Arrays;

/**
 *
 * @author Umran
 */
public class CircularByteBuffer {
    
    private byte[] array;
    private int increments;
    private int readLocation;
    private int writeLocation;
    private int contentLength;

    public CircularByteBuffer(int initialCapacity, int increments) {
    	this.array = new byte[initialCapacity+1];
        this.increments = increments;
        this.readLocation = 0;
        this.writeLocation = 0;
        this.contentLength = 0;
    }
    
    synchronized
    public void ensureCapacity(int capacity) {
        if (array.length<capacity+1) {
        	capacity = (capacity/increments)*increments;
        	
        	//	extra byte to avoid readLocation==writeLocation
            byte[] newArray = new byte[capacity+1];
            int newReadLocation;
            int newWriteLocation;
            
            if (readLocation<=writeLocation) {
                newReadLocation = readLocation;
                newWriteLocation = writeLocation;
                //  from read location to the write location
                for (int i=readLocation; i<writeLocation; ++i) {
                    newArray[i] = array[i];
                }
            } else {
                newReadLocation = capacity - (array.length-readLocation);
                newWriteLocation = writeLocation;
                
                //  from the beginning to the write location
                for (int i=0; i<writeLocation; ++i) {
                    newArray[i] = array[i];
                }
                
                //  from read location to the end of the array
                for (int i=readLocation; i<array.length; ++i) {
                    newArray[newReadLocation+(i-readLocation)] = array[i];
                }
            }
            
            array = newArray;
            readLocation = newReadLocation;
            writeLocation = newWriteLocation;
        }
    }
    
    public void ensureWritableSpace(int capacity) {
        ensureCapacity(contentLength+capacity);
    }

    /**
     * @return array containing data
     */
    synchronized
    public byte[] getArray() {
        return array;
    }

    /**
     * @return index of current first readable byte in the array
     */
    public int getReadLocation() {
        return readLocation;
    }

    /**
     * @return index of current first writable byte in the array
     */
    public int getWriteLocation() {
        return writeLocation;
    }

    /**
     * @return the number of bytes that can be stored in the circular buffer
     */
    public int getCapacity() {
    	//	extra byte to avoid readLocation==writeLocation
        return array.length-1;
    }
    
    /**
     * @return number of bytes that are in store, that can be read
     */
    public int getContentLength() {
        return contentLength;
    }
    
    /**
     * @return number of bytes that can be written before either reaching end of array,
     * 			or the read location
     */
    synchronized
    public int getContigiousWritableLength() {
        if (readLocation<=writeLocation) {
            // can write to the end
            return array.length - writeLocation;
        } else {
            // can write (readLocation - writeLocation), but avoid
            //      a situation where (readLocation==writeLocation)
            return readLocation - writeLocation - 1;
        }
    }
    
    /**
     * Copies bytes of given length, from the circular buffer,
     * to the output array given 
     * 
     * @param output array to write bytes to
     * @param length maximum number of bytes to write to output array 
     * @return number of bytes actually written
     */
    synchronized
    public int copyOut(byte[] output, int length) {
        if (length>output.length)
            length = output.length;
        if (length>contentLength)
            length = contentLength;
        
        for (int writeLoc=0, readLoc = readLocation; writeLoc<length; ++writeLoc, ++readLoc) {
            if (readLoc>=array.length)
                readLoc -= array.length;
            
            output[writeLoc] = array[readLoc];
        }
        
        return length;
    }
    
    /**
     * Fetches the byte at the index-th position relative to the read location in the buffer
     *  
     * @param index	the index of the byte, relative to the read location
     * @return the byte at the index-th position relative to the read location
     */
    synchronized
    public byte get(int index) {
        if (index>=contentLength)
            throw new IndexOutOfBoundsException("index="+index+", length="+contentLength);
        if (index<0)
        	throw new InvalidParameterException("Negative indexes not allowed (index="+index+")");
        
        int readLoc = readLocation + index;
        if (readLoc>=array.length)
            readLoc -= array.length;
        
        return array[readLoc];
    }
    
    /**
     * Writes a single byte into the buffer
     * 
     * @param b byte to be written
     */
    synchronized
    public void append(byte b) {
        ensureWritableSpace(1);
        array[writeLocation] = b;
        advanceWriteLocation(1);
    }
    
    /**
     * Writes bytes from a byte array into the buffer
     * 
     * @param b	byte array whose contents are to be written
     * @param offset index in byte array to obtain first byte from 
     * @param length number of bytes from byte array to write
     * @return the number of bytes actually written
     */
    synchronized 
    public int append(byte[] b, int offset, int length) {
    	if (length>b.length-offset)
    		length = b.length-offset;
        ensureWritableSpace(length);
        for (int readLoc=0, writeLoc=writeLocation; readLoc<length; ++readLoc, ++writeLoc) {
            if (writeLoc>=array.length)
                writeLoc -= array.length;
            array[writeLoc] = b[offset+readLoc];
        }
        advanceWriteLocation(length);
        return length;
    }

    /*
    synchronized 
    public void append(InputStream is, boolean block) throws IOException {
        while (is.available()>0 || block) {
            ensureWritableSpace(is.available());
            int len = is.read(array, writeLocation, getContigiousWritableLength());
            advanceWriteLocation(len);
            
            if (block && is.available()==0)
                break;
        }
    }
    */
    
    synchronized
    public void advanceReadLocation(int numBytes) {
        readLocation += numBytes;
        if (readLocation>=array.length)
            readLocation -= array.length;
        contentLength -= numBytes;
    }
    
    synchronized
    public void advanceWriteLocation(int numBytes) {
        writeLocation += numBytes;
        if (writeLocation>=array.length)
            writeLocation -= array.length;
        contentLength += numBytes;
    }
    
}
