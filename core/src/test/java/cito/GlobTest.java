/*
 * Copyright 2016-2017 Daniel Siviter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


/**
 * Unit tests for {@link Glob}.
 *
 * @author Daniel Siviter
 * @since v1.0 [22 Sep 2016]
 */
public class GlobTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void go() {
		assertFalse(Glob.matches("/foo.bar/??/blagh", null));

		assertTrue(Glob.matches("/foo/bar/", "/foo/bar/"));
		assertFalse(Glob.matches("/foo/bar/", "/foo/bar"));
		assertFalse(Glob.matches("/foo/bar/", "/foo/bar/blagh"));
		assertTrue(Glob.matches("/foo/bar/*", "/foo/bar/blagh"));
		assertTrue(Glob.matches("*/bar/*", "/foo/bar/blagh"));
		assertFalse(Glob.matches("/foo/bar/?", "/foo/bar/blagh"));
		assertTrue(Glob.matches("/foo/bar/?", "/foo/bar/b"));
		assertTrue(Glob.matches("/foo/bar/?/blagh", "/foo/bar/b/blagh"));
		assertTrue(Glob.matches("?/foo/bar/", "b/foo/bar/"));
		assertFalse(Glob.matches("?/foo/bar/", "bb/foo/bar/"));
		assertFalse(Glob.matches("/foo/bar/??/blagh", "/foo/bar/b/blagh"));
		assertTrue(Glob.matches("/foo/bar/??/blagh", "/foo/bar/bl/blagh"));
		assertFalse(Glob.matches("/foo/bar/??/blagh", "/foo/bar/b/blagh"));
		assertTrue(Glob.matches("/foo/bar/??/blagh", "/foo/bar/bl/blagh"));

		assertTrue(Glob.matches("/foo.bar/", "/foo.bar/"));
		assertFalse(Glob.matches("/foo.bar/", "/foo.bar"));
		assertFalse(Glob.matches("/foo.bar/", "/foo.bar/blagh"));
		assertTrue(Glob.matches("/foo.bar/*", "/foo.bar/blagh"));
		assertTrue(Glob.matches("*.bar/*", "/foo.bar/blagh"));
		assertFalse(Glob.matches("/foo.bar/?", "/foo.bar/blagh"));
		assertTrue(Glob.matches("/foo.bar/?", "/foo.bar/b"));
		assertTrue(Glob.matches("/foo.bar/?/blagh", "/foo.bar/b/blagh"));
		assertTrue(Glob.matches("?/foo.bar/", "b/foo.bar/"));
		assertFalse(Glob.matches("?/foo.bar/", "bb/foo.bar/"));
		assertFalse(Glob.matches("/foo.bar/??/blagh", "/foo.bar/b/blagh"));
		assertTrue(Glob.matches("/foo.bar/??/blagh", "/foo.bar/bl/blagh"));
		assertFalse(Glob.matches("/foo.bar/??/blagh", "/foo.bar/b/blagh"));
		assertTrue(Glob.matches("/foo.bar/??/blagh", "/foo.bar/bl/blagh"));

		assertTrue(Glob.matches("/foo.bar,blagh", "/foo.bar,blagh"));
	}

	@Test
	public void wildCard() {
		assertTrue(new Glob("/foo/*").hasWildcard());
		assertTrue(new Glob("/foo/*/bar").hasWildcard());
		assertTrue(new Glob("/foo/?").hasWildcard());
		assertTrue(new Glob("/foo/?/bar").hasWildcard());
		assertFalse(new Glob("/foo/").hasWildcard());
	}

	@Test
	public void capture() {
		assertTrue(Glob.matches("/foo.bar/{hello}/blagh", "/foo.bar/hello/blagh"));
	}

	@Test
	public void capture_hyphen() {
		assertTrue(Glob.matches("/foo.bar/{hello}/blagh", "/foo.bar/hello-world/blagh"));
	}

	@Test
	public void validEscapeCharacter() {
		assertTrue(Glob.matches("/foo/bar/\\.", "/foo/bar/."));
		assertFalse(Glob.matches("/foo/bar/\\.", "/foo/bar/z"));
	}

	@Test
	public void exclamationMark() {
		assertTrue(Glob.matches("!", "!"));
		assertFalse(Glob.matches("/foo/bar/[!a-z]+", "/foo/bar/a!bc"));
		assertFalse(Glob.matches("/foo/bar/![a-z]+", "/foo/bar/!a"));
	}

	@Test
	public void missingEscapeCharacter() {
		this.exception.expect(PatternSyntaxException.class);
		this.exception.expectMessage("Missing escaped character");

		new Glob("/foo/\\");
	}

	@Test
	public void invalidComma() {
		this.exception.expect(PatternSyntaxException.class);
		this.exception.expectMessage("Invalid comma");

		Glob.matches("/foo.{bar,blagh}", "/foo.bar");
	}

	@Test
	public void unOpenedGroup() {
		this.exception.expect(PatternSyntaxException.class);
		this.exception.expectMessage("Unexpected group close");

		new Glob("/foo.bar}");
	}

	@Test
	public void uncloseGroup() {
		this.exception.expect(PatternSyntaxException.class);
		this.exception.expectMessage("Unclosed group");

		Glob.matches("/foo.{bar", "/foo.bar");
	}

	@Test
	public void uncloseOpen() {
		this.exception.expect(PatternSyntaxException.class);
		this.exception.expectMessage("Unclosed character class");

		Glob.matches("/foo.[bar", "/foo.bar");
	}

	@Test
	public void characterClassStillOpen() {
		this.exception.expect(PatternSyntaxException.class);
		this.exception.expectMessage("Unexpected group close");

		new Glob("/foo.bar[[a-z]A-Z]");
	}

	@Test
	public void compile() {
		final Pattern pattern = Glob.compile("/foo.bar/{hello}/blagh");
		assertEquals("/foo\\.bar/(?<hello>[A-Za-z0-9\\-\\_]*)/blagh", pattern.pattern());
	}
}
