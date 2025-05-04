package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.repository.ContactAttemptRepository;
import com.testehan.springai.immobiliare.model.ContactAttempt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
    public String getContactAttempts(Model model) {
        model.addAttribute("newContactAttempt", new ContactAttempt());
        model.addAttribute("contactAttempts", contactAttemptRepository.findAll());
        return "admin-contact-attempts";
    }

    @PostMapping("/contact-attempts")
    public String createContactAttempt(@ModelAttribute ContactAttempt contactAttempt) {
        contactAttemptRepository.save(contactAttempt);
        return "redirect:/admin/contact-attempts";
    }
}
