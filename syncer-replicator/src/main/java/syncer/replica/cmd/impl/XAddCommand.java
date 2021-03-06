/*
 * Copyright 2016-2018 Leon Chen
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

package syncer.replica.cmd.impl;

import java.util.Map;

/**
 * @author Leon Chen
 * @since 2.6.0
 */
public class XAddCommand extends GenericKeyCommand {

    private static final long serialVersionUID = 1L;

    private MaxLen maxLen;
    private boolean nomkstream = false;
    private byte[] id;
    private Map<byte[], byte[]> fields;

    public XAddCommand() {

    }

    public XAddCommand(byte[] key, MaxLen maxLen, byte[] id, Map<byte[], byte[]> fields) {
        this(key, maxLen, false, id, fields);
    }

    /**
     *
     * @param key key
     * @param maxLen maxlen
     * @param nomkstream  6.2 nomkstream
     * @param id id or *
     * @param fields
     */
    public XAddCommand(byte[] key, MaxLen maxLen, boolean nomkstream, byte[] id, Map<byte[], byte[]> fields) {
        super(key);
        this.maxLen = maxLen;
        this.nomkstream = nomkstream;
        this.id = id;
        this.fields = fields;
    }

    public boolean isNomkstream() {
        return nomkstream;
    }

    /**
     * @param nomkstream redis 6.2
     */
    public void setNomkstream(boolean nomkstream) {
        this.nomkstream = nomkstream;
    }

    public MaxLen getMaxLen() {
        return maxLen;
    }

    public void setMaxLen(MaxLen maxLen) {
        this.maxLen = maxLen;
    }

    public byte[] getId() {
        return id;
    }

    public void setId(byte[] id) {
        this.id = id;
    }

    public Map<byte[], byte[]> getFields() {
        return fields;
    }

    public void setFields(Map<byte[], byte[]> fields) {
        this.fields = fields;
    }
}
