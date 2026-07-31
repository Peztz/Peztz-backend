package com.peztz.backend.config;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class HibernateNamingConfiguration {

	@Bean
	public CamelCaseToUnderscoresNamingStrategy caseSensitivePhysicalNamingStrategy() {
		return new CamelCaseToUnderscoresNamingStrategy() {
			@Override
			protected boolean isCaseInsensitive(JdbcEnvironment jdbcEnvironment) {
				return false;
			}
		};
	}
}
