package com.wg.twtdatatest.util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class ObjectAppendOutputStream extends ObjectOutputStream {
    public ObjectAppendOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    protected ObjectAppendOutputStream() throws IOException, SecurityException {
    }

    @Override
    protected void writeStreamHeader() throws IOException {
        return;
    }
}
