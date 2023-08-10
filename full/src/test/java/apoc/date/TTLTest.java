/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package apoc.date;

import apoc.periodic.Periodic;
import apoc.util.TestUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.helpers.collection.Iterators;
import org.neo4j.test.rule.DbmsRule;
import org.neo4j.test.rule.ImpermanentDbmsRule;

import java.util.Collections;

import static apoc.ApocConfig.APOC_TTL_ENABLED;
import static apoc.ApocConfig.APOC_TTL_SCHEDULE;
import static org.junit.Assert.assertEquals;
import static org.neo4j.configuration.GraphDatabaseSettings.procedure_unrestricted;

/**
 * @author mh
 * @since 21.05.16
 */
public class TTLTest {

    public static DbmsRule db = new ImpermanentDbmsRule()
            .withSetting(procedure_unrestricted, Collections.singletonList("apoc.*"));

    public static ProvideSystemProperty systemPropertyRule
            = new ProvideSystemProperty(APOC_TTL_ENABLED, "true")
            .and(APOC_TTL_SCHEDULE, "5");

    @ClassRule
    public static TestRule chain = RuleChain.outerRule(systemPropertyRule).around(db);

    @BeforeClass
    public static void setUp() throws Exception {
        TestUtil.registerProcedure(db, DateExpiry.class, Periodic.class);
        db.executeTransactionally("CREATE (n:Foo:TTL) SET n.ttl = timestamp() + 100");
        db.executeTransactionally("CREATE (n:Bar) WITH n CALL apoc.date.expireIn(n,500,'ms') RETURN count(*)");
        testNodes(1,1);
    }

    @AfterClass
    public static void teardown() {
        db.shutdown();
    }

    @Test
    public void testExpire() throws Exception {
        Thread.sleep(10*1000);
        testNodes(0,0);
    }

    private static void testNodes(int foo, int bar) {
        try (Transaction tx=db.beginTx()) {
            assertEquals(foo, Iterators.count(tx.findNodes(Label.label("Foo"))));
            assertEquals(bar, Iterators.count(tx.findNodes(Label.label("Bar"))));
            assertEquals(foo + bar, Iterators.count(tx.findNodes(Label.label("TTL"))));
            tx.commit();
        }
    }
}
