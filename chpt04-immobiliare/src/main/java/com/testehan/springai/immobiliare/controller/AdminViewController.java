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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
            Page<Lead> leads = leadRepository.findAll(pageRequest);

            model.addAttribute("appUrl", appUrl);
            model.addAttribute("newLead", new Lead());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", leads.getTotalPages());
            model.addAttribute("leads", leads.getContent());
            model.addAttribute("randomInitialMessages", getLeadInitialMessages(leads.getSize()));
            model.addAttribute("currentSort", sortBy);
            model.addAttribute("currentDirection", direction);

            return "admin-leads";
        } else {
            model.addAttribute("errorMessage", messageSource.getMessage("error.notfound",null, localeUtils.getCurrentLocale()));
            return "error-404";
        }
    }

    private List<String> getLeadInitialMessages(int numberOfLeads){
        List<String> messages = new ArrayList<>(List.of(
                "Buna! Am vazut anuntul tau cu proprietatea si voiam sa te intrebam daca ai fi de acord sa-l adaugam (GRATUIT) si pe o platforma noua de cautare de chirii pe care o dezvoltam. \n Se numeste www.casamia.ai si vrem sa ajutam oamenii sa gaseasca chirii mai usor. \n E complet gratuit, doar mai multa vizibilitate pentru apartamentul tau. \n Esti de acord cu asta?",
                "Bună! Am văzut anunțul tău cu apartamentul și voiam să te întreb dacă ai fi ok să îl adăugăm și pe o platformă nouă de închirieri — gratuit.\n" +
                        "Se numește www.casamia.ai și vrem să ajutăm lumea să găsească chirii mai ușor.\n" +
                        "Doar mai multă vizibilitate pentru tine, fără niciun cost. Ce zici?",
                "Salut! Îți scriem legat de anunțul cu apartamentul. Noi lucrăm la o platformă nouă de închirieri, casamia.ai, și am vrea să-l includem acolo, gratuit.\n" +
                        "Scopul e să ajutăm chiriașii să găsească mai ușor oferte.\n" +
                        "Poți avea mai multă vizibilitate fără niciun cost. Te-ar interesa?",
                "Bună! Am descoperit anunțul tău și voiam să te întreb dacă ai fi deschis(ă) să îl publicăm și pe www.casamia.ai, o platformă nouă pentru închirieri.\n" +
                        "E complet gratuit – ne dorim doar să creștem vizibilitatea proprietăților disponibile.\n" +
                        "Te-ar interesa?",
                "Salut! Am dat de anunțul tău și ne-a plăcut cum e prezentat. Lucrăm la un site nou de chirii – casamia.ai – și oferim listări gratuite.\n" +
                        "Ai fi de acord să adăugăm și apartamentul tău? E doar pentru expunere în plus. Mulțumim!",
                "Bună ziua! Îți scriem legat de apartamentul pe care îl ai listat. Noi construim un nou site de chirii (casamia.ai) și oferim listări gratuite proprietarilor.\n" +
                        "Ne-ar plăcea să includem și oferta ta – complet gratuit, fără obligații. Ce părere ai?"
        ));

        while (messages.size() < numberOfLeads) {
            messages.add(messages.get(new Random().nextInt(messages.size())));
        }

        List<String> encodedMessages = messages.stream()
                .map(msg -> URLEncoder.encode(msg, StandardCharsets.UTF_8))
                .collect(Collectors.toList());

        return encodedMessages;
    }


}
