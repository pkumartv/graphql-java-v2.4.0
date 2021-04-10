package com.gql.graphql.schema.idl;

//package com.gql.serializer;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLUnionType;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.gql.graphql.schema.idl.ScalarInfo;

/**
 * This can print an in memory GraphQL schema back to a logical schema
 * definition
 */
public class SchemaPrinter {

    /**
     * Options to use when printing a schema
     */
    public static class Options {
        private final boolean includeIntrospectionTypes;

        private final boolean includeScalars;

        private Options(boolean includeIntrospectionTypes, boolean includeScalars) {
            this.includeIntrospectionTypes = includeIntrospectionTypes;
            this.includeScalars = includeScalars;
        }

        public boolean isIncludeIntrospectionTypes() {
            return includeIntrospectionTypes;
        }

        public boolean isIncludeScalars() {
            return includeScalars;
        }

        public static Options defaultOptions() {
            return new Options(false, false);
        }

        /**
         * This will allow you to include introspection types that are contained in a
         * schema
         *
         * @param flag whether to include them
         *
         * @return options
         */
        public Options includeIntrospectionTypes(boolean flag) {
            return new Options(flag, this.includeScalars);
        }

        /**
         * This will allow you to include scalar types that are contained in a schema
         *
         * @param flag whether to include them
         *
         * @return options
         */
        public Options includeScalarTypes(boolean flag) {
            return new Options(this.includeIntrospectionTypes, flag);
        }
    }

    private final Map<Class, String> getMethodName = new LinkedHashMap<Class, String>();
    private final List<String> baseNodes = new ArrayList<String>() {
        {
            add("Note");
            add("_nodes_Note");
            add("pageInfo");
            add("Attachment");
            add("_nodes_Attachment");
            add("sortOrder"); // Enum
            add("sortableFieldsNote");
            add("sortableFieldsAttachment");
            add("stringCondition");
            add("dateCondition");
            add("bigDecimalCondition");
            add("stringCondition");
        }
    };
    private final Options options;
    private boolean extMode;

    public SchemaPrinter() {
        this(Options.defaultOptions());
    }

    public SchemaPrinter(Options options) {
        this.options = options;

        getMethodName.put(GraphQLSchema.class, "schemaPrinter");
        getMethodName.put(GraphQLObjectType.class, "objectPrinter");
        getMethodName.put(GraphQLEnumType.class, "enumPrinter");
        getMethodName.put(GraphQLScalarType.class, "scalarPrinter");
        getMethodName.put(GraphQLInterfaceType.class, "interfacePrinter");
        getMethodName.put(GraphQLUnionType.class, "unionPrinter");
        getMethodName.put(GraphQLInputObjectType.class, "inputObjectPrinter");
    }

    /**
     * This can print an in memory GraphQL schema back to a logical schema
     * definition
     *
     * @param schema the schema in play
     *
     * @return the logical schema definition
     */
    public String print(GraphQLSchema schema) {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        String methodToCall = getMethodName.get(schema.getClass());
        if ("schemaPrinter".equals(methodToCall)) {
            schemaPrinter(out, schema);

        }

        List<GraphQLType> typesAsList = new ArrayList<GraphQLType>(schema.getAllTypesAsList());

        printType(out, typesAsList, GraphQLInputType.class);
        printType(out, typesAsList, GraphQLInterfaceType.class);
        printType(out, typesAsList, GraphQLUnionType.class);
        printType(out, typesAsList, GraphQLObjectType.class);
        printType(out, typesAsList, GraphQLEnumType.class);
        printType(out, typesAsList, GraphQLScalarType.class);
        printType(out, typesAsList, GraphQLInputObjectType.class);

        return sw.toString();
    }

    public String print(GraphQLSchema schema, boolean printBaseNodes) {
        this.extMode = printBaseNodes;

        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        String methodToCall = getMethodName.get(schema.getClass());
        if ("schemaPrinter".equals(methodToCall)) {
            schemaPrinter(out, schema);
        }

        List<GraphQLType> typesAsList = new ArrayList<GraphQLType>(schema.getAllTypesAsList());

        printType(out, typesAsList, GraphQLInputType.class);
        printType(out, typesAsList, GraphQLInterfaceType.class);
        printType(out, typesAsList, GraphQLUnionType.class);
        printType(out, typesAsList, GraphQLObjectType.class);
        printType(out, typesAsList, GraphQLEnumType.class);
        printType(out, typesAsList, GraphQLScalarType.class);
        printType(out, typesAsList, GraphQLInputObjectType.class);

        return sw.toString();
    }

