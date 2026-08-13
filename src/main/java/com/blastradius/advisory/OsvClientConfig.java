package com.blastradius.advisory;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OsvProperties.class)
public class OsvClientConfig {

	/**
	 * Dedicated {@link RestClient} for OSV so its timeouts stay independent of any other
	 * outbound client we add later. Explicit timeouts matter: without them a hung OSV
	 * response would block a request thread indefinitely.
	 */
	@Bean
	RestClient osvRestClient(OsvProperties properties, RestClient.Builder builder) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());
		ClientHttpRequestFactory factory = requestFactory;
		return builder
				.baseUrl(properties.baseUrl())
				.requestFactory(factory)
				.build();
	}
}
