package org.sample.flink.tabii;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.flink.api.common.functions.MapFunction;
import org.sample.flink.tabii.model.avro.ArrayValueAvro;
import org.sample.flink.tabii.model.avro.AttributeAvro;
import org.sample.flink.tabii.model.avro.BodyAvro;
import org.sample.flink.tabii.model.avro.LogRecordAvro;
import org.sample.flink.tabii.model.avro.LogWrapperAvro;
import org.sample.flink.tabii.model.avro.ResourceAvro;
import org.sample.flink.tabii.model.avro.ResourceLogAvro;
import org.sample.flink.tabii.model.avro.ScopeAvro;
import org.sample.flink.tabii.model.avro.ScopeLogAvro;
import org.sample.flink.tabii.model.avro.SimpleValueAvro;
import org.sample.flink.tabii.model.avro.ValueAvro;
import org.sample.flink.tabii.model.message.ArrayValue;
import org.sample.flink.tabii.model.message.Attribute;
import org.sample.flink.tabii.model.message.Body;
import org.sample.flink.tabii.model.message.LogRecord;
import org.sample.flink.tabii.model.message.LogWrapper;
import org.sample.flink.tabii.model.message.Resource;
import org.sample.flink.tabii.model.message.ResourceLog;
import org.sample.flink.tabii.model.message.Scope;
import org.sample.flink.tabii.model.message.ScopeLog;
import org.sample.flink.tabii.model.message.Value;

public class LogWrapperToAvroMapper implements MapFunction<LogWrapper, LogWrapperAvro> {
    private static final long serialVersionUID = 1L;

    @Override
    public LogWrapperAvro map(LogWrapper logWrapper) throws Exception {
        if (logWrapper == null) {
            return null;
        }

        LogWrapperAvro avro = new LogWrapperAvro();
        avro.setResourceLogs(convertResourceLogs(logWrapper.getResourceLogs()));
        return avro;
    }

    private List<ResourceLogAvro> convertResourceLogs(List<ResourceLog> resourceLogs) {
        if (resourceLogs == null || resourceLogs.isEmpty()) {
            return Collections.emptyList();
        }

        List<ResourceLogAvro> resourceLogsAvro = new ArrayList<>(resourceLogs.size());
        for (ResourceLog rl : resourceLogs) {
            ResourceLogAvro rlAvro = new ResourceLogAvro();
            rlAvro.setResource(convertResource(rl.getResource()));
            rlAvro.setScopeLogs(convertScopeLogs(rl.getScopeLogs()));
            rlAvro.setSchemaUrl(rl.getSchemaUrl());
            resourceLogsAvro.add(rlAvro);
        }
        return resourceLogsAvro;
    }

    private ResourceAvro convertResource(Resource resource) {
        if (resource == null) {
            return null;
        }
        ResourceAvro rAvro = new ResourceAvro();
        rAvro.setAttributes(convertAttributes(resource.getAttributes()));
        return rAvro;
    }

    private List<AttributeAvro> convertAttributes(List<Attribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Collections.emptyList();
        }
        List<AttributeAvro> attrsAvro = new ArrayList<>(attributes.size());
        for (Attribute attr : attributes) {
            AttributeAvro aAvro = new AttributeAvro();
            aAvro.setKey(attr.getKey());
            aAvro.setValue(convertValue(attr.getValue()));
            attrsAvro.add(aAvro);
        }
        return attrsAvro;
    }

    private ValueAvro convertValue(Value value) {
        if (value == null) {
            return null;
        }
        ValueAvro vAvro = new ValueAvro();
        vAvro.setStringValue(value.getStringValue());
        vAvro.setIntValue(value.getIntValue());
        vAvro.setArrayValue(convertArrayValue(value.getArrayValue()));
        return vAvro;
    }

    private ArrayValueAvro convertArrayValue(ArrayValue arrayValue) {
        if (arrayValue == null) {
            return null;
        }
        ArrayValueAvro avAvro = new ArrayValueAvro();
        avAvro.setValues(convertSimpleValues(arrayValue.getValues()));
        return avAvro;
    }

    private List<SimpleValueAvro> convertSimpleValues(List<Value> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<SimpleValueAvro> simpleValuesAvro = new ArrayList<>(values.size());
        for (Value v : values) {
            SimpleValueAvro svAvro = new SimpleValueAvro();
            svAvro.setStringValue(v.getStringValue());
            svAvro.setIntValue(v.getIntValue());
            simpleValuesAvro.add(svAvro);
        }
        return simpleValuesAvro;
    }

    private List<ScopeLogAvro> convertScopeLogs(List<ScopeLog> scopeLogs) {
        if (scopeLogs == null || scopeLogs.isEmpty()) {
            return Collections.emptyList();
        }
        List<ScopeLogAvro> slsAvro = new ArrayList<>(scopeLogs.size());
        for (ScopeLog sl : scopeLogs) {
            ScopeLogAvro slAvro = new ScopeLogAvro();
            slAvro.setScope(convertScope(sl.getScope()));
            slAvro.setLogRecords(convertLogRecords(sl.getLogRecords()));
            slsAvro.add(slAvro);
        }
        return slsAvro;
    }

    private ScopeAvro convertScope(Scope scope) {
        if (scope == null) {
            return null;
        }
        ScopeAvro sAvro = new ScopeAvro();
        sAvro.setName(scope.getName());
        return sAvro;
    }

    private List<LogRecordAvro> convertLogRecords(List<LogRecord> logRecords) {
        if (logRecords == null || logRecords.isEmpty()) {
            return Collections.emptyList();
        }
        List<LogRecordAvro> lrsAvro = new ArrayList<>(logRecords.size());
        for (LogRecord lr : logRecords) {
            LogRecordAvro lrAvro = new LogRecordAvro();
            lrAvro.setTimeUnixNano(lr.getTimeUnixNano());
            lrAvro.setObservedTimeUnixNano(lr.getObservedTimeUnixNano());
            lrAvro.setSeverityNumber(lr.getSeverityNumber());
            lrAvro.setSeverityText(lr.getSeverityText());
            lrAvro.setBody(convertBody(lr.getBody()));
            lrAvro.setAttributes(convertAttributes(lr.getAttributes()));
            lrAvro.setTraceId(lr.getTraceId());
            lrAvro.setSpanId(lr.getSpanId());
            lrsAvro.add(lrAvro);
        }
        return lrsAvro;
    }

    private BodyAvro convertBody(Body body) {
        if (body == null) {
            return null;
        }
        BodyAvro bAvro = new BodyAvro();
        bAvro.setStringValue(body.getStringValue());
        return bAvro;
    }
}