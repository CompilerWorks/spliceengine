package com.splicemachine.db.impl.sql;

import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.reference.SQLState;
import com.splicemachine.db.iapi.services.daemon.IndexStatisticsDaemon;
import com.splicemachine.db.iapi.services.loader.GeneratedClass;
import com.splicemachine.db.iapi.services.monitor.Monitor;
import com.splicemachine.db.iapi.services.sanity.SanityManager;
import com.splicemachine.db.iapi.services.stream.HeaderPrintWriter;
import com.splicemachine.db.iapi.sql.PreparedStatement;
import com.splicemachine.db.iapi.sql.Statement;
import com.splicemachine.db.iapi.sql.compile.*;
import com.splicemachine.db.iapi.sql.conn.LanguageConnectionContext;
import com.splicemachine.db.iapi.sql.conn.StatementContext;
import com.splicemachine.db.iapi.sql.dictionary.DataDictionary;
import com.splicemachine.db.iapi.sql.dictionary.SchemaDescriptor;
import com.splicemachine.db.iapi.sql.dictionary.TableDescriptor;
import com.splicemachine.db.iapi.sql.execute.ExecutionContext;
import com.splicemachine.db.iapi.util.ByteArray;
import com.splicemachine.db.iapi.util.InterruptStatus;
import com.splicemachine.db.impl.ast.JsonTreeBuilderVisitor;
import com.splicemachine.db.impl.sql.compile.ExplainNode;
import com.splicemachine.db.impl.sql.compile.StatementNode;
import com.splicemachine.db.impl.sql.conn.GenericLanguageConnectionContext;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;

@SuppressWarnings("SynchronizeOnNonFinalField")
public class GenericStatement implements Statement{

    private static final Logger JSON_TREE_LOG = Logger.getLogger(JsonTreeBuilderVisitor.class);

    // these fields define the identity of the statement
    private final SchemaDescriptor compilationSchema;
    private final String statementText;
    private final boolean isForReadOnly;
    private int prepareIsolationLevel;
    private GenericStorablePreparedStatement preparedStmt;

    /**
     * Constructor for a Statement given the text of the statement in a String
     *
     * @param compilationSchema schema
     * @param statementText     The text of the statement
     * @param isForReadOnly     if the statement is opened with level CONCUR_READ_ONLY
     */

    public GenericStatement(SchemaDescriptor compilationSchema,String statementText,boolean isForReadOnly){
        this.compilationSchema=compilationSchema;
        this.statementText=statementText;
        this.isForReadOnly=isForReadOnly;
    }

    public PreparedStatement prepare(LanguageConnectionContext lcc) throws StandardException{
		/*
		** Note: don't reset state since this might be
		** a recompilation of an already prepared statement.
		*/
        return prepare(lcc,false);
    }

