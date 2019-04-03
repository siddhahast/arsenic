package com.util;

import java.io.InputStream;

public interface Transformer<I, O>
{

    public O transform(I input) throws TransformException;

}
