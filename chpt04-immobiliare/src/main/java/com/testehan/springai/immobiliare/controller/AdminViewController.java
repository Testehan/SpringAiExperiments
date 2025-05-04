package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.model.ContactAttempt;
import com.testehan.springai.immobiliare.repository.ContactAttemptRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminViewController {

    private final ContactAttemptRepository contactAttemptRepository;

    public AdminViewController(ContactAttemptRepository contactAttemptRepository) {
        this.contactAttemptRepository = contactAttemptRepository;
    }

    @GetMapping("/")
    public String getMainAdminPage() {
        return "admin-main";
    }


    @GetMapping("/contact-attempts")
    public String getContactAttempts(Model model,
                                     @RequestParam(defaultValue = "createdAt") String sortBy,
                                     @RequestParam(defaultValue = "desc") String direction,
                                     @RequestParam(defaultValue = "0") int page) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(Sort.Order.desc(sortBy)) : Sort.by(Sort.Order.asc(sortBy));

        // PageRequest with page number and sorting
        PageRequest pageRequest = PageRequest.of(page, 10, sort);
        Page<ContactAttempt> attempts = contactAttemptRepository.findAll(pageRequest);

        model.addAttribute("newContactAttempt", new ContactAttempt());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", attempts.getTotalPages());
        model.addAttribute("contactAttempts", attempts.getContent());
        model.addAttribute("currentSort", sortBy);
        model.addAttribute("currentDirection", direction);

        return "admin-contact-attempts";
    }

    @PostMapping("/contact-attempts")
    public String createContactAttempt(@ModelAttribute ContactAttempt contactAttempt) {
        contactAttemptRepository.save(contactAttempt);
        return "redirect:/admin/contact-attempts";
    }
}
