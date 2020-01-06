package com.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializationCloneUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SerializationCloneUtils.class);

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T clone(T objectToClone) {
        if (objectToClone == null) {
            return null;
        }

        long startTimeMillis = System.currentTimeMillis();

        byte[] serializedBytes = serialize(objectToClone);

        long timeToSerialize = System.currentTimeMillis() - startTimeMillis;

        long deserializeStartMillis = System.currentTimeMillis();

        Serializable deserializedObject = deserialize(serializedBytes);

        long timeToDeserialize = System.currentTimeMillis() - deserializeStartMillis;

        long totalTime = System.currentTimeMillis() - startTimeMillis;

        LOG.trace("Cloning class [{}] of size [{}] took [{}] ms. Serialization took [{}] ms and deserialization took [{}] ms",
                objectToClone.getClass(), serializedBytes.length, totalTime, timeToSerialize, timeToDeserialize);

        return (T) deserializedObject;
    }

    public static <T extends Serializable> List<T> clone(List<T> objectsToClone) {
        List<T> clonedObjects = null;
        if(objectsToClone != null) {
            ListWrapper<T> listWrapper = new ListWrapper<>(objectsToClone);
            ListWrapper<T> clonedListWrapper = clone(listWrapper);
            if (clonedListWrapper != null) {
                clonedObjects = clonedListWrapper.getList();
            }
        }
        return clonedObjects;
    }

    public static <T extends Serializable> String serializeBase64(T objectToSerialize) {
        byte[] bytes = serialize(objectToSerialize);
        return Base64.encodeBase64String(bytes);
    }

    public static <T extends Serializable> byte[] serialize(T objectToSerialize) {
        byte[] serializedBytes = null;
        if(objectToSerialize != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(objectToSerialize);
                oos.flush();
                serializedBytes = baos.toByteArray();
            } catch (IOException ex) {
                throw new IllegalArgumentException("Failed to serialize object of type: "
                        + objectToSerialize.getClass(), ex);
            }
        }
        return serializedBytes;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserialize(byte[] bytesToDeserialize) {
        T deserializedObject = null;
        if(bytesToDeserialize != null) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(bytesToDeserialize);
                ObjectInputStream ois = new ObjectInputStream(bais);
                deserializedObject = (T) ois.readObject();
            } catch (IOException ex) {
                throw new IllegalArgumentException("Failed to deserialize object", ex);
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("Failed to deserialize object type", ex);
            }
        }
        return deserializedObject;
    }

    public static <T extends Serializable> T deserializeBase64(String base64String) {
        byte[] bytes = Base64.decodeBase64(base64String);
        return deserialize(bytes);
    }

}
