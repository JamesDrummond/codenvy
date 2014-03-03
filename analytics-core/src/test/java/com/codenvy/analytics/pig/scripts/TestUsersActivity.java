/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.analytics.pig.scripts;

import com.codenvy.analytics.BaseTest;
import com.codenvy.analytics.Utils;
import com.codenvy.analytics.datamodel.*;
import com.codenvy.analytics.metrics.*;
import com.codenvy.analytics.metrics.users.UsersActivity;
import com.codenvy.analytics.metrics.users.UsersActivityList;
import com.codenvy.analytics.pig.scripts.util.Event;
import com.codenvy.analytics.pig.scripts.util.LogGenerator;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mongodb.util.MyAsserts.assertEquals;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
public class TestUsersActivity extends BaseTest {

    private Map<String, String> params;

    private static final String COLLECTION                  = TestUsersActivity.class.getSimpleName().toLowerCase();
    private static final String FIRST_TARGET_SESSION_ID     = "8AA06F22-3755-4BDD-9242-8A6371BAB53A";
    private static final String TARGET_USER                 = "user1@gmail.com";
    private static final String TARGET_WORKSPACE            = "ws1";
    private static final String SESSION_EVENTS_FILTER_VALUE =
            "~session-started,~session-finished,~session-factory-started,~session-factory-stopped";

    @BeforeClass
    public void prepare() throws Exception {
        params = Utils.newContext();

        List<Event> events = new ArrayList<>();

        // start main session
        events.add(
                Event.Builder.createSessionStartedEvent(TARGET_USER, TARGET_WORKSPACE, "ide", FIRST_TARGET_SESSION_ID)
                             .withDate("2013-11-01").withTime("19:00:00,155").build());

        // event of target user in the target workspace and in time of first session
        events.add(Event.Builder.createRunStartedEvent(TARGET_USER, TARGET_WORKSPACE, "project", "type", "id1")
                                .withDate("2013-11-01").withTime("19:08:00,600").build());
        events.add(Event.Builder.createRunFinishedEvent(TARGET_USER, TARGET_WORKSPACE, "project", "type", "id1")
                                .withDate("2013-11-01").withTime("19:10:00,900").build());

        // event of target user in another workspace and in time of main session
        events.add(Event.Builder.createBuildStartedEvent(TARGET_USER, "ws2", "project", "type", "id2")
                                .withDate("2013-11-01").withTime("19:12:00").build());
        events.add(Event.Builder.createBuildFinishedEvent(TARGET_USER, "ws2", "project", "type", "id2")
                                .withDate("2013-11-01").withTime("19:14:00").build());

        // event of another user in the target workspace and in time of main session
        events.add(Event.Builder.createRunStartedEvent("user2@gmail.com", "ws2", "project", "type", "id1")
                                .withDate("2013-11-01").withTime("19:08:00").build());
        events.add(Event.Builder.createRunFinishedEvent("user2@gmail.com", "ws2", "project", "type", "id1")
                                .withDate("2013-11-01").withTime("19:10:00").build());

        // finish main session
        events.add(
                Event.Builder.createSessionFinishedEvent(TARGET_USER, TARGET_WORKSPACE, "ide", FIRST_TARGET_SESSION_ID)
                             .withDate("2013-11-01").withTime("19:55:00,555").build());

        // second micro-sessions (240 sec, 120 millisec) of target user in the target workspace
        events.add(Event.Builder.createSessionStartedEvent(TARGET_USER, TARGET_WORKSPACE, "ide", "1")
                                .withDate("2013-11-01").withTime("20:00:00,100").build());
        events.add(Event.Builder.createSessionFinishedEvent(TARGET_USER, TARGET_WORKSPACE, "ide", "1")
                                .withDate("2013-11-01").withTime("20:04:00,220").build());

        // event of target user in the target workspace and in time of second session
        events.add(Event.Builder.createDebugStartedEvent(TARGET_USER, TARGET_WORKSPACE, "project", "type", "id1")
                                .withDate("2013-11-01").withTime("20:02:00,600").build());

        // event of target user in the target workspace and after the second session is finished
        events.add(Event.Builder.createProjectBuiltEvent(TARGET_USER, TARGET_WORKSPACE, "project", "type", "id1")
                                .withDate("2013-11-01").withTime("20:04:00,320").build());

        // factory session, won't be taken in account
        events.add(Event.Builder.createSessionFactoryStartedEvent("sessionId1", "tmpWs", "tmpUser", "", "")
                                .withDate("2013-11-01").withTime("19:00:00,155").build());
        events.add(Event.Builder.createSessionFactoryStoppedEvent("sessionId1", "tmpWs", "tmpUser")
                                .withDate("2013-11-01").withTime("19:55:00,555").build());


        File log = LogGenerator.generateLog(events);

        Parameters.FROM_DATE.put(params, "20131101");
        Parameters.TO_DATE.put(params, "20131101");
        Parameters.USER.put(params, Parameters.USER_TYPES.REGISTERED.name());
        Parameters.WS.put(params, Parameters.WS_TYPES.ANY.name());

        Parameters.STORAGE_TABLE.put(params, COLLECTION);
        Parameters.LOG.put(params, log.getAbsolutePath());
        pigServer.execute(ScriptType.USERS_ACTIVITY, params);

        String ProductUsageSessionsTableName =
                ((ReadBasedMetric)MetricFactory.getMetric(MetricType.PRODUCT_USAGE_SESSIONS_LIST))
                        .getStorageCollectionName();
        Parameters.STORAGE_TABLE.put(params, ProductUsageSessionsTableName);

        Parameters.STORAGE_TABLE_USERS_STATISTICS.put(params, "testuserssessions-stat");
        Parameters.STORAGE_TABLE_USERS_PROFILES.put(params, "testuserssessions-profiles");
        Parameters.LOG.put(params, log.getAbsolutePath());
        pigServer.execute(ScriptType.PRODUCT_USAGE_SESSIONS, params);
    }

