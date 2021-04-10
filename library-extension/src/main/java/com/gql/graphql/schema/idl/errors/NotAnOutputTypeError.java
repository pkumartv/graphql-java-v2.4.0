package com.gql.graphql.schema.idl.errors;

import graphql.language.TypeDefinition;

import static java.lang.String.format;

public class NotAnOutputTypeError extends BaseError {

    /**
     *
     */
    private static final long serialVersionUID = -5944777817441840911L;

    public NotAnOutputTypeError(TypeDefinition typeDefinition) {
        super(typeDefinition, format("expected OutputType, but found %s type %s", typeDefinition.getName(), lineCol(typeDefinition)));
    }
}
