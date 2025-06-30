package simpledb.execution;

import javax.xml.crypto.Data;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.storage.BufferPool;
import simpledb.storage.IntField;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

/**
 * Inserts tuples read from the child operator into the tableId specified in the
 * constructor
 */
public class Insert extends Operator {

    private static final long serialVersionUID = 1L;
    private TransactionId transactionId;
    private OpIterator child;
    private int tableId;
    //private boolean hasInserted;

    /**
     * Constructor.
     *
     * @param t
     *            The transaction running the insert.
     * @param child
     *            The child operator from which to read tuples to be inserted.
     * @param tableId
     *            The table in which to insert tuples.
     * @throws DbException
     *             if TupleDesc of child differs from table into which we are to
     *             insert.
     */
    public Insert(TransactionId t, OpIterator child, int tableId)
            throws DbException {
        // some code goes here
        this.transactionId = t;
        this.child = child;
        this.tableId = tableId;
        //this.hasInserted = false;
    }

    /**
     * Returns the TupleDesc of this operator. The tuple will contain a single
     * field - an integer representing the number of inserted records.
     *
     * @return TupleDesc of this operator.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        TupleDesc td=new TupleDesc(new Type[]{Type.INT_TYPE});
        return td;
    }

    public void open() throws DbException, TransactionAbortedException {
        // some code goes here
       child.open();
    }

    public void close() {
        // some code goes here
        child.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        child.rewind();
        // some code goes here
    }

    /**
     * Inserts tuples read from child into the tableId specified by the
     * constructor. It returns a one field tuple containing the number of
     * inserted records. Inserts should be passed through BufferPool. An
     * instances of BufferPool is available via Database.getBufferPool(). Note
     * that insert DOES NOT need check to see if a particular tuple is a
     * duplicate before inserting it.
     *
     * @return A 1-field tuple containing the number of inserted records, or
     *         null if called more than once.
     * @see Database#getBufferPool
     * @see BufferPool#insertTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here
        Tuple res= new Tuple(getTupleDesc());
        int count = 0;
         while(child.hasNext()){
                Tuple tuple = child.next();
                try{
                    Database.getBufferPool().insertTuple(transactionId, tableId, tuple);
                    count++;
                }
                catch(DbException | java.io.IOException e) {
                    // If there is an error inserting the tuple, we can just skip it.
                    // This could happen if the tuple does not match the schema of the table,
                    // or if there is an IO error.
                    continue;
                }

                
            }   
        res.setField(0, new IntField(count));
        return res;
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
