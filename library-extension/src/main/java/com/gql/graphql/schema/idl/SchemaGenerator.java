package com.gql.graphql.schema.idl;

import graphql.GraphQLError;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
//import graphql.language.Comment;
import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValue;
import graphql.language.EnumValueDefinition;
import graphql.language.Field;
import graphql.language.FieldDefinition;
import graphql.language.FloatValue;
import graphql.language.InputObjectTypeDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.IntValue;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.Node;

import graphql.language.ObjectField;
import graphql.language.ObjectTypeDefinition;
import graphql.language.ObjectValue;
import graphql.language.OperationTypeDefinition;
import graphql.language.ScalarTypeDefinition;
import graphql.language.SchemaDefinition;
import graphql.language.StringValue;
import graphql.language.Type;
import graphql.language.TypeDefinition;
import graphql.language.TypeExtensionDefinition;
import graphql.language.UnionTypeDefinition;
import graphql.language.Value;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;
import graphql.schema.PropertyDataFetcher;
import graphql.schema.TypeResolver;
import graphql.schema.TypeResolverProxy;

import java.util.ArrayList;

import com.gql.graphql.schema.idl.errors.NotAnInputTypeError;
import com.gql.graphql.schema.idl.errors.NotAnOutputTypeError;
import com.gql.graphql.schema.idl.errors.SchemaProblem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
//import java.util.Optional;
import java.util.Stack;

import com.gql.graphql.language.AbstractNode;
import com.gql.graphql.language.Comment;

//import com.gql.libraryExt.language.Node;

import static graphql.Assert.assertNotNull;

/**
 * This can generate a working runtime schema from a type registry and runtime wiring
 */
public class SchemaGenerator {

    /**
     * We pass this around so we know what we have defined in a stack like manner plus
     * it gives us helper functions
     */
    class BuildContext {
        private final TypeDefinitionRegistry typeRegistry;
        private final RuntimeWiring wiring;
        private final Stack<String> definitionStack = new Stack<String>();

        private final Map<String, GraphQLOutputType> outputGTypes = new HashMap<String, GraphQLOutputType>();
        private final Map<String, GraphQLInputType> inputGTypes = new HashMap<String, GraphQLInputType>();

        BuildContext(TypeDefinitionRegistry typeRegistry, RuntimeWiring wiring) {
            this.typeRegistry = typeRegistry;
            this.wiring = wiring;
        }

        public TypeDefinitionRegistry getTypeRegistry() {
            return typeRegistry;
        }

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        TypeDefinition getTypeDefinition(Type type) {
            //return typeRegistry.getType(type).get();
            return typeRegistry.getType(type);//.get();
        }

        boolean stackContains(TypeInfo typeInfo) {
            return definitionStack.contains(typeInfo.getName());
        }

        void push(TypeInfo typeInfo) {
            definitionStack.push(typeInfo.getName());
        }

        String pop() {
            return definitionStack.pop();
        }

        GraphQLOutputType hasOutputType(TypeDefinition typeDefinition) {
            return outputGTypes.get(typeDefinition.getName());
        }

        GraphQLInputType hasInputType(TypeDefinition typeDefinition) {
          if(typeDefinition!=null)
            return inputGTypes.get(typeDefinition.getName());
          else
            {
            return null;
          }
        }

        void put(GraphQLOutputType outputType) {
            outputGTypes.put(outputType.getName(), outputType);
            // certain types can be both input and output types, for example enums
            if (outputType instanceof GraphQLInputType) {
                inputGTypes.put(outputType.getName(), (GraphQLInputType) outputType);
            }
        }

        void put(GraphQLInputType inputType) {
            inputGTypes.put(inputType.getName(), inputType);
            // certain types can be both input and output types, for example enums
            if (inputType instanceof GraphQLOutputType) {
                outputGTypes.put(inputType.getName(), (GraphQLOutputType) inputType);
            }
        }

        RuntimeWiring getWiring() {
            return wiring;
        }

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        public SchemaDefinition getSchemaDefinition() {
            //return typeRegistry.schemaDefinition().get();
            return typeRegistry.schemaDefinition();//.get();
        }
    }
    //private final SchemaTypeChecker typeChecker = new SchemaTypeChecker();

    public SchemaGenerator() {
    }

