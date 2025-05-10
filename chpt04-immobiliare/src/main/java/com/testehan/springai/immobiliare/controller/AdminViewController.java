package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.Lead;
import com.testehan.springai.immobiliare.repository.LeadRepository;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/a")
public class AdminViewController {

    @Value("${app.url}")
    private String appUrl;

    private final LeadRepository leadRepository;
    private final ConversationSession conversationSession;

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public AdminViewController(LeadRepository leadRepository, ConversationSession conversationSession, MessageSource messageSource, LocaleUtils localeUtils) {
        this.leadRepository = leadRepository;
        this.conversationSession = conversationSession;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @GetMapping
    public String getMainAdminPage(Model model) {
        var user = conversationSession.getImmobiliareUser().get();
        if (user.isAdmin()) {
            return "admin-main";
        } else {
            model.addAttribute("errorMessage", messageSource.getMessage("error.notfound",null, localeUtils.getCurrentLocale()));
            return "error-404";
        }
    }


    @GetMapping("/leads")
    public String getLeads(Model model,
                                     @RequestParam(defaultValue = "createdAt") String sortBy,
                                     @RequestParam(defaultValue = "desc") String direction,
                                     @RequestParam(defaultValue = "0") int page) {

        var user = conversationSession.getImmobiliareUser().get();

        if (user.isAdmin()) {
            Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(Sort.Order.desc(sortBy)) : Sort.by(Sort.Order.asc(sortBy));

            // PageRequest with page number and sorting
            PageRequest pageRequest = PageRequest.of(page, 50, sort);
            Page<Lead> attempts = leadRepository.findAll(pageRequest);

            model.addAttribute("appUrl", appUrl);
            model.addAttribute("newLead", new Lead());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", attempts.getTotalPages());
            model.addAttribute("leads", attempts.getContent());
            model.addAttribute("currentSort", sortBy);
            model.addAttribute("currentDirection", direction);

            return "admin-leads";
        } else {
            model.addAttribute("errorMessage", messageSource.getMessage("error.notfound",null, localeUtils.getCurrentLocale()));
            return "error-404";
        }
    }


}
