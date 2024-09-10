import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final HttpClient httpClient; // RestClient/ Webclient
    private final ObjectMapper objectMapper;
    // Можно было и без отдельного класса Limiter, но не хотелось все одну кучу
    private final Limiter limiter;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.limiter = new Limiter(timeUnit, requestLimit);
    }

    //try-catch не добавил ради экономии времени
    public void createDocument(Document document, String signature) throws IOException, InterruptedException {
        limiter.acquire();

        try {
            String jsonRequest = objectMapper.writeValueAsString(document);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("Failed to create document: " + response.body()); //тут нужно кастомное исключение + перехватчик
            }
        } finally {
            limiter.release();
        }
    }

    private static class Limiter {
        private final Semaphore semaphore;

        Limiter(TimeUnit timeUnit, int requestLimit) {
            this.semaphore = new Semaphore(requestLimit);

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            long periodMillis = timeUnit.toMillis(1);

            scheduler.scheduleAtFixedRate(this::resetSemaphore, periodMillis, periodMillis, TimeUnit.MILLISECONDS);
        }

        void acquire() throws InterruptedException {
            semaphore.acquire();
        }

        void release() {
            semaphore.release();
        }

        private void resetSemaphore() {
            semaphore.release(semaphore.drainPermits());
        }
    }

    public class Document {
        @JsonProperty("description")
        private Description description;
        @JsonProperty("doc_id")
        private String docId;
        @JsonProperty("doc_status")
        private String docStatus;
        @JsonProperty("doc_type")
        private String docType;
        @JsonProperty("importRequest")
        private boolean importRequest;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("participant_inn")
        private String participantInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        private LocalDate productionDate;
        @JsonProperty("production_type")
        private String productionType;
        @JsonProperty("reg_date")
        private LocalDate regDate;
        @JsonProperty("reg_number")
        private String regNumber;
        @JsonProperty("products")
        private List<Product> products;

        public Description getDescription() { return description; }
        public void setDescription(Description description) { this.description = description; }
        public String getDocId() { return docId; }
        public void setDocId(String docId) { this.docId = docId; }
        public String getDocStatus() { return docStatus; }
        public void setDocStatus(String docStatus) { this.docStatus = docStatus; }
        public String getDocType() { return docType; }
        public void setDocType(String docType) { this.docType = docType; }
        public boolean isImportRequest() { return importRequest; }
        public void setImportRequest(boolean importRequest) { this.importRequest = importRequest; }
        public String getOwnerInn() { return ownerInn; }
        public void setOwnerInn(String ownerInn) { this.ownerInn = ownerInn; }
        public String getParticipantInn() { return participantInn; }
        public void setParticipantInn(String participantInn) { this.participantInn = participantInn; }
        public String getProducerInn() { return producerInn; }
        public void setProducerInn(String producerInn) { this.producerInn = producerInn; }
        public LocalDate getProductionDate() { return productionDate; }
        public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
        public String getProductionType() { return productionType; }
        public void setProductionType(String productionType) { this.productionType = productionType; }
        public LocalDate getRegDate() { return regDate; }
        public void setRegDate(LocalDate regDate) { this.regDate = regDate; }
        public String getRegNumber() { return regNumber; }
        public void setRegNumber(String regNumber) { this.regNumber = regNumber; }
        public List<Product> getProducts() { return products; }
        public void setProducts(List<Product> products) { this.products = products; }
    }

    public class Description {
        @JsonProperty("participantInn")
        private String participantInn;

        public String getParticipantInn() { return participantInn; }
        public void setParticipantInn(String participantInn) { this.participantInn = participantInn; }
    }

    public class Product {
        @JsonProperty("certificate_document")
        private String certificateDocument;
        @JsonProperty("certificate_document_date")
        private String certificateDocumentDate;
        @JsonProperty("certificate_document_number")
        private String certificateDocumentNumber;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        private LocalDate productionDate;
        @JsonProperty("tnved_code")
        private String tnvedCode;
        @JsonProperty("uit_code")
        private String uitCode;
        @JsonProperty("uitu_code")
        private String uituCode;

        public String getCertificateDocument() { return certificateDocument; }
        public void setCertificateDocument(String certificateDocument) { this.certificateDocument = certificateDocument; }
        public String getCertificateDocumentDate() { return certificateDocumentDate; }
        public void setCertificateDocumentDate(String certificateDocumentDate) { this.certificateDocumentDate = certificateDocumentDate; }
        public String getCertificateDocumentNumber() { return certificateDocumentNumber; }
        public void setCertificateDocumentNumber(String certificateDocumentNumber) { this.certificateDocumentNumber = certificateDocumentNumber; }
        public String getOwnerInn() { return ownerInn; }
        public void setOwnerInn(String ownerInn) { this.ownerInn = ownerInn; }
        public String getProducerInn() { return producerInn; }
        public void setProducerInn(String producerInn) { this.producerInn = producerInn; }
        public LocalDate getProductionDate() { return productionDate; }
        public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
        public String getTnvedCode() { return tnvedCode; }
        public void setTnvedCode(String tnvedCode) { this.tnvedCode = tnvedCode; }
        public String getUitCode() { return uitCode; }
        public void setUitCode(String uitCode) { this.uitCode = uitCode; }
        public String getUitUCode() { return uituCode; }
        public void setUitUCode(String uituCode) { this.uituCode = uituCode; }
    }
}
