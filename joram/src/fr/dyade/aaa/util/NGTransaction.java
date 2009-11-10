/*
 * Copyright (C) 2009 ScalAgent Distributed Technologies
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA.
 *
 * Initial developer(s): ScalAgent Distributed Technologies
 * Contributor(s): 
 */
package fr.dyade.aaa.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;

import org.objectweb.util.monolog.api.BasicLevel;
import org.objectweb.util.monolog.api.Logger;

import fr.dyade.aaa.common.Configuration;
import fr.dyade.aaa.common.Debug;
import fr.dyade.aaa.common.Pool;

/**
 *  The NGTransaction class implements a transactional storage.
 *  For efficiency it uses a file for its transaction journal, the final
 * storage is provided through the Repository interface on filesystem or
 * database.
 *
 * @see Transaction
 * @see Repository
 * @see FileRepository
 * @see DBRepository
 * @see MySqlDBRepository
 */
/**
 *
 */
public final class NGTransaction extends AbstractTransaction implements NGTransactionMBean {
  // Logging monitor
  private static Logger logmon = null;

  /**
   *  Global in memory log initial capacity, by default 4096.
   *  This value can be adjusted for a particular server by setting
   * <code>NTLogMemoryCapacity</code> specific property.
   * <p>
   *  These property can be fixed either from <code>java</code> launching
   * command, or in <code>a3servers.xml</code> configuration file.
   */
  static int LogMemoryCapacity = 4096;

  /**
   * Returns the initial capacity of global in memory log (by default 4096).
   *
   * @return The initial capacity of global in memory log.
   */
  public final int getLogMemoryCapacity() {
    return LogMemoryCapacity;
  }

  /**
   * Returns the number of operation in the memory log.
   *
   * @return The number of operation in the memory log.
   */
  public int getLogMemorySize() {
    return logManager.log.size();
  }
  
  /**
   *  Size of disk log in Mb, by default 16Mb.
   *  This value can be adjusted (Mb) for a particular server by setting
   * <code>NTLogFileSize</code> specific property.
   * <p>
   *  These property can be fixed either from <code>java</code> launching
   * command, or in <code>a3servers.xml</code> configuration file.
   */
  static int MaxLogFileSize = 16 * Mb;

  /**
   * Returns the maximum size of disk log in Mb, by default 16Mb.
   *
   * @return The maximum size of disk log in Mb.
   */
  public final int getMaxLogFileSize() {
    return MaxLogFileSize/Mb;
  }

  /**
   * Sets the maximum size of disk log in Mb.
   *
   * @param size The maximum size of disk log in Mb.
   */
  public final void setMaxLogFileSize(int size) {
    if (size > 0) MaxLogFileSize = size *Mb;
  }

  /**
   * Returns the size of disk log in Kb.
   *
   * @return The size of disk log in Kb.
   */
  public final int getLogFileSize() {
    return (logManager.getLogFileSize() /Kb);
  }

  /**
   * Returns the number of rolled log files.
   * 
   * @return The number of rolled log files.
   */
  public final int getNbLogFiles() {
    return logManager.nbLogFile;
  }

  /**
   *  Number of pooled operation, by default 1000.
   *  This value can be adjusted for a particular server by setting
   * <code>NTLogThresholdOperation</code> specific property.
   * <p>
   *  These property can be fixed either from <code>java</code> launching
   * command, or in <code>a3servers.xml</code> configuration file.
   */
  static int LogThresholdOperation = 1000;

  /**
   * Returns the pool size for <code>operation</code> objects, by default 1000.
   *
   * @return The pool size for <code>operation</code> objects.
   */
  public final int getLogThresholdOperation() {
    return LogThresholdOperation;
  }

  /**
   * Returns the number of commit operation since starting up.
   *
   * @return The number of commit operation.
   */
  public final int getCommitCount() {
    return logManager.commitCount;
  }

  /**
   * Returns the number of garbage operation since starting up.
   *
   * @return The number of garbage operation.
   */
  public final int getGarbageCount() {
    return logManager.garbageCount;
  }

  /**
   * Returns the cumulated time of garbage operations since starting up.
   *
   * @return The cumulated time of garbage operations since starting up.
   */
  public long getGarbageTime() {
    return logManager.garbageTime;
  }

  /**
   * Returns the number of load operation from a log file since last start.
   * 
   * @return The number of load operation from a log file.
   */
  public int getNbLoadedFromLog() {
    return logManager.loadFromLog;
  }
  
  /**
   * Returns the ratio of garbage operations since starting up.
   *
   * @return The ratio of garbage operations since starting up.
   */
  public int getGarbageRatio() {
    return (int) ((logManager.garbageTime *100) / (System.currentTimeMillis() - startTime));
  }

