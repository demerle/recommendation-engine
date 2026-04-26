package reccommendation_engine.RecEngine.services.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListGraphQlError;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListGraphQlRequest;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListGraphQlResponse;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListCollectionDataDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListCollectionDto;
import reccommendation_engine.RecEngine.domain.entities.MediaType;
import reccommendation_engine.RecEngine.services.AniListClient;
import reccommendation_engine.RecEngine.services.AniListClientException;
import reccommendation_engine.RecEngine.services.AniListUserNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AniListClientImpl implements AniListClient {

    private static final ParameterizedTypeReference<AniListGraphQlResponse<AniListMediaListCollectionDataDto>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private static final String USER_MEDIA_LIST_QUERY = """
            query ($userName: String!, $type: MediaType!) {
              MediaListCollection(userName: $userName, type: $type) {
                userName
                type
                lists {
                  name
                  status
                  entries {
                    id
                    status
                    progress
                    score
                    updatedAt
                    media {
                      id
                      type
                      format
                      status
                      episodes
                      chapters
                      description
                      startDate {
                        year
                        month
                        day
                      }
                      title {
                        english
                        romaji
                        nativeTitle: native
                      }
                      genres
                      tags {
                        name
                      }
                      studios {
                        edges {
                          isMain
                          node {
                            name
                          }
                        }
                      }
                      staff {
                        edges {
                          role
                          roleNotes
                          node {
                            name {
                              full
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

    private final RestClient aniListRestClient;

    public AniListClientImpl(RestClient aniListRestClient) {
        this.aniListRestClient = aniListRestClient;
    }

    @Override
    public AniListMediaListCollectionDto fetchUserMediaList(String username, MediaType mediaType) {
        AniListGraphQlRequest request = AniListGraphQlRequest.builder()
                .query(USER_MEDIA_LIST_QUERY)
                .variables(Map.of(
                        "userName", username,
                        "type", mediaType.name()
                ))
                .build();

        AniListGraphQlResponse<AniListMediaListCollectionDataDto> response;
        try {
            response = aniListRestClient.post()
                    .uri("")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (clientRequest, clientResponse) -> {
                        throw new AniListClientException("AniList request failed with status " + clientResponse.getStatusCode());
                    })
                    .body(RESPONSE_TYPE);
        } catch (RestClientException ex) {
            throw new AniListClientException("Failed to call AniList API for username " + username, ex);
        }

        if (response == null) {
            throw new AniListClientException("AniList API returned an empty response for username " + username);
        }

        if (response.hasErrors()) {
            handleGraphQlErrors(username, response.getErrors());
        }

        if (response.getData() == null || response.getData().getMediaListCollection() == null) {
            throw new AniListUserNotFoundException(username);
        }

        return response.getData().getMediaListCollection();
    }

    private void handleGraphQlErrors(String username, List<AniListGraphQlError> errors) {
        String combinedMessages = errors.stream()
                .map(AniListGraphQlError::getMessage)
                .collect(Collectors.joining("; "));

        String normalizedMessages = combinedMessages.toLowerCase();
        if (normalizedMessages.contains("not found")) {
            throw new AniListUserNotFoundException(username);
        }

        throw new AniListClientException("AniList GraphQL error for username " + username + ": " + combinedMessages);
    }
}
