package com.gql.graphql.schema.idl;

import graphql.Assert;
//import graphql.PublicApi;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
//import java.util.function.UnaryOperator;

/**
 * A runtime wiring is a specification of data fetchers, type resolves and
 * custom scalars that are needed to wire together a functional
 * {@link GraphQLSchema}
 */
// @PublicApi
public class RuntimeWiring {

    private final Map<String, Map<String, DataFetcher>> dataFetchers;
    private final Map<String, GraphQLScalarType> scalars;
    private final Map<String, TypeResolver> typeResolvers;
    private final WiringFactory wiringFactory;

    private RuntimeWiring(Map<String, Map<String, DataFetcher>> dataFetchers, Map<String, GraphQLScalarType> scalars,
            Map<String, TypeResolver> typeResolvers, WiringFactory wiringFactory) {
        this.dataFetchers = dataFetchers;
        this.scalars = scalars;
        this.typeResolvers = typeResolvers;
        this.wiringFactory = wiringFactory;
    }

    public Map<String, GraphQLScalarType> getScalars() {
        return new LinkedHashMap<String, GraphQLScalarType>(scalars);
    }

    public Map<String, Map<String, DataFetcher>> getDataFetchers() {
        return dataFetchers;
    }

    public Map<String, DataFetcher> getDataFetcherForType(String typeName) {
        // JAVA1.8TOPORT computeIfAbsent
        // return dataFetchers.computeIfAbsent(typeName, k -> new LinkedHashMap<>());
        Map<String, DataFetcher> temp = dataFetchers.get(typeName);
        if (temp == null)
            temp = new LinkedHashMap<String, DataFetcher>();
        return temp;
        // return dataFetchers.computeIfAbsent(typeName, k -> new LinkedHashMap<>());
    }

    public Map<String, TypeResolver> getTypeResolvers() {
        return typeResolvers;
    }

    public WiringFactory getWiringFactory() {
        return wiringFactory;
    }

    /**
     * @return a builder of Runtime Wiring
     */
    public static Builder newRuntimeWiring() {
        return new Builder();
    }

    // @PublicApi
    public static class Builder {
        private final Map<String, Map<String, DataFetcher>> dataFetchers = new LinkedHashMap<String, Map<String, DataFetcher>>();
        private final Map<String, GraphQLScalarType> scalars = new LinkedHashMap<String, GraphQLScalarType>();
        private final Map<String, TypeResolver> typeResolvers = new LinkedHashMap<String, TypeResolver>();
        private WiringFactory wiringFactory = new NoopWiringFactory();

        private Builder() {
            // JAVA1.8TOPORT forEach
            // ScalarInfo.STANDARD_SCALARS.forEach(this::scalar);
            List<GraphQLScalarType> scalarTypes = ScalarInfo.STANDARD_SCALARS;
            for (GraphQLScalarType scalarType : scalarTypes) {
                scalar(scalarType);
            }
        }

        /**
         * Adds a wiring factory into the runtime wiring
         *
         * @param wiringFactory the wiring factory to add
         *
         * @return this outer builder
         */
        public Builder wiringFactory(WiringFactory wiringFactory) {
            Assert.assertNotNull(wiringFactory, "You must provide a wiring factory");
            this.wiringFactory = wiringFactory;
            return this;
        }

        /**
         * This allows you to add in new custom Scalar implementations beyond the
         * standard set.
         *
         * @param scalarType the new scalar implementation
         *
         * @return the runtime wiring builder
         */
        public Builder scalar(GraphQLScalarType scalarType) {
            scalars.put(scalarType.getName(), scalarType);
            return this;
        }

        /**
         * This allows you to add a new type wiring via a builder
         *
         * @param builder the type wiring builder to use
         *
         * @return this outer builder
         */
        public Builder type(TypeRuntimeWiring.Builder builder) {
            return type(builder.build());
        }

        /**
         * This form allows a lambda to be used as the builder of a type wiring
         *
         * @param typeName        the name of the type to wire
         * @param builderFunction a function that will be given the builder to use
         *
         * @return the runtime wiring builder
         */
        // public Builder type(String typeName, UnaryOperator<TypeRuntimeWiring.Builder>
        // builderFunction) {
        // TypeRuntimeWiring.Builder builder =
        // builderFunction.apply(TypeRuntimeWiring.newTypeWiring(typeName));
        // return type(builder.build());
        // }

        /**
         * This adds a type wiring
         *
         * @param typeRuntimeWiring the new type wiring
         *
         * @return the runtime wiring builder
         */
        public Builder type(TypeRuntimeWiring typeRuntimeWiring) {
            String typeName = typeRuntimeWiring.getTypeName();
            // JAVA1.8TOPORT computeIfAbset
            // Map<String, DataFetcher> typeDataFetchers =
            // dataFetchers.computeIfAbsent(typeName, k -> new LinkedHashMap<String,
            // DataFetcher>());
            Map<String, DataFetcher> typeDataFetchers = dataFetchers.get(typeName);
            if (typeDataFetchers == null)
                typeDataFetchers = new LinkedHashMap<String, DataFetcher>();
            // DONEJAVA1.8TOPORT forEach
            // typeRuntimeWiring.getFieldDataFetchers().forEach(typeDataFetchers::put);
            Map<String, DataFetcher> fieldDataFetchers = typeRuntimeWiring.getFieldDataFetchers();
            for (Map.Entry<String, DataFetcher> fieldDataFetcher : fieldDataFetchers.entrySet()) {
                // fieldDataFetcher.g
                typeDataFetchers.put(fieldDataFetcher.getKey(), fieldDataFetcher.getValue());
            }

            // ..defaultDataFetchers.
            dataFetchers.put(typeName, typeDataFetchers);

            TypeResolver typeResolver = typeRuntimeWiring.getTypeResolver();
            if (typeResolver != null) {
                this.typeResolvers.put(typeName, typeResolver);
            }
            return this;
        }

        /**
         * @return the built runtime wiring
         */
        public RuntimeWiring build() {
            return new RuntimeWiring(dataFetchers, scalars, typeResolvers, wiringFactory);
        }

    }

}
