package me.wrh.spring.feign.encoder;

import feign.RequestTemplate;
import feign.Util;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 支持feign进行远程调用时采用multipart/form-data格式编码
 *
 * @author wurenhai
 * @since 2018/3/9
 */
public class MultipartFormEncoder implements Encoder {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private final List<HttpMessageConverter<?>> converters = new RestTemplate().getMessageConverters();
    private final HttpHeaders multipartHeaders = new HttpHeaders();

    private static final Charset UTF_8 = Charset.forName("utf-8");

    public MultipartFormEncoder() {
        LOG.info("load multipart form encoder");
        multipartHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void encode(Object object, Type bodyType, RequestTemplate template) throws EncodeException {
        if (isFormRequest(bodyType)) {
            LOG.info("start encode multipart form");
            encodeMultipartFormRequest((Map<String, ?>) object, template);
        } else {
            throw new EncodeException("Not a form request");
        }
    }

    private void encodeMultipartFormRequest(Map<String, ?> form, RequestTemplate template) throws EncodeException {
        if (form == null) {
            throw new EncodeException("Cannot encode request with null form");
        }
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        for (Map.Entry<String, ?> entry : form.entrySet()) {
            Object value = entry.getValue();
            if (isMultipartFile(value)) {
                map.add(entry.getKey(), encodeMultipartFile((MultipartFile)value));
            } else if (isMultipartFileArray(value)) {
                encodeMultipartFiles(map, entry.getKey(), Arrays.asList((MultipartFile[])value));
            } else {
                map.add(entry.getKey(), encodeTextField(value));
            }
        }
        encodeRequest(map, multipartHeaders, template);
    }

    private HttpEntity<?> encodeTextField(Object o) {
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.TEXT_PLAIN);
        return new HttpEntity<>(o, partHeaders);
    }

    private HttpEntity<?> encodeMultipartFile(MultipartFile file) {
        HttpHeaders partHeaders = new HttpHeaders();
        if (file.getContentType() == null) {
            partHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        } else {
            partHeaders.set(HttpHeaders.CONTENT_TYPE, file.getContentType());
        }
        try {
            Resource resource = new MultipartFileResource(file.getOriginalFilename(), file.getSize(), file.getInputStream());
            return new HttpEntity<>(resource, partHeaders);
        } catch (IOException e) {
            throw new EncodeException("Cannot encode multipart file", e);
        }
    }

    private void encodeMultipartFiles(LinkedMultiValueMap<String, Object> map, String name, List<? extends MultipartFile> files) {
        for (MultipartFile file : files) {
            map.add(name, encodeMultipartFile(file));
        }
    }

    @SuppressWarnings("unchecked")
    private void encodeRequest(Object value, HttpHeaders requestHeaders, RequestTemplate template) throws EncodeException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HttpOutputMessage dummyRequest = new HttpOutputMessageImpl(outputStream, requestHeaders);
        try {
            Class<?> requestType = value.getClass();
            MediaType contentType = requestHeaders.getContentType();
            for (HttpMessageConverter<?> converter : converters) {
                if (converter.canWrite(requestType, contentType)) {
                    ((HttpMessageConverter<Object>)converter).write(value, contentType, dummyRequest);
                    break;
                }
            }
        } catch (IOException e) {
            throw new EncodeException("Cannot encode request", e);
        }
        HttpHeaders headers = dummyRequest.getHeaders();
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                template.header(entry.getKey(), entry.getValue());
            }
        }
        template.body(outputStream.toByteArray(), UTF_8);
    }

    private boolean isMultipartFile(Object o) {
        return o instanceof MultipartFile;
    }

    private boolean isMultipartFileArray(Object o) {
        return o != null && o.getClass().isArray() && MultipartFile.class.isAssignableFrom(o.getClass().getComponentType());
    }

    private static boolean isFormRequest(Type type) {
        return Util.MAP_STRING_WILDCARD.equals(type);
    }

    private class HttpOutputMessageImpl implements HttpOutputMessage {

        private final OutputStream body;

        private final HttpHeaders headers;

        private HttpOutputMessageImpl(OutputStream body, HttpHeaders headers) {
            this.body = body;
            this.headers = headers;
        }

        @Override
        public OutputStream getBody() {
            return body;
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }

    static class MultipartFileResource extends InputStreamResource {

        private final String filename;

        private final long size;

        private MultipartFileResource(String filename, long size, InputStream stream) {
            super(stream);
            this.filename = filename;
            this.size = size;
        }

        @Override
        public long contentLength() {
            return size;
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public InputStream getInputStream() throws IOException, IllegalStateException {
            return super.getInputStream();
        }
    }

}
