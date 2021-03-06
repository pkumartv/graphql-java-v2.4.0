package com.gql.graphql.schema.idl;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;

import java.util.LinkedHashMap;
import java.util.Map;
// import java.util.function.UnaryOperator;

import static graphql.Assert.assertNotNull;

/**
 * A type runtime wiring is a specification of the data fetchers and possible type resolver for a given type name.
 *
 * This is used by {@link RuntimeWiring} to wire together a functional {@link GraphQLSchema}
 */
public class TypeRuntimeWiring {
    private final String typeName;
    private final Map<String, DataFetcher> fieldDataFetchers;
    private final TypeResolver typeResolver;

    private TypeRuntimeWiring(String typeName, Map<String, DataFetcher> fieldDataFetchers, TypeResolver typeResolver) {
        this.typeName = typeName;
        this.fieldDataFetchers = fieldDataFetchers;
        this.typeResolver = typeResolver;
    }

    public String getTypeName() {
        return typeName;
    }

    public Map<String, DataFetcher> getFieldDataFetchers() {
        return fieldDataFetchers;
    }

    public TypeResolver getTypeResolver() {
        return typeResolver;
    }

    /**
     * Creates a new type wiring builder
     *
     * @param typeName the name of the type to wire
     *
     * @return the builder
     */
    public static Builder newTypeWiring(String typeName) {
        assertNotNull(typeName, "You must provide a type name");
        return new Builder().typeName(typeName);
    }

    /**
     * This form allows a lambda to be used as the builder
     *
     * @param typeName        the name of the type to wire
     * @param builderFunction a function that will be given the builder to use
     *
     * @return the same builder back please
     */
    // public static TypeRuntimeWiring newTypeWiring(String typeName, UnaryOperator<Builder> builderFunction) {
    //     return builderFunction.apply(newTypeWiring(typeName)).build();
    // }

    public static class Builder {
        private String typeName;
        private final Map<String, DataFetcher> fieldDataFetchers = new LinkedHashMap<String, DataFetcher>();
        private TypeResolver typeResolver;

        /**
         * Sets the type name for this type wiring.  You MUST set this.
         *
         * @param typeName the name of the type
         *
         * @return the current type wiring
         */
        public Builder typeName(String typeName) {
            this.typeName = typeName;
            return this;
        }

        /**
         * Adds a data fetcher for the current type to the specified field
         *
         * @param fieldName   the field that data fetcher should apply to
         * @param dataFetcher the new data Fetcher
         *
         * @return the current type wiring
         */
        public Builder dataFetcher(String fieldName, DataFetcher dataFetcher) {
            assertNotNull(dataFetcher, "you must provide a data fetcher");
            assertNotNull(fieldName, "you must tell us what field");
            fieldDataFetchers.put(fieldName, dataFetcher);
            return this;
        }

        /**
         * Adds data fetchers for the current type to the specified field
         *
         * @param dataFetchersMap a map of fields to data fetchers
         *
         * @return the current type wiring
         */
        public Builder dataFetchers(Map<String, DataFetcher> dataFetchersMap) {
            assertNotNull(dataFetchersMap, "you must provide a data fetchers map");
            fieldDataFetchers.putAll(dataFetchersMap);
            return this;
        }

        /**
         * Adds a {@link TypeResolver} to the current type.  This MUST be specified for Interface
         * and Union types.
         *
         * @param typeResolver the type resolver in play
         *
         * @return the current type wiring
         */
        public Builder typeResolver(TypeResolver typeResolver) {
            assertNotNull(typeResolver, "you must provide a type resolver");
            this.typeResolver = typeResolver;
            return this;
        }

        /**
         * @return the built type wiring
         */
        public TypeRuntimeWiring build() {
            assertNotNull(typeName, "you must provide a type name");
            return new TypeRuntimeWiring(typeName, fieldDataFetchers, typeResolver);
        }
    }

}
