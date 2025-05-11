package com.testehan.springai.immobiliare.security;

import com.testehan.springai.immobiliare.model.auth.AuthenticationType;
import com.testehan.springai.immobiliare.model.auth.DeletedUser;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.repository.DeletedUserRepository;
import com.testehan.springai.immobiliare.repository.ImmobiliareUserRepository;
import com.testehan.springai.immobiliare.service.EmailService;
import com.testehan.springai.immobiliare.util.FormattingUtil;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final EmailService emailService;
    private final ImmobiliareUserRepository immobiliareUserRepository;
    private final DeletedUserRepository deletedUserRepository;
    private final MongoRememberMeTokenRepository rememberMeTokenRepository;

    private final LocaleUtils localeUtils;
    private final FormattingUtil formattingUtil;

    public UserService(HttpServletRequest request, HttpServletResponse response,
                       EmailService emailService, ImmobiliareUserRepository immobiliareUserRepository, DeletedUserRepository deletedUserRepository, MongoRememberMeTokenRepository rememberMeTokenRepository, LocaleUtils localeUtils, FormattingUtil formattingUtil) {
        this.request = request;
        this.response = response;
        this.emailService = emailService;
        this.immobiliareUserRepository = immobiliareUserRepository;
        this.deletedUserRepository = deletedUserRepository;
        this.rememberMeTokenRepository = rememberMeTokenRepository;
        this.localeUtils = localeUtils;
        this.formattingUtil = formattingUtil;
    }

    public boolean isEmailUnique(String email) {
        var user = immobiliareUserRepository.findUserByEmail(email);
        return user == null;
    }

    public void updateAuthenticationType(ImmobiliareUser user, AuthenticationType authenticationType){
        if (!user.getAuthenticationType().equals(authenticationType)) {
            immobiliareUserRepository.updateAuthenticationType(user.getId(), authenticationType);
        }
    }

    public ImmobiliareUser updateUser(ImmobiliareUser user){
        return immobiliareUserRepository.update(user);
    }

    @Transactional
    public void deleteUser(ImmobiliareUser user){
        LocalDateTime now = LocalDateTime.now();
        String formattedDateCustom = formattingUtil.getFormattedDateCustom(now);

        deletedUserRepository.save(new DeletedUser(user.getEmail(), user.getSearchesAvailable(), formattedDateCustom));
        rememberMeTokenRepository.removeUserTokens(user.getEmail());
        immobiliareUserRepository.deleteById(user.getId());
        logoutUser();
    }

    public Optional<ImmobiliareUser> getImmobiliareUserByEmail(String email){
        return immobiliareUserRepository.findUserByEmail(email);
    }

    public Optional<ImmobiliareUser> getImmobiliareUserByInviteUuid(String inviteUuid){
        return immobiliareUserRepository.findUserByInviteUuid(inviteUuid);
    }

    public List<String> getAdminUsersEmail(){
        var adminUsers = immobiliareUserRepository.findAdminUsers();
        return adminUsers.stream().map(ImmobiliareUser::getEmail).collect(Collectors.toList());
    }

    public void resetSearchesAvailable(){
        immobiliareUserRepository.resetSearchesAvailable();
    }

    public Optional<DeletedUser> findDeletedUserByEmail(String email){
        return deletedUserRepository.findById(email);
    }

    public void deleteDeletedUserByEmail(String email){
        deletedUserRepository.deleteById(email);
    }

    public void deleteDeletedUsers(LocalDateTime date) {
        deletedUserRepository.deleteDeletedUsers(date);
    }

    public void addNewCustomerAfterOAuth2Login(String name, String email, AuthenticationType authenticationType) {
        var user = new ImmobiliareUser();

        user.setName(name);
        user.setEmail(email);
        user.setAuthenticationType(authenticationType);
        user.setPassword("");
        user.setIsAdmin("false");
        user.setInviteUuid(UUID.randomUUID().toString());

        immobiliareUserRepository.save(user);
        emailService.sendWelcomeEmail(email, name, localeUtils.getCurrentLocale());
    }

    private void logoutUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
    }
}