    /**
     * This will take a {@link TypeDefinitionRegistry} and a {@link RuntimeWiring} and put them together to create a executable schema
     *
     * @param typeRegistry this can be obtained via {@link SchemaParser#parse(String)}
     * @param wiring       this can be built using {@link RuntimeWiring#newRuntimeWiring()}
     * @return an executable schema
     * @throws SchemaProblem if there are problems in assembling a schema such as missing type resolvers or no operations defined
     */
    public GraphQLSchema makeExecutableSchema(TypeDefinitionRegistry typeRegistry, RuntimeWiring wiring) throws SchemaProblem {
        BuildContext buildCtx = new BuildContext(typeRegistry, wiring);

        return makeExecutableSchemaImpl(buildCtx);
    }

    private GraphQLSchema makeExecutableSchemaImpl(BuildContext buildCtx) {

        SchemaDefinition schemaDefinition = buildCtx.getSchemaDefinition();
        List<OperationTypeDefinition> operationTypes = schemaDefinition.getOperationTypeDefinitions();

        // pre-flight checked via checker
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        OperationTypeDefinition queryOp =null;;
        for(OperationTypeDefinition op:operationTypes){
            if("query".equals(op.getName()))
                queryOp =op;
        }
        // Optional<OperationTypeDefinition> mutationOp = operationTypes.stream().filter(op -> "mutation".equals(op.getName())).findFirst();
        // Optional<OperationTypeDefinition> subscriptionOp = operationTypes.stream().filter(op -> "subscription".equals(op.getName())).findFirst();

        GraphQLObjectType query = buildOperation(buildCtx, queryOp);
        // GraphQLObjectType mutation;
        // GraphQLObjectType subscription;

        GraphQLSchema.Builder schemaBuilder = GraphQLSchema
                .newSchema()
                .query(query);

        // if (mutationOp.isPresent()) {
        //     mutation = buildOperation(buildCtx, mutationOp.get());
        //     schemaBuilder.mutation(mutation);
        // }
        // if (subscriptionOp.isPresent()) {
        //     subscription = buildOperation(buildCtx, subscriptionOp.get());
        //     schemaBuilder.subscription(subscription);
        // }
        return schemaBuilder.build();
    }

    private GraphQLObjectType buildOperation(BuildContext buildCtx, OperationTypeDefinition operation) {
        Type type = operation.getType();

        return buildOutputType(buildCtx, type);
    }


