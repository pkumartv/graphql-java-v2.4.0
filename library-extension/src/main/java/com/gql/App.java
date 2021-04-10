package com.gql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.gql.graphql.schema.idl.RuntimeWiring;
import com.gql.graphql.schema.idl.SchemaGenerator;
import com.gql.graphql.schema.idl.SchemaParser;
import com.gql.graphql.schema.idl.SchemaPrinter;
import com.gql.graphql.schema.idl.TypeDefinitionRegistry;
import com.gql.graphql.schema.idl.errors.SchemaProblem;

import graphql.schema.GraphQLSchema;

public class App {

    private static final String starWarsSchema = "library-extension/src/test/resources/starWarsSchema.graphqls";
    private static final String blogSchema = "library-extension/src/test/resources/blogSchema.graphqls";

    private RuntimeWiring buildRuntimeWiring() {
        return RuntimeWiring.newRuntimeWiring().build();
    }

    public GraphQLSchema parseSchema(String fileContents) {

        SchemaParser schemaParser = new SchemaParser();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();
        try {
            typeRegistry.merge(schemaParser.parse(fileContents));
        } catch (SchemaProblem e) {
            e.printStackTrace();
        }
        RuntimeWiring wiring = buildRuntimeWiring();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, wiring);
        return graphQLSchema;
    }

    public GraphQLSchema parseSchema(File file) {

        SchemaParser schemaParser = new SchemaParser();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();
        try {
            typeRegistry.merge(schemaParser.parse(readFile(file)));
        } catch (SchemaProblem e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        RuntimeWiring wiring = buildRuntimeWiring();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, wiring);
        return graphQLSchema;
    }

    public String printSchema(GraphQLSchema gqlSchema) {
        SchemaPrinter schemaPrinter = new SchemaPrinter();
        return schemaPrinter.print(gqlSchema);
    }

    private String readFile(File file) throws IOException {
        String fileAsString = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line).append("\n");
                line = br.readLine();
            }
            fileAsString = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null)
                br.close();
        }
        return fileAsString;
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

    public static void main(final String[] args) {
        App app = new App();
        SchemaPrinter sp = new SchemaPrinter();
        GraphQLSchema gql = null;
        try {
            gql = app.parseSchema(app.getFileContents(starWarsSchema));
        } catch (Exception e) {
            e.printStackTrace();
        }
        String schemaTest = sp.print(gql);
        System.out.println("Schema is: " + schemaTest);

    }
}