    public PreparedStatement prepare(LanguageConnectionContext lcc,boolean forMetaData) throws StandardException{
		/*
		** Note: don't reset state since this might be
		** a recompilation of an already prepared statement.
		*/

        final int depth=lcc.getStatementDepth();
        String prevErrorId=null;
        while(true){
            boolean recompile=false;
            try{
                return prepMinion(lcc,true,null,null,forMetaData);
            }catch(StandardException se){
                // There is a chance that we didn't see the invalidation
                // request from a DDL operation in another thread because
                // the statement wasn't registered as a dependent until
                // after the invalidation had been completed. Assume that's
                // what has happened if we see a conglomerate does not exist
                // error, and force a retry even if the statement hasn't been
                // invalidated.
                if(SQLState.STORE_CONGLOMERATE_DOES_NOT_EXIST.equals(se.getMessageId())){
                    // STORE_CONGLOMERATE_DOES_NOT_EXIST has exactly one
                    // argument: the conglomerate id
                    String conglomId=String.valueOf(se.getArguments()[0]);

                    // Request a recompile of the statement if a conglomerate
                    // disappears while we are compiling it. But if we have
                    // already retried once because the same conglomerate was
                    // missing, there's probably no hope that yet another retry
                    // will help, so let's break out instead of potentially
                    // looping infinitely.
                    if(!conglomId.equals(prevErrorId)){
                        recompile=true;
                    }

                    prevErrorId=conglomId;
                }
                throw se;
            }finally{
                // Check if the statement was invalidated while it was
                // compiled. If so, the newly compiled plan may not be
                // up to date anymore, so we recompile the statement
                // if this happens. Note that this is checked in a finally
                // block, so we also retry if an exception was thrown. The
                // exception was probably thrown because of the changes
                // that invalidated the statement. If not, recompiling
                // will also fail, and the exception will be exposed to
                // the caller.
                //
                // invalidatedWhileCompiling and isValid are protected by
                // synchronization on the prepared statement.
                synchronized(preparedStmt){
                    if(recompile || preparedStmt.invalidatedWhileCompiling){
                        preparedStmt.isValid=false;
                        preparedStmt.invalidatedWhileCompiling=false;
                        recompile=true;
                    }
                }

                if(recompile){
                    // A new statement context is pushed while compiling.
                    // Typically, this context is popped by an error
                    // handler at a higher level. But since we retry the
                    // compilation, the error handler won't be invoked, so
                    // the stack must be reset to its original state first.
                    while(lcc.getStatementDepth()>depth){
                        lcc.popStatementContext(
                                lcc.getStatementContext(),null);
                    }

                    // Don't return yet. The statement was invalidated, so
                    // we must retry the compilation.
                    continue;
                }
            }
        }
    }

    /**
     * Generates an execution plan given a set of named parameters.
     * Does so for a storable prepared statement.
     *
     * @return A PreparedStatement that allows execution of the execution
     * plan.
     * @throws StandardException Thrown if this is an
     *                           execution-only version of the module (the prepare() method
     *                           relies on compilation).
     * @param    paramDefaults        Parameter defaults
     */
    public PreparedStatement prepareStorable(LanguageConnectionContext lcc,
                                             PreparedStatement ps,
                                             Object[] paramDefaults,
                                             SchemaDescriptor spsSchema,
                                             boolean internalSQL) throws StandardException{
        if(ps==null)
            ps=new GenericStorablePreparedStatement(this);
        else
            ((GenericStorablePreparedStatement)ps).statement=this;

        this.preparedStmt=(GenericStorablePreparedStatement)ps;
        return prepMinion(lcc,false,paramDefaults,spsSchema,internalSQL);
    }

    @Override
    public String getSource(){ return statementText; }

    public String getCompilationSchema(){ return compilationSchema.getDescriptorName(); }

    /**
     * Return the {@link PreparedStatement} currently associated with this
     * statement.
     *
     * @return the prepared statement that is associated with this statement
     */
    public PreparedStatement getPreparedStatement(){ return preparedStmt; }

    public boolean equals(Object other){
        if(other instanceof GenericStatement){
            GenericStatement os=(GenericStatement)other;
            return statementText.equals(os.statementText) && isForReadOnly==os.isForReadOnly
                    && compilationSchema.equals(os.compilationSchema) &&
                    (prepareIsolationLevel==os.prepareIsolationLevel);
        }
        return false;
    }

    public int hashCode(){ return statementText.hashCode(); }

    private static long getCurrentTimeMillis(LanguageConnectionContext lcc){
        if(lcc.getStatisticsTiming()){
            return System.currentTimeMillis();
        }else{
            return 0;
        }
    }

    private boolean isExplainStatement(){

        String s=statementText.trim().toUpperCase();
        if(!s.contains("EXPLAIN")){
            // If the statement does not contain keyword explain, this is not an explain statement
            return false;
        }

        // Strip off all comments before keyword explain
        while(s.length()>0 && s.startsWith("--")){
            int index=s.indexOf('\n');
            if(index==-1){
                index=s.length();
            }
            s=s.substring(index+1).trim();
        }

        return s.startsWith("EXPLAIN");
    }

