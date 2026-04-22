package com.naturecode.langchain.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class ClaimServiceTest {

  private MockWebServer mockWebServer;
  private ClaimService claimService;

  @BeforeEach
  public void setUp() {
    mockWebServer = new MockWebServer();
    try {
      mockWebServer.start();
    } catch (IOException e) {
      throw new RuntimeException("Failed to start MockWebServer", e);
    }
    claimService = new ClaimService("http://localhost:" + mockWebServer.getPort());
  }

  @AfterEach
  public void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  // --- getClaimStatusById ---

  @Test
  void getClaimStatusById_success() throws InterruptedException {
    mockWebServer.enqueue(new MockResponse()
        .setBody("APPROVED")
        .addHeader("Content-Type", "text/plain"));

    String result = claimService.getClaimStatusById("CLM-001");

    assertThat(result).isEqualTo("APPROVED");

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getPath()).isEqualTo("/claims/CLM-001");
  }

  @Test
  void getClaimStatusById_serverError_retriesThenReturnsFallback() throws InterruptedException {
    // retry(1) = 2 total attempts before onErrorResume kicks in
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    String result = claimService.getClaimStatusById("CLM-001");

    assertThat(result).isEqualTo("Error: Unable to fetch claim CLM-001");
    assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
  }

  @Test
  void getClaimStatusById_firstAttemptFails_retrySucceeds() throws InterruptedException {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));
    mockWebServer.enqueue(new MockResponse()
        .setBody("PENDING")
        .addHeader("Content-Type", "text/plain"));

    String result = claimService.getClaimStatusById("CLM-001");

    assertThat(result).isEqualTo("PENDING");
    assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
  }

  // --- getClaimStatusByCustomer ---

  @Test
  void getClaimStatusByCustomer_success() throws InterruptedException {
    mockWebServer.enqueue(new MockResponse()
        .setBody("[{\"claimId\":\"CLM-001\",\"status\":\"APPROVED\"}]")
        .addHeader("Content-Type", "application/json"));

    String result = claimService.getClaimStatusByCustomer("CUST-123");

    assertThat(result).contains("CLM-001");

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getPath()).isEqualTo("/claims?customerId=CUST-123");
  }

  @Test
  void getClaimStatusByCustomer_serverError_retriesThenReturnsFallback() throws InterruptedException {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    String result = claimService.getClaimStatusByCustomer("CUST-123");

    assertThat(result).isEqualTo("Error: Unable to fetch claims for CUST-123");
    assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
  }

  @Test
  void getClaimStatusByCustomer_firstAttemptFails_retrySucceeds() throws InterruptedException {
    mockWebServer.enqueue(new MockResponse().setResponseCode(503));
    mockWebServer.enqueue(new MockResponse()
        .setBody("NO_CLAIMS")
        .addHeader("Content-Type", "text/plain"));

    String result = claimService.getClaimStatusByCustomer("CUST-123");

    assertThat(result).isEqualTo("NO_CLAIMS");
    assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
  }
}
