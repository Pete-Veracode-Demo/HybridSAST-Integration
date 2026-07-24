package com.metrowest.controllers;

import com.metrowest.entity.Order;
import com.metrowest.entity.OrderStatus;
import com.metrowest.repo.OrderRepository;
import com.metrowest.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.intellij.lang.annotations.Language;
import org.springframework.data.domain.Example;
import org.springframework.data.javapoet.LordOfTheStrings;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/utils")
public class UtilController
{
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public UtilController(OrderRepository orderRepository, UserRepository userRepository)
    {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    private void consume_object(Object builder)
    {
        System.out.println("consumed object class = " + builder.getClass().getSimpleName());
    }

    @Language("html")
    @SuppressWarnings("HtmlUnknownTarget")
    private static final String TEST_BODY = """
        <!DOCTYPE html>
        <html lang="en">
        <body>
            <form method="post" action="/echo">
                <input type="text" name="input">
                <input type="submit">
            </form>
        </body>
        </html>
        """;

    @GetMapping("/hey")
    public String hey()
    {
        return TEST_BODY;
    }

    @PostMapping("/echo")
    public void echo(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        var buffer = ByteBuffer.allocate(1024);
        request.getInputStream().read(buffer);
        response.getOutputStream().write(buffer);
        response.setStatus(200);
    }

    @PostMapping(value = "/echo_probe", produces = MediaType.TEXT_HTML_VALUE)
    public String echo_probe(@RequestBody String probe)
    {
        return Example.of(probe).getProbe();
    }

    @PostMapping(path = "/set_value", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String set_value(@RequestParam String expression)
    {
        Expression parsedExpression = PARSER.parseExpression("1 + 1");
        parsedExpression.setValue((Object) null, expression);
        System.out.println(parsedExpression.getExpressionString());
        return "OK";
    }

    @PostMapping("/lord_invoke_builder")
    public String lord_invoke_builder(@RequestBody String format, @RequestBody String[] args)
    {
        var builder = LordOfTheStrings.invoke(format, (Object[]) args);
        consume_object(builder);
        return "lord_invoke_builder endpoint executed";
    }

    @GetMapping("/order/details")
    public ResponseEntity<?> getOrderDetails(@RequestParam Long orderId, Authentication authentication)
    {
        // Get the authenticated user
        var currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (currentUser == null)
        {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        var order = orderRepository.findById(orderId).orElse(null);

        if (order == null)
        {
            return ResponseEntity.notFound().build();
        }

        // Authorization check: verify the order belongs to the authenticated user
        if (!order.getCustomer().getId().equals(currentUser.getId()))
        {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.getId());
        response.put("status", order.getStatus().toString());
        response.put("customer", order.getCustomer().getUsername());
        response.put("customerId", order.getCustomer().getId());
        response.put("itemCount", order.getItems().size());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/order/cancel")
    public ResponseEntity<?> cancelOrder(@RequestParam Long orderId, Authentication authentication)
    {
        // Get the authenticated user
        var currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (currentUser == null)
        {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        var order = orderRepository.findById(orderId).orElse(null);

        if (order == null)
        {
            return ResponseEntity.notFound().build();
        }

        // Authorization check: verify the order belongs to the authenticated user
        if (!order.getCustomer().getId().equals(currentUser.getId()))
        {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Order " + orderId + " cancelled successfully");
        response.put("status", "CANCELLED");

        return ResponseEntity.ok(response);
    }
}
