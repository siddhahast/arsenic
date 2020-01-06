package com.http;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;

import java.io.IOException;
import java.io.OutputStream;

public class OutputMessage implements HttpOutputMessage {
    @Override
    public OutputStream getBody() throws IOException {
        return null;
    }

    @Override
    public HttpHeaders getHeaders() {
        return null;
    }

    public String getBodyAsString()
    {
        return null;
    }

    private class Imp implements Comparable<Imp>
    {

        @Override
        public int compareTo(Imp o) {
            return 0;
        }
    }
}