    @Test
    public void testActivity() throws Exception {
        Map<String, String> context = Utils.newContext();
        Parameters.FROM_DATE.put(context, "20131101");
        Parameters.TO_DATE.put(context, "20131101");

        Metric metric = new TestUsersActivityList();
        ListValueData value = (ListValueData)metric.getValue(context);

        assertEquals(value.size(), 12);

        assertItem(value,
                   0,
                   "session-started",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   "127.0.0.1 2013-11-01 19:00:00,155,000[main] [INFO] [HelloWorld 1010]  - EVENT#session-started# "
                   + "SESSION-ID#" + FIRST_TARGET_SESSION_ID + "# WS#" + TARGET_WORKSPACE + "# USER#" + TARGET_USER +
                   "# WINDOW#ide# ",
                   fullTimeFormat.parse("2013-11-01 19:00:00,155").getTime(),
                   0);

        assertItem(value,
                   1,
                   "run-started",
                   "ws2",
                   "user2@gmail.com",
                   null,
                   fullTimeFormat.parse("2013-11-01 19:08:00,000").getTime(),
                   0);

        assertItem(value,
                   2,
                   "run-started",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 19:08:00,600").getTime(),
                   0);

        assertItem(value,
                   3,
                   "run-finished",
                   "ws2",
                   "user2@gmail.com",
                   null,
                   fullTimeFormat.parse("2013-11-01 19:10:00,000").getTime(),
                   0);

        assertItem(value,
                   4,
                   "run-finished",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 19:10:00,900").getTime(),
                   0);

        assertItem(value,
                   5,
                   "build-started",
                   "ws2",
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 19:12:00,000").getTime(),
                   0);

        assertItem(value,
                   6,
                   "build-finished",
                   "ws2",
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 19:14:00,000").getTime(),
                   0);

        assertItem(value,
                   7,
                   "session-finished",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   "127.0.0.1 2013-11-01 19:55:00,555,000[main] [INFO] [HelloWorld 1010]  - EVENT#session-finished# "
                   + "SESSION-ID#" + FIRST_TARGET_SESSION_ID + "# WS#" + TARGET_WORKSPACE + "# USER#" + TARGET_USER +
                   "# WINDOW#ide# ",
                   fullTimeFormat.parse("2013-11-01 19:55:00,555").getTime(),
                   0);

        assertItem(value,
                   8,
                   "session-started",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 20:00:00,100").getTime(),
                   0);

        assertItem(value,
                   9,
                   "debug-started",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 20:02:00,600").getTime(),
                   0);

        assertItem(value,
                   10,
                   "session-finished",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 20:04:00,220").getTime(),
                   0);

        assertItem(value,
                   11,
                   "project-built",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 20:04:00,320").getTime(),
                   0);

        metric = new TestNumberOfUsersOfActivity();
        Assert.assertEquals(metric.getValue(context), LongValueData.valueOf(12));
    }

