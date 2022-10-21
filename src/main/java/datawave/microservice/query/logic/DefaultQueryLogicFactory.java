package datawave.microservice.query.logic;

import datawave.core.query.logic.BaseQueryLogic;
import datawave.core.query.logic.QueryLogic;
import datawave.core.query.logic.QueryLogicFactory;
import datawave.microservice.authorization.user.ProxiedUserDetails;
import datawave.microservice.query.logic.config.QueryLogicFactoryProperties;
import datawave.security.authorization.JWTTokenHandler;
import datawave.webservice.query.exception.DatawaveErrorCode;
import datawave.webservice.query.exception.QueryException;
import datawave.webservice.query.exception.UnauthorizedQueryException;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.security.Principal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "datawave.query.logic.factory.enabled", havingValue = "true", matchIfMissing = true)
public class DefaultQueryLogicFactory implements QueryLogicFactory, ApplicationContextAware {
    
    /**
     * Configuration for parameters that are for all query logic types
     */
    private final QueryLogicFactoryProperties queryLogicFactoryProperties;
    
    private ApplicationContext applicationContext;
    
    private final WebClient webClient;
    
    private final JWTTokenHandler jwtTokenHandler;
    
    private final String authorizationUri;
    
    private ProxiedUserDetails serverUserDetails;
    
    public DefaultQueryLogicFactory(QueryLogicFactoryProperties queryLogicFactoryProperties, JWTTokenHandler jwtTokenHandler,
                    @Qualifier("outboundNettySslContext") SslContext nettySslContext, WebClient.Builder webClientBuilder,
                    @Value("${datawave.authorization.uri:https://authorization:8443/authorization/v1/authorize}") String authorizationUri) {
        this.queryLogicFactoryProperties = queryLogicFactoryProperties;
        
        // @formatter:off
        TcpClient timeoutClient = TcpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .doOnConnected(con -> con.addHandlerLast(new ReadTimeoutHandler(6)))
                .secure(sslContextSpec -> sslContextSpec.sslContext(nettySslContext));
        this.webClient = webClientBuilder.clone().clientConnector(new ReactorClientHttpConnector(HttpClient.from(timeoutClient))).build();
        // @formatter:on
        
        this.jwtTokenHandler = jwtTokenHandler;
        this.authorizationUri = authorizationUri;
        this.serverUserDetails = getServerUserDetails();
    }
    
    @Override
    public QueryLogic<?> getQueryLogic(String name, Principal principal) throws QueryException, IllegalArgumentException, CloneNotSupportedException {
        throw new UnsupportedOperationException("Using a principal to create a query logic is not supported for spring boot microservice deployments");
    }
    
    @Override
    public QueryLogic<?> getQueryLogic(String name, ProxiedUserDetails currentUser) throws QueryException {
        return getQueryLogic(name, currentUser, true);
    }
    
    @Override
    public QueryLogic<?> getQueryLogic(String name) throws QueryException {
        return getQueryLogic(name, null, false);
    }
    
    private QueryLogic<?> getQueryLogic(String name, ProxiedUserDetails currentUser, boolean checkRoles) throws QueryException {
        QueryLogic<?> logic;
        try {
            logic = (QueryLogic<?>) applicationContext.getBean(name);
        } catch (ClassCastException | NoSuchBeanDefinitionException cce) {
            throw new IllegalArgumentException("Logic name '" + name + "' does not exist in the configuration");
        }
        
        Set<String> userRoles = new HashSet<>();
        if (currentUser != null) {
            userRoles.addAll(currentUser.getPrimaryUser().getRoles());
        }
        
        if (checkRoles && !logic.canRunQuery(userRoles)) {
            throw new UnauthorizedQueryException(DatawaveErrorCode.MISSING_REQUIRED_ROLES,
                            new IllegalAccessException("User does not have required role(s): " + logic.getRequiredRoles()));
        }
        
        logic.setLogicName(name);
        if (logic.getMaxPageSize() == 0) {
            logic.setMaxPageSize(queryLogicFactoryProperties.getMaxPageSize());
        }
        if (logic.getPageByteTrigger() == 0) {
            logic.setPageByteTrigger(queryLogicFactoryProperties.getPageByteTrigger());
        }
        
        if (logic instanceof BaseQueryLogic) {
            // update server user details
            serverUserDetails = getServerUserDetails();
            ((BaseQueryLogic<?>) logic).setCurrentUser(currentUser);
            ((BaseQueryLogic<?>) logic).setServerUser(serverUserDetails);
        }
        
        return logic;
    }
    
    @Override
    public List<QueryLogic<?>> getQueryLogicList() {
        Map<String,QueryLogic> logicMap = applicationContext.getBeansOfType(QueryLogic.class);
        
        List<QueryLogic<?>> logicList = new ArrayList<>();
        
        for (Map.Entry<String,QueryLogic> entry : logicMap.entrySet()) {
            QueryLogic<?> logic = entry.getValue();
            logic.setLogicName(entry.getKey());
            logicList.add(logic);
        }
        return logicList;
        
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    private ProxiedUserDetails getServerUserDetails() {
        ProxiedUserDetails serverUserDetails = this.serverUserDetails;
        if (serverUserDetails == null || (System.currentTimeMillis() > (this.serverUserDetails.getCreationTime() + TimeUnit.DAYS.toMillis(1)))) {
            // @formatter:off
            WebClient.ResponseSpec response = webClient.get()
                    .uri(authorizationUri)
                    .retrieve();
            // @formatter:on
            
            String jwtString = response.bodyToMono(String.class).block(Duration.ofSeconds(30));
            
            serverUserDetails = new ProxiedUserDetails(jwtTokenHandler.createUsersFromToken(jwtString), System.currentTimeMillis());
        }
        return serverUserDetails;
    }
}
