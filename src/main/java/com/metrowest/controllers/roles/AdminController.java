package com.metrowest.controllers.roles;

import com.metrowest.entity.Product;
import com.metrowest.entity.ProductType;
import com.metrowest.entity.Role;
import com.metrowest.entity.UserEntry;
import com.metrowest.repo.ProductRepository;
import com.metrowest.repo.UserRepository;
import com.metrowest.services.diagnostics.DiagnosticsService;
import com.metrowest.util.TextValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
public class AdminController
{
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final DiagnosticsService diagnosticsService;

    public AdminController(UserRepository userRepository,
                           ProductRepository productRepository,
                           PasswordEncoder passwordEncoder,
                           DiagnosticsService diagnosticsService)
    {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.diagnosticsService = diagnosticsService;
    }

    private BigDecimal convert_decimal(String decimal_string)
    {
        try { return BigDecimal.valueOf(Double.parseDouble(decimal_string)); }
        catch (NumberFormatException nme) { return null; }
    }

    @GetMapping("/dashboard")
    public String root(Model model)
    {
        var users = userRepository.findAll();
        var products = productRepository.findAll();
        model.addAttribute("users", users);
        model.addAttribute("products", products);
        return "admin/dashboard";
    }

    // Network diagnostics: run a connectivity check against a customer site host.
    @GetMapping("/diagnostics")
    public String diagnostics(Model model, @RequestParam(value = "host", required = false) String host)
    {
        if (host != null && !host.isBlank())
        {
            String result = diagnosticsService.pingHost(host);
            model.addAttribute("host", host);
            model.addAttribute("result", result);
        }
        return "admin/diagnostics";
    }

    @PostMapping("/new_user")
    public String new_user(Model model,
                           @RequestParam("email") String email,
                           @RequestParam("username") String username,
                           @RequestParam("password") String password,
                           @RequestParam("role") String role_string)
    {
        var role = Role.from_string(role_string);
        if (role == null)
        {
            model.addAttribute("error", "role: " + role_string + " is invalid");
            return "admin/failure";
        }
        if (userRepository.existsByUsername(username))
        {
            model.addAttribute("error", "user: " + username + " already exists");
            return "admin/failure";
        }
        if (userRepository.existsByEmail(email))
        {
            model.addAttribute("error", "email: " + email + " already in use by another user");
            return "admin/failure";
        }
        if (!EmailValidator.getInstance().isValid(email))
        {
            model.addAttribute("error", "email: " + email + " is invalid");
            return "admin/failure";
        }
        if (password == null || password.isEmpty())
        {
            model.addAttribute("error", "password is null or empty");
            return "admin/failure";
        }
        if (password.length() < 8)
        {
            model.addAttribute("error", "password is too short, must be at least 8 characters");
            return "admin/failure";
        }

        var user = new UserEntry();
        user.setEmail(email);
        user.setUsername(username);
        user.setRole(role);
        user.setPassword_hash(passwordEncoder.encode(password));
        var saved = userRepository.save(user);
        userRepository.flush();

        model.addAttribute("user", "id="+ saved.getId() + " name=" + username);
        return "admin/success";
    }

    @PostMapping("/new_product")
    public String new_product(Model model,
                              @RequestParam("name") String name,
                              @RequestParam("type") String type_string,
                              @RequestParam("price") String price_string)
    {
        var type = ProductType.from_string(type_string);
        var price = convert_decimal(price_string);
        if (type == null)
        {
            model.addAttribute("error", "product type: " + type_string + " is invalid");
            return "admin/failure";
        }
        if (price == null)
        {
            model.addAttribute("error", "product price: " + price_string + " is invalid");
            return "admin/failure";
        }
        if (!TextValidator.alphanumeric_w_spaces(name))
        {
            model.addAttribute("error", "product name: " + name + " is invalid");
            return "admin/failure";
        }
        if (productRepository.existsByName(name))
        {
            model.addAttribute("error", "product: " + name + " already exists");
            return "admin/failure";
        }

        var product = new Product();
        product.setName(name);
        product.setType(type);
        product.setPrice(price);
        var saved = productRepository.save(product);
        model.addAttribute("message", "product id: "+ saved.getId() + " with name: " + name + " was created");
        return "admin/success";
    }
}
