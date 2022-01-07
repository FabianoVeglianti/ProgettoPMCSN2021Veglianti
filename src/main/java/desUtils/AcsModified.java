/* -------------------------------------------------------------------------
 * This program is based on a one-pass algorithm for the calculation of an
 * array of autocorrelations r[1], r[2], ... r[K].  The key feature of this
 * algorithm is the circular array 'hold' which stores the (K + 1) most
 * recent data points and the associated index 'p' which points to the
 * (rotating) head of the array.
 *
 * Data is read from a text file in the format 1-data-point-per-line (with
 * no blank lines).  Similar to programs UVS and BVS, this program is
 * designed to be used with OS redirection.
 *
 * NOTE: the constant K (maximum lag) MUST be smaller than the # of data
 * points in the text file, n.  Moreover, if the autocorrelations are to be
 * statistically meaningful, K should be MUCH smaller than n.
 *
 * Name              : Acs.java (AutoCorrelation Statistics)
 * Authors           : Steve Park & Dave Geyer
 * Translation by    : Jun Wang
 * Language          : Java
 * Latest Revision   : 6-16-06
 * -------------------------------------------------------------------------
 */
package desUtils;


import reteDiCode.CenterEnum;

import java.text.DecimalFormat;

public class AcsModified {

    private static final int K = 1;               /* K is the maximum lag          */
    private static final int SIZE = 2;

    private int    i;                   /* data point index              */
    private int    j;                       /* lag index                     */
    private int    p;                   /* points to the head of 'hold'  */
    private double x;                       /* current x[i] data point       */
    private double sum;               /* sums x[i]                     */
    long   n;                       /* number of data points         */
    private double[] hold; /* K + 1 most recent data points */
    private double[] cosum; /* cosum[j] sums x[i] * x[i+j]   */


    public AcsModified(){
        i = 0;                   /* data point index              */
        p = 0;                   /* points to the head of 'hold'  */
        sum = 0.0;               /* sums x[i]                     */
        hold  = new double [SIZE]; /* K + 1 most recent data points */
        cosum = new double [SIZE]; /* cosum[j] sums x[i] * x[i+j]   */
    }

    public void insertValue(double value){
        x = value;
        sum += x;
        hold[i] = x;
        i++;
    }


    public void updateValue(double value){
        for (j = 0; j < SIZE; j++)
            cosum[j] += hold[p] * hold[(p + j) % SIZE];
        x       = value;
        sum    += x;
        hold[p] = x;
        p       = (p + 1) % SIZE;
        i++;


    }


    public double getAutocorrelationWithLagOneComputation(){
        return cosum[1] / cosum[0];
    }


    public void resetValues() {
        i = 0;                   /* data point index              */
        p = 0;                   /* points to the head of 'hold'  */
        sum = 0.0;               /* sums x[i]                     */
        hold  = new double [SIZE]; /* K + 1 most recent data points */
        cosum = new double [SIZE]; /* cosum[j] sums x[i] * x[i+j]   */
    }

    public void computeAutocorrelationValues() {
        /* number of data points         */
        long n = i;
        while (i < n + SIZE) {        /* empty the circular array       */
            for (j = 0; j < SIZE; j++)
                cosum[j] += hold[p] * hold[(p + j) % SIZE];
            hold[p] = 0.0;
            p       = (p + 1) % SIZE;
            i++;
        }

        double mean = sum / n;
        for (j = 0; j <= K; j++)
            cosum[j] = (cosum[j] / (n - j)) - (mean * mean);
    }
}
