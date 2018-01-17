/*
 * Copyright 2007-2013 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */
package eu.europeana.metis.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Enrichment input class wrapper. It defines the basics needed for enrichment
 * as the value to be enriched, the Controlled vocabulary to be used and the
 * field (optional) from which the value originated
 * 
 * @author Yorgos.Mamakis@ europeana.eu
 * 
 */
@XmlRootElement
@JsonInclude(Include.ALWAYS)
public class InputValue {

	private String originalField;

	private String value;

	private String language;

	private List<EntityClass> vocabularies;

	public InputValue() {
	}

	public String getOriginalField() {
		return originalField;
	}

	public void setOriginalField(String originalField) {
		this.originalField = originalField;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public List<EntityClass> getVocabularies() {
		return vocabularies;
	}

	public void setVocabularies(List<EntityClass> vocabularies) {
		this.vocabularies = vocabularies;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
}