  /**
   *  The Repository classname implementation.
   *  This value can be set for a particular server by setting the
   * <code>NTRepositoryImpl</code> specific property. By default its value
   * is "fr.dyade.aaa.util.FileRepository".
   * <p>
   *  These property can be fixed either from <code>java</code> launching
   * command, or in <code>a3servers.xml</code> configuration file.
   */
  String repositoryImpl = "fr.dyade.aaa.util.FileRepository";

  /**
   * Returns the Repository classname implementation.
   *
   * @return The Repository classname implementation.
   */
  public String getRepositoryImpl() {
    return repositoryImpl;
  }

  /**
   * Returns the number of save operation to repository.
   *
   * @return The number of save operation to repository.
   */
  public int getNbSavedObjects() {
    return repository.getNbSavedObjects();
  }

  /**
   * Returns the number of delete operation on repository.
   *
   * @return The number of delete operation on repository.
   */
  public int getNbDeletedObjects() {
    return repository.getNbDeletedObjects();
  }

  /**
   * Returns the number of useless delete operation on repository.
   *
   * @return The number of useless delete operation on repository.
   */
  public int getNbBadDeletedObjects() {
    return repository.getNbBadDeletedObjects();
  }

  /**
   * Returns the number of load operation from repository.
   *
   * @return The number of load operation from repository.
   */
  public int getNbLoadedObjects() {
    return repository.getNbLoadedObjects();
  }

  long startTime = 0L;

  /**
   * Returns the starting time.
   *
   * @return The starting time.
   */
  public long getStartTime() {
    return startTime;
  }

  File dir = null;

  LogManager logManager = null;

  Repository repository = null;

  public NGTransaction() {}

  public final void init(String path) throws IOException {
    phase = INIT;

    logmon = Debug.getLogger(Transaction.class.getName());
    if (logmon.isLoggable(BasicLevel.INFO))
      logmon.log(BasicLevel.INFO, "NTransaction, init():");

    LogMemoryCapacity = Configuration.getInteger("NTLogMemoryCapacity", LogMemoryCapacity).intValue();
    MaxLogFileSize = Configuration.getInteger("NTLogFileSize", MaxLogFileSize / Mb).intValue() * Mb;

    LogThresholdOperation = Configuration.getInteger("NTLogThresholdOperation", LogThresholdOperation).intValue();
    Operation.pool = new Pool("NGTransaction$Operation", LogThresholdOperation);

    dir = new File(path);
    if (!dir.exists()) dir.mkdir();
    if (!dir.isDirectory())
      throw new FileNotFoundException(path + " is not a directory.");

    // Saves the transaction classname in order to prevent use of a
    // different one after restart (see AgentServer.init).
    DataOutputStream ldos = null;
    try {
      File tfc = new File(dir, "TFC");
      if (! tfc.exists()) {
        ldos = new DataOutputStream(new FileOutputStream(tfc));
        ldos.writeUTF(getClass().getName());
        ldos.flush();
      }
    } finally {
      if (ldos != null) ldos.close();
    }

    try {
      repositoryImpl = System.getProperty("NTRepositoryImpl", repositoryImpl);
      repository = (Repository) Class.forName(repositoryImpl).newInstance();
      repository.init(dir);
    } catch (ClassNotFoundException exc) {
      logmon.log(BasicLevel.FATAL,
                 "NTransaction, cannot initializes the repository ", exc);
      throw new IOException(exc.getMessage());
    } catch (InstantiationException exc) {
      logmon.log(BasicLevel.FATAL,
                 "NTransaction, cannot initializes the repository ", exc);
      throw new IOException(exc.getMessage());
    } catch (IllegalAccessException exc) {
      logmon.log(BasicLevel.FATAL,
                 "NTransaction, cannot initializes the repository ", exc);
      throw new IOException(exc.getMessage());
    }
    logManager = new LogManager(dir, repository);

    perThreadContext = new ThreadLocal() {
      protected synchronized Object initialValue() {
        return new Context();
      }
    };
    
    startTime = System.currentTimeMillis();

    if (logmon.isLoggable(BasicLevel.INFO))
      logmon.log(BasicLevel.INFO, "NTransaction, initialized " + startTime);

    /* The Transaction subsystem is ready */
    setPhase(FREE);
  }

  /**
   * Tests if the Transaction component is persistent.
   *
   * @return true.
   */
  public boolean isPersistent() {
    return true;
  }

  /**
   * Returns the path of persistence directory.
   *
   * @return The path of persistence directory.
   */
  public String getPersistenceDir() {
    return dir.getPath();
  }

  protected final void setPhase(int newPhase) {
    phase = newPhase;
  }

  /**
   *  Returns an array of strings naming the persistent objects denoted by
   * a name that satisfy the specified prefix. Each string is an object name.
   * 
   * @param prefix	the prefix
   * @return		An array of strings naming the persistent objects
   *		 denoted by a name that satisfy the specified prefix. The
   *		 array will be empty if no names match.
   */
  public synchronized String[] getList(String prefix) {
    return logManager.getList(prefix);
  }

