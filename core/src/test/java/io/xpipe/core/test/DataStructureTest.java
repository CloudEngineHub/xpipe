package io.xpipe.core.test;

import io.xpipe.core.data.generic.GenericDataStreamParser;
import io.xpipe.core.data.generic.GenericDataStreamWriter;
import io.xpipe.core.data.generic.GenericDataStructureNodeReader;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;
import io.xpipe.core.data.typed.TypedDataStreamParser;
import io.xpipe.core.data.typed.TypedDataStreamWriter;
import io.xpipe.core.data.typed.TypedDataStructureNodeReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class DataStructureTest {

    public static DataStructureNode createTestData() {
        var val = ValueNode.of("value");
        var flatArray = ArrayNode.of(List.of(ValueNode.of(1), ValueNode.of(2)));
        var flatTuple = TupleNode.builder().add("key1", val).build();
        var nestedArray = ArrayNode.of(List.of(flatArray, flatTuple));
        return TupleNode.builder().add("key1", val).add("key2", flatArray).add("key3", flatTuple).add("key4", nestedArray).build();
    }

    @Test
    public void testBasicOperations() {
        var obj = createTestData();
        Assertions.assertEquals(obj.size(), 4);
        Assertions.assertTrue(obj.isTuple());

        var objCopy = createTestData();
        Assertions.assertEquals(obj, objCopy);

        var key1 = obj.forKey("key1").asString();
        Assertions.assertEquals(key1, "value");

        var key2 = obj.forKey("key2");
        Assertions.assertTrue(key2.isArray());
        Assertions.assertEquals(key2.at(0), ValueNode.of(1));
        Assertions.assertEquals(key2.at(0).asString(), "1");
        Assertions.assertEquals(key2.at(0).asInt(), 1);

        var key3 = obj.forKey("key3");
        Assertions.assertTrue(key3.isTuple());
        Assertions.assertEquals(key3.forKey("key1"), ValueNode.of("value"));
        Assertions.assertEquals(key3.forKey("key1").asString(), "value");

        var key4 = obj.forKey("key4");
        Assertions.assertTrue(key4.isArray());
        Assertions.assertEquals(key4.at(0), ArrayNode.of(ValueNode.of(1), ValueNode.of(2)));
        Assertions.assertEquals(key4.at(0).at(0).asInt(), 1);
    }

    @ParameterizedTest
    @EnumSource(DataStructureTests.TypedDataset.class)
    public void testTypes(DataStructureTests.TypedDataset ds) throws IOException {
        for (var el : ds.nodes) {
            Assertions.assertTrue(ds.type.matches(el));
        }
    }

    @ParameterizedTest
    @EnumSource(DataStructureTests.TypedDataset.class)
    public void testGenericIo(DataStructureTests.TypedDataset ds) throws IOException {
        for (var el : ds.nodes) {
            var dataOut = new ByteArrayOutputStream();
            GenericDataStreamWriter.writeStructure(dataOut, el);
            var data = dataOut.toByteArray();
            var reader = new GenericDataStructureNodeReader();
            GenericDataStreamParser.parse(new ByteArrayInputStream(data), reader);
            var readNode = reader.create();

            Assertions.assertEquals(el, readNode);
        }
    }

    @ParameterizedTest
    @EnumSource(DataStructureTests.TypedDataset.class)
    public void testTypedIo(DataStructureTests.TypedDataset ds) throws IOException {
        for (var node : ds.nodes) {
            var dataOut = new ByteArrayOutputStream();
            TypedDataStreamWriter.writeStructure(dataOut, node, ds.type);
            var data = dataOut.toByteArray();

            var reader = TypedDataStructureNodeReader.of(ds.type);
            new TypedDataStreamParser(ds.type).parseStructure(new ByteArrayInputStream(data), reader);
            var readNode = reader.create();

            Assertions.assertEquals(node, readNode);
            if (readNode.isTuple() || readNode.isArray()) {
                Assertions.assertEquals(readNode.size(), node.size());
            }
        }
    }
}
