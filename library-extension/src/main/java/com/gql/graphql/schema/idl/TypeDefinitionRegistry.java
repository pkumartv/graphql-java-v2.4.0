package com.gql.graphql.schema.idl;

import graphql.GraphQLError;
import graphql.language.Definition;
import graphql.language.ScalarTypeDefinition;
import graphql.language.SchemaDefinition;
import graphql.language.Type;
import graphql.language.TypeDefinition;
import graphql.language.TypeExtensionDefinition;
import graphql.language.TypeName;
import com.gql.graphql.schema.idl.errors.SchemaProblem;
import com.gql.graphql.schema.idl.errors.SchemaRedefinitionError;
import com.gql.graphql.schema.idl.errors.TypeRedefinitionError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


//import java.util.Optional;
//import static java.util.Optional.ofNullable;

/**
 * A {@link TypeDefinitionRegistry} contains the set of type definitions that come from compiling
 * a graphql schema definition file via {@link SchemaParser#parse(String)}
 */
public class TypeDefinitionRegistry {

    private final Map<String, ScalarTypeDefinition> scalarTypes = new LinkedHashMap<String, ScalarTypeDefinition>();
    private final Map<String, List<TypeExtensionDefinition>> typeExtensions = new LinkedHashMap<String, List<TypeExtensionDefinition>>();
    private final Map<String, TypeDefinition> types = new LinkedHashMap<String, TypeDefinition>();
    private SchemaDefinition schema;

    /**
     * This will merge these type registries together and return this one
     *
     * @param typeRegistry the registry to be merged into this one
     *
     * @return this registry
     *
     * @throws SchemaProblem if there are problems merging the types such as redefinitions
     */
    public TypeDefinitionRegistry merge(TypeDefinitionRegistry  typeRegistry) throws SchemaProblem {
        List<GraphQLError> errors = new ArrayList<GraphQLError>();

        Map<String, TypeDefinition> tempTypes = new LinkedHashMap<String, TypeDefinition>();
        //JAVA1.8TOPORT ifPresent
        //typeRegistry.types.values().forEach(newEntry -> define(this.types, tempTypes, newEntry));//.ifPresent(errors::add));

       
        //Map<String, TypeDefinition> types =typeRegistry.types;
        Collection<TypeDefinition> typesCollection= typeRegistry.types.values();
        Iterator<TypeDefinition> typeIterator= typesCollection.iterator();
        while(typeIterator.hasNext()){
            TypeDefinition newEntry= typeIterator.next();
            define(this.types, tempTypes, newEntry);
        }

        Map<String, ScalarTypeDefinition> tempScalarTypes = new LinkedHashMap<String, ScalarTypeDefinition>();
        //JAVA1.8TOPORT ifPresent
        //typeRegistry.scalarTypes.values().forEach(newEntry -> define(this.scalarTypes, tempScalarTypes, newEntry));//.ifPresent(errors::add));

        Collection<ScalarTypeDefinition> scalarTypesCollection= typeRegistry.scalarTypes.values();
        Iterator<ScalarTypeDefinition> scalarIterator= scalarTypesCollection.iterator();
        while(scalarIterator.hasNext()){
            ScalarTypeDefinition newEntry= scalarIterator.next();
            define(this.scalarTypes, tempScalarTypes, newEntry);
        }

        if (typeRegistry.schema != null && this.schema != null) {
            errors.add(new SchemaRedefinitionError(this.schema, typeRegistry.schema));
        }

        if (!errors.isEmpty()) {
            throw new SchemaProblem(errors);
        }

        if (this.schema == null) {
            // ensure schema is not overwritten by merge
            this.schema = typeRegistry.schema;
        }

        // ok commit to the merge
        this.types.putAll(tempTypes);
        this.scalarTypes.putAll(tempScalarTypes);

        // merge type extensions since they can be redefined by design
        // typeRegistry.typeExtensions.entrySet().forEach(newEntry -> {
        //     //JAVA1.8TOPORT computeIfAbsent
        //     List<TypeExtensionDefinition> currentList = this.typeExtensions
        //             .computeIfAbsent(newEntry.getKey(), k -> new ArrayList<>());
        //     currentList.addAll(newEntry.getValue());
        // });

        Set<Entry<String, List<TypeExtensionDefinition>>> typeExtensionsSet=typeRegistry.typeExtensions.entrySet();
        for(Entry<String, List<TypeExtensionDefinition>> entry: typeExtensionsSet){

            List<TypeExtensionDefinition> currentList = this.typeExtensions.get(entry.getKey());
            if(currentList==null){
                currentList= new ArrayList<TypeExtensionDefinition>();
            }
            currentList.addAll(entry.getValue()); 
            this.typeExtensions.put(entry.getKey(), currentList);           

        }   
        return this;
    }

    /**
     * Adds a definition to the registry
     *
     * @param definition the definition to add
     *
     * @return an optional error
     */
    // public Optional<GraphQLError> add(Definition definition) {
    //     if (definition instanceof TypeExtensionDefinition) {
    //         TypeExtensionDefinition newEntry = (TypeExtensionDefinition) definition;
    //         return defineExt(typeExtensions, newEntry);
    //     } else if (definition instanceof ScalarTypeDefinition) {
    //         ScalarTypeDefinition newEntry = (ScalarTypeDefinition) definition;
    //         return define(scalarTypes, scalarTypes, newEntry);
    //     } else if (definition instanceof TypeDefinition) {
    //         TypeDefinition newEntry = (TypeDefinition) definition;
    //         return define(types, types, newEntry);
    //     } else if (definition instanceof SchemaDefinition) {
    //         SchemaDefinition newSchema = (SchemaDefinition) definition;
    //         if (schema != null) {
    //             return Optional.of(new SchemaRedefinitionError(this.schema, newSchema));
    //         } else {
    //             schema = newSchema;
    //         }
    //     }
    //     return Optional.empty();
    // }

