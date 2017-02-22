/*
 * Copyright 2011-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mongodb.core;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.mongodb.core.query.Criteria.*;
import static org.springframework.data.mongodb.core.query.Query.*;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate.QueryCursorPreparer;
import org.springframework.data.mongodb.core.query.Meta;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.Bytes;
import com.mongodb.DBCursor;

/**
 * Unit tests for {@link QueryCursorPreparer}.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class QueryCursorPreparerUnitTests {

	@Mock MongoDbFactory factory;
	@Mock DBCursor cursor;

	@Mock DBCursor cursorToUse;

	@Before
	public void setUp() {
		when(cursor.copy()).thenReturn(cursorToUse);
	}

	@Test // DATAMONGO-185
	public void appliesHintsCorrectly() {

		Query query = query(where("foo").is("bar")).withHint("hint");

		prepare(query);

		verify(cursorToUse).hint("hint");
	}

	@Test // DATAMONGO-957
	public void doesNotApplyMetaWhenEmpty() {

		Query query = query(where("foo").is("bar"));
		query.setMeta(new Meta());

		prepare(query);

		verify(cursor, never()).copy();
		verify(cursorToUse, never()).addSpecial(any(String.class), anyObject());
	}

	@Test // DATAMONGO-957
	public void appliesMaxScanCorrectly() {

		Query query = query(where("foo").is("bar")).maxScan(100);

		prepare(query);

		verify(cursorToUse).addSpecial(eq("$maxScan"), eq(100L));
	}

	@Test // DATAMONGO-957
	public void appliesMaxTimeCorrectly() {

		Query query = query(where("foo").is("bar")).maxTime(1, TimeUnit.SECONDS);

		prepare(query);

		verify(cursorToUse).addSpecial(eq("$maxTimeMS"), eq(1000L));
	}

	@Test // DATAMONGO-957
	public void appliesCommentCorrectly() {

		Query query = query(where("foo").is("bar")).comment("spring data");

		prepare(query);

		verify(cursorToUse).addSpecial(eq("$comment"), eq("spring data"));
	}

	@Test // DATAMONGO-957
	public void appliesSnapshotCorrectly() {

		Query query = query(where("foo").is("bar")).useSnapshot();

		prepare(query);

		verify(cursorToUse).addSpecial(eq("$snapshot"), eq(true));
	}

	@Test // DATAMONGO-1480
	public void appliesNoCursorTimeoutCorrectly() {

		Query query = query(where("foo").is("bar")).noCursorTimeout();

		prepare(query);

		verify(cursorToUse).addOption(Bytes.QUERYOPTION_NOTIMEOUT);
	}

	private DBCursor prepare(Query query) {

		CursorPreparer preparer = new MongoTemplate(factory).new QueryCursorPreparer(query, null);
		return preparer.prepare(cursor);
	}
}
