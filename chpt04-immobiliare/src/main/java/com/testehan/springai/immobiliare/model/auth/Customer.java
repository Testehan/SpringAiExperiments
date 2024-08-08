package com.testehan.springai.immobiliare.model.auth;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

public record Customer (

    @BsonProperty("_id")
    ObjectId _id,
    String email,

    String password,

    AuthenticationType authenticationType){


}
