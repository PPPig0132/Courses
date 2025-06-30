package simpledb.execution;

import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;

import java.util.NoSuchElementException;


/**
 * The Aggregation operator that computes an aggregate (e.g., sum, avg, max,
 * min). Note that we only support aggregates over a single column, grouped by a
 * single column.
 */
public class Aggregate extends Operator {

    private static final long serialVersionUID = 1L;
    private OpIterator child; // The child iterator feeding us tuples
    private int afield; // The column over which we are computing an aggregate
    private int gfield; // The column over which we are grouping the result, or -1 if no grouping
    private Aggregator.Op aop; // The aggregation operator to use

     private final Aggregator agg;
    /**
     * Constructor.
     * <p>
     * Implementation hint: depending on the type of afield, you will want to
     * construct an {@link IntegerAggregator} or {@link StringAggregator} to help
     * you with your implementation of readNext().
     *
     * @param child  The OpIterator that is feeding us tuples.
     * @param afield The column over which we are computing an aggregate.
     * @param gfield The column over which we are grouping the result, or -1 if
     *               there is no grouping
     * @param aop    The aggregation operator to use
     */
    public Aggregate(OpIterator child, int afield, int gfield, Aggregator.Op aop) {
        // some code goes here
        this.child = child;
        this.afield = afield;
        this.gfield = gfield;
        this.aop = aop;
         if(this.child.getTupleDesc().getFieldType(afield).equals(Type.INT_TYPE))
            agg = new IntegerAggregator(this.gfield, Type.INT_TYPE, this.afield, this.aop);
        else
            agg = new StringAggregator(this.gfield, Type.STRING_TYPE, this.afield, this.aop);

    }

    /**
     * @return If this aggregate is accompanied by a groupby, return the groupby
     * field index in the <b>INPUT</b> tuples. If not, return
     * {@link Aggregator#NO_GROUPING}
     */
    public int groupField() {
        // some code goes here
        if(gfield >= 0) {
            return gfield;
        }
        return -1;// #define No_GROUPING -1
    }

    /**
     * @return If this aggregate is accompanied by a group by, return the name
     * of the groupby field in the <b>OUTPUT</b> tuples. If not, return
     * null;
     */
    public String groupFieldName() {
        // some code goes here
        if(gfield >= 0) {
            return child.getTupleDesc().getFieldName(gfield);
        }
        return null;
    }

    /**
     * @return the aggregate field
     */
    public int aggregateField() {
        // some code goes here
        
        return afield;
    }

    /**
     * @return return the name of the aggregate field in the <b>OUTPUT</b>
     * tuples
     */
    public String aggregateFieldName() {
        // some code goes here
        return child.getTupleDesc().getFieldName(afield);
    }

    /**
     * @return return the aggregate operator
     */
    public Aggregator.Op aggregateOp() {
        // some code goes here
        return aop;
    }

    public static String nameOfAggregatorOp(Aggregator.Op aop) {
        return aop.toString();
    }

    private OpIterator item;
    private boolean added = false;
    public void open() throws NoSuchElementException, DbException,
            TransactionAbortedException {
        child.open();
        while (child.hasNext() && ! added) {
            agg.mergeTupleIntoGroup(child.next());
        }
        added = true; child.rewind();
        item = agg.iterator();
        item.open();
        super.open();
    }

    /**
     * Returns the next tuple. If there is a group by field, then the first
     * field is the field by which we are grouping, and the second field is the
     * result of computing the aggregate. If there is no group by field, then
     * the result tuple should contain one field representing the result of the
     * aggregate. Should return null if there are no more tuples.
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here
        if(item.hasNext())
            return item.next();
        return null;
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        item = agg.iterator();
        item.open();
    }

    /**
     * Returns the TupleDesc of this Aggregate. If there is no group by field,
     * this will have one field - the aggregate column. If there is a group by
     * field, the first field will be the group by field, and the second will be
     * the aggregate value column.
     * <p>
     * The name of an aggregate column should be informative. For example:
     * "aggName(aop) (child_td.getFieldName(afield))" where aop and afield are
     * given in the constructor, and child_td is the TupleDesc of the child
     * iterator.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        if(gfield>=0){
            // Grouping is required
            TupleDesc childTd = child.getTupleDesc();
            Type groupType = childTd.getFieldType(gfield);
            Type aggType = childTd.getFieldType(afield);
            String groupName = childTd.getFieldName(gfield);
            String aggName = childTd.getFieldName(afield);;
            return new TupleDesc(new Type[]{groupType, aggType}, new String[]{groupName, aggName});
        }
        else{
            // No grouping, just aggregate
            TupleDesc childTd = child.getTupleDesc();
            Type aggType = childTd.getFieldType(afield);
            String aggName = nameOfAggregatorOp(aop) + "(" + childTd.getFieldName(afield) + ")";
            return new TupleDesc(new Type[]{aggType}, new String[]{aggName});
        }
    }

    public void close() {
        // some code goes here
        super.close();
        //child.close();
    }

    @Override
    public OpIterator[] getChildren() {
        // some code goes here
        return new OpIterator[]{child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        // some code goes here
        this.child = children[0];
    }

}
