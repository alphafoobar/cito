package cito.ext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

/**
 * 
 * @author Daniel Siviter
 * @since v1.0 [24 Aug 2016]
 */
@ApplicationScoped
public class Serialiser {
	@Inject
	private Instance<BodyReader<?>> readers;
	@Inject
	private Instance<BodyWriter<?>> writers;

	/**
	 * 
	 * @param type
	 * @param mediaType
	 * @param is
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public <T> T readFrom(Type type, MediaType mediaType, InputStream is) throws IOException {
		for (BodyReader<?> reader : this.readers) {
			if (reader.isReadable(type, mediaType)) {
				return (T) reader.readFrom(type, mediaType, is);
			}
		}
		throw new IOException("Unable to read!");
	}

	/**
	 * 
	 * @param t
	 * @param type
	 * @param mediaType
	 * @param os
	 * @throws IOException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> void writeTo(T t, Class<?> type, MediaType mediaType, OutputStream os) throws IOException {
		for (BodyWriter<?> writer : this.writers) {
			if (writer.isWriteable(type, mediaType)) {
				((BodyWriter) writer).writeTo(t, type, mediaType, os);
				return;
			}
		}
		throw new IOException("Unable to write!");
	}
}