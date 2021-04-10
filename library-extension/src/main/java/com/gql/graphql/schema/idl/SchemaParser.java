package com.gql.graphql.schema.idl;

import graphql.GraphQLError;
import graphql.InvalidSyntaxError;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.SourceLocation;
import graphql.parser.Parser;
import com.gql.graphql.schema.idl.errors.SchemaProblem;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * This can take a graphql schema definition and parse it into a {@link TypeDefinitionRegistry} of
 * definitions ready to be placed into {@link SchemaGenerator} say
 */
public class SchemaParser {
    
    
    //private static final Logger _LOG = new Logger(SchemaParser.class);

    /**
     * Parse a file of schema definitions and create a {@link TypeDefinitionRegistry}
     *
     * @param file the file to parse
     *
     * @return registry of type definitions
     *
     * @throws SchemaProblem if there are problems compiling the schema definitions
     */
    public TypeDefinitionRegistry parse(final File file) throws SchemaProblem {
        try {
            return parse(new FileReader(file));
        } catch (final FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse a reader of schema definitions and create a {@link TypeDefinitionRegistry}
     *
     * @param reader the reader to parse
     *
     * @return registry of type definitions
     *
     * @throws SchemaProblem if there are problems compiling the schema definitions
     */
    public TypeDefinitionRegistry parse(final Reader reader) throws SchemaProblem {
        try (Reader input = reader) {
            return parse(read(input));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse a string of schema definitions and create a {@link TypeDefinitionRegistry}
     *
     * @param schemaInput the schema string to parse
     *
     * @return registry of type definitions
     *
     * @throws SchemaProblem if there are problems compiling the schema definitions
     */
    public TypeDefinitionRegistry parse(String schemaInput) throws SchemaProblem 
    {
        Document document=null;
        try {
            final Parser parser = new Parser();
            document = parser.parseDocument(schemaInput);

           
        } catch (final Exception e) {
            //throw new handleParseException(e);
            
            RecognitionException recognitionException = (RecognitionException) e.getCause();
            SourceLocation sourceLocation = new SourceLocation(recognitionException.getOffendingToken().getLine(), recognitionException.getOffendingToken().getCharPositionInLine());
        }
        return buildRegistry(document);

    }

//     private SchemaProblem handleParseException(ParseCancellationException e) throws RuntimeException {
//         RecognitionException recognitionException = (RecognitionException) e.getCause();
//         SourceLocation sourceLocation = new SourceLocation(recognitionException.getOffendingToken().getLine(), recognitionException.getOffendingToken().getCharPositionInLine());
//         InvalidSyntaxError invalidSyntaxError = new InvalidSyntaxError(sourceLocation);
//         return new SchemaProblem(Collections.singletonList(invalidSyntaxError));
//     }

    private TypeDefinitionRegistry buildRegistry(final Document document) {
        final List<GraphQLError> errors = new ArrayList<GraphQLError>();
        final TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();
        final List<Definition> definitions = document.getDefinitions();
        for (final Definition definition : definitions) {
            //JAVA1.8TOPORT ifPresent
            typeRegistry.add(definition);//.ifPresent(errors::add);
        }
        if (errors.size() > 0) {
            throw new SchemaProblem(errors);
        } else {
            return typeRegistry;
        }
    }

    private String read(final Reader reader) throws IOException {
        final char[] buffer = new char[1024 * 4];
        final StringWriter sw = new StringWriter();
        int n;
        while (-1 != (n = reader.read(buffer))) {
            sw.write(buffer, 0, n);
        }
        return sw.toString();
    }
}