    private PreparedStatement prepMinion(LanguageConnectionContext lcc,
                                         boolean cacheMe,
                                         Object[] paramDefaults,
                                         SchemaDescriptor spsSchema,
                                         boolean internalSQL) throws StandardException{

        /*
         * An array holding timestamps for various points in time. The order is
         *
         * 0:   beginTime
         * 1:   parseTime
         * 2:   bindTime
         * 3:   optimizeTime
         * 4:   generateTime
         */
        long[] timestamps = new long[5];
        Timestamp beginTimestamp=null;
        StatementContext statementContext=null;

        // verify it isn't already prepared...
        // if it is, and is valid, simply return that tree.
        // if it is invalid, we will recompile now.
        if(preparedStmt!=null){
            if(preparedStmt.upToDate())
                return preparedStmt;
        }

        // Clear the optimizer trace from the last statement
        if(lcc.getOptimizerTrace())
            lcc.setOptimizerTraceOutput(getSource()+"\n");

        timestamps[0]=getCurrentTimeMillis(lcc);
		/* beginTimestamp only meaningful if beginTime is meaningful.
		 * beginTime is meaningful if STATISTICS TIMING is ON.
		 */
        if(timestamps[0]!=0){
            beginTimestamp=new Timestamp(timestamps[0]);
        }

        /** set the prepare Isolaton from the LanguageConnectionContext now as
         * we need to consider it in caching decisions
         */
        prepareIsolationLevel=lcc.getPrepareIsolationLevel();

		/* a note on statement caching:
		 *
		 * A GenericPreparedStatement (GPS) is only added it to the cache if the
		 * parameter cacheMe is set to TRUE when the GPS is created.
		 *
		 * Earlier only CacheStatement (CS) looked in the statement cache for a
		 * prepared statement when prepare was called. Now the functionality
		 * of CS has been folded into GenericStatement (GS). So we search the
		 * cache for an existing PreparedStatement only when cacheMe is TRUE.
		 * i.e if the user calls prepare with cacheMe set to TRUE:
		 * then we
		 *         a) look for the prepared statement in the cache.
		 *         b) add the prepared statement to the cache.
		 *
		 * In cases where the statement cache has been disabled (by setting the
		 * relevant Derby property) then the value of cacheMe is irrelevant.
		 */
        boolean foundInCache=false;
//        boolean isExplain=isExplainStatement();
        if(preparedStmt==null){
            if(cacheMe)
                preparedStmt=(GenericStorablePreparedStatement)((GenericLanguageConnectionContext)lcc).lookupStatement(this);

            if(preparedStmt==null){
                preparedStmt=new GenericStorablePreparedStatement(this);
            }else{
                foundInCache=true;
            }
        }

        // if anyone else also has this prepared statement,
        // we don't want them trying to compile with it while
        // we are.  So, we synchronize on it and re-check
        // its validity first.
        // this is a no-op if and until there is a central
        // cache of prepared statement objects...
        synchronized(preparedStmt){
            for(;;){
                if(foundInCache){
                    if(preparedStmt.referencesSessionSchema()){
                        // cannot use this state since it is private to a connection.
                        // switch to a new statement.
                        foundInCache=false;
                        preparedStmt=new GenericStorablePreparedStatement(this);
                        break;
                    }
                }

                // did it get updated while we waited for the lock on it?
                if(preparedStmt.upToDate()){
                    return preparedStmt;
                }

                if(!preparedStmt.compilingStatement){
                    break;
                }

                try{
                    preparedStmt.wait();
                }catch(InterruptedException ie){
                    InterruptStatus.setInterrupted();
                }
            }

            preparedStmt.compilingStatement=true;
            preparedStmt.setActivationClass(null);
        }

        try{
			/*
			** For stored prepared statements, we want all
			** errors, etc in the context of the underlying
			** EXECUTE STATEMENT statement, so don't push/pop
			** another statement context unless we don't have
			** one.  We won't have one if it is an internal
			** SPS (e.g. jdbcmetadata).
			*/
            if(!preparedStmt.isStorable() || lcc.getStatementDepth()==0){
                // since this is for compilation only, set atomic
                // param to true and timeout param to 0
                statementContext=lcc.pushStatementContext(true,isForReadOnly,getSource(), null,false,0L);
            }

			/*
			** RESOLVE: we may ultimately wish to pass in
			** whether we are a jdbc metadata query or not to
			** get the CompilerContext to make the createDependency()
			** call a noop.
			*/
            CompilerContext cc=lcc.pushCompilerContext(compilationSchema);

            if(prepareIsolationLevel!=ExecutionContext.UNSPECIFIED_ISOLATION_LEVEL){
                cc.setScanIsolationLevel(prepareIsolationLevel);
            }

            // Look for stored statements that are in a system schema
            // and with a match compilation schema. If so, allow them
            // to compile using internal SQL constructs.
            if(internalSQL ||
                    (spsSchema!=null) && (spsSchema.isSystemSchema()) && (spsSchema.equals(compilationSchema))){
                cc.setReliability(CompilerContext.INTERNAL_SQL_LEGAL);
            }

            fourPhasePrepare(lcc,paramDefaults,timestamps,beginTimestamp,foundInCache,cc);
        }catch(StandardException se){
            if(foundInCache)
                ((GenericLanguageConnectionContext)lcc).removeStatement(this);

            throw se;
        }finally{
            synchronized(preparedStmt){
                preparedStmt.compilingStatement=false;
                preparedStmt.notifyAll();
            }
        }

        lcc.commitNestedTransaction();

        if(statementContext!=null)
            lcc.popStatementContext(statementContext,null);

        return preparedStmt;
    }

