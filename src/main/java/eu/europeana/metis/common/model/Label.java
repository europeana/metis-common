package eu.europeana.metis.common.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class Label {

  @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
  private String lang;

  @XmlValue
  private String value;

  public Label(String value1) {
    this(null, value1);
  }

  public Label() {}
  public String getLang() {
    return lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }


  public Label(String lang1, String value1) {
    lang = lang1;
    value = value1;
  }
}
