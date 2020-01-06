package com.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.stream.StreamSource;

import com.common.def.ApiVersion;
import com.exception.RawResponseAwareException;
import org.apache.commons.collections.CollectionUtils;
import org.reflections.Reflections;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTypeResolverBuilder;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.google.common.collect.Sets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SchemaVersionSupportHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    private static final Logger LOG = LogManager.getLogger(SchemaVersionSupportHttpMessageConverter.class);
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    private static final String ANNOTATION_DEFAULT_VALUE = "##default";

    protected Map<Class<?>, ContextPair> classToContextPairMap;
    protected Map<ApiVersion, ContextPair> contextKeyToContextPairMap;
    protected Map<Class<?>, Class<?>> customSerializationsRegistry;
    protected TypeFactory typeFactory;

    private SimpleDateFormat dateFormat;

    public static final Set<String> DEFAULT_CONTEXT_ROOTS =
            Collections.unmodifiableSet(Sets.newHashSet("com.dibs.service.v1", "com.dibs.service.v2", "com.dibs.service.v3", "com.dibs.service.v4"));

    private static final List<Class<?>> DEFAULT_CLASSES;

    private static final String DIBS_SERVICE_PACKAGE = "com.dibs.service";

    static {

        List<Class<?>> defaultClasses = new ArrayList<Class<?>>();
        defaultClasses.add(ArrayList.class);
        defaultClasses.add(LinkedList.class);
        defaultClasses.add(HashMap.class);
        defaultClasses.add(LinkedHashMap.class);

        DEFAULT_CLASSES = new ArrayList<Class<?>>(defaultClasses);
    }

    private final Set<String> contextRoots;

    private class ContextPair {
        public JAXBContext jaxbContext;
        public ObjectMapper jacksonMapper;

        public ContextPair(JAXBContext jaxbContext, ObjectMapper jacksonMapper) {
            this.jaxbContext = jaxbContext;
            this.jacksonMapper = jacksonMapper;
        }
    }

    protected SchemaVersionSupportHttpMessageConverter() {
        this(null, null);
    }

    protected SchemaVersionSupportHttpMessageConverter(SimpleDateFormat dateFormat, Set<String> contextRoots) {
        super(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_XHTML_XML);

        if (CollectionUtils.isEmpty(contextRoots)) {
            contextRoots = DEFAULT_CONTEXT_ROOTS;
        }

        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat(System.getProperty("json.date.format", DEFAULT_DATE_FORMAT));
        }

        this.contextRoots = contextRoots;
        this.dateFormat = dateFormat;
        this.customSerializationsRegistry = new HashMap<>();
        initialize();
    }

    public void setDateFormat(SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    @Override
    protected boolean supports(Class<? extends Object> clazz) {
        return true; // This could probably be upgraded to a smarter check
    }

    @Override
    protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        if (String.class == clazz) {
            return readAsString(inputMessage);
        }

        ContextPair contextPair = getContextPairForClass(clazz);
        return readObjectForSchema(contextPair, clazz, inputMessage);
    }

    private String readAsString(HttpInputMessage inputMessage) throws IOException {
        String text = null;
        try (Scanner scanner = new Scanner(inputMessage.getBody(), StandardCharsets.UTF_8.name())) {
            text = scanner.useDelimiter("\\A").next();
        }
        return text;
    }

    public Object read(ApiVersion contextKey, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return read(contextKey, null, inputMessage);
    }

    public Object read(ApiVersion contextKey, Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        ContextPair contextPair = contextKeyToContextPairMap.get(contextKey);
        return readObjectForSchema(contextPair, clazz, inputMessage);
    }

    private Object readObjectForSchema(ContextPair contextPair, Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        MediaType contentType = inputMessage.getHeaders().getContentType();

        /*
         * if there is a custom internal serialization class mapped to this class, use it instead.
         */
        Class<?> customSerializationClass = customSerializationsRegistry.get(clazz);
        clazz = customSerializationClass == null ? clazz : customSerializationClass;

        if (inputMessage instanceof DelegatingHttpInputMessage) {
            clazz = ((DelegatingHttpInputMessage) inputMessage).getTrueTargetType();
        }

        if (MediaType.APPLICATION_JSON.includes(contentType)) {
            ObjectMapper mapper = contextPair.jacksonMapper;

            if (clazz == null) {
                throw new RuntimeException("Return class required for application/json media types");
            }
            return mapper.readValue(inputMessage.getBody(), clazz);
        } else if (MediaType.APPLICATION_XML.includes(contentType)) {
            try {
                Unmarshaller unmarshaller = createUnmarshaller(contextPair.jaxbContext);

                if (clazz == null || clazz.isInterface()) {
                    return unmarshaller.unmarshal(new StreamSource(inputMessage.getBody()));
                } else {
                    return unmarshaller.unmarshal(new StreamSource(inputMessage.getBody()), clazz).getValue();
                }
            } catch (JAXBException e) {
                throw new RawResponseAwareException(e, inputMessage.getBody());
            }
        } else {
            throw new HttpMessageNotReadableException("Invalid content type");
        }
    }

    public void write(ApiVersion contextKey, Object t, MediaType mediaType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        ContextPair contextPair = contextKeyToContextPairMap.get(contextKey);
        outputMessage.getHeaders().setContentType(mediaType);
        writeObjectForSchema(contextPair, t, outputMessage);
    }

    @Override
    protected void writeInternal(Object t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        Class<?> versionedClass = t.getClass();

        ContextPair contextPair = getContextPairForClass(versionedClass);

        writeObjectForSchema(contextPair, t, outputMessage);
    }

    private void writeObjectForSchema(ContextPair contextPair, Object t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        MediaType contentType = outputMessage.getHeaders().getContentType();

        if (MediaType.APPLICATION_JSON.includes(contentType)) {

            ObjectMapper mapper = contextPair.jacksonMapper;
            mapper.writeValue(outputMessage.getBody(), t);

        } else if (MediaType.APPLICATION_XML.includes(contentType) || MediaType.APPLICATION_XHTML_XML.includes(contentType)) {
            try {
                Marshaller marshaller = createMarshaller(contextPair.jaxbContext);
                marshaller.marshal(t, outputMessage.getBody());
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOG.error(outputMessage.getBody());
            throw new HttpMessageNotReadableException("Invalid content type");
        }
    }

    private void initialize() {
        if (contextKeyToContextPairMap == null) {
            classToContextPairMap = new HashMap<>();
            contextKeyToContextPairMap = new HashMap<>();
            typeFactory = TypeFactory.defaultInstance();
            createContexts(contextRoots);
        }
    }

    private ContextPair getContextPairForClass(Class<?> clazz) {
        ContextPair contextPair = classToContextPairMap.get(clazz);

        if (contextPair == null) {
            ApiVersion contextKey = ApiVersion.fromClass(clazz);
            contextPair = contextKeyToContextPairMap.get(contextKey);
            classToContextPairMap.put(clazz, contextPair);
        }

        return contextPair;
    }

    protected Marshaller createMarshaller(JAXBContext jaxbContext) throws JAXBException {
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
        jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        return jaxbMarshaller;
    }

    protected Unmarshaller createUnmarshaller(JAXBContext jaxbContext) throws JAXBException {
        return jaxbContext.createUnmarshaller();
    }

    protected void createContexts(Set<String> packageNames) {
        Map<ApiVersion, Set<Class<?>>> rootClasses = new HashMap<>();

        for (String packageName : packageNames) {
            processContextRoot(packageName, rootClasses);
        }

        /*
         * Want to gaurantee that no version is at least initialized to a blank
         * context since it is the default in the case that a class is passed in which doesnt
         * map to a specific version. If it is null, then we null pointer when attempting serialization
         */
        if (!rootClasses.containsKey(ApiVersion.NO_VERSION)) {
            rootClasses.put(ApiVersion.NO_VERSION, new HashSet<Class<?>>());
        }

        for (Entry<ApiVersion, Set<Class<?>>> entry : rootClasses.entrySet()) {
            createContext(entry.getKey(), entry.getValue());
        }
    }

    private void createContext(ApiVersion contextKey, Set<Class<?>> rootClasses) {
        try {
            //json object mapper
            ObjectMapper jsonObjectMapper = createJacksonMapperForContext(contextKey, rootClasses);

            //jaxb xml context
            JAXBContext jaxbContext = JAXBContext.newInstance(rootClasses.toArray(new Class<?>[rootClasses.size()]));

            contextKeyToContextPairMap.put(contextKey, new ContextPair(jaxbContext, jsonObjectMapper));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectMapper createJacksonMapperForContext(ApiVersion contextKey, Set<Class<?>> rootClasses) {
        JaxbAnnotationIntrospector jaxb = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        JacksonAnnotationIntrospector jackson = new JacksonAnnotationIntrospector();

        AnnotationIntrospectorPair pair = new AnnotationIntrospectorPair(jackson, jaxb);

        Collection<NamedType> namedTypes = getNamedTypesForRootClasses(rootClasses);

        // JACKSON - JSON CONFIG
        TypeResolverBuilder<?> typeResolver = new CustomTypeResolverBuilder(namedTypes)
                .init(JsonTypeInfo.Id.NAME, null)
                .inclusion(As.PROPERTY)
                .typeProperty("apiType");

        ObjectMapper jsonObjectMapper = new ObjectMapper();
        jsonObjectMapper.addHandler(new ApiTypeIgnoringDeserializationProblemHandler());
        jsonObjectMapper.setDefaultTyping(typeResolver);

        jsonObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        jsonObjectMapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, true);
        jsonObjectMapper.setDateFormat(dateFormat);
        jsonObjectMapper.setAnnotationIntrospector(pair);
        jsonObjectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        jsonObjectMapper.getSubtypeResolver().registerSubtypes(namedTypes.toArray(new NamedType[namedTypes.size()]));

        jsonObjectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);

        return jsonObjectMapper;
    }

    private Set<Class<?>> findRootClasses(String packageName) {
        Reflections reflections = new Reflections(packageName);

        Set<Class<?>> xmlTypeClasses = reflections.getTypesAnnotatedWith(XmlType.class);
        Set<Class<?>> xmlRootElementClasses = reflections.getTypesAnnotatedWith(XmlRootElement.class);

        Set<Class<?>> customSerializationClasses = reflections.getTypesAnnotatedWith(InternalSerializationHint.class);
        configureCustomSerializationRegistry(customSerializationClasses);

        ApiVersion packageApiVersion = ApiVersion.fromPackageName(packageName);

        Set<Class<?>> rootClasses = new HashSet<>();
        rootClasses.addAll(DEFAULT_CLASSES);
        rootClasses.addAll(xmlTypeClasses);
        rootClasses.addAll(xmlRootElementClasses);

        if (packageApiVersion.getResponseClass() != null && !packageApiVersion.getResponseClass().isInterface()) {
            rootClasses.add(packageApiVersion.getResponseClass());
        }

        if (LOG.isDebugEnabled()) {
            List<String> classList = rootClasses.stream().map(Class::getName).collect(Collectors.toList());
            Collections.sort(classList);


            LOG.debug("findRootClasses(): packageName={} numOfClasses={}", packageName, classList.size());

            for (int idx = 0; idx < classList.size(); idx++) {
                LOG.debug("findRootClasses(): classIdx={} class={}", idx, classList.get(idx));
            }
        }


        return rootClasses;
    }

    private void configureCustomSerializationRegistry(Set<Class<?>> customSerializationClasses) {

        for (Class<?> customSerializationClass : customSerializationClasses) {
            InternalSerializationHint internalSerializationHint = customSerializationClass.getAnnotation(InternalSerializationHint.class);

            if (internalSerializationHint != null) {
                Class<?> internalSerializationClass = internalSerializationHint.value();

                this.customSerializationsRegistry.put(customSerializationClass, internalSerializationClass);

                LOG.info("configureCustomSerializationRegistry(): customSerializationClass={}, internalSerializationClass={}",
                        customSerializationClass, internalSerializationClass);

            } else {
                LOG.info("configureCustomSerializationRegistry(): customSerializationClass={} doesn't have a {} annotation", customSerializationClass, InternalSerializationHint.class);
            }


        }
    }

    private Set<NamedType> getNamedTypesForRootClasses(Set<Class<?>> rootClasses) {
        Set<NamedType> namedTypes = new HashSet<>();

        for (Class<?> rootClass : rootClasses) {
            if (DEFAULT_CLASSES.contains(rootClass)) {
                namedTypes.add(new NamedType(rootClass));
            } else {
                NamedType namedType = buildNamedType(rootClass);
                if (namedType != null) {
                    namedTypes.add(namedType);
                }
            }
        }

        return namedTypes;
    }

    private NamedType buildNamedType(Class<?> rootClass) {
        try {
            XmlType xmlTypeAnno = rootClass.getAnnotation(XmlType.class);
            XmlRootElement xmlRootAnno = rootClass.getAnnotation(XmlRootElement.class);

            NamedType namedType = null;

            if (xmlTypeAnno != null) {

                String xmlTypeName = xmlTypeAnno.name();

                if (xmlTypeName == null || xmlTypeName.isEmpty() || ANNOTATION_DEFAULT_VALUE.equals(xmlTypeName)) {
                    String className = rootClass.getSimpleName();
                    xmlTypeName = className.substring(0, 1).toLowerCase() + className.substring(1);
                } else {
                    if (xmlRootAnno != null) {
                        String xmlRootName = xmlRootAnno.name();

                        if (!ANNOTATION_DEFAULT_VALUE.equals(xmlRootAnno.name()) && !xmlTypeName.equals(xmlRootName)) {
                            throw new RuntimeException("XmlRootElement name cannot differ from XmlType name for class: " + rootClass);
                        }
                    }
                }

                namedType = new NamedType(rootClass, xmlTypeName);
            }

            return namedType;
        } catch (Exception ex) {
            throw new RuntimeException("Error building NamedType for " + rootClass, ex);
        }
    }

    private void processContextRoot(String packageName, Map<ApiVersion, Set<Class<?>>> contextKeyToRootClasses) {
        ApiVersion contextKey = ApiVersion.fromPackageName(packageName);

        Set<Class<?>> rootClasses = findRootClasses(packageName);

        if (contextKeyToRootClasses.containsKey(contextKey)) {
            contextKeyToRootClasses.get(contextKey).addAll(rootClasses);
        } else {
            contextKeyToRootClasses.put(contextKey, rootClasses);
        }
    }

    public class CustomTypeResolverBuilder extends DefaultTypeResolverBuilder {

        private static final long serialVersionUID = -9141435977728735502L;
        private final Collection<NamedType> dibsSubTypes;

        public CustomTypeResolverBuilder(Collection<NamedType> disbSubTypes) {
            super(DefaultTyping.NON_FINAL);
            this.dibsSubTypes = disbSubTypes;
        }

        @Override
        public boolean useForType(JavaType t) {
            Class<?> rawClass = t.getRawClass();
            if (rawClass.getName().startsWith(DIBS_SERVICE_PACKAGE)) {
                if (t.isEnumType() || t.isMapLikeType() || t.isCollectionLikeType()) {
                    return false;
                }

                return true;
            }

            return false;
        }

        @Override
        public TypeDeserializer buildTypeDeserializer(DeserializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
            //TODO figure out how to utilize this to get Map response working, without breaking other stuff.
            //			return useForType(baseType)  && inContext(baseType) ? super.buildTypeDeserializer(config, baseType, dibsSubTypes) : null;
            return null;
        }

        @Override
        public TypeSerializer buildTypeSerializer(SerializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
            return useForType(baseType) ? super.buildTypeSerializer(config, baseType, dibsSubTypes) : null;
        }
    }

    public class ApiTypeIgnoringDeserializationProblemHandler extends DeserializationProblemHandler {
        public static final String API_TYPE_PROPERTY_NAME = "apiType";

        @Override
        public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser jp, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException, JsonProcessingException {
            if (API_TYPE_PROPERTY_NAME.equals(propertyName)) {
                return true;
            }
            return super.handleUnknownProperty(ctxt, jp, deserializer, beanOrClass, propertyName);
        }
    }
}
