package com.testehan.springai.immobiliare;

import org.springframework.ai.chat.messages.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class State {
    // TODO Yes, this is not how it would work in a real app...but this is good enough for a prototype
    // purpose of this map is to hold a list of assistant (LLM) and user messages for each session;
    // in a serious app one would use spring session and either Redis or the DB to save the conversation state i think
    // see my SpringSessionRedisExperiments repo for ex
    public static Map<String, List<Message>> conversations = new HashMap();
}
