/*
 * Copyright (c) 2012 - 2017 Splice Machine, Inc.
 *
 * This file is part of Splice Machine.
 * Splice Machine is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3, or (at your option) any later version.
 * Splice Machine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with Splice Machine.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.splicemachine.derby.impl.sql.execute.operations;

import com.splicemachine.db.shared.common.reference.SQLState;
import com.splicemachine.derby.test.framework.SpliceSchemaWatcher;
import com.splicemachine.derby.test.framework.SpliceWatcher;
import com.splicemachine.derby.test.framework.TestConnection;
import com.splicemachine.test.SerialTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.spark_project.guava.collect.Lists;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Properties;

@Category(value = {SerialTest.class})
@RunWith(Parameterized.class)
public class CustomerSysfunsIT {

    private static final String SCHEMA = "CUSTOMERFUNS";

    @ClassRule
    public static SpliceSchemaWatcher spliceSchemaWatcher = new SpliceSchemaWatcher(SCHEMA);

    @Rule
    public SpliceWatcher methodWatcher = new SpliceWatcher(SCHEMA);

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> params = Lists.newArrayListWithCapacity(2);
        params.add(new Object[]{"jdbc:splice://localhost:1527/splicedb;user=splice;password=admin"});
        params.add(new Object[]{"jdbc:splice://localhost:1527/splicedb;user=splice;password=admin;useSpark=true"});
        return params;
    }

    private String connectionString;

    public CustomerSysfunsIT(String connecitonString) {
        this.connectionString = connecitonString;
    }

    @Before
    public void createTables() throws Exception {
        Connection conn = new TestConnection(DriverManager.getConnection(connectionString, new Properties()));
        conn.setSchema(SCHEMA.toUpperCase());
        methodWatcher.setConnection(conn);
    }

    @Test
    public void testCustomerTrunc() throws Exception {
        testCustomerTrunc(11.111, 0, 11);
        testCustomerTrunc(11.111, 1, 11.1);
        testCustomerTrunc(11.111, 2, 11.11);
        testCustomerTrunc(11.111,-1, 10);

        try {
            testCustomerTrunc(123., null, Double.NaN);
        } catch (SQLException e) {
            Assert.assertEquals(SQLState.LANG_NULL_TO_PRIMITIVE_PARAMETER, e.getSQLState());
        }
        try {
            testCustomerTrunc(null, 123, Double.NaN);
        } catch (SQLException e) {
            Assert.assertEquals(SQLState.LANG_NULL_TO_PRIMITIVE_PARAMETER, e.getSQLState());
        }
        try {
            testCustomerTrunc(null, null, Double.NaN);
        } catch (SQLException e) {
            Assert.assertEquals(SQLState.LANG_NULL_TO_PRIMITIVE_PARAMETER, e.getSQLState());
        }
    }

    private void testCustomerTrunc(Double x, Integer n, double expected) throws Exception {
        testCustomerTruncSimple(x, n, expected);
        testCustomerTruncPrepared(x, n, expected);
    }

    private void testCustomerTruncSimple(Double x, Integer n, double expected) throws Exception {
        try (ResultSet rs = methodWatcher.executeQuery("values customer_trunc(" + x + ", " + n + ")")) {
            Assert.assertTrue(rs.next());
            Assert.assertEquals(expected, rs.getDouble(1), Double.MIN_VALUE);
        }
    }

    private void testCustomerTruncPrepared(Double x, Integer n, double expected) throws Exception {
        try (PreparedStatement ps = methodWatcher.prepareStatement("values customer_trunc(?, ?)")) {
            ps.setDouble(1, x);
            ps.setInt(2, n);
            try (ResultSet rs = ps.executeQuery()) {
                Assert.assertTrue(rs.next());
                Assert.assertEquals(expected, rs.getDouble(1), Double.MIN_VALUE);
            }
        }
    }
}