  /**
   *  Save an object state already serialized. The byte array in parameter
   * may be modified so we must duplicate it.
   */
  protected final void saveInLog(byte[] buf,
                                 String dirName, String name,
                                 Hashtable log,
                                 boolean copy,
                                 boolean first) throws IOException {
    if (logmon.isLoggable(BasicLevel.DEBUG))
      logmon.log(BasicLevel.DEBUG,
                 "NGTransaction, saveInLog(" + dirName + '/' + name + ", " + copy + ", " + first + ")");

    Object key = OperationKey.newKey(dirName, name);
    Operation op = null;
    if (first)
      op = Operation.alloc(Operation.CREATE, dirName, name, buf);
    else
      op = Operation.alloc(Operation.SAVE, dirName, name, buf);
    Operation old = (Operation) log.put(key, op);
    if (copy) {
      if ((old != null) &&
          (old.type == Operation.SAVE) &&
          (old.value.length == buf.length)) {
        // reuse old buffer
        op.value = old.value;
      } else {
        // alloc a new one
        op.value = new byte[buf.length];
      }
      System.arraycopy(buf, 0, op.value, 0, buf.length);
    }
    if (old != null) old.free();

  }

  private final byte[] getFromLog(Hashtable log, Object key) throws IOException {
    // Searches in the log a new value for the object.
    Operation op = (Operation) log.get(key);
    if (op != null) {
      if ((op.type == Operation.SAVE) || (op.type == Operation.CREATE)) {
        return op.value;
      } else if (op.type == Operation.DELETE) {
        // The object was deleted.
        throw new FileNotFoundException();
      }
    }
    return null;
  }

  private final synchronized byte[] getFromLog(String dirName, String name) throws IOException {
    // First searches in the current transaction log a new value for the object.
    Object key = OperationKey.newKey(dirName, name);
    byte[] buf = getFromLog(((Context) perThreadContext.get()).log, key);
    if (buf != null) return buf;
    
    // Then search in the log files and repository.
    return logManager.load(dirName, name);  
  }


  public byte[] loadByteArray(String dirName, String name) throws IOException {
    if (logmon.isLoggable(BasicLevel.DEBUG))
      logmon.log(BasicLevel.DEBUG,
                 "NTransaction, loadByteArray(" + dirName + '/' + name + ")");

    // First searches in the logs a new value for the object.
    try {
      return getFromLog(dirName, name);
    } catch (FileNotFoundException exc) {
      if (logmon.isLoggable(BasicLevel.DEBUG))
        logmon.log(BasicLevel.DEBUG,
                   "NTransaction, loadByteArray(" + dirName + '/' + name + ") not found");

      return null;
    }
  }
  
  public final void delete(String dirName, String name) {
    if (logmon.isLoggable(BasicLevel.DEBUG))
      logmon.log(BasicLevel.DEBUG,
                 "NTransaction, delete(" + dirName + ", " + name + ")");

    Object key = OperationKey.newKey(dirName, name);

    Hashtable log = ((Context) perThreadContext.get()).log;
    Operation op = Operation.alloc(Operation.DELETE, dirName, name);
    Operation old = (Operation) log.put(key, op);
    if (old != null) {
      if (old.type == Operation.CREATE) op.type = Operation.NOOP;
      old.free();
    }
  }

  public final synchronized void commit(boolean release) throws IOException {
    if (phase != RUN)
      throw new IllegalStateException("Can not commit.");

    if (logmon.isLoggable(BasicLevel.DEBUG))
      logmon.log(BasicLevel.DEBUG, "NTransaction, commit");
    
    // TODO (AF): Only the call to logManager.commit and the phase change needs to
    // be synchronized..
    
    Hashtable log = ((Context) perThreadContext.get()).log;
    if (! log.isEmpty()) {
      logManager.commit(log);
      log.clear();
    }

    // Change the transaction state to COMMIT or FREE
    if (release) {
      setPhase(FREE);
      // wake-up an eventually user's thread in begin
      notify();
    } else {
      setPhase(COMMIT);
    }
    
    if (logmon.isLoggable(BasicLevel.DEBUG))
      logmon.log(BasicLevel.DEBUG, "NTransaction, committed");
  }

  /**
   * Stops the transaction module.
   * It waits all transactions termination, then the module is kept
   * in a FREE 'ready to use' state.
   * The log file is garbaged, all operations are reported to disk.
   */
  public synchronized void stop() {
    if (logmon.isLoggable(BasicLevel.INFO))
      logmon.log(BasicLevel.INFO, "NTransaction, stops");

    while (phase != FREE) {
      // Wait for the transaction subsystem to be free
      try {
        wait();
      } catch (InterruptedException exc) {
      }
    }

    setPhase(FINALIZE);
    try {
      logManager.garbage();
    } catch (IOException exc) {
      logmon.log(BasicLevel.WARN, "NTransaction, can't garbage log files", exc);
    }
    setPhase(FREE);

    if (logmon.isLoggable(BasicLevel.INFO)) {
      logmon.log(BasicLevel.INFO, "NTransaction, stopped: " + toString());
    }
  }

