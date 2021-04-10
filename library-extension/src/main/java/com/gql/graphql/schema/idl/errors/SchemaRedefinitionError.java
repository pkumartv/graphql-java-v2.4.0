package com.gql.graphql.schema.idl.errors;

import graphql.language.SchemaDefinition;

import static java.lang.String.format;

public class SchemaRedefinitionError extends BaseError {

    /**
     *
     */
    private static final long serialVersionUID = 3489594372602670468L;

    public SchemaRedefinitionError(SchemaDefinition oldEntry, SchemaDefinition newEntry) {
        super(oldEntry, format("There is already a schema defined %s.  The offending new new ones is here %s",
                lineCol(oldEntry), lineCol(newEntry)));
    }
}