    /*
     * Performs the 4-phase preparation of the statement. The four
     * phases are:
     *
     * 1. parse: Convert the Sql text into an abstract syntax tree (AST)
     * 2. bind: Bind tables and variables. Also performs some error detection (missing tables/columns,etc)
     * 3. optimize: Perform cost-based optimization
     * 4. generate: Generate the actual byte code to be executed
     */
    private void fourPhasePrepare(LanguageConnectionContext lcc,
                                  Object[] paramDefaults,
                                  long[] timestamps,
                                  Timestamp beginTimestamp,
                                  boolean foundInCache,
                                  CompilerContext cc) throws StandardException{
        HeaderPrintWriter istream=lcc.getLogStatementText()?Monitor.getStream():null;
        try{
            // Statement logging if lcc.getLogStatementText() is true
            if(istream!=null){
                String statement = "Begin compiling prepared statement: ";
                printStatementLine(lcc,istream,statement);
            }

            StatementNode qt=parse(lcc,paramDefaults,timestamps,cc);

            /*
            ** Tell the data dictionary that we are about to do
            ** a bunch of "get" operations that must be consistent with
            ** each other.
            */

            DataDictionary dataDictionary=lcc.getDataDictionary();

            bindAndOptimize(lcc,timestamps,foundInCache,istream,qt,dataDictionary);

            /* we need to move the commit of nested sub-transaction
             * after we mark PS valid, during compilation, we might need
             * to get some lock to synchronize with another thread's DDL
             * execution, in particular, the compilation of insert/update/
             * delete vs. create index/constraint (see Beetle 3976).  We
             * can't release such lock until after we mark the PS valid.
             * Otherwise we would just erase the DDL's invalidation when
             * we mark it valid.
             */
            Timestamp endTimestamp=generate(lcc,timestamps,cc,qt);

            saveTree(qt, CompilationPhase.AFTER_GENERATE);

            if(lcc.getRunTimeStatisticsMode()){
                preparedStmt.setCompileTimeMillis(
                        timestamps[1]-timestamps[0], //parse time
                        timestamps[2]-timestamps[1], //bind time
                        timestamps[3]-timestamps[2], //optimize time
                        timestamps[4]-timestamps[3], //generate time
                        timestamps[4]-timestamps[0], //total compile time
                        beginTimestamp,
                        endTimestamp);
            }
        }finally{ // for block introduced by pushCompilerContext()
            lcc.popCompilerContext(cc);
        }
    }

