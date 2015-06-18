package com.splicemachine.derby.impl.sql.compile;

import com.splicemachine.derby.test.framework.SpliceSchemaWatcher;
import com.splicemachine.derby.test.framework.SpliceTableWatcher;
import com.splicemachine.derby.test.framework.SpliceWatcher;
import junit.framework.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by yifuma on 6/18/15.
 */
public class ColumnReferenceIT {
    private static final String SCHEMA = ColumnReferenceIT.class.getSimpleName();

    @ClassRule
    public static SpliceSchemaWatcher spliceSchemaWatcher = new SpliceSchemaWatcher(SCHEMA);

    @Rule
    public SpliceWatcher methodWatcher = new SpliceWatcher(SCHEMA);

    @Test
    public void testColumnReference() throws Exception{
        try{
            methodWatcher.executeUpdate("create table A (TaskId INT NOT NULL, empId Varchar(3) NOT NULL, StartedAt INT NOT NULL, FinishedAt INT NOT NULL, primary key (TaskId))");
        } catch (SQLException e){
            System.out.println("Table A already exist!");
        }

        try{
            methodWatcher.executeUpdate("create table B (TaskId INT NOT NULL references A(TaskId), empId Varchar(3) NOT NULL, StartedAt INT NOT NULL, FinishedAt INT NOT NULL, primary key (TaskId))");
        } catch (SQLException e){
            System.out.println("Table B already exist!");
        }


        String query = "select t.tablename from --Splice-properties joinOrder=fixed\n" +
                "sys.sysschemas s \n" +
                "join sys.systables ti --Splice-properties joinStrategy=NESTEDLOOP\n" +
                "on s.schemaid = ti.schemaid\n" +
                "join sys.SYSCONSTRAINTS ci --Splice-properties joinStrategy=NESTEDLOOP\n" +
                "on ti.tableid = ci.tableid\n" +
                "join sys.sysforeignkeys fk --Splice-properties joinStrategy=SORTMERGE\n" +
                "on FK.KEYCONSTRAINTID = CI.CONSTRAINTID\n" +
                "join sys.SYSCONSTRAINTS c --Splice-properties joinStrategy=NESTEDLOOP\n" +
                "on c.constraintid = fk.constraintid\n" +
                "join sys.systables t --Splice-properties joinStrategy=NESTEDLOOP\n" +
                "on c.tableid = t.tableid\n" +
                "where s.SCHEMANAME='COLUMNREFERENCEIT'\n" +
                "and ti.tablename='A'";

        ResultSet rs = methodWatcher.executeQuery(query);

        Assert.assertEquals("The returned result set is empty!",true, rs.next());

        Assert.assertEquals("The returned result set has the wrong table!", "B", rs.getString(1));

        Assert.assertEquals("The returned result set has more than 1 entry!", false, rs.next());

    }

}
