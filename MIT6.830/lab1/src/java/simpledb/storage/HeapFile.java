package simpledb.storage;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Debug;
import simpledb.common.Permissions;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 * 
 * @see HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {
    private final File file;
    private final TupleDesc tupleDesc;
    /**
     * Constructs a heap file backed by the specified file.
     * 
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        this.file = f;
        this.tupleDesc = td;
        // some code goes here
    }

    /**
     * Returns the File backing this HeapFile on disk.
     * 
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // some code goes here
        return file;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     * 
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // some code goes here
        return file.getAbsoluteFile().hashCode();
        //throw new UnsupportedOperationException("implement this");
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     * 
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return tupleDesc;
        //throw new UnsupportedOperationException("implement this");
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        // some code goes here
       int tableId=pid.getTableId();
       int pgNo=pid.getPageNumber();
        RandomAccessFile f = null;
        try{
            f = new RandomAccessFile(file,"r");
            if((pgNo+1)*BufferPool.getPageSize() > f.length()){
                f.close();
                throw new IllegalArgumentException(String.format("table %d page %d is invalid", tableId, pgNo));
            }
            byte[] bytes = new byte[BufferPool.getPageSize()];
            f.seek(pgNo * BufferPool.getPageSize());
            // big end
            int read = f.read(bytes,0,BufferPool.getPageSize());
            if(read != BufferPool.getPageSize()){
                throw new IllegalArgumentException(String.format("table %d page %d read %d bytes", tableId, pgNo, read));
            }
            HeapPageId id = new HeapPageId(pid.getTableId(),pid.getPageNumber());
            return new HeapPage(id,bytes);
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try{
                f.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        throw new IllegalArgumentException(String.format("table %d page %d is invalid", tableId, pgNo));
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // some code goes here
        int numPages = (int) (file.length() / BufferPool.getPageSize());
        return numPages;
    }

    // see DbFile.java for javadocs
    public List<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here
        return  new DbFileIterator() {
 
            private int numPage = numPages();
            private int pid = 0;
            private BufferPool bufferPool = Database.getBufferPool();
            private HeapPage currPage;
            private Iterator<Tuple> currTupleIter;
            private boolean isOpen = false;
 
            @Override
            public void open() throws DbException, TransactionAbortedException {
                isOpen = true;
                getPage(pid++);
            }
 
            private boolean getPage(int pid) throws TransactionAbortedException, DbException {
                if (!isOpen) throw new DbException("not open");
                currPage = (HeapPage) bufferPool.getPage(tid, new HeapPageId(getId(), pid), Permissions.READ_ONLY);
                if (currPage == null) return false;
                currTupleIter = currPage.iterator();
                return true;
            }
 
            @Override
            public boolean hasNext() throws DbException, TransactionAbortedException {
               
                return (isOpen && pid < numPage) || (pid == numPage && currTupleIter.hasNext());
            }
 
            @Override
            public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
                if(!isOpen || currTupleIter == null){
                    throw new NoSuchElementException();
                }
                if(!currTupleIter.hasNext()){
                    getPage(pid++);
                }
                return currTupleIter.next();
            }
 
            @Override
            public void rewind() throws DbException, TransactionAbortedException {
                close();
                open();
            }
 
            @Override
            public void close() {
                isOpen = false;
                pid = 0;
                currPage = null;
                currTupleIter = null;
            }
        };

    }
        



}