    private boolean isIntrospectionType(GraphQLType type) {
        return !options.isIncludeIntrospectionTypes() && type.getName().startsWith("__");
    }

    private void scalarPrinter(PrintWriter out, GraphQLScalarType type) {

        if (!options.isIncludeScalars()) {
            return;
        }
        if (!extMode) {
            if (!ScalarInfo.isStandardScalar(type)) {
                out.format("scalar %s\n\n", type.getName());
            }
        } else {
            if (!baseNodes.contains(type.getName())) {
                if (!ScalarInfo.isStandardScalar(type)) {
                    out.format("scalar %s\n\n", type.getName());
                }
            }
        }
    }

    private void enumPrinter(PrintWriter out, GraphQLEnumType type) {

        if (isIntrospectionType(type)) {
            return;
        }

        if (!extMode) {
            out.format("enum %s {\n", type.getName());
            for (GraphQLEnumValueDefinition enumValueDefinition : type.getValues()) {
                out.format("   %s\n", enumValueDefinition.getName());
            }
            out.format("}\n\n");
        } else {
            if (!baseNodes.contains(type.getName())) {
                out.format("enum %s {\n", type.getName());
                for (GraphQLEnumValueDefinition enumValueDefinition : type.getValues()) {
                    out.format("   %s\n", enumValueDefinition.getName());
                }
                out.format("}\n\n");
            }
        }
    }

    private void interfacePrinter(PrintWriter out, GraphQLInterfaceType type) {
        if (isIntrospectionType(type)) {
            return;
        }
        if (!extMode) {
            out.format("interface %s {\n", type.getName());
            List<GraphQLFieldDefinition> gqlDefinitionsList = type.getFieldDefinitions();
            for (GraphQLFieldDefinition fd : gqlDefinitionsList) {
                out.format("   %s%s : %s\n", fd.getName(), argsString(fd.getArguments()), typeString(fd.getType()));
            }

            out.format("}\n\n");
        } else {
            if (!baseNodes.contains(type.getName())) {
                out.format("interface %s {\n", type.getName());
                List<GraphQLFieldDefinition> gqlDefinitionsList = type.getFieldDefinitions();
                for (GraphQLFieldDefinition fd : gqlDefinitionsList) {
                    out.format("   %s%s : %s\n", fd.getName(), argsString(fd.getArguments()), typeString(fd.getType()));
                }

                out.format("}\n\n");
            }
        }
    }

    private void unionPrinter(PrintWriter out, GraphQLUnionType type) {

        if (isIntrospectionType(type)) {
            return;
        }
        if (!extMode) {
            out.format("union %s = ", type.getName());
            List<GraphQLObjectType> types = type.getTypes();
            for (int i = 0; i < types.size(); i++) {
                GraphQLOutputType objectType = types.get(i);
                if (i > 0) {
                    out.format(" | ");
                }
                out.format("%s", objectType.getName());
            }
            out.format("}\n\n");
        } else {
            if (!baseNodes.contains(type.getName())) {
                out.format("union %s = ", type.getName());
                List<GraphQLObjectType> types = type.getTypes();
                for (int i = 0; i < types.size(); i++) {
                    GraphQLOutputType objectType = types.get(i);
                    if (i > 0) {
                        out.format(" | ");
                    }
                    out.format("%s", objectType.getName());
                }
                out.format("}\n\n");
            }
        }

    }

    private void objectPrinter(PrintWriter out, GraphQLObjectType type) {
        if (isIntrospectionType(type)) {
            return;
        }
        if (!extMode) {
            out.format("type %s {\n", type.getName());
            List<GraphQLFieldDefinition> gqlDefinitionsList = type.getFieldDefinitions();
            for (GraphQLFieldDefinition fd : gqlDefinitionsList) {
                out.format("   %s%s : %s\n", fd.getName(), argsString(fd.getArguments()), typeString(fd.getType()));
            }
            out.format("}\n\n");
        } else {
            if (!baseNodes.contains(type.getName())) {
                if ("Query".equals(type.getName())) {
                    out.format("extend type %s {\n", type.getName());
                    List<GraphQLFieldDefinition> gqlDefinitionsList = type.getFieldDefinitions();
                    for (GraphQLFieldDefinition fd : gqlDefinitionsList) {
                        out.format("   %s%s : %s\n", fd.getName(), argsString(fd.getArguments()),
                                typeString(fd.getType()));
                    }
                    out.format("}\n\n");
                } else {
                    out.format("type %s {\n", type.getName());
                    List<GraphQLFieldDefinition> gqlDefinitionsList = type.getFieldDefinitions();
                    for (GraphQLFieldDefinition fd : gqlDefinitionsList) {
                        out.format("   %s%s : %s\n", fd.getName(), argsString(fd.getArguments()),
                                typeString(fd.getType()));
                    }
                    out.format("}\n\n");
                }
            }
        }
    }