    public GraphQLError add(Definition definition) {
        if (definition instanceof TypeExtensionDefinition) {
            TypeExtensionDefinition newEntry = (TypeExtensionDefinition) definition;
            return defineExt(typeExtensions, newEntry);
        } else if (definition instanceof ScalarTypeDefinition) {
            ScalarTypeDefinition newEntry = (ScalarTypeDefinition) definition;
            return define(scalarTypes, scalarTypes, newEntry);
        } else if (definition instanceof TypeDefinition) {
            TypeDefinition newEntry = (TypeDefinition) definition;
            return define(types, types, newEntry);
        } else if (definition instanceof SchemaDefinition) {
            SchemaDefinition newSchema = (SchemaDefinition) definition;
            if (schema != null) {
                return new SchemaRedefinitionError(this.schema, newSchema);
            } else {
                schema = newSchema;
            }
        }
        return null;
    }

    

    // private <T extends TypeDefinition> Optional<GraphQLError> define(Map<String, T> source, Map<String, T> target, T newEntry) {
    //     String name = newEntry.getName();

    //     T olderEntry = source.get(name);
    //     if (olderEntry != null) {
    //         return Optional.of(handleReDefinition(olderEntry, newEntry));
    //     } else {
    //         target.put(name, newEntry);
    //     }
    //     return Optional.empty();
    // }

    private <T extends TypeDefinition> GraphQLError define(Map<String, T> source, Map<String, T> target, T newEntry) {
        String name = newEntry.getName();

        T olderEntry = source.get(name);
        if (olderEntry != null) {
            return handleReDefinition(olderEntry, newEntry);
        } else {
            target.put(name, newEntry);
        }
        return null;
    }

    //JAVA1.8TOPORT Optional
    // private Optional<GraphQLError> defineExt(Map<String, List<TypeExtensionDefinition>> typeExtensions, TypeExtensionDefinition newEntry) {
    //     //JAVA1.8TOPORT computeIfAbsent
    //     List<TypeExtensionDefinition> currentList = typeExtensions.computeIfAbsent(newEntry.getName(), k -> new ArrayList<>());
    //     currentList.add(newEntry);
    //     return Optional.empty();
    // }


    private GraphQLError defineExt(Map<String, List<TypeExtensionDefinition>> typeExtensions, TypeExtensionDefinition newEntry) {
        //JAVA1.8TOPORT computeIfAbsent
        //List<TypeExtensionDefinition> currentList = typeExtensions.computeIfAbsent(newEntry.getName(), k -> new ArrayList<>());
        List<TypeExtensionDefinition> currentList=typeExtensions.get(newEntry.getName());
        if(currentList==null)
            currentList= new ArrayList<TypeExtensionDefinition>();

        currentList.add(newEntry);
        typeExtensions.put(newEntry.getName(), currentList);
        return null;
    }

    public Map<String, TypeDefinition> types() {
        return new LinkedHashMap<String, TypeDefinition>(types);
    }

    public Map<String, ScalarTypeDefinition> scalars() {
        LinkedHashMap<String, ScalarTypeDefinition> scalars = new LinkedHashMap<String, ScalarTypeDefinition>(ScalarInfo.STANDARD_SCALAR_DEFINITIONS);
        scalars.putAll(scalarTypes);
        return scalars;
    }

    public Map<String, List<TypeExtensionDefinition>> typeExtensions() {
        return new LinkedHashMap<String, List<TypeExtensionDefinition>>(typeExtensions);
    }

    // public Optional<SchemaDefinition> schemaDefinition() {
    //     return ofNullable(schema);
    // }

    public SchemaDefinition schemaDefinition() {
        return schema;
    }    

     private GraphQLError handleReDefinition(TypeDefinition oldEntry, TypeDefinition newEntry) {
        return new TypeRedefinitionError(newEntry, oldEntry);
    }

    public boolean hasType(TypeName typeName) {
        String name = typeName.getName();
        return types.containsKey(name) || ScalarInfo.STANDARD_SCALAR_DEFINITIONS.containsKey(name) || scalarTypes.containsKey(name) || typeExtensions.containsKey(name);
    }

    // public Optional<TypeDefinition> getType(Type type) {
    //     String typeName = TypeInfo.typeInfo(type).getName();
    //     return getType(typeName);
    // }

    public TypeDefinition getType(Type type) {
        String typeName = TypeInfo.typeInfo(type).getName();
        return getType(typeName);
    }    

    // public Optional<TypeDefinition> getType(String typeName) {
    //     TypeDefinition typeDefinition = types.get(typeName);
    //     if (typeDefinition != null) {
    //         return Optional.of(typeDefinition);
    //     }
    //     typeDefinition = scalars().get(typeName);
    //     if (typeDefinition != null) {
    //         return Optional.of(typeDefinition);
    //     }
    //     return Optional.empty();
    // }

    public TypeDefinition getType(String typeName) {
        TypeDefinition typeDefinition = types.get(typeName);
        if (typeDefinition != null) {
            return typeDefinition;
        }
        typeDefinition = scalars().get(typeName);
        if (typeDefinition != null) {
            return typeDefinition;
        }
        return null;
    }    
}
