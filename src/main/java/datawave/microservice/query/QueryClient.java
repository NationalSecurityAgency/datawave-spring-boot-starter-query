package datawave.microservice.query;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import datawave.microservice.authorization.preauth.ProxiedEntityX509Filter;
import datawave.microservice.authorization.user.ProxiedUserDetails;
import datawave.query.exceptions.DatawaveQueryException;
import datawave.security.authorization.JWTTokenHandler;
import datawave.webservice.result.BaseQueryResponse;
import datawave.webservice.result.GenericResponse;
import datawave.webservice.result.VoidResponse;
import org.apache.http.Header;
import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.DefaultCookieSpecProvider;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Rest client for submitting queries
 *
 */
public class QueryClient {
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private String host;
    
    private int port;
    
    private ObjectMapper objectMapper;
    
    private JWTTokenHandler jwtTokenHandler;
    
    private CookieStore cookieStore;
    
    private static final String CREATE_PATH = "{0}/DataWave/Query/{1}/create";
    private static final String NEXT_PATH = "{0}/DataWave/Query/{1}/next";
    private static final String CLOSE_PATH = "{0}/DataWave/Query/{1}/close";
    
    private QueryClient(String host, int port, JWTTokenHandler jwtTokenHandler) {
        this.host = host;
        this.port = port;
        
        // Tell Jackson that it should honor JAX-B annotations when de-serializing the JSON response.
        this.objectMapper = JsonMapper.builder().enable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME).build();
        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector(objectMapper.getTypeFactory());
        this.objectMapper.setAnnotationIntrospector(introspector);
        
        this.jwtTokenHandler = jwtTokenHandler;
        
        this.cookieStore = new BasicCookieStore();
    }
    
    public static QueryClient create(String host, int port, JWTTokenHandler handler) {
        return new QueryClient(host, port, handler);
    }
    
    public class CookieInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            CookieSpec spec = new DefaultCookieSpecProvider().create(new HttpClientContext());
            for (Header header : spec.formatCookies(cookieStore.getCookies())) {
                request.getHeaders().add(header.getName(), header.getValue());
            }
            
            ClientHttpResponse response = execution.execute(request, body);
            
            URI uri = request.getURI();
            final CookieOrigin cookieOrigin = new CookieOrigin(uri.getHost(), uri.getPort(), uri.getPath(), true);
            
            for (String setCookie : Arrays.asList(HttpHeaders.SET_COOKIE, HttpHeaders.SET_COOKIE2)) {
                for (String cookieValue : response.getHeaders().get(setCookie)) {
                    Header header = new BasicHeader(setCookie, cookieValue);
                    try {
                        List<Cookie> cookies = spec.parse(header, cookieOrigin);
                        for (Cookie cookie : cookies) {
                            cookieStore.addCookie(cookie);
                        }
                    } catch (MalformedCookieException mce) {
                        log.error(mce.getMessage(), mce);
                    }
                    cookieStore.clearExpired(new Date());
                }
            }
            
            return response;
        }
    }
    
    public GenericResponse<String> createQuery(ProxiedUserDetails user, ProxiedUserDetails trustedUser, String logicName, MultiValueMap paramsToMap)
                    throws DatawaveQueryException {
        ResponseEntity<GenericResponse> response;
        try {
            String url = new URL("http", host, port, "").toExternalForm();
            
            HttpHeaders headers = getHeaders(user, trustedUser);
            
            CookieInterceptor interceptor = new CookieInterceptor();
            RestTemplateBuilder builder = new RestTemplateBuilder().interceptors(interceptor);
            for (Map.Entry<String,List<String>> entry : headers.entrySet()) {
                builder = builder.defaultHeader(entry.getKey(), entry.getValue().toArray(new String[0]));
            }
            
            response = builder.build().postForEntity(MessageFormat.format(CREATE_PATH, url, logicName), paramsToMap, GenericResponse.class);
        } catch (Exception e) {
            throw new DatawaveQueryException(e);
        }
        
        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Create method failed with code: " + response.getStatusCode());
            throw new DatawaveQueryException("Create method failed with code: " + response.getStatusCode());
        }
        
        @SuppressWarnings("unchecked")
        GenericResponse<String> createResponse = response.getBody();
        return createResponse;
    }
    
    public BaseQueryResponse next(ProxiedUserDetails user, ProxiedUserDetails trustedUser, String id) throws DatawaveQueryException {
        ResponseEntity<BaseQueryResponse> response;
        try {
            String url = new URL("http", host, port, "").toExternalForm();
            
            HttpHeaders headers = getHeaders(user, trustedUser);
            
            CookieInterceptor interceptor = new CookieInterceptor();
            RestTemplateBuilder builder = new RestTemplateBuilder().interceptors(interceptor);
            for (Map.Entry<String,List<String>> entry : headers.entrySet()) {
                builder = builder.defaultHeader(entry.getKey(), entry.getValue().toArray(new String[0]));
            }
            
            response = builder.build().getForEntity(MessageFormat.format(NEXT_PATH, url, id), BaseQueryResponse.class);
        } catch (Exception e) {
            throw new DatawaveQueryException(e);
        }
        
        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Next method failed with code: " + response.getStatusCode());
            throw new DatawaveQueryException("Next method failed with code: " + response.getStatusCode());
        }
        
        BaseQueryResponse nextResponse = response.getBody();
        return nextResponse;
    }
    
    public VoidResponse close(ProxiedUserDetails user, ProxiedUserDetails trustedUser, String id) throws DatawaveQueryException {
        ResponseEntity<VoidResponse> response;
        try {
            String url = new URL("http", host, port, "").toExternalForm();
            
            HttpHeaders headers = getHeaders(user, trustedUser);
            
            CookieInterceptor interceptor = new CookieInterceptor();
            RestTemplateBuilder builder = new RestTemplateBuilder().interceptors(interceptor);
            for (Map.Entry<String,List<String>> entry : headers.entrySet()) {
                builder = builder.defaultHeader(entry.getKey(), entry.getValue().toArray(new String[0]));
            }
            
            response = builder.build().postForEntity(MessageFormat.format(CLOSE_PATH, url, id), "", VoidResponse.class);
        } catch (Exception e) {
            throw new DatawaveQueryException(e);
        }
        
        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Next method failed with code: " + response.getStatusCode());
            throw new DatawaveQueryException("Next method failed with code: " + response.getStatusCode());
        }
        
        VoidResponse closeResponse = response.getBody();
        return closeResponse;
    }
    
    protected HttpHeaders getHeaders(ProxiedUserDetails user, ProxiedUserDetails trustedUser) {
        
        HttpHeaders headers = new HttpHeaders();
        if (this.jwtTokenHandler != null && user != null) {
            String token = this.jwtTokenHandler.createTokenFromUsers(user.getUsername(), user.getProxiedUsers());
            headers.add("Authorization", "Bearer " + token);
        }
        if (trustedUser != null) {
            headers.add(ProxiedEntityX509Filter.SUBJECT_DN_HEADER, trustedUser.getPrimaryUser().getDn().subjectDN());
            headers.add(ProxiedEntityX509Filter.ISSUER_DN_HEADER, trustedUser.getPrimaryUser().getDn().issuerDN());
        }
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
    
}