    private void inputObjectPrinter(PrintWriter out, GraphQLInputObjectType type) {
        if (isIntrospectionType(type)) {
            return;
        }
        if (!extMode) {
            out.format("input %s {\n", type.getName());
            List<GraphQLInputObjectField> gqlInputObjectField = type.getFieldDefinitions();
            for (GraphQLInputObjectField fd : gqlInputObjectField) {
                out.format("   %s : %s\n", fd.getName(), typeString(fd.getType()));
            }
            out.format("}\n\n");
        } else {
            if (!baseNodes.contains(type.getName())) {
                out.format("input %s {\n", type.getName());
                List<GraphQLInputObjectField> gqlInputObjectField = type.getFieldDefinitions();
                for (GraphQLInputObjectField fd : gqlInputObjectField) {
                    out.format("   %s : %s\n", fd.getName(), typeString(fd.getType()));
                }
                out.format("}\n\n");
            }
        }
    }

    private void schemaPrinter(PrintWriter out, GraphQLSchema type) {

        if (!extMode) {
            out.format("schema {\n");
            GraphQLObjectType queryType = type.getQueryType();
            GraphQLObjectType mutationType = type.getMutationType();
            if (queryType != null) {
                out.format("   query : %s\n", queryType.getName());
            }
            if (mutationType != null) {
                out.format("   mutation : %s\n", mutationType.getName());
            }
            out.format("}\n\n");
        }
    }

    String typeString(GraphQLType rawType) {
        StringBuilder sb = new StringBuilder();
        Stack<String> stack = new Stack<String>();

        GraphQLType type = rawType;
        while (true) {
            if (type instanceof GraphQLNonNull) {
                type = ((GraphQLNonNull) type).getWrappedType();
                stack.push("!");
            } else if (type instanceof GraphQLList) {
                type = ((GraphQLList) type).getWrappedType();
                sb.append("[");
                stack.push("]");
            } else {
                sb.append(type.getName());
                break;
            }
        }
        while (!stack.isEmpty()) {
            sb.append(stack.pop());
        }
        return sb.toString();
    }

    String argsString(List<GraphQLArgument> arguments) {
        int count = 0;
        StringBuilder sb = new StringBuilder();
        for (GraphQLArgument argument : arguments) {
            if (count == 0) {
                sb.append("(");
            } else {
                sb.append(", ");
            }
            sb.append(argument.getName()).append(" : ").append(typeString(argument.getType()));
            Object defaultValue = argument.getDefaultValue();
            if (defaultValue != null) {
                sb.append(" = ");
                if (defaultValue instanceof Number) {
                    sb.append(defaultValue);
                } else {
                    sb.append('"').append(defaultValue).append('"');
                }
            }
            count++;
        }
        if (count > 0) {
            sb.append(")");
        }
        return sb.toString();
    }

    public String print(GraphQLType type) {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        printType(out, type);

        return sw.toString();
    }

    private void printType(PrintWriter out, List<GraphQLType> typesAsList, Class typeClazz) {
        for (GraphQLType gqlType : typesAsList) {
            if (gqlType.getClass().equals(typeClazz))
                printType(out, gqlType);
        }
    }

    private void printType(PrintWriter out, GraphQLType type) {
        String methodToCall = getMethodName.get(type.getClass());
        if ("schemaPrinter".equals(methodToCall)) {
            schemaPrinter(out, (GraphQLSchema) type);
        } else if ("objectPrinter".equals(methodToCall)) {
            objectPrinter(out, (GraphQLObjectType) type);

        } else if ("enumPrinter".equals(methodToCall)) {
            enumPrinter(out, (GraphQLEnumType) type);

        } else if ("scalarPrinter".equals(methodToCall)) {
            scalarPrinter(out, (GraphQLScalarType) type);

        } else if ("interfacePrinter".equals(methodToCall)) {
            interfacePrinter(out, (GraphQLInterfaceType) type);

        } else if ("unionPrinter".equals(methodToCall)) {
            unionPrinter(out, (GraphQLUnionType) type);

        } else if ("inputObjectPrinter".equals(methodToCall)) {
            inputObjectPrinter(out, (GraphQLInputObjectType) type);
        }
    }
}
