package com.selsup.crpt;

import com.selsup.crpt.api.CrptApi;
import com.selsup.crpt.api.CrptApi.Description;
import com.selsup.crpt.api.CrptApi.Product;
import lombok.extern.log4j.Log4j2;
import okhttp3.Response;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.selsup.crpt.api.CrptApi.Document;

@Log4j2
@SpringBootApplication
public class CrptApplication {
    private static final String SOME_SIGNATURE = "some_signature";

    public static void main(String[] args) {
        SpringApplication.run(CrptApplication.class, args);

        var crptApi = new CrptApi(TimeUnit.SECONDS, 10);
        var document = buildDocument();
        Response response = crptApi.createDocument(document, SOME_SIGNATURE);
        log.warn("[main] response={}", response);
    }

    private static Document buildDocument() {
        var description = buildDescription();
        var product = buildProduct();
        var document = new Document();
        document.setDescription(description);
        document.setProducts(List.of(product));
        document.setDocStatus("doc_status");
        document.setDocType("LP_INTRODUCE_GOODS");
        document.setProductionDate("2023-07-08");
        return document;
    }

    private static Product buildProduct() {
        var product = new Product();
        product.setOwnerInn("213792149821");
        product.setProductionDate("2023-07-08");
        return product;
    }

    private static Description buildDescription() {
        var description = new Description();
        description.setParticipantInn("1234");
        return description;
    }
}