  /**
   * Close the transaction module.
   * It waits all transactions termination, the module will be initialized
   * anew before reusing it.
   * The log file is garbaged then closed.
   */
  public synchronized void close() {
    if (logmon.isLoggable(BasicLevel.INFO))
      logmon.log(BasicLevel.INFO, "NTransaction, closes");

    if (phase == INIT) return;

    while (phase != FREE) {
      // Wait for the transaction subsystem to be free
      try {
        wait();
      } catch (InterruptedException exc) {
      }
    }

    setPhase(FINALIZE);
    logManager.stop();
    setPhase(INIT);

    if (logmon.isLoggable(BasicLevel.INFO)) {
      logmon.log(BasicLevel.INFO, "NTransaction, closed: " + toString());
    }
  }

  /**
   *  This class manages the memory log of operations and the multiples
   * log files.
   */
  static final class LogManager extends ByteArrayOutputStream {
    /**
     * Log of all operations already committed but not reported on disk.
     */
    Hashtable<Object, Operation> log = null;
    
    int nbLogFile = 4;
    
    int logidx;
    
    /** log file */
    LogFile[] logFile = null;

    /** Current file pointer in log */
    int current = -1;
    
    /**
     * Returns the size of disk log in bytes.
     *
     * @return The size of disk log in bytes.
     */
    int getLogFileSize() {
      return current;
    }

    /**
     * Number of commit operation since starting up.
     */
    int commitCount = 0;

    /**
     * Number of load from a log file.
     */
    int loadFromLog = 0;
    
    /**
     * Number of garbage operation since starting up.
     */
    int garbageCount = 0;

    /**
     * Cumulated time of garbage operations since starting up.
     */
    long garbageTime = 0l;

    /**
     * Date of last garbage.
     */
    long lastGarbageTime = 0L;

    private Repository repository = null;

    File dir;
    
