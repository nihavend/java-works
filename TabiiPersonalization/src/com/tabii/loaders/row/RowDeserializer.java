package com.tabii.loaders.row;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.tabii.data.model.json.BannerRow;
import com.tabii.data.model.json.CwRow;
import com.tabii.data.model.json.GenreRow;
import com.tabii.data.model.json.LiveStreamRow;
import com.tabii.data.model.json.Row;
import com.tabii.data.model.json.ShowRow;
import com.tabii.data.model.json.SpecialRow;

public class RowDeserializer extends JsonDeserializer<Row> {
    @Override
    public Row deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode node = codec.readTree(p);

        String rowType = node.get("rowType").asText();

        Class<? extends Row> targetClass;
        switch (rowType) {
            case "banner":
                targetClass = BannerRow.class;
                break;
            case "continueWatching":
                targetClass = CwRow.class;
                break;
            case "genre":
                targetClass = GenreRow.class;
                break;
            case "livestream":
                targetClass = LiveStreamRow.class;
                break;
            case "show":
                targetClass = ShowRow.class;
                break;
            case "special":
                targetClass = SpecialRow.class;
                break;
            default:
                throw new IllegalArgumentException("Unknown rowType: " + rowType);
        }

        return codec.treeToValue(node, targetClass);
    }
}
