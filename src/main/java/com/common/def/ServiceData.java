package com.common.def;

import com.sun.xml.bind.AnyTypeAdapter;

import java.io.Serializable;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlJavaTypeAdapter(AnyTypeAdapter.class)
public interface ServiceData extends Serializable
{

}
