package org.jolokia.service.serializer;

import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Objects;

import static org.jolokia.server.core.util.EscapeUtil.parsePath;
import static org.testng.Assert.assertEquals;

public class JacksonSerializerTest {

    private final JacksonSerializer seralizer = new JacksonSerializer(100);
    private final String expected = "{\"someString\":\"someStringDummyValue\",\"innerBean\":{\"someString\":\"anotherStringDummyValue\",\"innerBean\":null}}";

    public static class DummyBean {
        private String someString;

        private DummyBean innerBean;

        public String getSomeString() {
            return someString;
        }

        public void setSomeString(String someString) {
            this.someString = someString;
        }

        public DummyBean getInnerBean() {
            return innerBean;
        }

        public void setInnerBean(DummyBean innerBean) {
            this.innerBean = innerBean;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            DummyBean dummyBean = (DummyBean) o;
            return Objects.equals(someString, dummyBean.someString) &&
                    Objects.equals(innerBean, dummyBean.innerBean);
        }

        @Override
        public int hashCode() {
            return Objects.hash(someString, innerBean);
        }
    }

    private DummyBean createDummyValue() {
        DummyBean dummyBean = new DummyBean();
        dummyBean.setSomeString("someStringDummyValue");
        DummyBean innerDummyBean = new DummyBean();
        innerDummyBean.setSomeString("anotherStringDummyValue");
        dummyBean.setInnerBean(innerDummyBean);
        return dummyBean;
    }

    @Test
    public void testSerializeWithoutPath() throws Exception {
        assertEquals(expected, seralizer.serialize(createDummyValue(), Collections.<String>emptyList(), null));
    }

    @Test
    public void testSerializeWithPath() throws Exception {
        assertEquals("anotherStringDummyValue", seralizer.serialize(createDummyValue(), parsePath("$.innerBean.someString"), null));
    }

    @Test
    public void testDeserialize() throws Exception {
        assertEquals(createDummyValue(), seralizer.deserialize(DummyBean.class.getName(), expected));
    }

    @Test
    public void testSetInnerValue() throws Exception {
        DummyBean newDummyBean = new DummyBean();
        newDummyBean.setSomeString("yetAnotherOne");
        DummyBean pOuterObject = createDummyValue();
        pOuterObject.setInnerBean(newDummyBean);
        DummyBean updatedByPath = (DummyBean) seralizer.setInnerValue(pOuterObject, newDummyBean, parsePath("$.innerBean"));
        assertEquals(pOuterObject, updatedByPath);
        assertEquals("yetAnotherOne", updatedByPath.getInnerBean().getSomeString());
    }
}