    LogManager(File dir, Repository repository) throws IOException {
      super(4 * Kb);
      this.repository = repository;

//      boolean nolock = Boolean.getBoolean("NTNoLockFile");
//      if (! nolock) {
//        lockFile = new File(dir, LockPathname);
//        if (! lockFile.createNewFile()) {
//          logmon.log(BasicLevel.FATAL,
//                     "NTransaction.init(): " +
//                     "Either the server is already running, " + 
//                     "either you have to remove lock file: " +
//                     lockFile.getAbsolutePath());
//          throw new IOException("Transaction already running.");
//        }
//        lockFile.deleteOnExit();
//      }
      
      log = new Hashtable(LogMemoryCapacity);
      
      long start = System.currentTimeMillis();
      
      logidx = -1;
      logFile = new LogFile[nbLogFile];
      
      this.dir = dir ;
      
      String[] list = dir.list(new StartWithFilter("log#"));
      if (list == null) {
        throw new IOException("NGTransaction error opening " + dir.getAbsolutePath());
      } else if (list.length == 0) {
        logidx = 0;
      } else {
        //  Recovery of log files..
        Arrays.sort(list);
        for (int i=0; i<list.length; i++) {
          logmon.log(BasicLevel.WARN, "NGTransaction.LogManager, rebuilds index: " + list[i]);
          
          int idx = Integer.parseInt(list[i].substring(4));
          // Fix the log index to the lower index, it is needed if all log files
          // are garbaged.
          if (logidx == -1) logidx = idx;
          try {
            LogFile logf = new LogFile(dir, idx);
            int optype = logf.read();
            if (optype == Operation.END) {
              // The log is empty
              logf.close();
              continue;
            }
            
            // The index of current log is the bigger index of log with 'live' operations. 
            logidx = idx;
            logFile[logidx%nbLogFile] = logf;
            // current if fixed after the log reading

            while (optype == Operation.COMMIT) {
              String dirName;
              String name;
              optype = logFile[logidx%nbLogFile].read();
   
              while ((optype == Operation.CREATE) ||
                     (optype == Operation.SAVE) ||
                     (optype == Operation.DELETE)) {
                int ptr = (int) logFile[logidx%nbLogFile].getFilePointer() -1;
                logFile[logidx%nbLogFile].logCounter += 1;
                //  Gets all operations of one committed transaction then
                // adds them to specified log.
                dirName = logFile[logidx%nbLogFile].readUTF();
                if (dirName.length() == 0) dirName = null;
                name = logFile[logidx%nbLogFile].readUTF();

                Object key = OperationKey.newKey(dirName, name);

                byte buf[] = null;
                if ((optype == Operation.SAVE) || (optype == Operation.CREATE)) {
                  buf = new byte[logFile[logidx%nbLogFile].readInt()];
                  logFile[logidx%nbLogFile].readFully(buf);

//                  logFile[logidx%nbLogFile].skipBytes(logFile[logidx%nbLogFile].readInt());
                }

                if (Debug.debug && logmon.isLoggable(BasicLevel.DEBUG))
                  logmon.log(BasicLevel.DEBUG,
                             "NGTransaction.LogManager, OPERATION=" + optype + ", " + name + " buf=" + Arrays.toString(buf));
                
                Operation old = log.get(key);
                if (old != null) {
                  logFile[old.logidx%nbLogFile].logCounter -= 1;

                  // There is 6 different cases:
                  //
                  //   new |
                  // old   |  C  |  S  |  D
                  // ------+-----+-----+-----+
                  //   C   |  C  |  C  | NOP
                  // ------+-----+-----+-----+
                  //   S   |  S  |  S  |  D
                  // ------+-----+-----+-----+
                  //   D   |  S  |  S  |  D
                  //

                  if ((old.type == Operation.CREATE) || (old.type == Operation.SAVE)) {
                    if ((optype == Operation.CREATE) || (optype == Operation.SAVE)) {
                      // The resulting operation is still the same, just change the logidx
                      // and logptr informations.
                      old.logidx = logidx;
                      old.logptr = ptr;
                    } else {
                      // The operation is a delete
                      if (old.type == Operation.CREATE) {
                        // There is no need to memorize the deletion, the object will be never
                        // created on disk.
                        old.type = Operation.NOOP;
                        log.remove(key);
                        old.free();
                        logFile[logidx%nbLogFile].logCounter -= 1;
                      } else {
                        // The operation is a save, overload it.
                        old.type = Operation.DELETE;
                        old.logidx = logidx;
                        old.logptr = ptr;
                      }
                    }
                  } else if (old.type == Operation.DELETE) { 
                    if ((optype == Operation.CREATE) || (optype == Operation.SAVE)) {
                      // The resulting operation is a save 
                      old.type = Operation.SAVE;
                    }
                    old.logidx = logidx;
                    old.logptr = ptr;
                  } 
                } else {
                  Operation op = Operation.alloc(optype, dirName, name);
                  op.logidx = logidx;
                  op.logptr = ptr;
                  log.put(key, op);
                }
                
                optype = logFile[logidx%nbLogFile].read();
              }
              if (Debug.debug && logmon.isLoggable(BasicLevel.DEBUG))
                logmon.log(BasicLevel.DEBUG, "NGTransaction.LogManager, COMMIT#" + idx);
            }

            current = (int) logFile[logidx%nbLogFile].getFilePointer();
            if (Debug.debug && logmon.isLoggable(BasicLevel.DEBUG))
              logmon.log(BasicLevel.DEBUG, "NGTransaction.LogManager, END#" + logidx);

            if (optype != Operation.END)
              throw new IOException("Corrupted transaction log#" + logidx);
          } catch (IOException exc) {
            throw exc;
          }
        }
        
        logmon.log(BasicLevel.DEBUG,
                   "NGTransaction.LogManager, log=" + Arrays.toString(log.values().toArray()));
      }
      
      if (logFile[logidx%nbLogFile] == null) {
        // Creates a log file
        logFile[logidx%nbLogFile] = new LogFile(dir, logidx);
        logFile[logidx%nbLogFile].setLength(MaxLogFileSize);

        // Initializes the log file
        logFile[logidx%nbLogFile].seek(0);
        logFile[logidx%nbLogFile].write(Operation.END);
        current = 1;
      }
      
      logmon.log(BasicLevel.INFO,
                 "NGTransaction.LogManager, ends: " + (System.currentTimeMillis() - start));
    }

