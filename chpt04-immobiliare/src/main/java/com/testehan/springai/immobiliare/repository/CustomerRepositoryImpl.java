package com.testehan.springai.immobiliare.repository;

import com.mongodb.client.MongoDatabase;
import com.testehan.springai.immobiliare.model.auth.Customer;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import java.util.NoSuchElementException;

@Repository
public class CustomerRepositoryImpl implements CustomerRepository{

    private final MongoDatabase mongoDatabase;

    public CustomerRepositoryImpl(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    @Override
    public Customer findByEmail(String email) {
        var mongoCollection = mongoDatabase.getCollection("customers", Customer.class);
        ObjectId objectId = new ObjectId(email);
        var customer = mongoCollection.find(new Document("email", objectId)).first();

        if (customer != null) {
            return customer;
        } else {
            throw new NoSuchElementException("Customer with email " + email + " not found");
        }
    }
}