    private StatementNode parse(LanguageConnectionContext lcc,
                                Object[] paramDefaults,
                                long[] timestamps,
                                CompilerContext cc) throws StandardException{
        Parser p=cc.getParser();

        cc.setCurrentDependent(preparedStmt);

        //Only top level statements go through here, nested statement
        //will invoke this method from other places
        StatementNode qt=(StatementNode)p.parseStatement(statementText,paramDefaults);

        timestamps[1]=getCurrentTimeMillis(lcc);

        // Call user-written tree-printer if it exists
        walkAST(lcc,qt, CompilationPhase.AFTER_PARSE);
        saveTree(qt, CompilationPhase.AFTER_PARSE);

        dumpParseTree(lcc,qt,true);
        return qt;
    }

    private void bindAndOptimize(LanguageConnectionContext lcc,
                                 long[] timestamps,
                                 boolean foundInCache,
                                 HeaderPrintWriter istream,
                                 StatementNode qt,
                                 DataDictionary dataDictionary) throws StandardException{
        int ddMode=dataDictionary==null?0:dataDictionary.startReading(lcc);
        try{
            // start a nested transaction -- all locks acquired by bind
            // and optimize will be released when we end the nested
            // transaction.
            lcc.beginNestedTransaction(true);
            dumpParseTree(lcc,qt,false);

            qt.bindStatement();
            timestamps[2]=getCurrentTimeMillis(lcc);

            // Call user-written tree-printer if it exists
            walkAST(lcc,qt, CompilationPhase.AFTER_BIND);
            saveTree(qt, CompilationPhase.AFTER_BIND);

            dumpBoundTree(lcc,qt);

            //Derby424 - In order to avoid caching select statements referencing
            // any SESSION schema objects (including statements referencing views
            // in SESSION schema), we need to do the SESSION schema object check
            // here.
            //a specific eg for statement referencing a view in SESSION schema
            //CREATE TABLE t28A (c28 int)
            //INSERT INTO t28A VALUES (280),(281)
            //CREATE VIEW SESSION.t28v1 as select * from t28A
            //SELECT * from SESSION.t28v1 should show contents of view and we
            // should not cache this statement because a user can later define
            // a global temporary table with the same name as the view name.
            //Following demonstrates that
            //DECLARE GLOBAL TEMPORARY TABLE SESSION.t28v1(c21 int, c22 int) not
            //     logged
            //INSERT INTO SESSION.t28v1 VALUES (280,1),(281,2)
            //SELECT * from SESSION.t28v1 should show contents of global temporary
            //table and not the view.  Since this select statement was not cached
            // earlier, it will be compiled again and will go to global temporary
            // table to fetch data. This plan will not be cached either because
            // select statement is using SESSION schema object.
            //
            //Following if statement makes sure that if the statement is
            // referencing SESSION schema objects, then we do not want to cache it.
            // We will remove the entry that was made into the cache for
            //this statement at the beginning of the compile phase.
            //The reason we do this check here rather than later in the compile
            // phase is because for a view, later on, we loose the information that
            // it was referencing SESSION schema because the reference
            //view gets replaced with the actual view definition. Right after
            // binding, we still have the information on the view and that is why
            // we do the check here.
            if(preparedStmt.referencesSessionSchema(qt)){
                if(foundInCache)
                    ((GenericLanguageConnectionContext)lcc).removeStatement(this);
            }
            /*
             * If we have an explain statement, then we should remove it from the
             * cache to prevent future readers from fetching it
             */
            if(foundInCache && qt instanceof ExplainNode){
                ((GenericLanguageConnectionContext)lcc).removeStatement(this);
            }
            qt.optimizeStatement();
            dumpOptimizedTree(lcc,qt,false);
            timestamps[3]=getCurrentTimeMillis(lcc);

            // Call user-written tree-printer if it exists
            walkAST(lcc,qt, CompilationPhase.AFTER_OPTIMIZE);
            saveTree(qt, CompilationPhase.AFTER_OPTIMIZE);

            // Statement logging if lcc.getLogStatementText() is true
            if(istream!=null){
                String endStatement = "End compiling prepared statement: ";
                printStatementLine(lcc,istream,endStatement);
            }
        }catch(StandardException se){
            lcc.commitNestedTransaction();

            // Statement logging if lcc.getLogStatementText() is true
            if(istream!=null){
                String errorStatement = "Error compiling prepared statement: ";
                printStatementLine(lcc,istream,errorStatement);
            }
            throw se;
        }finally{
            /* Tell the data dictionary that we are done reading */
            if(dataDictionary!=null)
                dataDictionary.doneReading(ddMode,lcc);
        }
    }