    @Test
    public void testOneSessionActivityWithHidedSessionEvents() throws Exception {
        Map<String, String> context = Utils.newContext();
        MetricFilter.SESSION_ID.put(context, FIRST_TARGET_SESSION_ID);
        MetricFilter.EVENT.put(context, SESSION_EVENTS_FILTER_VALUE);

        Metric metric = new TestUsersActivityList();
        ListValueData value = (ListValueData)metric.getValue(context);

        assertEquals(value.size(), 3);

        assertItem(value,
                   0,
                   "run-started",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 19:08:00,600").getTime(),
                   480445);

        assertItem(value,
                   1,
                   "run-finished",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 19:10:00,900").getTime(),
                   600745);

        assertItem(value,
                   2,
                   "debug-started",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 20:02:00,600").getTime(),
                   3720445);

        metric = new TestNumberOfUsersOfActivity();
        Assert.assertEquals(metric.getValue(context), LongValueData.valueOf(3));
    }


    @Test
    public void testOneSessionActivity() throws Exception {
        Map<String, String> context = Utils.newContext();
        MetricFilter.SESSION_ID.put(context, FIRST_TARGET_SESSION_ID);

        Metric metric = new TestUsersActivityList();
        ListValueData value = (ListValueData)metric.getValue(context);

        assertEquals(value.size(), 7);

        assertItem(value,
                   0,
                   "session-started",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 19:00:00,155").getTime(),
                   0);

        assertItem(value,
                   1,
                   "run-started",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 19:08:00,600").getTime(),
                   480445);

        assertItem(value,
                   2,
                   "run-finished",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 19:10:00,900").getTime(),
                   600745);

        assertItem(value,
                   3,
                   "session-finished",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 19:55:00,555").getTime(),
                   3300400);

        assertItem(value,
                   4,
                   "session-started",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 20:00:00,100").getTime(),
                   3599945);

        assertItem(value,
                   5,
                   "debug-started",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 20:02:00,600").getTime(),
                   3720445);

        assertItem(value,
                   6,
                   "session-finished",
                   TARGET_WORKSPACE,
                   TARGET_USER,
                   null,
                   fullTimeFormat.parse("2013-11-01 20:04:00,220").getTime(),
                   3840065);

        metric = new TestNumberOfUsersOfActivity();
        Assert.assertEquals(metric.getValue(context), LongValueData.valueOf(7));
    }


    private void assertItem(ListValueData items,
                            int itemIndex,
                            String event,
                            String ws,
                            String user,
                            String message,
                            long date,
                            long timeFromBeginning) {

        Map<String, ValueData> itemContent = ((MapValueData)items.getAll().get(itemIndex)).getAll();
        assertEquals(itemContent.get(UsersActivityList.EVENT), StringValueData.valueOf(event));
        assertEquals(itemContent.get(UsersActivityList.WS), StringValueData.valueOf(ws));
        assertEquals(itemContent.get(UsersActivityList.USER), StringValueData.valueOf(user));

        if (message != null) {
            assertEquals(itemContent.get("message"), StringValueData.valueOf(message));
        }

        assertEquals(itemContent.get(UsersActivityList.DATE), LongValueData.valueOf(date));
        assertEquals(itemContent.get(UsersActivityList.TIME_FROM_BEGINNING), LongValueData.valueOf(timeFromBeginning));
    }

    // ------------------------> Tested classes

    private class TestNumberOfUsersOfActivity extends UsersActivity {
        @Override
        public String getStorageCollectionName() {
            return COLLECTION;
        }
    }

    private class TestUsersActivityList extends UsersActivityList {
        @Override
        public String getStorageCollectionName() {
            return COLLECTION;
        }
    }
}