    /**
     * Reports all buffered operations in logs.
     */
    void commit(Hashtable<Object, Operation> ctxlog) throws IOException {
      if (logmon.isLoggable(BasicLevel.DEBUG))
        logmon.log(BasicLevel.DEBUG, "NTransaction.LogFile.commit()");

      commitCount += 1;

      Set<Entry<Object, Operation>> entries = ctxlog.entrySet();
      Iterator<Entry<Object, Operation>> iterator = entries.iterator();
      try {
        while (true) {
          Entry<Object, Operation> entry = iterator.next();
          Object key = entry.getKey();
          Operation op = entry.getValue();

          if (op.type == Operation.NOOP) continue;

//        if (logmon.isLoggable(BasicLevel.DEBUG)) {
//           if (op.type == Operation.SAVE) {
//             logmon.log(BasicLevel.DEBUG, "NTransaction save " + op.name);
//           } else if (op.type == Operation.CREATE) {
//             logmon.log(BasicLevel.DEBUG, "NTransaction create " + op.name);
//           } else if (op.type == Operation.DELETE) {
//             logmon.log(BasicLevel.DEBUG, "NTransaction delete " + op.name);
//           } else {
//             logmon.log(BasicLevel.DEBUG, "NTransaction unknown(" + op.type + ") " + op.name);
//           }
//        }

          op.logidx = logidx;
          op.logptr = current + count;

          // Save the operation to the log on disk
          write(op.type);
          if (op.dirName != null) {
            writeUTF(op.dirName);
          } else {
            write(emptyUTFString);
          }
          writeUTF(op.name);
          if ((op.type == Operation.SAVE) || (op.type == Operation.CREATE)) {
            writeInt(op.value.length);
            write(op.value);
          }
          // TODO: Use SoftReference ?
          op.value = null;

          // Reports all committed operation in current log
          Operation old = log.put(key, op);
          logFile[logidx%nbLogFile].logCounter += 1;

          if (old != null) {
            logFile[old.logidx%nbLogFile].logCounter -= 1;

            // There is 6 different cases:
            //
            //   new |
            // old   |  C  |  S  |  D
            // ------+-----+-----+-----+
            //   C   |  C  |  C  | NOP
            // ------+-----+-----+-----+
            //   S   |  S  |  S  |  D
            // ------+-----+-----+-----+
            //   D   |  S  |  S  |  D
            //

            if (old.type == Operation.CREATE) {
              if (op.type == Operation.SAVE) {
                // The object has never been created on disk, the resulting operation
                // is still a creation.
                op.type = Operation.CREATE;
              } else if (op.type == Operation.DELETE) {
                // There is no more need to memorize the deletion the object will be
                // never created on disk.
                op.type = Operation.NOOP;
                log.remove(key);
                op.free();
                logFile[logidx%nbLogFile].logCounter -= 1;
              }
            }
            old.free();
          }
        }
      } catch (NoSuchElementException exc) {}
      write(Operation.END);

      logFile[logidx%nbLogFile].seek(current);
      logFile[logidx%nbLogFile].write(buf, 0, count);

//      if (logmon.isLoggable(BasicLevel.DEBUG)) {
//        logmon.log(DEBUG, "NTransaction commit(): " + current + ", " + count);
      
      // AF: May be we can avoid this second synchronous write, using a
      // marker: determination d'un marqueur lie au log courant (date en
      // millis par exemple), ecriture du marqueur au debut du log, puis
      // ecriture du marqueur apres chaque Operation.COMMIT.
      logFile[logidx%nbLogFile].seek(current -1);
      logFile[logidx%nbLogFile].write(Operation.COMMIT);

      current += (count);
      reset();
      ctxlog.clear();
      
      for (int i=0; i<nbLogFile; i++) {
        if ((logFile[i] != null) && (logFile[i].logCounter == 0)) {
          // The related log file is no longer useful, cleans it in order to speed up the
          // restart after a crash.
          if (logmon.isLoggable(BasicLevel.DEBUG))
            logmon.log(BasicLevel.DEBUG,
                       "NTransaction log#" + logFile[i].logidx + " is no longer needed, cleans it.");
          garbage(logFile[i]);
        }
      }

      if (current > MaxLogFileSize) {
        if (logmon.isLoggable(BasicLevel.DEBUG)) {
          for (int i=0; i<nbLogFile; i++)
            if (logFile[i] != null)
              logmon.log(BasicLevel.DEBUG, "logCounter[" + logFile[i].logidx + "]=" + logFile[i].logCounter);
          logmon.log(BasicLevel.DEBUG, "log -> " + log.size());
        }
        
        logidx += 1;
        if (logFile[logidx%nbLogFile] != null) {
          // The log file is an older one, garbage it before using it anew.
          garbage(logFile[logidx%nbLogFile]);
        }

        // Creates and initializes a new log file
        logFile[logidx%nbLogFile] = new LogFile(dir, logidx);
        logFile[logidx%nbLogFile].setLength(MaxLogFileSize);

        // Cleans log file (needed only for new log file, already done in garbage).
        logFile[logidx%nbLogFile].seek(0);
        logFile[logidx%nbLogFile].write(Operation.END);
        current = 1;
      }
    }

    public byte[] getFromLog(String dirName, String name) throws IOException {
      // First searches in the logs a new value for the object.
      Operation op = (Operation) log.get(OperationKey.newKey(dirName, name));
      if (op != null) {
        if ((op.type == Operation.SAVE) || (op.type == Operation.CREATE)) {
          // reads the value from the log file
          return getFromLog(op);
        } else if (op.type == Operation.DELETE) {
          // The object was deleted.
          throw new FileNotFoundException();
        }
      }
      
      return null;
    }

