package com.foodorder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodorder.food.FoodItem;
import com.foodorder.food.FoodRepository;
import com.foodorder.cart.CartRepository;
import com.foodorder.order.OrderRepository;
import com.foodorder.security.JwtService;
import com.foodorder.user.UserEntity;
import com.foodorder.user.UserRepository;
import com.foodorder.user.UserRole;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FoodOrderIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired FoodRepository foods;
    @Autowired CartRepository carts;
    @Autowired OrderRepository orders;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @BeforeEach
    void addFood() {
        carts.deleteAll();
        orders.deleteAll();
        foods.deleteAll();
        users.deleteAll();
        foods.save(new FoodItem("Garden Bowl", "Fresh greens and grains", "Healthy", new BigDecimal("12.50"),
                "https://example.com/bowl.jpg", true, true));
    }

    @Test
    void customerCanRegisterBrowseBuildCartAndPlaceOrder() throws Exception {
        String register = """
                {"firstName":"Alex","lastName":"Shah","mobile":"+919876543210",
                 "email":"alex@example.com","password":"StrongPass1"}
                """;
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(register))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role", is("CUSTOMER")))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(response).get("token").asText();

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alex@example.com\",\"password\":\"StrongPass1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role", is("CUSTOMER")));

        mvc.perform(get("/api/foods").param("search", "bowl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name", is("Garden Bowl")));

        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount", is(0)));

        long foodId = foods.findAll().get(0).getId();
        mvc.perform(post("/api/cart/items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodId\":" + foodId + ",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal", is(25.0)));

        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryAddress\":\"12 Garden Street, Bengaluru\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PLACED")))
                .andExpect(jsonPath("$.items[0].quantity", is(2)));

        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount", is(0)));

        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deliveryAddress", is("12 Garden Street, Bengaluru")));
    }

    @Test
    void publicRegistrationCannotGrantAdminRole() throws Exception {
        String request = """
                                {"firstName":"Sam","lastName":"Lee","mobile":"+919876543211",
                                 "email":"sam@example.com","password":"StrongPass1","role":"ADMIN"}
                                """;
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role", is("CUSTOMER")));

        var user = users.findByEmail("sam@example.com").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(user.getPassword()).startsWith("$2");

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());
    }

    @Test
    void adminOrderRoutesCannotBeUsedByCustomers() throws Exception {
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Taylor","lastName":"Park","mobile":"+919876543212",
                                 "email":"taylor@example.com","password":"StrongPass1"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(response).get("token").asText();

        mvc.perform(get("/api/orders/admin").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidCredentialsAreRejectedAndProtectedRoutesRequireAuthentication() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"WrongPass1\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/cart").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        mvc.perform(get("/api/foods").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void corsAllowsTheViteOriginAndRequiredBearerHeaders() throws Exception {
        mvc.perform(options("/api/auth/register")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers",
                        org.hamcrest.Matchers.containsString("authorization")));

        mvc.perform(options("/api/auth/register")
                        .header("Origin", "http://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCartIsIsolatedAndAdministratorEndpointsRequireAdminAuthority() throws Exception {
        String firstToken = registerAndGetToken("casey@example.com", "+919876543213");
        String secondToken = registerAndGetToken("jordan@example.com", "+919876543214");
        long foodId = foods.findAll().get(0).getId();

        mvc.perform(post("/api/cart/items").header("Authorization", "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodId\":" + foodId + ",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount", is(1)));

        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount", is(0)));

        mvc.perform(get("/api/orders/admin").header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        var admin = users.save(new UserEntity("Store", "Admin", "+10000000001", "store-admin@example.com",
                passwordEncoder.encode("AdminPass123"), UserRole.ADMIN));
        String adminToken = jwtService.createToken(admin);
        mvc.perform(get("/api/orders/admin").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    private String registerAndGetToken(String email, String mobile) throws Exception {
        String request = """
                {"firstName":"Food","lastName":"Customer","mobile":"%s",
                 "email":"%s","password":"StrongPass1"}
                """.formatted(mobile, email);
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }
}
