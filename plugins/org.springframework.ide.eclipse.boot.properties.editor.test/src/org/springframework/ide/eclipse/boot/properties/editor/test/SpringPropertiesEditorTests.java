/*******************************************************************************
 * Copyright (c) 2014 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.properties.editor.test;

import junit.framework.AssertionFailedError;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.springframework.ide.eclipse.boot.properties.editor.SpringPropertiesCompletionEngine;
import org.springframework.ide.eclipse.boot.properties.editor.StsConfigMetadataRepositoryJsonLoader;
import org.springframework.ide.eclipse.boot.properties.editor.util.AptUtils;
import org.springframework.ide.eclipse.boot.util.JavaProjectUtil;

public class SpringPropertiesEditorTests extends SpringPropertiesEditorTestHarness {

	//TODO: List type is assignable (but parametric),
	//  - handle this in reconciling?

	public void testServerPortCompletion() throws Exception {
		data("server.port", INTEGER, 8080, "Port where server listens for http.");
		if (SpringPropertiesCompletionEngine.DEFAULT_VALUE_INCLUDED) {
			assertCompletion("ser<*>", "server.port=8080<*>");
		} else {
			assertCompletion("ser<*>", "server.port=<*>");
		}
		assertCompletionDisplayString("ser<*>", "server.port=8080 : int Port where server listens for http.");
	}

	public void testLoggingLevelCompletion() throws Exception {
		data("logging.level", "java.util.Map<java.lang.String,java.lang.Object>", null, "Logging level per package.");
		assertCompletion("lolev<*>","logging.level.<*>");
	}

	public void testListCompletion() throws Exception {
		data("foo.bars", "java.util.List<java.lang.String>", null, "List of bars in foo.");
		assertCompletion("foba<*>","foo.bars=<*>");
	}

	public void testInetAddresCompletion() throws Exception {
		defaultTestData();
		assertCompletion("server.add<*>", "server.address=<*>");
	}

	public void testStringArrayCompletion() throws Exception {
		data("spring.freemarker.view-names", "java.lang.String[]", null, "White list of view names that can be resolved.");
		data("some.defaulted.array", "java.lang.String[]", new String[] {"a", "b", "c"} , "Stuff.");

		assertCompletion("spring.freemarker.vn<*>", "spring.freemarker.view-names=<*>");
		if (SpringPropertiesCompletionEngine.DEFAULT_VALUE_INCLUDED) {
			assertCompletion("some.d.a<*>", "some.defaulted.array=a,b,c<*>");
		} else {
			assertCompletion("some.d.a<*>", "some.defaulted.array=<*>");
		}
	}

	public void testEmptyPrefixProposalsSortedAlpabetically() throws Exception {
		defaultTestData();
		MockEditor editor = new MockEditor("");
		ICompletionProposal[] completions = getCompletions(editor);
		assertTrue(completions.length>100); //should be many proposals
		String previous = null;
		for (ICompletionProposal c : completions) {
			String current = c.getDisplayString();
			if (previous!=null) {
				assertTrue("Incorrect order: \n   "+previous+"\n   "+current, previous.compareTo(current)<=0);
			}
			previous = current;
		}
	}

	public void testValueCompletion() throws Exception {
		defaultTestData();
		assertCompletions("liquibase.enabled=<*>",
				"liquibase.enabled=true<*>",
				"liquibase.enabled=false<*>"
		);

		assertCompletions("liquibase.enabled:<*>",
				"liquibase.enabled:true<*>",
				"liquibase.enabled:false<*>"
		);

		assertCompletions("liquibase.enabled = <*>",
				"liquibase.enabled = true<*>",
				"liquibase.enabled = false<*>"
		);

		assertCompletions("liquibase.enabled   <*>",
				"liquibase.enabled   true<*>",
				"liquibase.enabled   false<*>"
		);

		assertCompletions("liquibase.enabled=f<*>",
				"liquibase.enabled=false<*>"
		);

		assertCompletions("liquibase.enabled=t<*>",
				"liquibase.enabled=true<*>"
		);

		assertCompletions("liquibase.enabled:f<*>",
				"liquibase.enabled:false<*>"
		);

		assertCompletions("liquibase.enabled:t<*>",
				"liquibase.enabled:true<*>"
		);

		assertCompletions("liquibase.enabled = f<*>",
				"liquibase.enabled = false<*>"
		);

		assertCompletions("liquibase.enabled = t<*>",
				"liquibase.enabled = true<*>"
		);

		assertCompletions("liquibase.enabled   t<*>",
				"liquibase.enabled   true<*>"
		);

		//one more... for special char like '-' in the name

		assertCompletions("liquibase.check-change-log-location=t<*>",
				"liquibase.check-change-log-location=true<*>"
		);
	}


	public void testHoverInfos() throws Exception {
		defaultTestData();
		MockEditor editor = new MockEditor(
				"#foo\n" +
				"# bar\n" +
				"server.port=8080\n" +
				"logging.level.com.acme=INFO\n"
		);
		//Case 1: an 'exact' match of the property is in the hover region
		assertHoverText(editor, "server.",
				"<b>server.port</b>"+
				"<br><a href=\"type%2Fjava.lang.Integer\">java.lang.Integer</a>"+
				"<br><br>Server HTTP port"
		);
		//Case 2: an object/map property has extra text after the property name
		assertHoverText(editor, "logging.", "<b>logging.level</b>");
	}

	public void testHoverInfosWithSpaces() throws Exception {
		defaultTestData();
		MockEditor editor = new MockEditor(
				"#foo\n" +
				"# bar\n"+
				"\n" +
				"  server.port = 8080\n" +
				"  logging.level.com.acme = INFO\n"
		);
		//Case 1: an 'exact' match of the property is in the hover region
		assertHoverText(editor, "server.",
				"<b>server.port</b>"+
				"<br><a href=\"type%2Fjava.lang.Integer\">java.lang.Integer</a>"+
				"<br><br>Server HTTP port"
		);
		//Case 2: an object/map property has extra text after the property name
		assertHoverText(editor, "logging.", "<b>logging.level</b>");
	}

	public void testHoverLongAndShort() throws Exception {
		data("server.port", INTEGER, 8080, "Port where server listens for http.");
		data("server.port.fancy", BOOLEAN, 8080, "Whether the port is fancy.");
		MockEditor editor = new MockEditor(
				"server.port=8080\n" +
				"server.port.fancy=true\n"
		);
		assertHoverText(editor, "server.", "<b>server.port</b>");
		assertHoverText(editor, "port.fa", "<b>server.port.fancy</b>");
	}


	public void testPredefinedProject() throws Exception {
		IProject p = createPredefinedProject("demo");
		IType type = JavaCore.create(p).findType("demo.DemoApplication");
		assertNotNull(type);
	}

	public void testEnableApt() throws Throwable {
		IProject p = createPredefinedProject("demo-live-metadata");
		IJavaProject jp = JavaCore.create(p);

		//Check some assumptions about the initial state of the test project (if these checks fail then
		// the test may be 'vacuous' since the things we are testing for already exist beforehand.
		assertFalse(AptUtils.isAptEnabled(jp));
		IFile metadataFile = JavaProjectUtil.getOutputFile(jp, StsConfigMetadataRepositoryJsonLoader.META_DATA_LOCATIONS[0]);
		assertFalse(metadataFile.exists());

		AptUtils.enableApt(jp);
		buildProject(jp);

		assertTrue(AptUtils.isAptEnabled(jp));
		assertTrue(metadataFile.exists()); //apt should create the json metadata file during project build.
		assertContains("\"name\": \"foo.counter\"", getContents(metadataFile));
	}

	public void testHyperlinkTargets() throws Exception {
		System.out.println(">>> testHyperlinkTargets");
		IProject p = createPredefinedProject("demo");
		IJavaProject jp = JavaCore.create(p);
		useProject(jp);

		MockEditor editor = new MockEditor(
				"server.port=888\n" +
				"spring.datasource.login-timeout=1000\n" +
				"flyway.init-sqls=a,b,c\n"
		);

		assertLinkTargets(editor, "server",
				"org.springframework.boot.autoconfigure.web.ServerProperties.setPort(Integer)"
		);
		assertLinkTargets(editor, "data",
				"org.springframework.boot.autoconfigure.jdbc.DataSourceConfigMetadata.hikariDataSource()",
				"org.springframework.boot.autoconfigure.jdbc.DataSourceConfigMetadata.tomcatDataSource()",
				"org.springframework.boot.autoconfigure.jdbc.DataSourceConfigMetadata.dbcpDataSource()"
		);
		assertLinkTargets(editor, "flyway",
				"org.springframework.boot.autoconfigure.flyway.FlywayProperties.setInitSqls(List<String>)");
		System.out.println("<<< testHyperlinkTargets");
	}

	public void testHyperlinkTargetsLoggingLevel() throws Exception {
		System.out.println(">>> testHyperlinkTargetsLoggingLevel");
		IProject p = createPredefinedProject("demo");
		IJavaProject jp = JavaCore.create(p);
		useProject(jp);

		MockEditor editor = new MockEditor(
				"logging.level.com.acme=INFO\n"
		);
		assertLinkTargets(editor, "level",
				"org.springframework.boot.logging.LoggingApplicationListener"
		);
		System.out.println("<<< testHyperlinkTargetsLoggingLevel");
	}

	public void testReconcile() throws Exception {
		defaultTestData();
		MockEditor editor = new MockEditor(
				"server.port=8080\n" +
				"server.port.extracrap=8080\n" +
				"logging.level.com.acme=INFO\n" +
				"logging.snuggem=what?\n" +
				"bogus.no.good=true\n"
		);
		assertProblems(editor,
				".extracrap|Can't use '.' navigation",
				"snuggem|unknown property",
				"ogus.no.good|unknown property"
		);

	}

	public void testReconcilePojoArray() throws Exception {
		createPredefinedProject("demo-list-of-pojo");
		data("volder.foo.list", "java.util.List<demo.Foo>", null, "A list of Foo pojos");
		MockEditor editor = new MockEditor(
				"token.bad.guy=problem\n"+
				"volder.foo.list[0].name=Kris\n" +
				"volder.foo.list[0].description=Kris\n" +
				"volder.foo.list[0].roles[0]=Developer\n"+
				"volder.foo.list[0]garbage=Grable\n"+
				"volder.foo.list[0].bogus=Bad\n"
		);

		try {
			//This is the more ambitious requirement but it is not implemented yet.
			assertProblems(editor,
					"token.bad.guy|unknown property",
					//'name' is ok
					//'description' is ok
					"garbage|'.' or '['",
					"bogus|unknown property"
			);
		} catch (AssertionFailedError e) {
			//This is the minimum requirement (detect that follow up with '.' after ']'
			// is acceptable for Pojo but do not attempt to check the pojo properties.
			assertProblems(editor,
					"token.bad.guy|unknown property",
					//..[0].<whatever> is okay
					"garbage|'.' or '['"
			);
		}
	}

	public void testReconcileArrayNotation() throws Exception {
		defaultTestData();
		MockEditor editor = new MockEditor(
				"borked=bad+\n" + //token problem, to make sure reconciler is working
				"security.user.role[0]=foo\n" +
				"security.user.role[${one}]=foo"
		);
		assertProblems(editor,
				"orked|unknown property"
				//no other problems
		);
	}

	public void testReconcileArrayNotationError() throws Exception {
		defaultTestData();
		MockEditor editor = new MockEditor(
				"security.user.role[bork]=foo\n" +
				"security.user.role[1=foo\n" +
				"security.user.role[1]crap=foo\n" +
				"server.port[0]=8888\n" +
				"spring.thymeleaf.view-names[1]=hello"
		);
		assertProblems(editor,
				"bork|Integer",
				"[|matching ']'",
				"crap|'.' or '['",
				"[0]|Can't use '[..]'",
				"[1]|Can't use '[..]'"
				//no other problems
		);
	}

	public void testRelaxedNameReconciling() throws Exception {
		data("connection.remote-host", "java.lang.String", "service.net", null);
		data("foo-bar.name", "java.lang.String", null, null);
		MockEditor editor = new MockEditor(
				"bork=foo\n" +
				"connection.remote-host=alternate.net\n" +
				"connection.remoteHost=alternate.net\n" +
				"foo-bar.name=Charlie\n" +
				"fooBar.name=Charlie\n"
		);
		assertProblems(editor,
				"bork|unknown property"
				//no other problems
		);
	}

	public void testRelaxedNameReconcilingErrors() throws Exception {
		//Tricky with relaxec names: the error positions have to be moved
		// around because the relaxed names aren't same length as the
		// canonical ids.
		data("foo-bar-zor.enabled", "java.lang.Boolean", null, null);
		MockEditor editor = new MockEditor(
				"fooBarZor.enabled=notBoolean\n" +
				"fooBarZor.enabled.subprop=true\n"
		);
		assertProblems(editor,
				"notBoolean|Boolean",
				".subprop|Can't use '.' navigation"
		);
	}

	public void testRelaxedNameContentAssist() throws Exception {
		data("foo-bar-zor.enabled", "java.lang.Boolean", null, null);
		assertCompletion("fooBar<*>", "foo-bar-zor.enabled=<*>");
	}

	public void testReconcileValues() throws Exception {
		defaultTestData();
		MockEditor editor = new MockEditor(
				"server.port=badPort\n" +
				"liquibase.enabled=nuggels"
		);
		assertProblems(editor,
				"badPort|Integer",
				"nuggels|Boolean"
		);
	}

	public void testNoReconcileInterpolatedValues() throws Exception {
		defaultTestData();
		MockEditor editor = new MockEditor(
				"server.port=${port}\n" +
				"liquibase.enabled=nuggels"
		);
		assertProblems(editor,
				//no problem should be reported for ${port}
				"nuggels|Boolean"
		);
	}

	public void testReconcileValuesWithSpaces() throws Exception {
		defaultTestData();
		MockEditor editor = new MockEditor(
				"server.port  =   badPort\n" +
				"liquibase.enabled   nuggels  \n" +
				"liquibase.enabled   : snikkers"
		);
		assertProblems(editor,
				"badPort|Integer",
				"nuggels|Boolean",
				"snikkers|Boolean"
		);
	}


	public void testReconcileWithExtraSpaces() throws Exception {
		defaultTestData();
		//Same test as previous but with extra spaces to make things more confusing
		MockEditor editor = new MockEditor(
				"   server.port   =  8080  \n" +
				"\n" +
				"  server.port.extracrap = 8080\n" +
				" logging.level.com.acme  : INFO\n" +
				"logging.snuggem = what?\n" +
				"bogus.no.good=  true\n"
		);
		assertProblems(editor,
				".extracrap|Can't use '.' navigation",
				"snuggem|unknown property",
				"ogus.no.good|unknown property"
		);
	}

	public void testEnumPropertyCompletion() throws Exception {
		IProject p = createPredefinedProject("demo-enum");
		IJavaProject jp = JavaCore.create(p);
		useProject(jp);
		assertNotNull(jp.findType("demo.Color"));

		data("foo.color", "demo.Color", null, "A foonky colour");

		assertCompletion("foo.c<*>", "foo.color=<*>"); //Should add the '=' because enums are 'simple' values.

		assertCompletion("foo.color=R<*>", "foo.color=RED<*>");
		assertCompletion("foo.color=G<*>", "foo.color=GREEN<*>");
		assertCompletion("foo.color=B<*>", "foo.color=BLUE<*>");
		assertCompletionsDisplayString("foo.color=<*>",
				"RED", "GREEN", "BLUE"
		);
	}

	public void testEnumPropertyReconciling() throws Exception {
		IProject p = createPredefinedProject("demo-enum");
		IJavaProject jp = JavaCore.create(p);
		useProject(jp);
		assertNotNull(jp.findType("demo.Color"));

		data("foo.color", "demo.Color", null, "A foonky colour");
		MockEditor editor = new MockEditor(
				"foo.color=BLUE\n"+
				"foo.color=RED\n"+
				"foo.color=GREEN\n"+
				"foo.color.bad=BLUE\n"+
				"foo.color=Bogus\n"
		);

		assertProblems(editor,
				"Bogus|Color"
		);
	}

	public void testEnumMapValueCompletion() throws Exception {
		IProject p = createPredefinedProject("demo-enum");
		IJavaProject jp = JavaCore.create(p);
		useProject(jp);
		data("foo.name-colors", "java.util.Map<java.lang.String,demo.Color>", null, "Map with colors in its values");
		assertNotNull(jp.findType("demo.Color"));

		assertCompletions("foo.nam<*>", "foo.name-colors.<*>");

		assertCompletionsDisplayString("foo.name-colors.something=<*>",
				"RED", "GREEN", "BLUE"
		);
		assertCompletions("foo.name-colors.something=G<*>", "foo.name-colors.something=GREEN<*>");
	}

	public void testEnumMapValueReconciling() throws Exception {
		IProject p = createPredefinedProject("demo-enum");
		IJavaProject jp = JavaCore.create(p);
		useProject(jp);
		data("foo.name-colors", "java.util.Map<java.lang.String,demo.Color>", null, "Map with colors in its values");

		assertNotNull(jp.findType("demo.Color"));

		MockEditor editor = new MockEditor(
				"foo.name-colors.jacket=BLUE\n" +
				"foo.name-colors.hat=RED\n" +
				"foo.name-colors.pants=GREEN\n" +
				"foo.name-colors.wrong=NOT_A_COLOR\n"
		);
		assertProblems(editor,
				"NOT_A_COLOR|Color"
		);
	}

	public void testEnumMapKeyCompletion() throws Exception {
		IProject p = createPredefinedProject("demo-enum");
		IJavaProject jp = JavaCore.create(p);
		useProject(jp);
		data("foo.color-names", "java.util.Map<demo.Color,java.lang.String>", null, "Map with colors in its keys");
		data("foo.color-data", "java.util.Map<demo.Color,demo.ColorData>", null, "Map with colors in its keys, and pojo in values");
		assertNotNull(jp.findType("demo.Color"));
		assertNotNull(jp.findType("demo.ColorData"));

		//Map Enum -> String:
		assertCompletions("foo.colnam<*>", "foo.color-names.<*>");
		assertCompletions("foo.color-names.<*>",
				"foo.color-names.RED=<*>",
				"foo.color-names.GREEN=<*>",
				"foo.color-names.BLUE=<*>"
		);
		assertCompletionsDisplayString("foo.color-names.<*>",
				"RED", "GREEN", "BLUE"
		);
		assertCompletions("foo.color-names.B<*>",
				"foo.color-names.BLUE=<*>"
		);

		//Map Enum -> Pojo:
		assertCompletions("foo.coldat<*>", "foo.color-data.<*>");
		assertCompletions("foo.color-data.<*>",
				"foo.color-data.RED.<*>",
				"foo.color-data.GREEN.<*>",
				"foo.color-data.BLUE.<*>"
		);
		assertCompletions("foo.color-data.B<*>",
				"foo.color-data.BLUE.<*>"
		);
		assertCompletionsDisplayString("foo.color-data.<*>",
				"RED", "GREEN", "BLUE"
		);
	}

	public void testEnumMapKeyReconciling() throws Exception {
		IProject p = createPredefinedProject("demo-enum");
		IJavaProject jp = JavaCore.create(p);
		useProject(jp);
		data("foo.color-names", "java.util.Map<demo.Color,java.lang.String>", null, "Map with colors in its keys");
		data("foo.color-data", "java.util.Map<demo.Color,demo.ColorData>", null, "Map with colors in its keys, and pojo in values");
		assertNotNull(jp.findType("demo.Color"));
		assertNotNull(jp.findType("demo.ColorData"));

		MockEditor editor = new MockEditor(
				"foo.color-names.RED=Rood\n"+
				"foo.color-names.GREEN=Groen\n"+
				"foo.color-names.BLUE=Blauw\n" +
				"foo.color-names.NOT_A_COLOR=Wrong\n" +
				"foo.color-names.BLUE.bad=Blauw\n"
		);
		assertProblems(editor,
				"NOT_A_COLOR|Color",
				"BLUE.bad|Color" //because value type is not dotable the dots will be taken to be part of map key
		);
	}

}