    public byte[] getFromLog(Operation op) throws IOException {
      loadFromLog += 1;
      
      if (logmon.isLoggable(BasicLevel.DEBUG))
        logmon.log(BasicLevel.DEBUG,
                   "getFromLog#" + op.logidx + ' ' + op.dirName + '/' + op.name + ": " + op.logptr);
      
      logFile[op.logidx%nbLogFile].seek(op.logptr);
      int optype = logFile[op.logidx%nbLogFile].read();    
//      if ((optype != Operation.CREATE) && (optype != Operation.SAVE) &&
//          (logmon.isLoggable(BasicLevel.DEBUG))) {
//        logmon.log(BasicLevel.DEBUG, "getFromLog#" + op.logidx + ": " + optype + ", " + op.type);
//      }

      String dirName = logFile[op.logidx%nbLogFile].readUTF();
      if (dirName.length() == 0) dirName = null;
      String name = logFile[op.logidx%nbLogFile].readUTF();

//      if (((dirName != null) && (! dirName.equals(op.dirName))) || (! name.equals(op.name)) &&
//          (logmon.isLoggable(BasicLevel.DEBUG))) {
//        logmon.log(BasicLevel.DEBUG,
//                   "getFromLog#" + op.logidx + ": " + dirName + '/' + name + ", " + op.dirName + '/' + op.name);
//      }

      byte buf[] = new byte[logFile[op.logidx%nbLogFile].readInt()];
      logFile[op.logidx%nbLogFile].readFully(buf);

//      if (Debug.debug && logmon.isLoggable(BasicLevel.DEBUG))
//        logmon.log(BasicLevel.DEBUG,
//                   "getFromLog#" + op.logidx + ", buf=" + Arrays.toString(buf));

      return buf;
    }
    
    public byte[] load(String dirName, String name) throws IOException {
      byte[] buf = getFromLog(dirName, name);
      if (buf == null) {
        // Gets it from disk.
        buf = repository.load(dirName, name);
      }
      return buf;
    }
    
    public String[] getList(String prefix) {
      if (logmon.isLoggable(BasicLevel.DEBUG))
        logmon.log(BasicLevel.DEBUG, "getList(" + prefix + ")");
      
      String[] list1 = null;
      try {
        list1 = repository.list(prefix);
      } catch (IOException exc) {
        // AF: TODO
      }
      if (list1 == null) list1 = new String[0];
      Object[] list2 = log.keySet().toArray();
      int nb = list1.length;
      for (int i=0; i<list2.length; i++) {
        if ((list2[i] instanceof String) &&
            (((String) list2[i]).startsWith(prefix))) {
          int j=0;
          for (; j<list1.length; j++) {
            if (list2[i].equals(list1[j])) break;
          }
          if (j<list1.length) {
            // The file is already in the directory list, it must be count
            // at most once.
            if (((Operation) log.get(list2[i])).type == Operation.DELETE) {
              // The file is deleted in transaction log.
              list1[j] = null;
              nb -= 1;
            }
            list2[i] = null;
          } else if ((log.get(list2[i]).type == Operation.SAVE) ||
              (log.get(list2[i]).type == Operation.CREATE)) {
            // The file is added in transaction log
            nb += 1;
          } else {
            list2[i] = null;
          }
        } else {
          list2[i] = null;
        }
      }
      String[] list = new String[nb];
      for (int i=list1.length-1; i>=0; i--) {
        if (list1[i] != null) list[--nb] = list1[i];
      }
      for (int i=list2.length-1; i>=0; i--) {
        if (list2[i] != null) list[--nb] = (String) list2[i];
      }

      if (logmon.isLoggable(BasicLevel.DEBUG))
        logmon.log(BasicLevel.DEBUG, "getList() -> " + Arrays.toString(list));

      return list;
    }

    /**
     * Reports all logged operations on disk.
     */
    private final void garbage() throws IOException {
      if (logmon.isLoggable(BasicLevel.DEBUG)) {
        logmon.log(BasicLevel.DEBUG, "log -> " + log.size());
        for (int i=0; i<logFile.length; i++) {
          if (logFile[i] != null)
            logmon.log(BasicLevel.FATAL, "logCounter[" + logFile[i].logidx + "]=" + logFile[i].logCounter);
        }

        for (Enumeration<Operation> e = log.elements(); e.hasMoreElements();) {
          Operation op = e.nextElement();
          logmon.log(BasicLevel.DEBUG, op);
        }
      }

      for (int i=0; i<nbLogFile; i++)
        garbage(logFile[i]);
    }
    
