package reccommendation_engine.RecEngine.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListCollectionDto;
import reccommendation_engine.RecEngine.services.AniListClientException;
import reccommendation_engine.RecEngine.services.AniListUserNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AniListClientImplTest {

    private static final String BASE_URL = "https://graphql.anilist.co";

    private MockRestServiceServer mockServer;
    private AniListClientImpl aniListClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        aniListClient = new AniListClientImpl(builder.build());
    }

    @Test
    void fetchUserMediaListReturnsCollectionForSuccessfulResponse() {
        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(successResponseBody(), MediaType.APPLICATION_JSON));

        AniListMediaListCollectionDto response = aniListClient.fetchUserMediaList(
                "test-user",
                reccommendation_engine.RecEngine.domain.entities.MediaType.ANIME
        );

        assertNotNull(response);
        assertEquals("test-user", response.getUserName());
        assertEquals("ANIME", response.getType());
        assertEquals(1, response.getLists().size());
        assertEquals(1, response.getLists().get(0).getEntries().size());
        assertEquals("Cowboy Bebop", response.getLists().get(0).getEntries().get(0).getMedia().getTitle().getEnglish());
        mockServer.verify();
    }

    @Test
    void fetchUserMediaListThrowsUserNotFoundWhenGraphQlReportsMissingUser() {
        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(notFoundGraphQlResponseBody(), MediaType.APPLICATION_JSON));

        assertThrows(
                AniListUserNotFoundException.class,
                () -> aniListClient.fetchUserMediaList(
                        "missing-user",
                        reccommendation_engine.RecEngine.domain.entities.MediaType.ANIME
                )
        );

        mockServer.verify();
    }

    @Test
    void fetchUserMediaListThrowsClientExceptionForOtherGraphQlErrors() {
        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(genericGraphQlErrorResponseBody(), MediaType.APPLICATION_JSON));

        AniListClientException exception = assertThrows(
                AniListClientException.class,
                () -> aniListClient.fetchUserMediaList(
                        "test-user",
                        reccommendation_engine.RecEngine.domain.entities.MediaType.MANGA
                )
        );

        assertEquals(
                "AniList GraphQL error for username test-user: upstream validation failed",
                exception.getMessage()
        );
        mockServer.verify();
    }

    @Test
    void fetchUserMediaListThrowsUserNotFoundWhenDataIsMissing() {
        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"data\":{\"MediaListCollection\":null}}", MediaType.APPLICATION_JSON));

        assertThrows(
                AniListUserNotFoundException.class,
                () -> aniListClient.fetchUserMediaList(
                        "ghost-user",
                        reccommendation_engine.RecEngine.domain.entities.MediaType.ANIME
                )
        );

        mockServer.verify();
    }

    @Test
    void fetchUserMediaListThrowsClientExceptionForHttpErrors() {
        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        AniListClientException exception = assertThrows(
                AniListClientException.class,
                () -> aniListClient.fetchUserMediaList(
                        "test-user",
                        reccommendation_engine.RecEngine.domain.entities.MediaType.ANIME
                )
        );

        assertEquals("AniList request failed with status 500 INTERNAL_SERVER_ERROR", exception.getMessage());
        mockServer.verify();
    }

    private String successResponseBody() {
        return """
                {
                  "data": {
                    "MediaListCollection": {
                      "userName": "test-user",
                      "type": "ANIME",
                      "lists": [
                        {
                          "name": "Completed",
                          "status": "COMPLETED",
                          "entries": [
                            {
                              "id": 101,
                              "status": "COMPLETED",
                              "progress": 26,
                              "score": 9.0,
                              "updatedAt": 1714099200,
                              "media": {
                                "id": 1,
                                "type": "ANIME",
                                "format": "TV",
                                "status": "FINISHED",
                                "episodes": 26,
                                "chapters": null,
                                "description": "Space bounty hunters.",
                                "startDate": {
                                  "year": 1998,
                                  "month": 4,
                                  "day": 3
                                },
                                "title": {
                                  "english": "Cowboy Bebop",
                                  "romaji": "Cowboy Bebop",
                                  "nativeTitle": "カウボーイビバップ"
                                },
                                "genres": ["Action", "Sci-Fi"],
                                "tags": [
                                  { "name": "Space" }
                                ],
                                "studios": {
                                  "edges": [
                                    {
                                      "isMain": true,
                                      "node": {
                                        "name": "Sunrise"
                                      }
                                    }
                                  ]
                                },
                                "staff": {
                                  "edges": [
                                    {
                                      "role": "Director",
                                      "roleNotes": [],
                                      "node": {
                                        "name": {
                                          "full": "Shinichiro Watanabe"
                                        }
                                      }
                                    }
                                  ]
                                }
                              }
                            }
                          ]
                        }
                      ]
                    }
                  }
                }
                """;
    }

    private String notFoundGraphQlResponseBody() {
        return """
                {
                  "data": {
                    "MediaListCollection": null
                  },
                  "errors": [
                    {
                      "message": "User not found"
                    }
                  ]
                }
                """;
    }

    private String genericGraphQlErrorResponseBody() {
        return """
                {
                  "data": null,
                  "errors": [
                    {
                      "message": "upstream validation failed"
                    }
                  ]
                }
                """;
    }
}
