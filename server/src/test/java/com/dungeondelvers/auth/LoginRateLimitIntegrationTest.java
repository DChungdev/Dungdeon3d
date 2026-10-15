package com.dungeondelvers.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.dungeondelvers.MockPersistence;
import com.dungeondelvers.persistence.repository.AccountRepository;
import com.dungeondelvers.persistence.repository.CharacterRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@MockPersistence
class LoginRateLimitIntegrationTest {

    @LocalServerPort
    int port;

    @MockitoBean
    AccountRepository accountRepository;

    @MockitoBean
    CharacterRepository characterRepository;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void sixthLoginAttemptWithinAMinuteIsRejected() throws Exception {
        when(accountRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        for (int i = 0; i < 5; i++) {
            assertThat(login("victim@test.dev").statusCode()).isEqualTo(401);
        }
        HttpResponse<String> limited = login("victim@test.dev");
        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(limited.body()).contains("\"code\":\"RATE_LIMITED\"");

        // Other accounts from the same client are not locked out.
        assertThat(login("someone-else@test.dev").statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> login(String email) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"email\":\"" + email + "\",\"password\":\"wrongpass1\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
