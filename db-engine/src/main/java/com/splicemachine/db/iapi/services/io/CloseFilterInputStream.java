/*
 * Apache Derby is a subproject of the Apache DB project, and is licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use these files
 * except in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Splice Machine, Inc. has modified this file.
 *
 * All Splice Machine modifications are Copyright 2012 - 2016 Splice Machine, Inc.,
 * and are licensed to you under the License; you may not use this file except in
 * compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */
package com.splicemachine.db.iapi.services.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.splicemachine.db.iapi.reference.MessageId;
import com.splicemachine.db.iapi.services.i18n.MessageService;

/**
 * A stream that will throw an exception if its methods are invoked after it
 * has been closed.
 */
public class CloseFilterInputStream
        extends FilterInputStream {

    /** Message, modeled after CloseFilterInputStream in the client. */
    private static final String MESSAGE =
            MessageService.getTextMessage(MessageId.OBJECT_CLOSED); 
    
    /** Tells if this stream has been closed. */
    private boolean closed;

    public CloseFilterInputStream(InputStream in) {
        super(in);
    }

    public void close() throws IOException {
        closed = true;        
        super.close();
    }

    public int available() throws IOException {
        checkIfClosed();
        return super.available();
    }

    public int read() throws IOException {
        checkIfClosed();
        return super.read();
    }

    public int read(byte[] b) throws IOException {
        checkIfClosed();
        return super.read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        checkIfClosed();
        return super.read(b, off, len);
    }

    public long skip(long n) throws IOException {
        checkIfClosed();
        return super.skip(n);
    }
    
    /** Throws exception if this stream has been closed. */
    private void checkIfClosed() throws IOException {
        if (closed) {
            throw new IOException(MESSAGE);
        }
    }
}