    private Timestamp generate(LanguageConnectionContext lcc,
                               long[] timestamps,
                               CompilerContext cc,
                               StatementNode qt) throws StandardException{
        Timestamp endTimestamp = null;
        try{        // put in try block, commit sub-transaction if bad
            dumpOptimizedTree(lcc,qt,true);

            ByteArray array=preparedStmt.getByteCodeSaver();
            GeneratedClass ac=qt.generate(array);

            timestamps[4]=getCurrentTimeMillis(lcc);
            /* endTimestamp only meaningful if generateTime is meaningful.
             * generateTime is meaningful if STATISTICS TIMING is ON.
             */
            if(timestamps[4]!=0){
                endTimestamp=new Timestamp(timestamps[4]);
            }

            if(SanityManager.DEBUG){
                if(SanityManager.DEBUG_ON("StopAfterGenerating")){
                    throw StandardException.newException(SQLState.LANG_STOP_AFTER_GENERATING);
                }
            }

            /*
                copy over the compile-time created objects
                to the prepared statement.  This always happens
                at the end of a compile, so there is no need
                to erase the previous entries on a re-compile --
                this erases as it replaces.  Set the activation
                class in case it came from a StorablePreparedStatement
            */
            preparedStmt.setConstantAction(qt.makeConstantAction());
            preparedStmt.setSavedObjects(cc.getSavedObjects());
            preparedStmt.setRequiredPermissionsList(cc.getRequiredPermissionsList());
            preparedStmt.incrementVersionCounter();
            preparedStmt.setActivationClass(ac);
            preparedStmt.setNeedsSavepoint(qt.needsSavepoint());
            preparedStmt.setCursorInfo((CursorInfo)cc.getCursorInfo());
            preparedStmt.setIsAtomic(qt.isAtomic());
            preparedStmt.setExecuteStatementNameAndSchema(qt.executeStatementName(), qt.executeSchemaName());
            preparedStmt.setSPSName(qt.getSPSName());
            preparedStmt.completeCompile(qt);
            preparedStmt.setCompileTimeWarnings(cc.getWarnings());

            scheduleStatsUpdates(lcc,qt);

        }catch(StandardException e){    // hold it, throw it
            lcc.commitNestedTransaction();
            throw e;
        }
        return endTimestamp;
    }

    private void scheduleStatsUpdates(LanguageConnectionContext lcc,StatementNode qt) throws StandardException{
        // Schedule updates of any stale index statistics we may
        // have detected when creating the plan.
        TableDescriptor[] tds=qt.updateIndexStatisticsFor();
        if(tds.length>0){
            IndexStatisticsDaemon isd=lcc.getDataDictionary().
                    getIndexStatsRefresher(true);
            if(isd!=null){
                for(TableDescriptor td : tds){
                    isd.schedule(td);
                }
            }
        }
    }

    private void printStatementLine(LanguageConnectionContext lcc,HeaderPrintWriter istream,String endStatement){
        String xactId=lcc.getTransactionExecute().getActiveStateTxIdString();
        istream.printlnWithHeader(LanguageConnectionContext.xidStr+ xactId+ "), "+
                LanguageConnectionContext.lccStr+ lcc.getInstanceNumber()+ "), "+
                LanguageConnectionContext.dbnameStr+ lcc.getDbname()+ "), "+
                LanguageConnectionContext.drdaStr+ lcc.getDrdaID()+
                "), "+endStatement+ getSource()+ " :End prepared statement");
    }

