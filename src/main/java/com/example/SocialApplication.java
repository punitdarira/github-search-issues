/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import com.example.model.Issue;
import com.example.service.IssueService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

@SpringBootApplication
@RestController
public class SocialApplication  {

	@Autowired
	IssueService issueService;

	private final OAuth2AuthorizedClientService authorizedClientService;

	public SocialApplication(OAuth2AuthorizedClientService authorizedClientService) {
		this.authorizedClientService = authorizedClientService;
	}

	@RequestMapping("/demo")
	public Flux<ServerSentEvent<Integer>> demo(){
		return Flux.range(1, 5)
				.delayElements(Duration.ofSeconds(1))
				.map(data -> ServerSentEvent.<Integer>builder()
						.data(data)
						.build());
	}

	@RequestMapping("/user")
	public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal, OAuth2AuthenticationToken authentication) {
		OAuth2AuthorizedClient authorizedClient = this.authorizedClientService.loadAuthorizedClient(
				authentication.getAuthorizedClientRegistrationId(),
				authentication.getName()
		);

		String accessToken = authorizedClient.getAccessToken().getTokenValue();

		System.out.println("Access token: " + accessToken);;

		//forkReactRepo(accessToken);
		return Collections.singletonMap("name", principal.getAttribute("login"));
	}

	@GetMapping(value = "/get-issues")
	public Flux<ServerSentEvent<Issue>> getIssues(@RequestParam String language, @RequestParam String search_text, OAuth2AuthenticationToken authentication) {
		// Call the service that fetches the issues based on field1 and field2
		// For example:
		Flux<ServerSentEvent<Issue>> issues = issueService.getIssues(language, search_text, authentication).map(data -> ServerSentEvent.<Issue>builder()
				.data(data)
				.build());
		return issues;
	}

	@GetMapping(value = "/test")
	public Object test(@RequestParam String language, @RequestParam String search_text, OAuth2AuthenticationToken authentication) {
		return Flux.generate(() -> "a", (o, objectSynchronousSink) -> {
			for (int i = 0; i < 10; i++) {
				objectSynchronousSink.next(o + "a");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			return o + "a";
		}, o -> {

		});
	}


	public void forkReactRepo(String accessToken) {
		String url = "https://api.github.com/repos/facebook/react/forks";

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + accessToken);
		headers.set("Accept", "application/vnd.github+json");

		JSONObject json = new JSONObject();
		json.put("organization", "octocat");
		json.put("name", "Hello-World");
		json.put("default_branch_only", true);

		HttpEntity<String> entity = new HttpEntity<>(json.toString(), headers);

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

		System.out.println(response.getBody());
	}

	public void getTrendingRepos(String accessToken) {
		String url = "https://api.github.com/search/repositories?q=stars:>1&sort=stars&order=desc";

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + accessToken);
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

		System.out.println(response.getBody());
	}

	/*@Override
	protected void configure(HttpSecurity http) throws Exception {

		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
		corsConfiguration.setAllowedOrigins(List.of("http://localhost:3000"));
		corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PUT","OPTIONS","PATCH", "DELETE"));
		corsConfiguration.setAllowCredentials(true);
		corsConfiguration.setExposedHeaders(List.of("Authorization"));

		// @formatter:off
		http
			.cors().configurationSource(request -> corsConfiguration).and()
			.authorizeRequests(a -> a
				.antMatchers("/", "/error", "/webjars/**").permitAll()
				.anyRequest().authenticated()
			)
			.exceptionHandling(e -> e
				.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
			)
			.csrf(c -> c
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
			)
			.logout(l -> l
				.logoutSuccessUrl("/").permitAll()
			)
			.oauth2Login();
		// @formatter:on
	}*/

	public static void main(String[] args) {
		SpringApplication.run(SocialApplication.class, args);
	}

}