    /**
     * This is the main recursive spot that builds out the various forms of Output types
     *
     * @param buildCtx the context we need to work out what we are doing
     * @param rawType  the type to be built
     * @return an output type
     */
    @SuppressWarnings("unchecked")
    private <T extends GraphQLOutputType> T buildOutputType(BuildContext buildCtx, Type rawType) {

        TypeDefinition typeDefinition = buildCtx.getTypeDefinition(rawType);
        TypeInfo typeInfo = TypeInfo.typeInfo(rawType);

        GraphQLOutputType outputType = buildCtx.hasOutputType(typeDefinition);
        if (outputType != null) {
            return typeInfo.decorate(outputType);
        }

        if (buildCtx.stackContains(typeInfo)) {
            // we have circled around so put in a type reference and fix it up later
            // otherwise we will go into an infinite loop
            return typeInfo.decorate(new GraphQLTypeReference(typeInfo.getName()));
        }

        buildCtx.push(typeInfo);

        if (typeDefinition instanceof ObjectTypeDefinition) {
            outputType = buildObjectType(buildCtx, (ObjectTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof InterfaceTypeDefinition) {
            outputType = buildInterfaceType(buildCtx, (InterfaceTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof UnionTypeDefinition) {
            outputType = buildUnionType(buildCtx, (UnionTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof EnumTypeDefinition) {
            outputType = buildEnumType((EnumTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof ScalarTypeDefinition) {
            outputType = buildScalar(buildCtx, (ScalarTypeDefinition) typeDefinition);
        } else {
            // typeDefinition is not a valid output type
            throw new NotAnOutputTypeError(typeDefinition);
        }

        buildCtx.put(outputType);
        buildCtx.pop();
        return (T) typeInfo.decorate(outputType);
    }

    private GraphQLInputType buildInputType(BuildContext buildCtx, Type rawType) {

        TypeDefinition typeDefinition = buildCtx.getTypeDefinition(rawType);
        TypeInfo typeInfo = TypeInfo.typeInfo(rawType);
        GraphQLInputType inputType = buildCtx.hasInputType(typeDefinition);
        if (inputType != null) {
            return typeInfo.decorate(inputType);
        }

        if (buildCtx.stackContains(typeInfo)) {
            // we have circled around so put in a type reference and fix it later
            return typeInfo.decorate(new GraphQLTypeReference(typeInfo.getName()));
        }

        buildCtx.push(typeInfo);

        if (typeDefinition instanceof InputObjectTypeDefinition) {
            inputType = buildInputObjectType(buildCtx, (InputObjectTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof EnumTypeDefinition) {
            inputType = buildEnumType((EnumTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof ScalarTypeDefinition) {
            inputType = buildScalar(buildCtx, (ScalarTypeDefinition) typeDefinition);
        } else {
            // typeDefinition is not a valid InputType
            throw new NotAnInputTypeError(typeDefinition);
        }

        buildCtx.put(inputType);
        buildCtx.pop();
        return typeInfo.decorate(inputType);
    }

    private GraphQLObjectType buildObjectType(BuildContext buildCtx, ObjectTypeDefinition typeDefinition) {

        GraphQLObjectType.Builder builder = GraphQLObjectType.newObject();
        builder.name(typeDefinition.getName());
        builder.description(buildDescription(typeDefinition));

        List<TypeExtensionDefinition> typeExtensions = getTypeExtensionsOf(typeDefinition, buildCtx);

        buildObjectTypeFields(buildCtx, typeDefinition, builder, typeExtensions);

        buildObjectTypeInterfaces(buildCtx, typeDefinition, builder, typeExtensions);

        return builder.build();
    }

    private void buildObjectTypeFields(BuildContext buildCtx, ObjectTypeDefinition typeDefinition, GraphQLObjectType.Builder builder, List<TypeExtensionDefinition> typeExtensions) {
        Map<String, GraphQLFieldDefinition> fieldDefinitions = new LinkedHashMap<String, GraphQLFieldDefinition>();

        //JAVA1.8TOPORT
        // typeDefinition.getFieldDefinitions().forEach(fieldDef -> {
        //     GraphQLFieldDefinition newFieldDefinition = buildField(buildCtx, typeDefinition, fieldDef);
        //     fieldDefinitions.put(newFieldDefinition.getName(), newFieldDefinition);
        // });

        List<FieldDefinition> fieldDefs=typeDefinition.getFieldDefinitions();
        for(FieldDefinition fieldDef: fieldDefs){
            GraphQLFieldDefinition newFieldDefinition = buildField(buildCtx, typeDefinition, fieldDef);
            fieldDefinitions.put(newFieldDefinition.getName(), newFieldDefinition);            
        }


        // an object consists of the fields it gets from its definition AND its type extensions
        //JAVA1.8TOPORT
        // typeExtensions.forEach(typeExt -> typeExt.getFieldDefinitions().forEach(fieldDef -> {
        //     GraphQLFieldDefinition newFieldDefinition = buildField(buildCtx, typeDefinition, fieldDef);
        //     //
        //     // de-dupe here - pre-flight checks ensure all dupes are of the same type
        //     if (!fieldDefinitions.containsKey(newFieldDefinition.getName())) {
        //         fieldDefinitions.put(newFieldDefinition.getName(), newFieldDefinition);
        //     }
        // }));

        for(TypeExtensionDefinition typeExt:typeExtensions){
            List<FieldDefinition>  fieldsDefs= typeExt.getFieldDefinitions();
            for(FieldDefinition fieldDef: fieldsDefs){
                GraphQLFieldDefinition newFieldDefinition = buildField(buildCtx, typeDefinition, fieldDef);
                //
                // de-dupe here - pre-flight checks ensure all dupes are of the same type
                if (!fieldDefinitions.containsKey(newFieldDefinition.getName())) {
                    fieldDefinitions.put(newFieldDefinition.getName(), newFieldDefinition);
                }                
            }

        }
        //JAVA1.8TOPORT
        //fieldDefinitions.values().forEach(builder::field);
        Collection<GraphQLFieldDefinition> fieldDefValuesColl =fieldDefinitions.values();
        Iterator<GraphQLFieldDefinition> fieldDefValueIterator=fieldDefValuesColl.iterator();
        while(fieldDefValueIterator.hasNext()){
            GraphQLFieldDefinition fieldDefValue= fieldDefValueIterator.next();
            builder.field(fieldDefValue);
        }
    }

    private void buildObjectTypeInterfaces(BuildContext buildCtx, ObjectTypeDefinition typeDefinition, GraphQLObjectType.Builder builder, List<TypeExtensionDefinition> typeExtensions) {
        Map<String, GraphQLInterfaceType> interfaces = new LinkedHashMap<String, GraphQLInterfaceType>();
        // typeDefinition.getImplements().forEach(type -> {
        //     GraphQLInterfaceType newInterfaceType = buildOutputType(buildCtx, type);
        //     interfaces.put(newInterfaceType.getName(), newInterfaceType);
        // });
        List<Type> lt= typeDefinition.getImplements();
        for(Type type: lt){
        //JAVA1.8TOPORT forEach
        GraphQLInterfaceType newInterfaceType = buildOutputType(buildCtx, type);
        interfaces.put(newInterfaceType.getName(), newInterfaceType);
        }

        // an object consists of the interfaces it gets from its definition AND its type extensions
        //JAVA1.8TOPORT forEach
        // typeExtensions.forEach(typeExt -> typeExt.getImplements().forEach(type -> {
        //     GraphQLInterfaceType interfaceType = buildOutputType(buildCtx, type);
        //     //
        //     // de-dupe here - pre-flight checks ensure all dupes are of the same type
        //     if (!interfaces.containsKey(interfaceType.getName())) {
        //         interfaces.put(interfaceType.getName(), interfaceType);
        //     }
        // }));
        //List<TypeExtensionDefinition> typeExt= typeExtensions.
        for(TypeExtensionDefinition typeExt: typeExtensions){
            List<Type> types=typeExt.getImplements();
            for(Type type: types){
            GraphQLInterfaceType interfaceType = buildOutputType(buildCtx, type);
            //
            // de-dupe here - pre-flight checks ensure all dupes are of the same type
            if (!interfaces.containsKey(interfaceType.getName())) {
                interfaces.put(interfaceType.getName(), interfaceType);
                }   
            }
        }

        //interfaces.values().forEach(builder::withInterface);

        Collection<GraphQLInterfaceType> interfaceValues =interfaces.values();
         Iterator<GraphQLInterfaceType>  interfaceIterator=interfaceValues.iterator();
         while(interfaceIterator.hasNext()){
            GraphQLInterfaceType gqlInterfaceType =interfaceIterator.next();
            builder.withInterface(gqlInterfaceType);
         }
    }

    private List<TypeExtensionDefinition> getTypeExtensionsOf(ObjectTypeDefinition objectTypeDefinition, BuildContext buildCtx) {
        List<TypeExtensionDefinition> typeExtensionDefinitions = buildCtx.typeRegistry.typeExtensions().get(objectTypeDefinition.getName());
        return (List<TypeExtensionDefinition>) (typeExtensionDefinitions == null ? Collections.emptyList() : typeExtensionDefinitions);
    }

    private GraphQLInterfaceType buildInterfaceType(BuildContext buildCtx, InterfaceTypeDefinition typeDefinition) {
        GraphQLInterfaceType.Builder builder = GraphQLInterfaceType.newInterface();
        builder.name(typeDefinition.getName());
        builder.description(buildDescription(typeDefinition));

        builder.typeResolver(getTypeResolverForInterface(buildCtx, typeDefinition));
        //JAVA1.8TOPORT
        // typeDefinition.getFieldDefinitions().forEach(fieldDef ->
        //         builder.field(buildField(buildCtx, typeDefinition, fieldDef)));

        List<FieldDefinition> fieldDefinitions= typeDefinition.getFieldDefinitions();
        for(FieldDefinition fieldDef: fieldDefinitions){
            builder.field(buildField(buildCtx, typeDefinition, fieldDef));
        }
    
        return builder.build();
    }

    private GraphQLUnionType buildUnionType(BuildContext buildCtx, UnionTypeDefinition typeDefinition) {
        GraphQLUnionType.Builder builder = GraphQLUnionType.newUnionType();
        builder.name(typeDefinition.getName());
        builder.description(buildDescription(typeDefinition));
        builder.typeResolver(getTypeResolverForUnion(buildCtx, typeDefinition));

        List<Type> mts=typeDefinition.getMemberTypes();
        for(Type mt:mts){
            GraphQLOutputType outputType = buildOutputType(buildCtx, mt);
            if (outputType instanceof GraphQLTypeReference) {
                //builder.possibleTypes((GraphQLTypeReference) outputType);
            } else {
                builder.possibleType((GraphQLObjectType) outputType);
            }            

        }
        return builder.build();
    }

    private GraphQLEnumType buildEnumType(EnumTypeDefinition typeDefinition) {
        GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum();
        builder.name(typeDefinition.getName());
        builder.description(buildDescription(typeDefinition));

        //typeDefinition.getEnumValueDefinitions().forEach(evd -> builder.value(evd.getName()));
        List<EnumValueDefinition> evds=typeDefinition.getEnumValueDefinitions();
        for(EnumValueDefinition evd: evds){
            builder.value(evd.getName());
        }
        return builder.build();
    }

    private GraphQLScalarType buildScalar(BuildContext buildCtx, ScalarTypeDefinition typeDefinition) {
        return buildCtx.getWiring().getScalars().get(typeDefinition.getName());
    }

    private GraphQLFieldDefinition buildField(BuildContext buildCtx, TypeDefinition parentType, FieldDefinition fieldDef) {
        GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition.newFieldDefinition();
        builder.name(fieldDef.getName());
        builder.description(buildDescription(fieldDef));

        builder.dataFetcher(buildDataFetcher(buildCtx, parentType, fieldDef));

        // fieldDef.getInputValueDefinitions().forEach(inputValueDefinition ->
        //         builder.argument(buildArgument(buildCtx, inputValueDefinition)));

        List<InputValueDefinition> inputValueDefinitions=fieldDef.getInputValueDefinitions();
        for(InputValueDefinition inputValueDefinition: inputValueDefinitions){
            builder.argument(buildArgument(buildCtx, inputValueDefinition));
        }
        GraphQLOutputType outputType = buildOutputType(buildCtx, fieldDef.getType());
        builder.type(outputType);

        return builder.build();
    }

    private DataFetcher buildDataFetcher(BuildContext buildCtx, TypeDefinition parentType, FieldDefinition fieldDef) {
        String fieldName = fieldDef.getName();
        TypeDefinitionRegistry typeRegistry = buildCtx.getTypeRegistry();
        RuntimeWiring wiring = buildCtx.getWiring();
        WiringFactory wiringFactory = wiring.getWiringFactory();

        DataFetcher dataFetcher;
        if (wiringFactory.providesDataFetcher(typeRegistry, fieldDef)) {
            dataFetcher = wiringFactory.getDataFetcher(typeRegistry, fieldDef);
            assertNotNull(dataFetcher, "The WiringFactory indicated it provides a data fetcher but then returned null");
        } else {
            dataFetcher = wiring.getDataFetcherForType(parentType.getName()).get(fieldName);
            if (dataFetcher == null) {
                //
                // in the future we could support FieldDateFetcher but we would need a way to indicate that in the schema spec
                // perhaps by a directive
                dataFetcher = new PropertyDataFetcher(fieldName);
            }
        }
        return dataFetcher;
    }

    private GraphQLInputObjectType buildInputObjectType(BuildContext buildCtx, InputObjectTypeDefinition typeDefinition) {
        GraphQLInputObjectType.Builder builder = GraphQLInputObjectType.newInputObject();
        builder.name(typeDefinition.getName());
        builder.description(buildDescription(typeDefinition));

        // typeDefinition.getInputValueDefinitions().forEach(fieldDef ->
        //         builder.field(buildInputField(buildCtx, fieldDef)));

        List<InputValueDefinition> fieldDefs=   typeDefinition.getInputValueDefinitions();
        for(InputValueDefinition fieldDef : fieldDefs){
            builder.field(buildInputField(buildCtx, fieldDef));
        }    
        return builder.build();
    }

    private GraphQLInputObjectField buildInputField(BuildContext buildCtx, InputValueDefinition fieldDef) {
        GraphQLInputObjectField.Builder fieldBuilder = GraphQLInputObjectField.newInputObjectField();
        fieldBuilder.name(fieldDef.getName());
        fieldBuilder.description(buildDescription(fieldDef));

        fieldBuilder.type(buildInputType(buildCtx, fieldDef.getType()));
        fieldBuilder.defaultValue(buildValue(fieldDef.getDefaultValue()));

        return fieldBuilder.build();
    }


    private GraphQLArgument buildArgument(BuildContext buildCtx, InputValueDefinition valueDefinition) {
        GraphQLArgument.Builder builder = GraphQLArgument.newArgument();
        builder.name(valueDefinition.getName());
        builder.description(buildDescription(valueDefinition));

        builder.type(buildInputType(buildCtx, valueDefinition.getType()));
        builder.defaultValue(buildValue(valueDefinition.getDefaultValue()));

        return builder.build();
    }

    private Object buildValue(Value value) {
        Object result = null;
        if (value instanceof IntValue) {
            result = ((IntValue) value).getValue();
        } else if (value instanceof FloatValue) {
            result = ((FloatValue) value).getValue();
        } else if (value instanceof StringValue) {
            result = ((StringValue) value).getValue();
        } else if (value instanceof EnumValue) {
            result = ((EnumValue) value).getName();
        } else if (value instanceof BooleanValue) {
            result = ((BooleanValue) value).isValue();
        } else if (value instanceof ArrayValue) {
            ArrayValue arrayValue = (ArrayValue) value;
            //result = arrayValue.getValues().stream().map(this::buildValue).toArray();
            List<Value> values=arrayValue.getValues();
            for(Value val:values){
                this.buildValue(val);
            }

        } else if (value instanceof ObjectValue) {
            result = buildObjectValue((ObjectValue) value);
        }
        return result;

    }

    private Object buildObjectValue(ObjectValue defaultValue) {
        HashMap<String, Object> map = new LinkedHashMap<String, Object>();
        List<ObjectField> objectFields = defaultValue.getObjectFields();
        for(ObjectField of: objectFields){
            map.put(of.getName(), buildValue(of.getValue()));
        }
        return map;
    }

    private TypeResolver getTypeResolverForUnion(BuildContext buildCtx, UnionTypeDefinition unionType) {
        TypeDefinitionRegistry typeRegistry = buildCtx.getTypeRegistry();
        RuntimeWiring wiring = buildCtx.getWiring();
        WiringFactory wiringFactory = wiring.getWiringFactory();

        TypeResolver typeResolver;
        if (wiringFactory.providesTypeResolver(typeRegistry, unionType)) {
            typeResolver = wiringFactory.getTypeResolver(typeRegistry, unionType);
            assertNotNull(typeResolver, "The WiringFactory indicated it provides a type resolver but then returned null");

        } else {
            typeResolver = wiring.getTypeResolvers().get(unionType.getName());
            if (typeResolver == null) {
                // this really should be checked earlier via a pre-flight check
                typeResolver = new TypeResolverProxy();
            }
        }

        return typeResolver;
    }

    private TypeResolver getTypeResolverForInterface(BuildContext buildCtx, InterfaceTypeDefinition interfaceType) {
        TypeDefinitionRegistry typeRegistry = buildCtx.getTypeRegistry();
        RuntimeWiring wiring = buildCtx.getWiring();
        WiringFactory wiringFactory = wiring.getWiringFactory();

        TypeResolver typeResolver;
        if (wiringFactory.providesTypeResolver(typeRegistry, interfaceType)) {
            typeResolver = wiringFactory.getTypeResolver(typeRegistry, interfaceType);
            assertNotNull(typeResolver, "The WiringFactory indicated it provides a type resolver but then returned null");

        } else {
            typeResolver = wiring.getTypeResolvers().get(interfaceType.getName());
            if (typeResolver == null) {
                // this really should be checked earlier via a pre-flight check
                typeResolver = new TypeResolverProxy();
            }
        }

        return typeResolver;
    }


    private String buildDescription(Node node) {
        StringBuilder sb = new StringBuilder();
//         List<Comment> comments =  node.getComments();//new ArrayList<Comment>()
//         for (int i = 0; i < comments.size(); i++) {
//             if (i > 0) {
//                 sb.append("\n");
//             }
//             sb.append(comments.get(i).getContent().trim());
//         }
        return sb.toString();
    }
    
    
    private String buildDescriptionForInputValue(AbstractNode node) {
        StringBuilder sb = new StringBuilder();
         List<Comment> comments =  node.getComments();//new ArrayList<Comment>()
         for (int i = 0; i < comments.size(); i++) {
             if (i > 0) {
                 sb.append("\n");
             }
             sb.append(comments.get(i).getContent().trim());
         }
        return sb.toString();
    }
}