    /**
     * Reports all 'live' operations of a particular log file in the repository, the
     * log file is then cleaned and closed.
     * 
     * @param logf The log file to garbage.
     * @throws IOException
     */
    private final void garbage(LogFile logf) throws IOException {
      if (logf == null) return;

      garbageCount += 1;
      long start = System.currentTimeMillis();
      
      if (logf.logCounter > 0) {
        Iterator<Operation> iterator = log.values().iterator();
        try {
          while (true) {
            Operation op = iterator.next();

            if (op.logidx != logf.logidx) continue;

            if ((op.type == Operation.SAVE) || (op.type == Operation.CREATE)) {
              if (logmon.isLoggable(BasicLevel.DEBUG))
                logmon.log(BasicLevel.DEBUG,
                           "NTransaction, LogFile.Save (" + op.dirName + '/' + op.name + ')');

              byte buf[] = getFromLog(op);

              repository.save(op.dirName, op.name, buf);
            } else if (op.type == Operation.DELETE) {
              if (logmon.isLoggable(BasicLevel.DEBUG))
                logmon.log(BasicLevel.DEBUG,
                           "NTransaction, LogFile.Delete (" + op.dirName + '/' + op.name + ')');

              repository.delete(op.dirName, op.name);
            }

            iterator.remove();
            op.free();
          }
        } catch (NoSuchElementException exc) {}

        repository.commit();
      }

      // Cleans log file
      logf.seek(0);
      logf.write(Operation.END);
      
      if (logf.logidx == logidx) {
        // If the log file is the current one don't close it ! just reset
        // the file pointer so the log can be used a new.
        current = 1;
      } else {
        // Closes the log file and renames it for future use.
        logf.close();

        // Rename the log for a future use
        logf.renameTo(logf.logidx + nbLogFile);
        // Cleans the log file array
        logFile[logf.logidx%nbLogFile] = null;
      }
      
      long end = System.currentTimeMillis();
      lastGarbageTime = end;
      garbageTime += end - start;

      if (logmon.isLoggable(BasicLevel.DEBUG))
        logmon.log(BasicLevel.DEBUG,
                   "NTransaction.LogFile.garbage() - end: " + (end - start));
    }

    void stop() {
      try {
        garbage();
      } catch (IOException exc) {
        // TODO Auto-generated catch block
        exc.printStackTrace();
      }
      
      if (logmon.isLoggable(BasicLevel.DEBUG)) {
        for (int i=0; i<logFile.length; i++)
          if (logFile[i] != null)
            logmon.log(BasicLevel.DEBUG, "logCounter[" + i + "]=" + logFile[i].logCounter);
        logmon.log(BasicLevel.DEBUG, "log -> " + log.size());

        for (Enumeration<Operation> e = log.elements(); e.hasMoreElements();) {
          logmon.log(BasicLevel.DEBUG, e.nextElement());
        }
      }
    }
    
    static private final byte[] emptyUTFString = {0, 0};

    void writeUTF(String str)  {
      int strlen = str.length() ;

      int newcount = count + strlen +2;
      if (newcount > buf.length) {
        byte newbuf[] = new byte[Math.max(buf.length << 1, newcount)];
        System.arraycopy(buf, 0, newbuf, 0, count);
        buf = newbuf;
      }

      buf[count++] = (byte) ((strlen >>> 8) & 0xFF);
      buf[count++] = (byte) ((strlen >>> 0) & 0xFF);

      str.getBytes(0, strlen, buf, count);
      count = newcount;
    }

    void writeInt(int v) throws IOException {
      int newcount = count +4;
      if (newcount > buf.length) {
        byte newbuf[] = new byte[buf.length << 1];
        System.arraycopy(buf, 0, newbuf, 0, count);
        buf = newbuf;
      }

      buf[count++] = (byte) ((v >>> 24) & 0xFF);
      buf[count++] = (byte) ((v >>> 16) & 0xFF);
      buf[count++] = (byte) ((v >>>  8) & 0xFF);
      buf[count++] = (byte) ((v >>>  0) & 0xFF);
    }
  }

  public static class LogFile extends RandomAccessFile {
    /** Unique index of this log file */
    int logidx;
    /** Number of valid operation in this log file */
    int logCounter = 0;

    File dir;
    
    /**
     *  Creates a random access file stream to read from and to write to the file specified
     * by the File argument.
     *  The file is open in "rwd" mode and require that every update to the file's content be
     * written synchronously to the underlying storage device. 
     *  
     * @param file the specified file.
     */
    public LogFile(File dir, int logidx) throws FileNotFoundException {
      super(new File(dir, "log#" + logidx), "rwd");
      this.logidx = logidx;
      this.dir = dir;
    }

    public void renameTo(int newidx) {
      new File(dir, "log#" + logidx).renameTo(new File(dir, "log#" + newidx));
    }
  }
  
  /**
   * Returns a string representation for this object.
   *
   * @return	A string representation of this object. 
   */
  public String toString() {
    StringBuffer strbuf = new StringBuffer();

    strbuf.append('(').append(super.toString());
    strbuf.append(",LogMemorySize=").append(getLogMemorySize());
    strbuf.append(",LogFileSize=").append(getLogFileSize());
    strbuf.append(",CommitCount=").append(getCommitCount());
    strbuf.append(",GarbageCount=").append(getGarbageCount());
    strbuf.append(",GarbageRatio=").append(getGarbageRatio());
    strbuf.append(",NbLoadedFromLog=").append(getNbLoadedFromLog());
    strbuf.append(",NbSavedObjects=").append(getNbSavedObjects());
    strbuf.append(",NbDeletedObjects=").append(getNbDeletedObjects());
    strbuf.append(",NbBadDeletedObjects=").append(getNbBadDeletedObjects());
    strbuf.append(",NbLoadedObjects=").append(getNbLoadedObjects());
    strbuf.append(')');
    
    return strbuf.toString();
  }
}

