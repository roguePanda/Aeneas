/**
 * Copyright 2015 Benjamin Navetta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bennavetta.aeneas.zookeeper.impl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.net.HostSpecifier;

import java.io.IOException;
import java.text.ParseException;

/**
 * Jackson deserializer for {@link HostSpecifier} objects
 */
public class HostSpecifierDeserializer extends JsonDeserializer<HostSpecifier>
{
	@Override
	public HostSpecifier deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException
	{
		try
		{
			return HostSpecifier.from(jp.getValueAsString());
		}
		catch (ParseException e)
		{
			throw new JsonParseException("Invalid host specifier", jp.getCurrentLocation(), e);
		}
	}
}
