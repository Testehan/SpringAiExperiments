package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.auth.Customer;

public interface CustomerRepository  {

    Customer findByEmail(String email);
}
