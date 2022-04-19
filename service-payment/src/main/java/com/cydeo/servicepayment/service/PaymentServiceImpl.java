package com.cydeo.servicepayment.service;


import com.cydeo.servicepayment.dto.InstitutionsResponse;
import com.cydeo.servicepayment.dto.paymentReqBody.AccountIdentification;
import com.cydeo.servicepayment.dto.paymentReqBody.Address;
import com.cydeo.servicepayment.dto.paymentReqBody.Amount;
import com.cydeo.servicepayment.dto.paymentReqBody.Payee;
import com.cydeo.servicepayment.dto.paymentReqBody.PaymentAuthorizationBody;
import com.cydeo.servicepayment.dto.PaymentAuthorizationRequest;

import com.cydeo.servicepayment.dto.paymentReqBody.PaymentRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import yapily.ApiClient;
import yapily.ApiException;
import yapily.Configuration;
import yapily.auth.HttpBasicAuth;
import yapily.sdk.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService,ConfigPaymentDetailForService {


    private WebClient.Builder webClient;
    private ObjectMapper jacksonMapper;

    public PaymentServiceImpl(WebClient.Builder webClient, ObjectMapper jacksonMapper) {
        this.webClient = webClient;
        this.jacksonMapper = jacksonMapper;
    }

    private String institutionId = "modelo-sandbox";
    private String callback = "https://display-parameters.com/";

    private final String BASE_URI = "https://api.yapily.com";
    private final String client_id = "3fda055a-77f7-4496-a93e-adbc39b39011";
    private final String client_secret = "40316b6d-4656-409c-bd1b-ac1f11738ae8";
    private final String applicatonUserId = "tommy@gmail.com";
    private final String userUUID = "5f9965bc-175c-4654-ba8f-898f65cd37c0";

    //These lines are for Yapily
    private final String APPLICATION_ID = client_id;
    private final String APPLICATION_SECRET = client_secret;

// @Value("${client_secret}")
// private String client_secret;

    public static void main(String[] args) throws ApiException {

        PaymentServiceImpl paymentService = new PaymentServiceImpl();
        paymentService.getInstitutionsWithSdk();
    }

    public String generateToken() {

        String token = Base64.getEncoder().encodeToString(new String(this.client_id + ":" + this.client_secret).getBytes(StandardCharsets.UTF_8));
        log.info("Token generated by method : "+token);
        return token;

    }

    /**
     * curl --location --request GET 'https://api.yapily.com/institutions' \
     * --header 'Authorization: Basic {authToken}'
     */
    public InstitutionsResponse getInstitutions() {

        InstitutionsResponse authorization = webClient.build()
                .get()
                .uri(BASE_URI + "/institutions")
                .header("Authorization", "Basic " + generateToken())
                .retrieve()
                .bodyToMono(InstitutionsResponse.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(100)))
                .doOnError(error -> log.error("An error has occurred {}", error.getMessage()))
                .block();

        return authorization;
    }

    @Override

    public String accountAuth() throws JsonProcessingException {

//        log.info(getPayment().toString());
        return
                webClient.build()
                        .post()
                        .uri(BASE_URI + "/payment-auth-requests")
                        .header("Authorization", "Basic " + generateToken())
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(Flux.just(getPayment()), PaymentAuthorizationRequest.class)
                        .retrieve()
                        .bodyToMono(Object.class)
                        .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(100)))
                        .doOnError(error -> log.error("An error has occurred {}", error.getMessage()))
                        .block().toString();
    }

    @Override
    public com.cydeo.servicepayment.dto.PaymentResponse makePayment() {
        return null;
    }

    @Override
    public void authForPayment() {

    }


    @Override
    public String createPaymentAuthorization() throws JsonProcessingException {
        /**
         *
         *
         * this is body need to post
         * {
         *    "applicationUserId": "{{application-user-id}}",
         *    "institutionId": "{{institution-id}}",
         *    "paymentRequest": {
         *       "type": "DOMESTIC_PAYMENT",
         *       "paymentIdempotencyId": "1d54cf71bfe44b1b8e67247aed455d96",
         *       "reference": "REFERENCE",
         *       "contextType": "OTHER",
         *       "amount": {
         *          "amount": "4.00",
         *          "currency": "GBP"
         *       },
         *       "payee": {
         *          "name": "John Doe",
         *          "address": {
         *             "country": "GB"
         *          },
         *          "accountIdentifications": [
         *             {
         *                "type": "SORT_CODE",
         *                "identification": "123456"
         *             },
         *             {
         *                "type": "ACCOUNT_NUMBER",
         *                "identification": "12345678"
         *             }
         *          ]
         *       }
         *    }
         * }
         *
         * ****** this is body need to handle
         *
         * {
         *     "meta": {
         *         "tracingId": "274109a96f3c8ddd9d46c7d18e964eaf"
         *     },
         *     "data": {
         *         "id": "e7f1b269-943b-445b-8cea-b0556ceb8048",
         *         "userUuid": "27aa4942-de6a-4acc-b133-bdeee40c939e",
         *         "applicationUserId": "user12345",
         *         "institutionId": "monzo_ob",
         *         "status": "AWAITING_AUTHORIZATION",
         *         "createdAt": "2020-08-05T10:11:06.625Z",
         *         "featureScope": [
         *             "CREATE_DOMESTIC_SINGLE_PAYMENT",
         *             "EXISTING_PAYMENTS_DETAILS",
         *             "EXISTING_PAYMENT_INITIATION_DETAILS"
         *         ],
         *         "authorisationUrl": "https://verify.monzo.com/open-banking/authorize?client_id=oauth2client_00009pp1CRt4KarIZM7Pr1&response_type=code+id_token&state=6f3926d09372453595fac3fa6754e01c&nonce=6f3926d09372453595fac3fa6754e01c&scope=openid+payments&redirect_uri=https%3A%2F%2Fauth.yapily.com%2F&request=eyJraWQiOiJPNWp3ZXpxTlNzeVlacHotZHpfVUhEbkJINHciLCJhbGciOiJQUzI1NiJ9.eyJhdWQiOiJodHRwczovL2FwaS5tb256by5jb20vb3Blbi1iYW5raW5nLyIsInNjb3BlIjoib3BlbmlkIHBheW1lbnRzIiwiaXNzIjoib2F1dGgyY2xpZW50XzAwMDA5cHAxQ1J0NEthcklaTTdQcjEiLCJjbGllbnRfaWQiOiJvYXV0aDJjbGllbnRfMDAwMDlwcDFDUnQ0S2FySVpNN1ByMSIsInJlc3BvbnNlX3R5cGUiOiJjb2RlIGlkX3Rva2VuIiwicmVkaXJlY3RfdXJpIjoiaHR0cHM6Ly9hdXRoLnlhcGlseS5jb20vIiwic3RhdGUiOiI2ZjM5MjZkMDkzNzI0NTM1OTVmYWMzZmE2NzU0ZTAxYyIsImNsYWltcyI6eyJpZF90b2tlbiI6eyJhY3IiOnsidmFsdWVzIjpbInVybjpvcGVuYmFua2luZzpwc2QyOnNjYSJdLCJlc3NlbnRpYWwiOnRydWV9LCJvcGVuYmFua2luZ19pbnRlbnRfaWQiOnsidmFsdWUiOiJvYnBpc3Bkb21lc3RpY3BheW1lbnRjb25zZW50XzAwMDA5eG9ESXJMQnowYlVma0tOaFIiLCJlc3NlbnRpYWwiOnRydWV9fSwidXNlcmluZm8iOnsib3BlbmJhbmtpbmdfaW50ZW50X2lkIjp7InZhbHVlIjoib2JwaXNwZG9tZXN0aWNwYXltZW50Y29uc2VudF8wMDAwOXhvRElyTEJ6MGJVZmtLTmhSIiwiZXNzZW50aWFsIjp0cnVlfX19LCJub25jZSI6IjZmMzkyNmQwOTM3MjQ1MzU5NWZhYzNmYTY3NTRlMDFjIiwianRpIjoiMjJhZTRmODgtNjgzZi00NWYwLTgxNDYtZjdhN2U3ZWY2NzA5IiwiaWF0IjoxNTk2NjIyMjY2LCJleHAiOjE1OTY2MjQwNjZ9.fGz7g_tE_oAtbvBqmbcvzBdGFL79NIv2mA99ZjvMVlqqqVW1mohY2_1MaRE27w5WVzrOYYe1h-gCKTA95m5znZFD3eWgDLiqTTaB08KZrv4Vi0tmzsITDcXSLNQKj3N8znck7iTPUYHTqVZyMcoxV7e3hPHrSexVzo5eVtaIX5AHAG0tu3_qWyIRe7h48D55jJGyc7bD7bt5Q73jsEgz4sYos15pOYkxd7W-74arLS10Pube-SjGWoBVFXtPj_pwaxbj9Ub-P7WuhMw5r9Qvaf0rB0Yg0QdTiaCvn05oRD7ssVTezGqHEf5aDu3HXHBfD45f0siFxo5a8QGM2fGaYA",
         *         "qrCodeUrl": "https://images.yapily.com/image/080b9721-9cc5-4ec5-a882-8aab5c67752a/1596622266?size=0"
         *     }
         * }
         */

        log.info("payment body created : ");
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            String jsonBodyOfPaymentReq = objectMapper.writeValueAsString(getPayment());
            log.info("BODY IS : "+ jsonBodyOfPaymentReq);
        }catch(IOException e){
            e.printStackTrace();

        }
        String authorization;
        try {
             authorization = webClient.build()
                    .post()
                    .uri(BASE_URI + "/payment-auth-requests")
                    .header("Authorization", "Basic " + generateToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(Mono.just(getPaymentAuthorizationBody()), PaymentAuthorizationBody.class)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(100)))
                    .doOnError(error -> log.error("An error has occurred {}", error.getMessage()))
                    .block().toString();
        }catch(Exception e){
             authorization = webClient.build()
                    .post()
                    .uri(BASE_URI + "/payment-auth-requests")
                    .header("Authorization", "Basic " + generateToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(Mono.just(getPayment()), PaymentAuthorizationRequest.class)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(100)))
                    .doOnError(error -> log.error("An error has occurred on inside catch block{}", error.getMessage()))
                    .block().toString();
        }

        return authorization ;
    }

    @Override
    public String getConsent() {

        return
        webClient.build()
                .get()
                .uri(uriBuilder -> {
                    return uriBuilder
                            .path("/users/"+this.userUUID+"/consents")
                            .queryParam("institutionId",this.institutionId)
                    .build();
                })
                .header("Authorization","Basic "+generateToken())
                .header(HttpHeaders.CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(Object.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(100)))
                .doOnError(error -> log.error("An error has occurred while getting concent token {}", error.getMessage()))
                .block().toString();


    }

    public PaymentAuthorizationBody getPayment() throws JsonProcessingException {

        PaymentAuthorizationBody paymentAuthorizationBody = new PaymentAuthorizationBody();
        log.info("payment body created by getpaymentBody : ");
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            String jsonBodyOfPaymentReq = objectMapper.writeValueAsString(paymentAuthorizationBody);
            log.info(jsonBodyOfPaymentReq);
        }catch(IOException e){
            e.printStackTrace();

        }
        return paymentAuthorizationBody;
    }

    public void loginWithSdk() {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        // Configure the API authentication
        HttpBasicAuth basicAuth = (HttpBasicAuth) defaultClient.getAuthentication("basicAuth");
        // Replace these demo constants with your application credentials
        basicAuth.setUsername(APPLICATION_ID);
        basicAuth.setPassword(APPLICATION_SECRET);
    }

    public void getInstitutionsWithSdk() throws ApiException {
        InstitutionsApi institutionsApi = new InstitutionsApi();
        List<Institution> institutions = institutionsApi.getInstitutionsUsingGET("1.0").getData();
    }

    public void loginWithSdkForMultipleApplicationCases() {
        ApiClient applicationClient = new ApiClient();
        // Configure the API authentication
        HttpBasicAuth basicAuth = (HttpBasicAuth) applicationClient.getAuthentication("basicAuth");
        // Replace these demo constants with your application credentials
        basicAuth.setUsername(APPLICATION_ID);
        basicAuth.setPassword(APPLICATION_SECRET);
        InstitutionsApi institutionsApi = new InstitutionsApi();
        institutionsApi.setApiClient(applicationClient);
    }

    public void getUrlwitSdk() throws ApiException {
        final AccountsApi accountsApi = new AccountsApi();
        AccountAuthorisationRequest accountAuthorisationRequest = new AccountAuthorisationRequest();
        accountAuthorisationRequest.setApplicationUserId(APPLICATION_ID);
        accountAuthorisationRequest.setInstitutionId(institutionId);
/**
 * Use the defaults
 */
        accountAuthorisationRequest.setAccountRequest(new AccountRequest());
        ApiResponseOfAuthorisationRequestResponse authorizationResponse = accountsApi.initiateAccountRequestUsingPOST(accountAuthorisationRequest, "1.0", null, null, null);
        String directUrl = authorizationResponse.getData().getAuthorisationUrl();
    }

    @Override
    public Amount getAmount() {
        //todo amount dynamic
        Amount amount = new Amount();
        amount.setAmount("10");
        return amount;
    }

    @Override
    public Address getAddress() {
        Address address= new Address();
        return address;
    }

    @Override
    public Payee getPayee() {
        Payee payee = new Payee();
        payee.setAccountIdentifications(getAccountIdentificationList());
        return payee;
    }

    @Override
    public PaymentRequest getPaymentRequest() {
        PaymentRequest paymentRequest = new PaymentRequest();
        log.info("PaymentRequest from Config  interface");
        paymentRequest.setPayee(getPayee());
        paymentRequest.setAmount(getAmount());
        return paymentRequest;
    }

    @Override
    public PaymentAuthorizationBody getPaymentAuthorizationBody() {
        PaymentAuthorizationBody paymentAuthorizationBody = new PaymentAuthorizationBody();

        paymentAuthorizationBody.setApplicationUserId(this.applicatonUserId);
        paymentAuthorizationBody.setInstitutionId(this.institutionId);
        log.info("Payment Authorization Body from Config Interface");

//        from config method
        paymentAuthorizationBody
                .setPaymentRequest(
                        getPaymentRequest());


        ObjectMapper objectMapper = new ObjectMapper();
        try{
            String jsonBodyOfPaymentReq = objectMapper.writeValueAsString(paymentAuthorizationBody);
            log.info(jsonBodyOfPaymentReq);
        }catch(IOException e){
            e.printStackTrace();

        }
        return paymentAuthorizationBody;
    }

    @Override
    public List<AccountIdentification> getAccountIdentificationList() {
        List<AccountIdentification> listOfAccounIdentification = new ArrayList<>();


        com.cydeo.servicepayment.dto.paymentReqBody.AccountIdentification accountIdentification1 = new com.cydeo.servicepayment.dto.paymentReqBody.AccountIdentification();
        accountIdentification1.setIdentification("123456");
        accountIdentification1.setType(com.cydeo.servicepayment.dto.paymentReqBody.AccountIdentification.TypeEnum.SORT_CODE);
        com.cydeo.servicepayment.dto.paymentReqBody.AccountIdentification accountIdentification2 = new com.cydeo.servicepayment.dto.paymentReqBody.AccountIdentification();
        accountIdentification2.setIdentification("12345678");
        accountIdentification2.setType(AccountIdentification.TypeEnum.fromValue("ACCOUNT_NUMBER"));

        listOfAccounIdentification.add(accountIdentification1);
        listOfAccounIdentification.add(accountIdentification2);
        log.info("Account Identification  from Config Interface");


        return listOfAccounIdentification;
    }
}
