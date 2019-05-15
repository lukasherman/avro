/*
 * Copyright 2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.avro.logicalTypes;

import java.util.Map;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypeFactory;
import org.apache.avro.Schema;

/**
 * @author zfarkas
 */
public class TimestampMicrosLogicalTypeFactory implements LogicalTypeFactory {

  @Override
  public String getLogicalTypeName() {
    return "timestamp-micros";
  }

  @Override
  public LogicalType fromSchema(final Schema schema) {
    Schema.Type type = schema.getType();
    switch (type) {
      case LONG:
        return new NoConvLogicalType(schema, getLogicalTypeName(), Long.class);
      default:
        throw new IllegalArgumentException("Unsupported schema for instant " + schema);
    }
  }

  @Override
  public LogicalType create(Schema.Type schemaType, Map<String, Object> attributes) {
    throw new UnsupportedOperationException();
  }

}
