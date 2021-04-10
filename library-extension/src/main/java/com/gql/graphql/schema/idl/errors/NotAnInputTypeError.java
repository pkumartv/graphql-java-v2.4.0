package com.gql.graphql.schema.idl.errors;

import graphql.language.TypeDefinition;

import static java.lang.String.format;

public class NotAnInputTypeError extends BaseError {

    /**
     *
     */
    private static final long serialVersionUID = -2218439409989984466L;

    public NotAnInputTypeError(TypeDefinition typeDefinition) {
        super(typeDefinition, format("expected InputType, but found %s type %s", typeDefinition.getName(), lineCol(typeDefinition)));
    }
}
