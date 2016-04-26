package org.jolokia.service.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.EvaluationListener;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.jolokia.server.core.service.api.AbstractJolokiaService;
import org.jolokia.server.core.service.serializer.SerializeOptions;
import org.jolokia.server.core.service.serializer.Serializer;
import org.jolokia.server.core.service.serializer.WriteRequestValues;
import org.jolokia.server.core.util.ClassUtil;
import org.jolokia.server.core.util.EscapeUtil;
import org.jolokia.service.serializer.object.OpenTypeDeserializer;
import org.jolokia.service.serializer.object.StringToObjectConverter;

import javax.management.AttributeNotFoundException;
import javax.management.openmbean.OpenType;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class JacksonSerializer extends AbstractJolokiaService<Serializer> implements Serializer {

    private final ObjectMapper objectMapper;
    private final OpenTypeDeserializer toOpenTypeConverter;
    private final Configuration jsonPathconfiguration;

    public JacksonSerializer(final ObjectMapper objectMapper, int pOrderId) {
        super(Serializer.class, pOrderId);
        this.objectMapper = objectMapper;
        jsonPathconfiguration = new Configuration.ConfigurationBuilder()
                .jsonProvider(new JacksonJsonProvider(objectMapper))
                .mappingProvider(new JacksonMappingProvider(objectMapper))
                .options(EnumSet.noneOf(Option.class))
                .evaluationListener(Collections.<EvaluationListener>emptyList())
                .build();
        // TODO move this one to jackson too
        toOpenTypeConverter = new OpenTypeDeserializer(new StringToObjectConverter());
    }

    public JacksonSerializer(int pOrderId) {
        this(new ObjectMapper(), pOrderId);
    }

    public Object serialize(Object pValue, List<String> pPathParts, SerializeOptions pOptions) throws AttributeNotFoundException {
        if (pValue == null) {
            return null;
        }
        // not sure this is the best way to do that, maybe delegating path parts splitting would be a better solution
        String path = EscapeUtil.combineToPath(pPathParts);
        try {
            String json = objectMapper.writeValueAsString(pValue);
            if (path == null) {
                return json;
            } else {
                return JsonPath.parse(json, jsonPathconfiguration).read(path, String.class);
            }
        } catch (JsonProcessingException e) {
            getJolokiaContext().error(String.format("Unexpected error when serializing %s", pValue.getClass().getSimpleName()), e);
            return null;
        }
    }

    public Object deserialize(String pExpectedClassName, Object pValue) {
        if (pValue == null) {
            return null;
        }
        Class<?> expectedClass = ClassUtil.classForName(pExpectedClassName);
        if (expectedClass == null) {
            return null;
        }
        if (pValue instanceof String) {
            try {
                return objectMapper.readValue((String) pValue, expectedClass);
            } catch (java.io.IOException e) {
                getJolokiaContext().error(String.format("Unexpected error when deserializing %s", pValue), e);
            }
        }
        getJolokiaContext().info(String.format("Do not know how to deserialize: %s is not a String", pValue));
        return null;
    }

    public WriteRequestValues setInnerValue(Object pOuterObject, Object pNewValue, List<String> pPathParts) throws AttributeNotFoundException, IllegalAccessException, InvocationTargetException {
        if (pOuterObject == null) {
            return null;
        }
        String path = EscapeUtil.combineToPath(pPathParts);
        String originalJson = (String) serialize(pOuterObject, Collections.<String>emptyList(), null);
        String updatedJson = JsonPath.parse(originalJson, jsonPathconfiguration).set(path, pNewValue).jsonString();
        return  new WriteRequestValues(deserialize(pOuterObject.getClass().getName(), updatedJson), );

    }

    public Object deserializeOpenType(OpenType<?> pOpenType, Object pValue) {
        return toOpenTypeConverter.deserialize(pOpenType, pValue);
    }
}
