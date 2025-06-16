package simpledb.common;

import simpledb.common.Type;
import simpledb.storage.DbFile;
import simpledb.storage.HeapFile;
import simpledb.storage.TupleDesc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Catalog keeps track of all available tables in the database and their
 * associated schemas.
 * For now, this is a stub catalog that must be populated with tables by a
 * user program before it can be used -- eventually, this should be converted
 * to a catalog that reads a catalog table from disk.
 * 
 * @Threadsafe
 */
public class Catalog {

    //使用hashmap处理表id与Dbfile之间的唯一性
    private final ConcurrentHashMap<Integer,table> tableMap;

    public static class table implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The schema of table
         * */
       // public final TupleDesc td;

        /**
         * The dbfile of table
         * */
        public final DbFile file;

        public final String tableName;

        public String pkey;

        public table(DbFile d, String n ,String k) {
            this.file = d;
            this.tableName = n;
            this.pkey = k;
        }

       // public String toString() {
           // return tableName + "(" + fieldType + ")";
       // }
    }

    /**
     * Constructor.
     * Creates a new, empty catalog.
     */
    public Catalog() {
        this.tableMap = new ConcurrentHashMap<>();
        // some code goes here
    }

    /**
     * Add a new table to the catalog.
     * This table's contents are stored in the specified DbFile.
     * @param file the contents of the table to add;  file.getId() is the identfier of
     *    this file/tupledesc param for the calls getTupleDesc and getFile
     * @param name the name of the table -- may be an empty string.  May not be null.  If a name
     * conflict exists, use the last table to be added as the table for a given name.
     * @param pkeyField the name of the primary key field
     */
    public void addTable(DbFile file, String name, String pkeyField) {
        this.tableMap.put(file.getId(),new table(file, name, pkeyField));
        // some code goes here
    }

    public void addTable(DbFile file, String name) {
        addTable(file, name, "");
    }

    /**
     * Add a new table to the catalog.
     * This table has tuples formatted using the specified TupleDesc and its
     * contents are stored in the specified DbFile.
     * @param file the contents of the table to add;  file.getId() is the identfier of
     *    this file/tupledesc param for the calls getTupleDesc and getFile
     */
    public void addTable(DbFile file) {
        addTable(file, (UUID.randomUUID()).toString());
    }

    /**
     * Return the id of the table with a specified name,
     * @throws NoSuchElementException if the table doesn't exist
     */
    public int getTableId(String name) throws NoSuchElementException {
        // some code goes here
        Integer res=null;
        for(table item : tableMap.values()) {
            if (item.tableName.equals(name)) {
                res = item.file.getId();
                break;
            }
        }
        // if(id==-1){
        //     throw new NoSuchElementException("Table with name " + name + " does not exist in the catalog.");
        // }
        // return id;

        // Integer res =tableMap.entrySet().stream()
        //         .filter(entry -> entry.getValue().tableName.equals(name))
        //         .map(Map.Entry::getKey)
        //         .findFirst()
        //         .orElse(null);
        if(res != null){
            return res.intValue();
        }else{
            throw new NoSuchElementException("not found id for table " + name);
        }
        
    }

    /**
     * Returns the tuple descriptor (schema) of the specified table
     * @param tableid The id of the table, as specified by the DbFile.getId()
     *     function passed to addTable
     * @throws NoSuchElementException if the table doesn't exist
     */
    public TupleDesc getTupleDesc(int tableid) throws NoSuchElementException {
        // some code goes here
        TupleDesc td = null;
        if(tableMap.containsKey(tableid)){
            td = tableMap.get(tableid).file.getTupleDesc();
            return td;
        }
       
        throw new NoSuchElementException("Table with id " + tableid + " does not exist in the catalog.");
    }

    /**
     * Returns the DbFile that can be used to read the contents of the
     * specified table.
     * @param tableid The id of the table, as specified by the DbFile.getId()
     *     function passed to addTable
     */
    public DbFile getDatabaseFile(int tableid) throws NoSuchElementException {
        DbFile db = null;
        if(tableMap.containsKey(tableid)){
            db = tableMap.get(tableid).file;
            return db;
        }
        throw new NoSuchElementException("Table with id " + tableid + " does not exist in the catalog.");
    }

    public String getPrimaryKey(int tableid) {
        // some code goes here
        String key = null;
        if(tableMap.containsKey(tableid)){
            key = tableMap.get(tableid).pkey;
            return key;
        }
        throw new NoSuchElementException("Table with id " + tableid + " does not exist in the catalog.");
    }

    public Iterator<Integer> tableIdIterator() {
        // some code goes here
        return tableMap.values().stream()
                .map(table -> table.file.getId())
                .iterator();
    }

    public String getTableName(int id) {
        // some code goes here
        String name = null;
        if (tableMap.containsKey(id)) {
            name = tableMap.get(id).tableName;
        } else {
            throw new NoSuchElementException("Table with id " + id + " does not exist in the catalog.");
        }
        return name;
    }
    
    /** Delete all tables from the catalog */
    public void clear() {
        tableMap.clear();
        // some code goes here
    }
    
    /**
     * Reads the schema from a file and creates the appropriate tables in the database.
     * @param catalogFile
     */
    public void loadSchema(String catalogFile) {
        String line = "";
        String baseFolder=new File(new File(catalogFile).getAbsolutePath()).getParent();
        try {
            BufferedReader br = new BufferedReader(new FileReader(catalogFile));
            
            while ((line = br.readLine()) != null) {
                //assume line is of the format name (field type, field type, ...)
                String name = line.substring(0, line.indexOf("(")).trim();
                //System.out.println("TABLE NAME: " + name);
                String fields = line.substring(line.indexOf("(") + 1, line.indexOf(")")).trim();
                String[] els = fields.split(",");
                ArrayList<String> names = new ArrayList<>();
                ArrayList<Type> types = new ArrayList<>();
                String primaryKey = "";
                for (String e : els) {
                    String[] els2 = e.trim().split(" ");
                    names.add(els2[0].trim());
                    if (els2[1].trim().equalsIgnoreCase("int"))
                        types.add(Type.INT_TYPE);
                    else if (els2[1].trim().equalsIgnoreCase("string"))
                        types.add(Type.STRING_TYPE);
                    else {
                        System.out.println("Unknown type " + els2[1]);
                        System.exit(0);
                    }
                    if (els2.length == 3) {
                        if (els2[2].trim().equals("pk"))
                            primaryKey = els2[0].trim();
                        else {
                            System.out.println("Unknown annotation " + els2[2]);
                            System.exit(0);
                        }
                    }
                }
                Type[] typeAr = types.toArray(new Type[0]);
                String[] namesAr = names.toArray(new String[0]);
                TupleDesc t = new TupleDesc(typeAr, namesAr);
                HeapFile tabHf = new HeapFile(new File(baseFolder+"/"+name + ".dat"), t);
                addTable(tabHf,name,primaryKey);
                System.out.println("Added table : " + name + " with schema " + t);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IndexOutOfBoundsException e) {
            System.out.println ("Invalid catalog entry : " + line);
            System.exit(0);
        }
    }
}

