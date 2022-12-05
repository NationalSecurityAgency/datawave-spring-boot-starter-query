package datawave.microservice.query.logic.config.converter;

import datawave.data.type.Type;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class StringToTypeConverter implements Converter<String,Type> {
    @Override
    public Type convert(String source) {
        try {
            Class<?> clazz = Class.forName(source);
            return (Type) clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create data type from string", e);
        }
    }
}
