package com.gql;

import static org.junit.Assume.assumeFalse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import graphql.schema.GraphQLSchema;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    String path = "src/test/resources/";
    private static final String starWarsSchema = "starWarsSchema.graphqls";
    private static final String blogSchema = "blogSchema.graphqls";

    public AppTest(String testName) {
        super(testName);
    }

    public String getFileContents(String filePath) {
        String fileAsString = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filePath));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line).append("\n");
                line = br.readLine();
            }
            fileAsString = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileAsString;
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        assertTrue(true);
    }

    /**
     * Tests for Resource "starWarsSchema.graphqls" under src/test
     */
    @org.junit.Test
    public void testForResourceStarWars() {
        File file = new File(path + starWarsSchema);
        String absolutePath = file.getAbsolutePath();
        System.out.println(absolutePath);

        assertTrue(absolutePath.endsWith(starWarsSchema));
    }

    /**
     * Tests for Resource "blogSchema.graphqls" under src/test
     */
    @org.junit.Test
    public void testForResourceBlog() {
        File file = new File(path + blogSchema);
        String absolutePath = file.getAbsolutePath();

        assertTrue(absolutePath.endsWith(blogSchema));
    }

    @org.junit.Test
    public void testParseStarWarsSchema() {
        App app = new App();
        GraphQLSchema gqlSchema = app.parseSchema(getFileContents(path + starWarsSchema));
        assertNotNull(gqlSchema);
    }

    @org.junit.Test
    public void testParseBlogSchema() {
        App app = new App();
        GraphQLSchema gqlSchema = app.parseSchema(getFileContents(path + blogSchema));
        assertNotNull(gqlSchema);
    }
}
