package datawave.microservice.query.logic.config;

import datawave.query.language.parser.QueryParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Configuration
@ConditionalOnProperty(name = "datawave.query.logic.factory.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({QueryLogicFactoryProperties.class})
public class FacetedQueryConfiguration {
    
    @Autowired
    private ApplicationContext appContext;
    
    @Bean
    @ConfigurationProperties("datawave.query.logic.logics.faceted-query")
    public ShardQueryLogicProperties facetedQueryProperties() {
        return new ShardQueryLogicProperties();
    }
    
    @Bean
    @Scope(SCOPE_PROTOTYPE)
    public Map<String,QueryParser> facetedQuerySyntaxParsers() {
        Map<String,QueryParser> querySyntaxParsers = new HashMap<>();
        facetedQueryProperties().getQuerySyntaxParsers().forEach((key, value) -> {
            if (!key.isEmpty()) {
                querySyntaxParsers.put(key, (!value.isEmpty()) ? appContext.getBean(value, QueryParser.class) : null);
            }
        });
        return querySyntaxParsers;
    }
}
