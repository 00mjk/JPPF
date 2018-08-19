/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.serialization;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A specfic serialization handler for {@link ConcurrentHashMap}.
 * @author Laurent Cohen
 */
public class ConcurrentHashMapHandler extends AbstractSerializationHandler {
  @Override
  public void writeDeclaredFields(final Serializer serializer, final ClassDescriptor cd, final Object obj) throws Exception {
    final Map<?, ?> map = (Map<?, ?>) obj;
    ClassDescriptor tmpDesc = null;
    try {
      tmpDesc = serializer.currentClassDescriptor;
      serializer.currentClassDescriptor = cd;
      serializer.writeInt(map.size());
      for (final Object o: map.entrySet()) {
        final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
        serializer.writeObject(entry.getKey());
        serializer.writeObject(entry.getValue());
      }
    } finally {
      serializer.currentClassDescriptor = tmpDesc;
    }
  }

  @Override
  public void readDeclaredFields(final Deserializer deserializer, final ClassDescriptor cd, final Object obj) throws Exception {
    @SuppressWarnings("unchecked")
    final Map<? super Object, ? super Object> map = (Map<? super Object, ? super Object>) obj;
    ClassDescriptor tmpDesc = null;
    try {
      tmpDesc = deserializer.currentClassDescriptor;
      deserializer.currentClassDescriptor = cd;
      if (map instanceof ConcurrentHashMap) copyFields(new ConcurrentHashMap<>(), obj, cd);
      final int size = deserializer.readInt();
      for (int i=0; i<size; i++) {
        final Object key = deserializer.readObject();
        final Object value = deserializer.readObject();
        map.put(key, value);
      }
    } finally {
      deserializer.currentClassDescriptor = tmpDesc;
    }
  }
}
