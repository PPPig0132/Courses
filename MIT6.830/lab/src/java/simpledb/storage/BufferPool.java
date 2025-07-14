package simpledb.storage;

import simpledb.common.Database;
import simpledb.common.Permissions;
import simpledb.common.DbException;
import simpledb.common.DeadlockException;
import simpledb.common.LockManager;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
 
/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 * 
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /** Bytes per page, including header. */
    private static final int DEFAULT_PAGE_SIZE = 4096;

    private static int pageSize = DEFAULT_PAGE_SIZE;
    
    /** Default number of pages passed to the constructor. This is used by
    other classes. BufferPool should use the numPages argument to the
    constructor instead. */
    public static final int DEFAULT_PAGES = 50;

    private final int numPages; // Maximum number of pages in the buffer pool
    private ConcurrentHashMap<Integer, Page> pageMap; // Map to hold pages in the buffer pool
    private ConcurrentHashMap<Integer, Integer> usedPages; // List to keep track of used pages for eviction,<pid,lruIndex>
    private Integer lruIndex; // Index for LRU eviction policy
    
    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    
    public BufferPool(int numPages) {
        // some code goes here
        this.numPages = numPages;
        pageMap = new ConcurrentHashMap<>();
        usedPages = new ConcurrentHashMap<>();
        lruIndex = 0; // Initialize the LRU index
    }
    
    public static int getPageSize() {
      return pageSize;
    }
    
    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
    	BufferPool.pageSize = pageSize;
    }
    
    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void resetPageSize() {
    	BufferPool.pageSize = DEFAULT_PAGE_SIZE;
    }

    public LockManager lockManager = new LockManager();

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, a page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid the ID of the transaction requesting the page
     * @param pid the ID of the requested page
     * @param perm the requested permissions on the page
     */

    //TODO :how to deal with the perimiision and transaction id (resolved with lab4)
    public  Page getPage(TransactionId tid, PageId pid, Permissions perm)
    throws TransactionAbortedException, DbException {
        // some code goes here
         if(pageMap.size() >= numPages) {
            try {
                evictPage(); // Evict a page if the buffer pool is full
            } catch (DbException e) {
                throw new DbException("Buffer pool is full and cannot evict a page.");
            }
            }
        synchronized (new Object()) {
           
        lockManager.addLock(pid, tid, perm); // Acquire lock for the transaction on the page
        
       
        
         if(!pageMap.containsKey(pid.hashCode())){
            DbFile dbFile = Database.getCatalog().getDatabaseFile(pid.getTableId());
            Page page = dbFile.readPage(pid);
            pageMap.put(pid.hashCode(), page);// Initialize min if it's the first page
            usedPages.put(pid.hashCode(), lruIndex++); // Add the page to the buffer pool with LRU index
         }
         
        return pageMap.get(pid.hashCode());
        }
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public  void unsafeReleasePage(TransactionId tid, PageId pid) {
        // some code goes here
        // not necessary for lab1|lab2
        lockManager.releaseExactLock(tid, pid);
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) {
        // some code goes here
        // not necessary for lab1|lab2
         Set<PageId> tidOfPage = lockManager.getTidPage(tid);
        lockManager.releaseTidLock(tid);
        if(tidOfPage == null) return;
        for(PageId pr : tidOfPage) {
            if(! pageMap.containsKey(pr.hashCode()))
                continue;
            pageMap.get(pr.hashCode()).markDirty( false, null);
            DbFile dbFile = Database.getCatalog().getDatabaseFile(pr.getTableId());
            try {
                if(pageMap.get(pr.hashCode()) == null) continue;
                pageMap.get(pr.hashCode()).setBeforeImage();
                Database.getLogFile().logWrite(tid, pageMap.get(pr.hashCode()).getBeforeImage(), pageMap.get(pr.hashCode()));
                Database.getLogFile().force();
                dbFile.writePage(pageMap.get(pr.hashCode()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** Return true if the specified transaction has a lock on the specified page */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // some code goes here
        // not necessary for lab1|lab2
        boolean hasLock = lockManager.HasLock(p, tid);
        return hasLock;
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit) {
        // some code goes here
        // not necessary for lab1|lab2
        if(commit) {
            transactionComplete(tid);
        } else {
            Set<PageId> pageIds = lockManager.getTidPage(tid);
            lockManager.releaseTidLock(tid);
            if(pageIds == null) return;
            for(PageId pr : pageIds)
                pageMap.remove(pr.hashCode());
        }
    }

    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other 
     * pages that are updated (Lock acquisition is not needed for lab2). 
     * May block if the lock(s) cannot be acquired.
     * 
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have 
     * been dirtied to the cache (replacing any existing versions of those pages) so 
     * that future requests see up-to-date pages. 
     *
     * @param tid the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        // some code goes here
    // HeapFile heapFile = (HeapFile) Database.getCatalog().getDatabaseFile(tableId);
    // List<Page> modifiedPages = heapFile.insertTuple(tid, t);
        //not only heapfile, but also btreefile
        DbFile dbFile = Database.getCatalog().getDatabaseFile(tableId);
        
         List<Page> modifiedPages = dbFile.insertTuple(tid, t);
        for (Page page : modifiedPages) {
            // Mark the page as dirty
            page.markDirty(true, tid);
            // Add the page to the buffer pool
            if(!pageMap.containsKey(page.getId().hashCode())) {
                pageMap.put(page.getId().hashCode(), page);
            }
            usedPages.put(page.getId().hashCode(), lruIndex++);
            // map.insert(page.getId(), page);
        }
        // not necessary for lab1
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have 
     * been dirtied to the cache (replacing any existing versions of those pages) so 
     * that future requests see up-to-date pages. 
     *
     * @param tid the transaction deleting the tuple.
     * @param t the tuple to delete
     */
    public  void deleteTuple(TransactionId tid, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        DbFile dbFile = Database.getCatalog().getDatabaseFile(t.getRecordId().getPageId().getTableId());
        List<Page> modifiedPages = dbFile.deleteTuple(tid, t);
        for (Page page : modifiedPages) {
            // Mark the page as dirty
            page.markDirty(true, tid);
            // Add the page to the buffer pool
            pageMap.put(page.getId().hashCode(), page);
            usedPages.put(page.getId().hashCode(), lruIndex++);
        }
        // not necessary for lab1
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // some code goes here
        // not necessary for lab1
        for(Map.Entry< Integer,Page> entry : pageMap.entrySet()){
            PageId pid = entry.getValue().getId();
            flushPage(pid);
        }

    }

    /** Remove the specific page id from the buffer pool.
        Needed by the recovery manager to ensure that the
        buffer pool doesn't keep a rolled back page in its
        cache.
        
        Also used by B+ tree files to ensure that deleted pages
        are removed from the cache so they can be reused safely
    */
    public synchronized void discardPage(PageId pid) {
        // some code goes here
        // not necessary for lab1
        if (pageMap.containsKey(pid.hashCode())) {
            pageMap.remove(pid.hashCode());
        }
    }

    /**
     * Flushes a certain page to disk
     * @param pid an ID indicating the page to flush
     */
    private synchronized  void flushPage(PageId pid)  {
        // some code goes here
        // not necessary for lab1
        Page page = pageMap.get(pid.hashCode());
        if (page != null && page.isDirty() != null) {
            try{
                // append an update record to the log, with 
                // a before-image and after-image.
                TransactionId dirtier = page.isDirty();
                if (dirtier != null){
                    Database.getLogFile().logWrite(dirtier, page.getBeforeImage(), page);
                    Database.getLogFile().force();
                }
                DbFile dbFile = Database.getCatalog().getDatabaseFile(pid.getTableId());
                dbFile.writePage(page);
                page.markDirty(false, null); // Mark the page as clean after flushing
            }catch (IOException e) {
                throw new RuntimeException(e);
            }
           
        }
    }

    /** Write all pages of the specified transaction to disk.
     */
    public synchronized  void flushPages(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
       
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     * which should be evicted: the page with the least recently used
     * (LRU) policy.
     */
    private synchronized  void evictPage() throws DbException {
        // some code goes here
        // not necessary for lab1
        int minIndex = Integer.MAX_VALUE;
        PageId min = null;
        for(Map.Entry< Integer,Page> entry : pageMap.entrySet()){
            PageId pid = entry.getValue().getId();
            int index = usedPages.get(pid.hashCode());
            if (index < minIndex && entry.getValue().isDirty() == null) {
                minIndex = index;
                min = pid;
            }
        }
        if(min == null) {
            throw new DbException("No clean page to evict");
        }
        flushPage(min);
        discardPage(min);
       
        
    }

}
