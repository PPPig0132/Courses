package simpledb.optimizer;

import java.util.Arrays;

import simpledb.execution.Predicate;
import simpledb.execution.Predicate.Op;

/** A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram {
    private int buckets; // Array to hold the count of values in each bucket
    private int min; // Minimum value in the histogram
    private int max; // Maximum value in the histogram
    private int[] bucket;
    private int ntuples;
    private int width; // Width of each bucket
    private double sum;
    private int count;
    /**
     * Create a new IntHistogram.
     * 
     * This IntHistogram should maintain a histogram of integer values that it receives.
     * It should split the histogram into "buckets" buckets.
     * 
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     * 
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't 
     * simply store every value that you see in a sorted list.
     * 
     * @param buckets The number of buckets to split the input value into.
     * @param min The minimum integer value that will ever be passed to this class for histogramming
     * @param max The maximum integer value that will ever be passed to this class for histogramming
     */
    public IntHistogram(int buckets, int min, int max) {
    	// some code goes here
        this.min = min;
        this.max = max;
        this.width=Math.max(1, (int)Math.ceil(((max - min + 1) * 1.0) / buckets));
        this.buckets = this.width==1?max-min+1:buckets; // Ensure at least one bucket per value
        this.bucket = new int[this.buckets];
        //Arrays.fill(bucket, 0); // Initialize the bucket array to zero
        this.ntuples = 0;
        this.sum = 0.0;
        this.count = 0;
    }

    /**
     * Add a value to the set of values that you are keeping a histogram of.
     * @param v Value to add to the histogram
     */
    public void addValue(int v) {
    	// some code goes here
        int idx= (v - min) /width;//width:(max-min)/ buckets; idx= (v - min) / width;
        bucket[idx] += 1;
        ntuples += 1;
    }

    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     * 
     * For example, if "op" is "GREATER_THAN" and "v" is 5, 
     * return your estimate of the fraction of elements that are greater than 5.
     * 
     * @param op Operator
     * @param v Value
     * @return Predicted selectivity of this particular operator and value
     */
    public double estimateSelectivity(Predicate.Op op, int v) {

    	// some code goes here
        count+=1;
        double res;
        int h;
        int idx= (v - min) / width; // Determine the bucket index for value v
        if(idx<0  ){
            if(op.equals(Op.NOT_EQUALS) || op.equals(Op.GREATER_THAN) || op.equals(Op.GREATER_THAN_OR_EQ)){
                res= 1;
            }
            else if(op.equals(Op.LESS_THAN) || op.equals(Op.LESS_THAN_OR_EQ) || op.equals(Op.EQUALS)){
                res= 0;
            }
            else{
                throw new IllegalArgumentException("Invalid operator");
            }
            sum+= res;
            return res;
        }
        else if(idx>=buckets) {
           if(op.equals(Op.NOT_EQUALS) || op.equals(Op.LESS_THAN) || op.equals(Op.LESS_THAN_OR_EQ) ){
                res= 1;
            }
            else if(op.equals(Op.GREATER_THAN) || op.equals(Op.GREATER_THAN_OR_EQ) || op.equals(Op.EQUALS)){
                res= 0;
            }
            else{
                throw new IllegalArgumentException("Invalid operator");
            }
            sum+= res;
            return res;
        } // Ensure idx is within bounds
        else {
            h = bucket[idx]; // Calculate the upper bound of the bucket
            if(op.equals(Op.EQUALS)){
                double b_part=bucket[idx]*1.0/width;
                res= b_part/ntuples;
            }
            else if(op.equals(Op.NOT_EQUALS)){
                double b_part=bucket[idx]*1.0/width;
                res= 1-b_part/ntuples;
            }
            else if(op.equals(Op.GREATER_THAN) ){
                int h_b=(idx+1)*width+min;
                double b_part=width>1?h*(h_b-v)*1.0/width:0;
                for(int i=idx+1;i<buckets;i++){
                    b_part+=bucket[i];
                }
                res= b_part/ntuples;
            }
            else if( op.equals(Op.GREATER_THAN_OR_EQ)){
                double res_eq=(bucket[idx]*1.0/width);
                int h_b=(idx+1)*width+min;
                double b_part=width>1?h*(h_b-v)*1.0/width:0;
                for(int i=idx+1;i<buckets;i++){
                    b_part+=bucket[i];
                }
                res= (b_part+res_eq)/ntuples;
            }
            else if(op.equals(Op.LESS_THAN) ){
                int h_b=idx*width+min;
                double b_part=width>1?h*(v-h_b)*1.0/width:0;
                for(int i=0;i<idx;i++){
                    b_part+=bucket[i];
                }
                res= b_part/ntuples;
            }
            else if( op.equals(Op.LESS_THAN_OR_EQ)){
                double res_eq=(bucket[idx]*1.0/width);
                 int h_b=idx*width+min;
                double b_part=width>1?h*(v-h_b)*1.0/width:0;
                for(int i=0;i<idx;i++){
                    b_part+=bucket[i];
                }
                res= (b_part+res_eq)/ntuples;
            }
            else{
                throw new IllegalArgumentException("Invalid operator");
            }
            sum+= res;
            return res;
        }
        
    }
    
    /**
     * @return
     *     the average selectivity of this histogram.
     *     
     *     This is not an indispensable method to implement the basic
     *     join optimization. It may be needed if you want to
     *     implement a more efficient optimization
     * */
    public double avgSelectivity()
    {
        // some code goes here
        return sum / count;
    }
    
    /**
     * @return A string describing this histogram, for debugging purposes
     */
    public String toString() {
        // some code goes here
        StringBuilder sb = new StringBuilder();
        sb.append("IntHistogram: \n");
        sb.append("Buckets: ").append(buckets).append("\n");
        sb.append("Min: ").append(min).append("\n");
        sb.append("Max: ").append(max).append("\n");
        return sb.toString();
    }
}
