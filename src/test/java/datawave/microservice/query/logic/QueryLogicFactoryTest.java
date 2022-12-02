package datawave.microservice.query.logic;

import datawave.core.query.logic.QueryLogic;
import datawave.core.query.logic.QueryLogicFactory;
import datawave.webservice.query.exception.QueryException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles({"QueryStarterDefaults", "QueryLogicFactoryTest"})
public class QueryLogicFactoryTest {
    
    @Autowired
    QueryLogicFactory queryLogicFactory;
    
    @Test
    public void createShardQueryLogicTest() throws QueryException, CloneNotSupportedException {
        QueryLogic<?> queryLogic = queryLogicFactory.getQueryLogic("EventQuery");
        
        Assertions.assertNotNull(queryLogic);
    }
    
    @ComponentScan(basePackages = "datawave.microservice")
    @Configuration
    @Profile("QueryLogicFactoryTest")
    public static class TestConfiguration {
        
    }
    
    @SpringBootApplication(scanBasePackages = "datawave.microservice", exclude = {ErrorMvcAutoConfiguration.class})
    public static class TestApplication {
        public static void main(String[] args) {
            SpringApplication.run(QueryLogicFactoryTest.TestApplication.class, args);
        }
    }
}
