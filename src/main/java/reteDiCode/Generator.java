package reteDiCode;

import desUtils.Rngs;

public class Generator extends Rngs {

    public Generator(){
        super();
    }

    public double exponential(double m) {
        /* ---------------------------------------------------
         * generate an Exponential random variate, use m > 0.0
         * ---------------------------------------------------
         */
        return (-m * Math.log(1.0 - this.random()));
    }

    public  double uniform(double a, double b) {
        /* ------------------------------------------------
         * generate an Uniform random variate, use a < b
         * ------------------------------------------------
         */
        return (a + (b - a) * this.random());
    }

}
