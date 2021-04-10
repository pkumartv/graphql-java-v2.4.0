package com.gql.graphql.schema.idl.errors;

import graphql.language.TypeDefinition;

import static java.lang.String.format;

public class TypeRedefinitionError extends BaseError {

    /**
     *
     */
    private static final long serialVersionUID = -8535221128263485957L;

    public TypeRedefinitionError(TypeDefinition newEntry, TypeDefinition oldEntry) {
        super(oldEntry,
                format("'%s' type %s tried to redefine existing '%s' type %s",
                        newEntry.getName(), BaseError.lineCol(newEntry), oldEntry.getName(), BaseError.lineCol(oldEntry)
                ));
    }
}
