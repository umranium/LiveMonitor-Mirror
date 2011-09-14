/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package activityclassifier;

/**
 *
 * @author oka
 */
public class Activity {

    public final String name;

    public final Long[] time;

    /**
     * Format: <br/>
     * data[row][column] <br/>
     * columns: x, y, z, a, b, c <br/>
     */
    public final float[][] data;

    public Activity(String name, Long[] time, float[][] data) {
        this.name = name;
        this.time = time;
        this.data = data;
    }

    public Activity(String name, Activity activity) {
        this.name = name;
        this.time = activity.time;
        this.data = activity.data;
    }
    

}
