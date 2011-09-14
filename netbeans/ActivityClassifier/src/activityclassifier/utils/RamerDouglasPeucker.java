package activityclassifier.utils;

/**
 *
 * @author Umran
 */
public class RamerDouglasPeucker
{

    private int size;
    private double[] vertHeights;
    private double[] times;
    private int timeStart;
    private boolean[] reduced;

    public RamerDouglasPeucker( int size, double[] vertHeights, double[] times, int timeStart ) {
        this.size = size;
        this.vertHeights = vertHeights;
        this.times = times;
        this.timeStart = timeStart;
    }

    public RamerDouglasPeucker( int size, double[] vertHeights, double[] times ) {
        this(size, vertHeights, times, 0);
    }

    /**
     * source: <a href="http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html">wolfram: Distance of Point to 2 Dimensional Line</a>
     * referenced on: 7th Feb 2011
     * 
     * @param lineStart
     * index of point which line starts from
     *
     * @param lineEnd
     * index of point which line ends at
     *
     * @param requiredPoint
     * index of the point from which distance to the line is required
     *
     * @return
     * the distance of the line made by the require point intersecting at
     * a right angle to the line given.
     *
     */
    private double calcOrthogonalDist( int lineStart, int lineEnd, int requiredPoint ) {
        double x1 = times[timeStart + lineStart],
                x2 = times[timeStart + lineEnd],
                x0 = times[timeStart + requiredPoint],
                y1 = vertHeights[lineStart],
                y2 = vertHeights[lineEnd],
                y0 = vertHeights[requiredPoint];

        return Math.abs((x2 - x1) * (y1 - y0) - (x1 - x0) * (y2 - y1))
               / Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    /**
     * source: <a href="http://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm">Ramer-Douglas-Peuker Algorithm</a>
     * referenced on: 7th Feb 2011
     *
     * @param first
     * The index of the first point in the segment to be reduced. (inclusive)
     *
     * @param last
     * The index of the last point in the segment to be reduced. (inclusive)
     * 
     */
    private void reduce( int first, int last, double epsilon ) {
        double dmax = 0.0;
        int index = -1;

        for ( int i = first + 1; i < last; ++i ) {
            double d = calcOrthogonalDist(first, last, i);
            if ( d > dmax ) {
                index = i;
                dmax = d;
            }
        }

        if ( dmax >= epsilon ) {
            if ( index != first ) {
                reduce(first, index, epsilon);
            }

            if ( index != last ) {
                reduce(index, last, epsilon);
            }
        } else {
            //  remove all points not including the first and last points
            for ( int i = first + 1; i < last; ++i ) {
                this.reduced[i] = true;
            }
        }
    }

    /**
     *
     * @param epsilon
     * value of point-line distances, under which any point is regarded as part of the line
     * and hence filtered.
     *
     * @return
     *  array of boolean, <br/>
     *  if true - remove point at index<br/>
     *  if false - keep point at index<br/>
     */
    public boolean[] reduce( double epsilon ) {
        this.reduced = new boolean[size];
        if ( size > 1 ) {
            reduce(0, size - 1, epsilon);
        }
        return this.reduced;
    }
}
