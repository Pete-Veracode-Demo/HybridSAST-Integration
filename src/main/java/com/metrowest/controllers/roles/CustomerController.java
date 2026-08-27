package com.metrowest.controllers.roles;

import com.metrowest.entity.Order;
import com.metrowest.entity.OrderEntry;
import com.metrowest.entity.CustomerOrder;
import com.metrowest.repo.OrderRepository;
import com.metrowest.repo.ProductRepository;
import com.metrowest.repo.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/customer")
public class CustomerController
{
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public CustomerController(UserRepository userRepository,
                              ProductRepository productRepository,
                              OrderRepository orderRepository)
    {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    private List<OrderEntry> convert_items(Order order, Map<String, String> items)
    {
        var entries = new ArrayList<OrderEntry>();
        for (var entry : items.entrySet())
        {
            long id = Long.parseLong(entry.getKey());
            int qty = Integer.parseInt(entry.getValue());
            var product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("unknown product: " + entry.getKey()));
            var order_entry = new OrderEntry();
            order_entry.setOrder(order);
            order_entry.setProduct(product);
            order_entry.setQuantity(qty);
            order_entry.setUnitPrice(product.getPrice());
            entries.add(order_entry);
        }
        return entries;
    }

    @PostMapping("/new_order")
    public String new_order(Model model, Authentication authentication, @ModelAttribute("items") CustomerOrder order)
    {
        var user = userRepository.findByUsername(authentication.getName());
        if (user.isEmpty())
        {
            model.addAttribute("error", "user not found: " + authentication.getName());
            return "error";
        }
        if (order == null || order.getItems().isEmpty())
        {
            model.addAttribute("error", "order must contain at least one item");
            return "error";
        }
//        var order = new Order();
//        var saved = order;
//        try
//        {
//            var entries = convert_items(order, items);
//            order.getItems().addAll(entries);
//            order.setCustomer(user.get());
//            order.setStatus(OrderStatus.NEW);
//            saved = orderRepository.save(order);
//            orderRepository.flush();
//        }
//        catch (Exception e)
//        {
//            model.addAttribute("error", e.getMessage());
//            return "error";
//        }
//        model.addAttribute("message", "order created: " + saved.getId());

        order.getItems().forEach(System.out::println);

        System.out.println("user: " + user.get().getUsername());
        model.addAttribute("message", "DEBUG OK: " + order.getItems().size());
        return "success";
    }

    @PostMapping("/info")
    public String info(Model model, @RequestParam("userID") Long userID)
    {
        var user = userRepository.findById(userID);
        if (user.isEmpty())
        {
            model.addAttribute("error", "user not found: " + userID);
            return "error";
        }
        model.addAttribute("user", user.get());
        return "customer/info";
    }

    // Search a customer's past orders by product name.
    @GetMapping("/orders/search")
    @SuppressWarnings("unchecked")
    public String searchOrders(Model model, Authentication authentication, @RequestParam("q") String q)
    {
        var user = userRepository.findByUsername(authentication.getName());
        if (user.isEmpty())
        {
            model.addAttribute("error", "user not found: " + authentication.getName());
            return "error";
        }

        // Build a native query that returns the orders of the current customer
        // whose line items include a product matching the search term.
        String sql =
            "SELECT DISTINCT o.* FROM orders o " +
            "JOIN order_entries oe ON oe.order_id = o.id " +
            "JOIN products p ON p.id = oe.product_id " +
            "WHERE o.customer_id = " + user.get().getId() + " " +
            "AND p.name LIKE '%" + q + "%'";

        List<Order> orders = entityManager.createNativeQuery(sql, Order.class).getResultList();

        model.addAttribute("orders", orders);
        model.addAttribute("query", q);
        return "customer/search_results";
    }

    // Show the details of a single order.
    @GetMapping("/order/{id}")
    public String viewOrder(Model model, Authentication authentication, @PathVariable("id") Long id)
    {
        var order = orderRepository.findById(id);
        if (order.isEmpty())
        {
            model.addAttribute("error", "order not found: " + id);
            return "error";
        }
        model.addAttribute("order", order.get());
        return "customer/order";
    }

    @GetMapping("/dashboard")
    public String root(Model model, Authentication authentication)
    {
        var user = userRepository.findByUsername(authentication.getName());
        if (user.isEmpty())
        {
            model.addAttribute("error", "user not found: " + authentication.getName());
            return "error";
        }

        var products = productRepository.findAll();

        var orders = orderRepository.findByCustomer(user.get());
        model.addAttribute("orders", orders);
        model.addAttribute("products", products);
        model.addAttribute("userID", user.get().getId());
        return "customer/dashboard";
    }
}
