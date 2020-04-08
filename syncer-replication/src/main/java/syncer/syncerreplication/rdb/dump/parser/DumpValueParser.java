/*
 * Copyright 2016-2017 Leon Chen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package syncer.syncerreplication.rdb.dump.parser;


import syncer.syncerplusredis.event.EventListener;
import syncer.syncerplusredis.rdb.datatype.KeyValuePair;
import syncer.syncerplusredis.rdb.dump.datatype.DumpKeyValuePair;

/**
 * @author Leon Chen
 * @since 3.1.0
 */
public interface DumpValueParser {

    KeyValuePair<?, ?> parse(DumpKeyValuePair kv);

    void parse(DumpKeyValuePair kv, EventListener listener);
}
