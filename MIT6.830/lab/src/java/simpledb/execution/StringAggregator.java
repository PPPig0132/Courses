package simpledb.execution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.storage.Field;
import simpledb.storage.IntField;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private int gbfield; // index of the group-by field
    private Type gbfieldtype; // type of the group-by field
    private int afield; // index of the aggregate field
    private Op what; // aggregation operator
    private AggOpera.Basic agg;
    private HashMap<Field, int[]> infos = new HashMap<>();
    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.what = what;
        this.agg = AggOpera.Tool.getBasic(what, afield);
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
   public Field getKey(Tuple tup) {
        return this.gbfield == -1 ? new IntField(-1) : tup.getField(this.gbfield);
    }

    public boolean _check(Tuple tup) {
        Field gid = getKey(tup);
        int[] tem = infos.get(gid);
        return tem != null;
    }

    public void mergeTupleIntoGroup(Tuple tup) {
        if(_check(tup)) {
            infos.put(getKey(tup), agg.oper(infos.get(getKey(tup)), tup));
        }else
            infos.put(getKey(tup), agg.init(tup));
    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public OpIterator iterator() {
        // some code goes here
        return new OpIterator() {
            private boolean opened = false;
            private Iterator<Tuple> item = null;
            private ArrayList<Tuple> tuples = new ArrayList<>();

            private void getIter() {
                tuples.clear();
                if(infos.isEmpty()) return;
                if(gbfield == Aggregator.NO_GROUPING) {
                    Tuple tuple = new Tuple(new TupleDesc(new Type[]{Type.INT_TYPE}));
                    tuple.setField(0, new IntField(
                            infos.get(new IntField(-1))[0]
                    ));
                    tuples.add(tuple);
                }else {
                    for(Map.Entry<Field, int[]> pr : infos.entrySet()) {
                        Tuple tuple = new Tuple(new TupleDesc(new Type[]{pr.getKey().getType() , Type.INT_TYPE}));
                        tuple.setField(0, pr.getKey());
                        tuple.setField(1, new IntField(pr.getValue()[0]));
                        tuples.add(tuple);
                    }
                }
                item = tuples.iterator();
            }

            @Override
            public void open() throws DbException, TransactionAbortedException {
                this.opened = true;
                this.getIter();
                this.rewind();
            }

            @Override
            public boolean hasNext() throws DbException, TransactionAbortedException {
                if(! this.opened)
                    throw new IllegalStateException();
                return item.hasNext();
            }

            @Override
            public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
                if(! this.opened)
                    throw new IllegalStateException();
                if(item.hasNext()) {
                    return item.next();
                }
                throw new NoSuchElementException();
            }

            @Override
            public void rewind() throws DbException, TransactionAbortedException {
                getIter();
            }

            @Override
            public TupleDesc getTupleDesc() {
                if(tuples == null) {
                    try {
                        open();
                    } catch (TransactionAbortedException | DbException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(tuples == null || tuples.isEmpty())
                    return null;
                return tuples.get(0).getTupleDesc();
            }

            @Override
            public void close() {
                this.opened = false;
            }
        };
        //throw new UnsupportedOperationException("please implement me for lab2");
    }

}
