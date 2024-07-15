package com.example.service;

import com.example.model.Issue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Service
public class IssueService {

    @Autowired
    OAuth2AuthorizedClientService authorizedClientService;

    public Flux<Issue> getIssues(String language, String searchText, OAuth2AuthenticationToken authentication) {
        List<String> repos = getRepos(language, authentication);
        WebClient webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken(authentication))
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .build();

        return Flux.fromIterable(repos)
                .log()
                .flatMap(repo -> {
                    System.out.println("Calling " + repo);
                    Flux<Issue> issue =  webClient.get()
                            .uri("https://api.github.com/repos/" + repo + "/issues")
                            .retrieve()
                            .bodyToFlux(Issue.class);
                    return issue;
                })
                .filter(issue -> {
                     System.out.println("Recieved issue from " + issue.getRepository_url());
                     //return (issue.getTitle() != null && issue.getTitle().contains(searchText));
                     return (issue.getTitle() != null && issue.getTitle().contains(searchText)) || (issue.getBody() != null && issue.getBody().contains(searchText));
                })
                .filter(issue -> issue.getPullRequest() == null);
    }

    private List<String> getRepos(String language, OAuth2AuthenticationToken authentication) {
        String url = "https://api.github.com/search/repositories?q=language:" + language + "&sort=forks&order=desc&per_page=30";

        String response = callGithubApi(url, authentication);

        // Parse the response to get the list of repositories
        // This depends on the structure of the response. Here is a basic example:
        JSONObject jsonObject = new JSONObject(response);
        JSONArray items = jsonObject.getJSONArray("items");

        List<String> repos = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            repos.add(item.getString("full_name"));
        }

        return repos;
    }

    private String callGithubApi(String url, OAuth2AuthenticationToken authentication) {
        String accessToken = getAccessToken(authentication);

        WebClient webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .build();

        String response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return response;
    }

    private String getAccessToken(OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName()
        );

        return authorizedClient.getAccessToken().getTokenValue();
    }
}