    private void dumpParseTree(LanguageConnectionContext lcc,StatementNode qt,boolean stopAfter) throws StandardException{
        if(SanityManager.DEBUG){
            if(SanityManager.DEBUG_ON("DumpParseTree")){
                SanityManager.GET_DEBUG_STREAM().print("\n\n============PARSE===========\n\n");
                qt.treePrint();
                lcc.getPrintedObjectsMap().clear();
                SanityManager.GET_DEBUG_STREAM().print("\n\n============END PARSE===========\n\n");
            }

            if(stopAfter && SanityManager.DEBUG_ON("StopAfterParsing")){
                lcc.setLastQueryTree(qt);

                throw StandardException.newException(SQLState.LANG_STOP_AFTER_PARSING);
            }
        }
    }

    private void dumpBoundTree(LanguageConnectionContext lcc,StatementNode qt) throws StandardException{
        if(SanityManager.DEBUG){
            if(SanityManager.DEBUG_ON("DumpBindTree")){
                SanityManager.GET_DEBUG_STREAM().print(
                        "\n\n============BIND===========\n\n");
                qt.treePrint();
                SanityManager.GET_DEBUG_STREAM().print(
                        "\n\n============END BIND===========\n\n");
                lcc.getPrintedObjectsMap().clear();
            }

            if(SanityManager.DEBUG_ON("StopAfterBinding")){
                throw StandardException.newException(SQLState.LANG_STOP_AFTER_BINDING);
            }
        }
    }

    private void dumpOptimizedTree(LanguageConnectionContext lcc,StatementNode qt,boolean stopAfter) throws StandardException{
        if(SanityManager.DEBUG){
            if(SanityManager.DEBUG_ON("DumpOptimizedTree")){
                SanityManager.GET_DEBUG_STREAM().print("\n\n============OPTIMIZED===========\n\n");
                qt.treePrint();
                lcc.getPrintedObjectsMap().clear();
                SanityManager.GET_DEBUG_STREAM().print("\n\n============END OPTIMIZED===========\n\n");
            }

            if(stopAfter && SanityManager.DEBUG_ON("StopAfterOptimizing")){
                throw StandardException.newException(SQLState.LANG_STOP_AFTER_OPTIMIZING);
            }
        }
    }

    /**
     * Walk the AST, using a (user-supplied) Visitor, write tree as JSON file if enabled.
     */
    private void walkAST(LanguageConnectionContext lcc, Visitable queryTree, CompilationPhase phase) throws StandardException {
        ASTVisitor visitor = lcc.getASTVisitor();
        if (visitor != null) {
            visitor.begin(statementText, phase);
            queryTree.accept(visitor);
            visitor.end(phase);
        }
    }

    /**
     * Saves AST tree as JSON in files (in working directory for now) for each phase.  This is of course intended to be
     * used only by splice developers.
     *
     * AFTER_PARSE.json
     * AFTER_BIND.json
     * AFTER_OPTIMIZE.json
     * AFTER_GENERATE.json
     *
     * View using ast-visualization.html
     */
    private void saveTree(Visitable queryTree, CompilationPhase phase) throws StandardException {
        if (JSON_TREE_LOG.isTraceEnabled()) {
            JSON_TREE_LOG.warn("JSON AST logging is enabled");
            try {
                JsonTreeBuilderVisitor jsonVisitor = new JsonTreeBuilderVisitor();
                queryTree.accept(jsonVisitor);
                String destinationFileName = phase + ".json";
                Files.write(Paths.get(destinationFileName), jsonVisitor.toJson().getBytes("UTF-8"));
            } catch (IOException e) {
                /* Don't let the exception propagate.  If we are trying to use this tool on a server where we can't
                   write to the destination, for example, then warn but let the query run. */
                JSON_TREE_LOG.warn("unable to save AST JSON file", e);
            }
        }
    }
}
