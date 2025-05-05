package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.ContactAttempt;
import com.testehan.springai.immobiliare.repository.ContactAttemptRepository;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import org.springframework.context.MessageSource;
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
    private final ConversationSession conversationSession;

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public AdminViewController(ContactAttemptRepository contactAttemptRepository, ConversationSession conversationSession, MessageSource messageSource, LocaleUtils localeUtils) {
        this.contactAttemptRepository = contactAttemptRepository;
        this.conversationSession = conversationSession;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @GetMapping("/")
    public String getMainAdminPage(Model model) {
        var user = conversationSession.getImmobiliareUser().get();
        if (user.isAdmin()) {
            return "admin-main";
        } else {
            model.addAttribute("errorMessage", messageSource.getMessage("error.notfound",null, localeUtils.getCurrentLocale()));
            return "error-404";
        }
    }


    @GetMapping("/contact-attempts")
    public String getContactAttempts(Model model,
                                     @RequestParam(defaultValue = "createdAt") String sortBy,
                                     @RequestParam(defaultValue = "desc") String direction,
                                     @RequestParam(defaultValue = "0") int page) {

        var user = conversationSession.getImmobiliareUser().get();

        if (user.isAdmin()) {
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
        } else {
            model.addAttribute("errorMessage", messageSource.getMessage("error.notfound",null, localeUtils.getCurrentLocale()));
            return "error-404";
        }
    }

    @PostMapping("/contact-attempts")
    public String createContactAttempt(@ModelAttribute ContactAttempt contactAttempt) {
        var user = conversationSession.getImmobiliareUser().get();
        if (user.isAdmin()) {
            contactAttemptRepository.save(contactAttempt);
        }

        return "redirect:/admin/contact-attempts";
    }
}
