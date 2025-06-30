package simpledb.execution;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.storage.BufferPool;
import simpledb.storage.IntField;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The delete operator. Delete reads tuples from its child operator and removes
 * them from the table they belong to.
 */
public class Delete extends Operator {

    private static final long serialVersionUID = 1L;
    private TransactionId transactionId; // The transaction this delete runs in
    private OpIterator child; // The child operator from which to read tuples for deletion
    private ArrayList<Tuple> tuples = new ArrayList<>();
    Iterator<Tuple> item;
    /**
     * Constructor specifying the transaction that this delete belongs to as
     * well as the child to read from.
     * 
     * @param t
     *            The transaction this delete runs in
     * @param child
     *            The child operator from which to read tuples for deletion
     */
    public Delete(TransactionId t, OpIterator child) {//Delete tuple form buffer pool which needn't a specific tableId
        // some code goes here
        this.transactionId = t;
        this.child = child;
    }

    public TupleDesc getTupleDesc() {
        // some code goes here
        TupleDesc td=new TupleDesc(new Type[]{Type.INT_TYPE});
        return td;
    }

    public void open() throws DbException, TransactionAbortedException {
        // some code goes here
        child.open();
        super.open(); // Call the open method of the parent class to set up the operator
        int count = 0;
         while(child.hasNext()){//在open的时候对结果进行存储，保证每次delete操作都能只执行一次
                Tuple tuple = child.next();
                try{
                    Database.getBufferPool().deleteTuple(transactionId, tuple);
                    count++;
                }
                catch(DbException | java.io.IOException e) {
                    // If there is an error inserting the tuple, we can just skip it.
                    // This could happen if the tuple does not match the schema of the table,
                    // or if there is an IO error.
                    continue;
                }

                
            }
        
            Tuple res= new Tuple(getTupleDesc());
            res.setField(0, new IntField(count));
            tuples.add(res);
            item = tuples.iterator(); // Initialize the iterator for the result tuples
    }

    public void close() {
        // some code goes here
        super.close(); // Call the close method of the parent class to clean up the operator
        child.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        child.rewind();
    }

    /**
     * Deletes tuples as they are read from the child operator. Deletes are
     * processed via the buffer pool (which can be accessed via the
     * Database.getBufferPool() method.
     * 
     * @return A 1-field tuple containing the number of deleted records.
     * @see Database#getBufferPool
     * @see BufferPool#deleteTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here
        if(item!= null && item.hasNext()) {
            return item.next(); // Return the next tuple from the result set
        }
       
        return null;
        
    }

    @Override
    public OpIterator[] getChildren() {
        // some code goes here
       return new OpIterator[] { this.child };
    }

    @Override
    public void setChildren(OpIterator[] children) {
        // some code goes here
        this.child = children[0];
    }

}
