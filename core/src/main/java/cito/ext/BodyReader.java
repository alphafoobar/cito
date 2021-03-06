/*
 * Copyright 2016-2017 Daniel Siviter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cito.ext;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;

/**
 * Performs deserialisation of a stream to an object.
 * 
 * @author Daniel Siviter
 * @since v1.0 [24 Aug 2016]
 * @param <T>
 * @see Serialiser
 */
public interface BodyReader<T> {
	/**
	 * 
	 * @param type
	 * @param mediaType
	 * @return
	 */
	boolean isReadable(Type type, MediaType mediaType);

	/**
	 * 
	 * @param type
	 * @param mediaType
	 * @param is
	 * @return
	 * @throws IOException
	 */
	T readFrom(Type type, MediaType mediaType, InputStream is) throws IOException;
}
